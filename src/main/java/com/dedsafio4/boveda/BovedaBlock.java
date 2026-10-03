package com.dedsafio4.boveda;

import com.dedsafio4.hermandad.Hermandades;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * La Bóveda: puerta de 2 de ancho por 3 de alto. Con click derecho (si eres de una Hermandad) la rueda
 * gira, los cerrojos se corren, la puerta se abre hacia afuera y aparece la pantalla de la Bóveda.
 * Cuando nadie la está usando, se vuelve a cerrar sola.
 *
 * Son 6 bloques; PARTE = fila * 2 + columna (fila 0 abajo, columna 0 a la izquierda mirando el frente,
 * del lado de las bisagras). El de abajo a la izquierda (parte 0) es el principal: tiene la entidad
 * que dibuja toda la puerta.
 */
public class BovedaBlock extends BaseEntityBlock {
	public static final MapCodec<BovedaBlock> CODEC = simpleCodec(BovedaBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 5);
	public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");
	public static final int PRINCIPAL = 0;

	public BovedaBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, PRINCIPAL).setValue(ABIERTA, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PARTE, ABIERTA);
	}

	// --- Las 6 partes ---

	/** La "derecha" de quien mira la puerta de frente. */
	static Direction derecha(Direction frente) {
		return frente.getCounterClockWise();
	}

	static BlockPos posParte(BlockPos principal, Direction frente, int parte) {
		return principal.relative(derecha(frente), parte % 2).above(parte / 2);
	}

	public static BlockPos principal(BlockPos pos, BlockState estado) {
		int parte = estado.getValue(PARTE);
		return pos.relative(derecha(estado.getValue(FACING)), -(parte % 2)).below(parte / 2);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		Direction frente = contexto.getHorizontalDirection().getOpposite();
		BlockPos pos = contexto.getClickedPos();
		Level level = contexto.getLevel();
		for (int parte = 0; parte < 6; parte++) {
			BlockPos p = posParte(pos, frente, parte);
			if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(contexto)) return null;
		}
		return defaultBlockState().setValue(FACING, frente);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		for (int parte = 1; parte < 6; parte++) {
			level.setBlock(posParte(pos, estado.getValue(FACING), parte), estado.setValue(PARTE, parte), 3);
		}
	}

	/** Al romperla en supervivencia (con pico) devuelve la Bóveda, rompas la parte que rompas. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
		if (!level.isClientSide && !jugador.isCreative() && jugador.hasCorrectToolForDrops(estado)) {
			popResource(level, pos, new ItemStack(ModBoveda.BOVEDA_ITEM));
		}
		return super.playerWillDestroy(level, pos, estado, jugador);
	}

	/** Si se saca una parte, se saca la puerta entera. */
	@Override
	protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
		if (!nuevo.is(this)) {
			BlockPos principal = principal(pos, estado);
			Direction frente = estado.getValue(FACING);
			for (int parte = 0; parte < 6; parte++) {
				BlockPos p = posParte(principal, frente, parte);
				if (p.equals(pos)) continue;
				BlockState otro = level.getBlockState(p);
				if (otro.is(this) && otro.getValue(PARTE) == parte && otro.getValue(FACING) == frente) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		super.onRemove(estado, level, pos, nuevo, movido);
	}

	// --- Abrir ---

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(jugador instanceof ServerPlayer servidor)) return InteractionResult.CONSUME;
		BlockPos principal = principal(pos, estado);
		if (Hermandades.data(servidor).deJugador(servidor.getUUID()).isEmpty()) {
			servidor.displayClientMessage(Component.literal("Solo los miembros de una Hermandad pueden usar la Bóveda.")
					.withColor(0xFF6B6B), true);
			level.playSound(null, principal.above(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.5f);
			return InteractionResult.CONSUME;
		}
		if (level.getBlockEntity(principal) instanceof BovedaBlockEntity boveda) boveda.pedirApertura(servidor);
		return InteractionResult.CONSUME;
	}

	/** Abre o cierra las 6 partes juntas. */
	static void setAbierta(Level level, BlockPos principal, BlockState estado, boolean abierta) {
		Direction frente = estado.getValue(FACING);
		Block bloque = estado.getBlock();
		for (int parte = 0; parte < 6; parte++) {
			BlockPos p = posParte(principal, frente, parte);
			BlockState otro = level.getBlockState(p);
			if (otro.is(bloque) && otro.getValue(ABIERTA) != abierta) level.setBlock(p, otro.setValue(ABIERTA, abierta), 3);
		}
	}

	// --- Forma: cerrada es una losa de 6 píxeles al frente del bloque; abierta se puede pasar ---

	/** Caja en coordenadas de la puerta: x a lo ancho (hacia la derecha), z de atrás (0) al frente (16). */
	private static VoxelShape caja(Direction frente, double x0, double y0, double z0, double x1, double y1, double z1) {
		Direction der = derecha(frente);
		boolean derPos = der.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		boolean frentePos = frente.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		double a = derPos ? x0 : 16 - x1, b = derPos ? x1 : 16 - x0;
		double c = frentePos ? z0 : 16 - z1, d = frentePos ? z1 : 16 - z0;
		return der.getAxis() == Direction.Axis.X ? Block.box(a, y0, c, b, y1, d) : Block.box(c, y0, a, d, y1, b);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
		return caja(estado.getValue(FACING), 0, 0, 10, 16, 16, 16);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
		return estado.getValue(ABIERTA) ? Shapes.empty() : getShape(estado, nivel, pos, contexto);
	}

	// --- Dibujo y entidad: las tiene solo el bloque principal ---

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == PRINCIPAL ? new BovedaBlockEntity(pos, estado) : null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
		return level.isClientSide ? null : createTickerHelper(tipo, ModBoveda.ENTIDAD, BovedaBlockEntity::tickServidor);
	}
}
