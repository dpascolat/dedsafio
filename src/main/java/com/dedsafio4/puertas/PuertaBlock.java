package com.dedsafio4.puertas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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

import java.util.function.Supplier;

/**
 * Puerta que se abre con una llave: al usar la llave sobre la puerta cerrada, la llave se gasta y la puerta se abre
 * (la mitad de arriba sube y la de abajo baja, dentro del marco) y queda abierta. No se puede romper en supervivencia.
 *
 * Es de 5x5 bloques (GRANDE; las que se pusieron antes, sin eso, siguen siendo de 3x3). PARTE = fila * lado + columna
 * (fila 0 la del bloque principal, columna 0 a la izquierda mirando el frente). El del medio de la fila 0 es el
 * principal: tiene la entidad que dibuja toda la puerta. BOCA_ABAJO: puesta desde un techo (o desde la mitad de arriba
 * de una pared) cuelga para abajo desde el principal y se dibuja dada vuelta.
 */
public class PuertaBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 24);
	public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");
	public static final BooleanProperty GRANDE = BooleanProperty.create("grande");
	public static final BooleanProperty BOCA_ABAJO = BooleanProperty.create("boca_abajo");

	private final Supplier<Item> llave;
	/** Nombre de la llave para el aviso ("Necesitas la ..."). */
	private final String nombreLlave;

	public PuertaBlock(Properties propiedades, Supplier<Item> llave, String nombreLlave) {
		super(propiedades);
		this.llave = llave;
		this.nombreLlave = nombreLlave;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, 1).setValue(ABIERTA, false)
				.setValue(GRANDE, false).setValue(BOCA_ABAJO, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(p -> new PuertaBlock(p, llave, nombreLlave));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PARTE, ABIERTA, GRANDE, BOCA_ABAJO);
	}

	// --- Las partes (9 o 25) ---

	/** Cuántos bloques de lado tiene: 5, o 3 las de antes. */
	public static int lado(BlockState estado) {
		return estado.getValue(GRANDE) ? 5 : 3;
	}

	/** La parte del bloque principal: el del medio de la fila 0. */
	public static int principalParte(BlockState estado) {
		return lado(estado) / 2;
	}

	/** Hacia dónde está la "derecha" de la puerta (mirandola de frente es la izquierda de quien mira). */
	private static Direction derecha(Direction frente) {
		return frente.getCounterClockWise();
	}

	private static BlockPos posParte(BlockPos principal, BlockState estado, int parte) {
		int n = lado(estado), col = parte % n, fila = parte / n;
		BlockPos p = principal.relative(derecha(estado.getValue(FACING)), col - n / 2);
		return estado.getValue(BOCA_ABAJO) ? p.below(fila) : p.above(fila);
	}

	public static BlockPos principal(BlockPos pos, BlockState estado) {
		int n = lado(estado), parte = estado.getValue(PARTE), col = parte % n, fila = parte / n;
		BlockPos p = pos.relative(derecha(estado.getValue(FACING)), n / 2 - col);
		return estado.getValue(BOCA_ABAJO) ? p.above(fila) : p.below(fila);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		Direction frente = contexto.getHorizontalDirection().getOpposite();
		BlockPos pos = contexto.getClickedPos();
		Level level = contexto.getLevel();
		// Boca abajo: puesta en un techo, o en la mitad de arriba de una pared.
		Direction cara = contexto.getClickedFace();
		boolean bocaAbajo = cara == Direction.DOWN
				|| cara != Direction.UP && contexto.getClickLocation().y - pos.getY() > 0.5;
		BlockState estado = defaultBlockState().setValue(FACING, frente).setValue(GRANDE, true).setValue(BOCA_ABAJO, bocaAbajo);
		estado = estado.setValue(PARTE, principalParte(estado));
		int n = lado(estado);
		for (int parte = 0; parte < n * n; parte++) {
			BlockPos p = posParte(pos, estado, parte);
			if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(contexto)) return null;
		}
		return estado;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		int n = lado(estado);
		for (int parte = 0; parte < n * n; parte++) {
			if (parte == principalParte(estado)) continue;
			level.setBlock(posParte(pos, estado, parte), estado.setValue(PARTE, parte), 3);
		}
	}

	/** Si se saca una parte, se saca la puerta entera. */
	@Override
	protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
		if (!nuevo.is(this)) {
			BlockPos principal = principal(pos, estado);
			int n = lado(estado);
			for (int parte = 0; parte < n * n; parte++) {
				BlockPos p = posParte(principal, estado, parte);
				if (p.equals(pos)) continue;
				BlockState otro = level.getBlockState(p);
				if (otro.is(this) && otro.getValue(PARTE) == parte && otro.getValue(FACING) == estado.getValue(FACING)
						&& otro.getValue(GRANDE) == estado.getValue(GRANDE) && otro.getValue(BOCA_ABAJO) == estado.getValue(BOCA_ABAJO)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		super.onRemove(estado, level, pos, nuevo, movido);
	}

	// --- Abrir con la llave ---

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pila, BlockState estado, Level level, BlockPos pos, Player jugador,
											  InteractionHand mano, BlockHitResult golpe) {
		if (estado.getValue(ABIERTA) || !pila.is(llave.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			pila.consume(1, jugador);
			abrir(level, pos, estado);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (estado.getValue(ABIERTA)) return InteractionResult.PASS;
		if (!level.isClientSide) {
			jugador.displayClientMessage(Component.literal("Está cerrada. Necesitas la " + nombreLlave + ".").withColor(0xFF6B6B), true);
			level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 0.6f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private void abrir(Level level, BlockPos pos, BlockState estado) {
		BlockPos principal = principal(pos, estado);
		int n = lado(estado);
		for (int parte = 0; parte < n * n; parte++) {
			BlockPos p = posParte(principal, estado, parte);
			BlockState otro = level.getBlockState(p);
			if (otro.is(this)) level.setBlock(p, otro.setValue(ABIERTA, true), 3);
		}
		BlockPos centro = estado.getValue(BOCA_ABAJO) ? principal.below(n / 2) : principal.above(n / 2);
		level.playSound(null, centro, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.6f);
		level.playSound(null, centro, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 0.7f);
	}

	// --- Forma ---

	/** Una caja en coordenadas de la puerta (lx a lo ancho, ly alto, en píxeles), con el grosor del marco. */
	private static VoxelShape caja(Direction frente, double lx0, double ly0, double lx1, double ly1) {
		double z0 = 6.25, z1 = 9.75;
		Direction der = derecha(frente);
		double a = der.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lx0 : 16 - lx1;
		double b = der.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lx1 : 16 - lx0;
		return der.getAxis() == Direction.Axis.X ? Block.box(a, ly0, z0, b, ly1, z1) : Block.box(z0, ly0, a, z1, ly1, b);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
		Direction frente = estado.getValue(FACING);
		if (!estado.getValue(ABIERTA)) return caja(frente, 0, 0, 16, 16);
		// Abierta: solo queda el marco de los bordes.
		int n = lado(estado), parte = estado.getValue(PARTE), col = parte % n, fila = parte / n;
		boolean abajo = estado.getValue(BOCA_ABAJO) ? fila == n - 1 : fila == 0;
		boolean arriba = estado.getValue(BOCA_ABAJO) ? fila == 0 : fila == n - 1;
		VoxelShape forma = Shapes.empty();
		if (col == 0) forma = Shapes.or(forma, caja(frente, 0, 0, 2, 16));
		if (col == n - 1) forma = Shapes.or(forma, caja(frente, 14, 0, 16, 16));
		if (abajo) forma = Shapes.or(forma, caja(frente, 0, 0, 16, 2));
		if (arriba) forma = Shapes.or(forma, caja(frente, 0, 14, 16, 16));
		return forma;
	}

	// --- Dibujo: lo hace la entidad del bloque principal ---

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == principalParte(estado) ? new PuertaBlockEntity(pos, estado) : null;
	}
}
