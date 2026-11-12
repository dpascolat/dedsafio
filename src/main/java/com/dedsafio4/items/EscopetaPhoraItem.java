package com.dedsafio4.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las armas Phora: Escopeta, Fusil de Ráfagas y Francotirador (los números de cada una en Arma; el Fusil y el
 * Francotirador usan el modelo de la escopeta, más largo y con otros colores).
 * Escopeta Phora (el modelo "escopeta_minecraft" de Patricio): clic izquierdo dispara 6 perdigones en abanico
 * (3 de daño cada uno, 4,5 si dan en la cabeza; no rompe bloques). Un disparo cada 0,4 segundos y 8 en el cargador.
 * Clic derecho (mantenido) apunta: la escopeta va al medio y se mira por arriba de ella.
 * Se recarga con Proyectil Phora (uno por bala) en 2,5 segundos: sola al quedarse sin balas, con la R o con Shift + clic derecho.
 * Las balas y el fin de la recarga se guardan en el ítem (custom_data).
 */
public class EscopetaPhoraItem extends Item {
	/**
	 * Los números de cada arma Phora (las tres comparten todo lo demás: Proyectil Phora, recarga, apuntar...).
	 * entreDisparos y recarga van en ticks; zoom es cuánto se acerca la vista al apuntar (0,18 = 18%).
	 */
	public record Arma(float danio, float danioCabeza, int cargador, int perdigones, int entreDisparos, int recarga,
					   double alcance, double dispersion, float zoom, String frecuencia, String tiempoRecarga) {}

	/** Escopeta: 6 perdigones en abanico, 0,4 s entre disparos, 2,5 s de recarga, 8 balas. */
	public static final Arma ESCOPETA = new Arma(3f, 4.5f, 8, 6, 8, 50, 24, 0.035, 0.18f, "0.40", "2.50");
	/** Fusil de Ráfagas: una bala por clic, 0,10 s entre disparos (2 ticks), 3 s de recarga, 30 balas, poco retroceso. */
	public static final Arma FUSIL = new Arma(4f, 6f, 30, 1, 2, 60, 64, 0.004, 0.3f, "0.10", "3.00");
	/** Francotirador: una bala que pega fuerte, 2 s entre disparos, 2,5 s de recarga, 5 balas, mucho zoom. */
	public static final Arma FRANCOTIRADOR = new Arma(100f, 200f, 5, 1, 40, 50, 160, 0, 0.75f, "2.00", "2.50");

	/** Los de la escopeta (los usaba el resto del mod). */
	public static final int CARGADOR = 8, RECARGA = 50;

	public final Arma arma;
	private static final String BALAS = "dedsafio4_balas", FIN_RECARGA = "dedsafio4_fin_recarga";
	private static final int BLANCO = 0xFFFFFF, CELESTE = 0x55FFFF, NARANJA = 0xFF5A14, LILA = 0xD58CFF;

	/**
	 * Los disparos (cada bala o perdigón: de dónde sale y dónde pega), para que el cliente dibuje el rayo naranja que vuela
	 * (como el de la nave, 6 veces más chico).
	 */
	public record BalasPayload(List<org.joml.Vector3f> desde, List<org.joml.Vector3f> hasta) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<BalasPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "balas_phora"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BalasPayload> CODEC =
				net.minecraft.network.codec.StreamCodec.composite(
						net.minecraft.network.codec.ByteBufCodecs.VECTOR3F.apply(net.minecraft.network.codec.ByteBufCodecs.list()), BalasPayload::desde,
						net.minecraft.network.codec.ByteBufCodecs.VECTOR3F.apply(net.minecraft.network.codec.ByteBufCodecs.list()), BalasPayload::hasta,
						BalasPayload::new);

		@Override
		public Type<BalasPayload> type() {
			return TYPE;
		}
	}

	/** Dónde pegaron los perdigones en los bloques (y en qué cara), para que el cliente dibuje los agujeritos. */
	public record AgujerosPayload(List<org.joml.Vector3f> lugares, List<Integer> caras) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<AgujerosPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "agujeros_bala"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, AgujerosPayload> CODEC =
				net.minecraft.network.codec.StreamCodec.composite(
						net.minecraft.network.codec.ByteBufCodecs.VECTOR3F.apply(net.minecraft.network.codec.ByteBufCodecs.list()), AgujerosPayload::lugares,
						net.minecraft.network.codec.ByteBufCodecs.VAR_INT.apply(net.minecraft.network.codec.ByteBufCodecs.list()), AgujerosPayload::caras,
						AgujerosPayload::new);

		@Override
		public Type<AgujerosPayload> type() {
			return TYPE;
		}
	}

	/** La R (recargar): la manda el cliente. */
	public record RecargarPayload() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<RecargarPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "recargar_arma"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RecargarPayload> CODEC =
				net.minecraft.network.codec.StreamCodec.unit(new RecargarPayload());

		@Override
		public Type<RecargarPayload> type() {
			return TYPE;
		}
	}

	/** El clic izquierdo (disparar): lo manda el cliente. */
	public record DispararPayload() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<DispararPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "disparar_arma"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DispararPayload> CODEC =
				net.minecraft.network.codec.StreamCodec.unit(new DispararPayload());

		@Override
		public Type<DispararPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(DispararPayload.TYPE, DispararPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(DispararPayload.TYPE, (payload, contexto) -> contexto.server().execute(() -> {
			if (contexto.player().getMainHandItem().getItem() instanceof EscopetaPhoraItem escopeta) escopeta.intentarDisparar(contexto.player());
		}));
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(AgujerosPayload.TYPE, AgujerosPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(BalasPayload.TYPE, BalasPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(RecargarPayload.TYPE, RecargarPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(RecargarPayload.TYPE, (payload, contexto) -> contexto.server().execute(() -> {
			ServerPlayer jugador = contexto.player();
			ItemStack pila = jugador.getMainHandItem();
			if (!(pila.getItem() instanceof EscopetaPhoraItem escopeta) || recargando(pila) || jugador.getCooldowns().isOnCooldown(escopeta)) return;
			escopeta.empezarRecarga(jugador, pila);
		}));
	}

	public EscopetaPhoraItem(Properties propiedades) {
		this(propiedades, ESCOPETA);
	}

	public EscopetaPhoraItem(Properties propiedades, Arma arma) {
		super(propiedades.stacksTo(1).rarity(Rarity.EPIC));
		this.arma = arma;
	}

	/** Los números del arma de esa pila (o los de la escopeta si no es un arma Phora). */
	public static Arma armaDe(ItemStack pila) {
		return pila.getItem() instanceof EscopetaPhoraItem e ? e.arma : ESCOPETA;
	}

	// ---------------------------------------------------------------- Datos guardados en el ítem

	private static CompoundTag datos(ItemStack pila) {
		CustomData c = pila.get(DataComponents.CUSTOM_DATA);
		return c == null ? new CompoundTag() : c.copyTag();
	}

	public static int balas(ItemStack pila) {
		return Mth.clamp(datos(pila).getInt(BALAS), 0, armaDe(pila).cargador());
	}

	private static void ponerBalas(ItemStack pila, int balas) {
		int maximo = armaDe(pila).cargador();
		CustomData.update(DataComponents.CUSTOM_DATA, pila, t -> t.putInt(BALAS, Mth.clamp(balas, 0, maximo)));
	}

	private static long finRecarga(ItemStack pila) {
		return datos(pila).getLong(FIN_RECARGA);
	}

	private static void ponerFinRecarga(ItemStack pila, long fin) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, t -> t.putLong(FIN_RECARGA, fin));
	}

	/** Cuándo termina la recarga (en el tiempo del mundo), o 0 si no está recargando. Lo usa el cliente para la animación. */
	public static long finRecargaDe(ItemStack pila) {
		return pila.getItem() instanceof EscopetaPhoraItem ? finRecarga(pila) : 0;
	}

	public static boolean recargando(ItemStack pila) {
		return finRecarga(pila) > 0;
	}

	// ---------------------------------------------------------------- Uso

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		// Shift + clic derecho: recarga. Clic derecho solo: apunta mientras lo mantienes.
		if (jugador.isShiftKeyDown()) {
			if (!level.isClientSide && jugador instanceof ServerPlayer sp && !recargando(pila)) empezarRecarga(sp, pila);
			return InteractionResultHolder.consume(pila);
		}
		jugador.startUsingItem(mano);
		return InteractionResultHolder.consume(pila);
	}

	@Override
	public int getUseDuration(ItemStack pila, LivingEntity quien) {
		return 72000;
	}

	@Override
	public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack pila) {
		return net.minecraft.world.item.UseAnim.NONE;
	}

	/** Clic izquierdo (lo manda el cliente): dispara; sin balas, recarga. */
	public void intentarDisparar(ServerPlayer jugador) {
		ItemStack pila = jugador.getMainHandItem();
		if (!pila.is(this) || recargando(pila) || jugador.getCooldowns().isOnCooldown(this)) return;
		if (balas(pila) == 0) empezarRecarga(jugador, pila);
		else disparar(jugador, pila);
	}

	/** Cuándo disparó por última vez el jugador de esta pantalla (sólo lo usa el cliente, para la animación). */
	public static volatile long disparoCliente = -1000;

	/** Cuántos Proyectiles Phora tiene en el inventario. */
	public static int municionTotal(Player jugador) {
		return jugador.getInventory().countItem(ModItems.PROYECTIL_PHORA);
	}

	private boolean tieneMunicion(Player jugador) {
		return jugador.getAbilities().instabuild || jugador.getInventory().countItem(ModItems.PROYECTIL_PHORA) > 0;
	}

	private void empezarRecarga(ServerPlayer jugador, ItemStack pila) {
		if (balas(pila) >= arma.cargador()) return;
		if (!tieneMunicion(jugador)) {
			jugador.displayClientMessage(Component.literal("Sin ").withColor(BLANCO)
					.append(Component.literal("Proyectil Phora").withColor(CELESTE)).append(Component.literal(" para recargar.").withColor(BLANCO)), true);
			jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.4f);
			return;
		}
		ponerFinRecarga(pila, jugador.level().getGameTime() + arma.recarga());
		jugador.getCooldowns().addCooldown(this, arma.recarga());
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 1f, 0.8f);
	}

	/** Termina la recarga cuando pasan los 2,5 segundos: carga una bala por cada Proyectil Phora que haya. */
	@Override
	public void inventoryTick(ItemStack pila, Level level, Entity duenio, int lugar, boolean enMano) {
		if (level.isClientSide || !(duenio instanceof ServerPlayer jugador)) return;
		long fin = finRecarga(pila);
		if (fin <= 0 || level.getGameTime() < fin) return;
		int faltan = arma.cargador() - balas(pila);
		int cargadas = faltan;
		if (!jugador.getAbilities().instabuild) {
			cargadas = 0;
			var inv = jugador.getInventory();
			for (int i = 0; i < inv.getContainerSize() && cargadas < faltan; i++) {
				ItemStack m = inv.getItem(i);
				if (!m.is(ModItems.PROYECTIL_PHORA)) continue;
				int usar = Math.min(m.getCount(), faltan - cargadas);
				m.shrink(usar);
				cargadas += usar;
			}
		}
		ponerBalas(pila, balas(pila) + cargadas);
		ponerFinRecarga(pila, 0);
		level.playSound(null, jugador.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1f, 0.7f);
		level.playSound(null, jugador.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 1.6f);
	}

	private void disparar(ServerPlayer jugador, ItemStack pila) {
		ServerLevel mundo = jugador.serverLevel();
		ponerBalas(pila, balas(pila) - 1);
		jugador.getCooldowns().addCooldown(this, arma.entreDisparos());
		Vec3 ojos = jugador.getEyePosition();
		Vec3 mira = jugador.getLookAngle();
		// La boca del cañón: un poco adelante y a la derecha de los ojos.
		Vec3 costado = mira.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 boca = ojos.add(mira.scale(0.9)).add(costado.scale(jugador.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.3 : -0.3)).add(0, -0.2, 0);
		mundo.sendParticles(ParticleTypes.FLAME, boca.x, boca.y, boca.z, 3, 0.03, 0.03, 0.03, 0.02);
		mundo.sendParticles(ParticleTypes.SMOKE, boca.x, boca.y, boca.z, 3, 0.03, 0.03, 0.03, 0.01);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.7f);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1f, 0.6f);

		// Cada perdigón: un rayo con un poco de desvío; se suma el daño por bicho y se aplica una sola vez
		// (si no, los perdigones del mismo disparo chocarían con la invulnerabilidad de medio segundo).
		Map<LivingEntity, Float> danios = new HashMap<>();
		List<org.joml.Vector3f> agujeros = new java.util.ArrayList<>();
		List<Integer> caras = new java.util.ArrayList<>();
		List<org.joml.Vector3f> desde = new java.util.ArrayList<>(), hasta = new java.util.ArrayList<>();
		var azar = jugador.getRandom();
		double desvio = arma.dispersion();
		for (int i = 0; i < arma.perdigones(); i++) {
			Vec3 dir = mira.add(azar.nextGaussian() * desvio, azar.nextGaussian() * desvio, azar.nextGaussian() * desvio).normalize();
			Vec3 fin = ojos.add(dir.scale(arma.alcance()));
			HitResult bloque = mundo.clip(new ClipContext(ojos, fin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jugador));
			if (bloque.getType() != HitResult.Type.MISS) fin = bloque.getLocation();
			EntityHitResult golpe = ProjectileUtil.getEntityHitResult(jugador, ojos, fin, jugador.getBoundingBox().expandTowards(fin.subtract(ojos)).inflate(1),
					e -> e instanceof LivingEntity v && v.isAlive() && !e.isSpectator() && e != jugador, ojos.distanceToSqr(fin));
			if (golpe != null) {
				fin = golpe.getLocation();
				LivingEntity v = (LivingEntity) golpe.getEntity();
				boolean cabeza = fin.y >= v.getEyeY() - 0.25;
				danios.merge(v, cabeza ? arma.danioCabeza() : arma.danio(), Float::sum);
				mundo.sendParticles(cabeza ? ParticleTypes.CRIT : ParticleTypes.DAMAGE_INDICATOR, fin.x, fin.y, fin.z, 3, 0.05, 0.05, 0.05, 0.1);
			} else if (bloque instanceof net.minecraft.world.phys.BlockHitResult bh && bloque.getType() == HitResult.Type.BLOCK) {
				mundo.sendParticles(ParticleTypes.SMOKE, fin.x, fin.y, fin.z, 2, 0.02, 0.02, 0.02, 0.01);
				mundo.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, mundo.getBlockState(bh.getBlockPos())),
						fin.x, fin.y, fin.z, 4, 0.05, 0.05, 0.05, 0.1);
				agujeros.add(new org.joml.Vector3f((float) fin.x, (float) fin.y, (float) fin.z));
				caras.add(bh.getDirection().get3DDataValue());
			}
			// El proyectil: un rayo naranja del cañón al impacto (lo dibuja el cliente).
			desde.add(new org.joml.Vector3f((float) boca.x, (float) boca.y, (float) boca.z));
			hasta.add(new org.joml.Vector3f((float) fin.x, (float) fin.y, (float) fin.z));
		}
		BalasPayload balas = new BalasPayload(desde, hasta);
		for (ServerPlayer p : mundo.players()) {
			if (p.distanceToSqr(jugador) < 128 * 128) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, balas);
		}
		if (!agujeros.isEmpty()) {
			AgujerosPayload aviso = new AgujerosPayload(agujeros, caras);
			for (ServerPlayer p : mundo.players()) {
				if (p.distanceToSqr(jugador) < 96 * 96) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, aviso);
			}
		}
		for (Map.Entry<LivingEntity, Float> e : danios.entrySet()) {
			e.getKey().invulnerableTime = 0;
			e.getKey().hurt(mundo.damageSources().playerAttack(jugador), e.getValue());
		}
		// Retroceso: empuja un poco para atrás (el fusil casi nada).
		double empuje = arma == FUSIL ? 0.05 : 0.25;
		jugador.push(-mira.x * empuje, 0.02 * empuje / 0.25, -mira.z * empuje);
		jugador.hurtMarked = true;
	}

	// ---------------------------------------------------------------- Dibujo y descripción

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(LILA);
	}


	private static MutableComponent t(String texto, int color) {
		return Component.literal(texto).withColor(color);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(t("Utiliza ", BLANCO).append(t("Proyectil Phora", CELESTE)));
		texto.add(t("para recargarla.", BLANCO));
		texto.add(Component.empty());
		texto.add(t(String.format(java.util.Locale.ROOT, "Daño: %.2f", arma.danio()), BLANCO));
		texto.add(t(String.format(java.util.Locale.ROOT, "Daño a la cabeza: %.2f", arma.danioCabeza()), BLANCO));
		texto.add(t("Daño a bloques: 0.00", BLANCO));
		texto.add(t("Frecuencia de disparo: " + arma.frecuencia(), BLANCO));
		texto.add(t("Tiempo de recarga: " + arma.tiempoRecarga(), BLANCO));
		texto.add(t("Tamaño del cargador: " + arma.cargador(), BLANCO));
		texto.add(t("Tipo de munición: ", BLANCO).append(t("Proyectil Phora", CELESTE)));
		texto.add(Component.empty());
		texto.add(t("Munición: ", BLANCO).append(t(balas(pila) + " / " + arma.cargador(), NARANJA))
				.append(recargando(pila) ? t("  (recargando...)", 0xAAAAAA) : Component.empty()));
	}

	@Override
	public boolean isFoil(ItemStack pila) {
		return false;
	}

	/**
	 * Que no se baje y vuelva a subir en la mano cada vez que cambian las balas (Fabric: FabricItem; NeoForge:
	 * IItemExtension). Van sin @Override porque cada método existe sólo en uno de los dos.
	 */
	public boolean allowComponentsUpdateAnimation(Player jugador, InteractionHand mano, ItemStack antes, ItemStack ahora) {
		return false;
	}

	public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack ahora, boolean cambioDeLugar) {
		return cambioDeLugar || !ItemStack.isSameItem(antes, ahora);
	}
}
