package com.dedsafio4.bestias;

import com.dedsafio4.nave.RayoEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Núcleo: no se mueve nunca. Si alguien le pega, se enoja y le dispara rayos rojos (como los de la nave) cada segundo,
 * mientras esté vivo y dentro del radio.
 * <p>La supernova es al morir: los jugadores que estén dentro del radio cuando explota (o mientras dura) quedan quietos,
 * sin poder moverse ni saltar, y el Núcleo les pega cada medio segundo (sin importar la armadura): les va rompiendo los
 * tótems hasta que se mueren.
 * <p>Cuando lo matan, empieza una cuenta (10 segundos, o lo que diga /tiempito) para escapar del radio: se ve el borde
 * y a todos los que están cerca les aparece un contador rojo grande en la pantalla. Al terminar explota como supernova: los que no escaparon quedan atrapados hasta morirse.
 * <ul>
 * <li>/dañomob nucleo &lt;daño&gt;: lo que saca cada golpe (20 = 10 corazones).</li>
 * <li>/vidamob nucleo &lt;vida&gt;: su vida (300).</li>
 * <li>/radios nucleo &lt;bloques&gt;: qué tan lejos llega la supernova (20 bloques).</li>
 * <li>/tiempito &lt;segundos&gt;: cuánto tarda en explotar después de matarlo (10 s).</li>
 * </ul>
 */
public class NucleoEntity extends Monster {
	private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(NucleoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TIEMPO = SynchedEntityData.defineId(NucleoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> RADIO = SynchedEntityData.defineId(NucleoEntity.class, EntityDataSerializers.FLOAT);
	/** Cuánto dura la cuenta para escapar de este Núcleo (ticks): lo que decía /tiempito cuando murió. */
	private static final EntityDataAccessor<Integer> CUENTA = SynchedEntityData.defineId(NucleoEntity.class, EntityDataSerializers.INT);
	public static final int QUIETO = 0, CARGANDO = 1, SUPERNOVA = 2, ESCAPEN = 3;
	/** Los segundos (en ticks) que tienen para escapar después de matarlo, si no se cambió con /tiempito. */
	public static final int ESCAPE = 200;
	/** Después de la supernova de la muerte, cuánto más se queda como mucho (60 s) pegándoles a los atrapados. */
	private static final int MAXIMO_ATRAPADOS = 1200;
	/** Cuánto carga (3 segundos) y cuánto dura la supernova atrapando gente (5 segundos). */
	public static final int CARGA = 60, DURACION = 100;
	public static final float RADIO_NORMAL = 20;
	/** El tamaño del modelo: cada "bloque" de la construcción es un píxel (1/16 de bloque). Mide 8 bloques de alto. */
	public static final float ESCALA = 1 / 16f, ALTO = 8f;
	/** Cada cuánto pegan los atrapados (medio segundo) y el golpe de vuelta a quien le pegó (1 segundo). */
	private static final int CADA_GOLPE = 10, CADA_DEVOLUCION = 20;

	private static final ResourceLocation QUIETO_ID = ResourceLocation.fromNamespaceAndPath("dedsafio4", "nucleo_quieto");

	/** Los jugadores atrapados por una supernova (solo en el servidor): quedan quietos hasta morirse. */
	private record Atrapado(ResourceKey<Level> mundo, Vec3 lugar, UUID nucleo) {}

	private static final Map<UUID, Atrapado> ATRAPADOS = new HashMap<>();

	private UUID enojadoCon;
	/** Ticks hasta el próximo rayo a quien le pegó. */
	private int proximoDisparo;

	public NucleoEntity(EntityType<? extends NucleoEntity> tipo, Level level) {
		super(tipo, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 300.0)
				// Cada golpe (de vuelta, y a los atrapados por la supernova). Se cambia con /dañomob.
				.add(Attributes.ATTACK_DAMAGE, 20.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder constructor) {
		super.defineSynchedData(constructor);
		constructor.define(ESTADO, QUIETO);
		constructor.define(TIEMPO, 0);
		constructor.define(RADIO, RADIO_NORMAL);
		constructor.define(CUENTA, ESCAPE);
	}

	public int estado() {
		return entityData.get(ESTADO);
	}

	/** Ticks desde que empezó el estado (cargando o supernova). */
	public int tiempo() {
		return entityData.get(TIEMPO);
	}

	public float radio() {
		return entityData.get(RADIO);
	}

	private void cambiar(int estado) {
		entityData.set(ESTADO, estado);
		entityData.set(TIEMPO, 0);
	}

	// ------------------------------------------------------------------------------- Quieto

	@Override
	protected void registerGoals() {
		// Nada: no camina, no mira, no busca a nadie.
	}

	@Override
	public void travel(Vec3 movimiento) {
		// Solo cae (si lo ponen en el aire); nunca se mueve para los costados.
		setDeltaMovement(0, getDeltaMovement().y, 0);
		super.travel(Vec3.ZERO);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity otro) {}

	@Override
	public void knockback(double fuerza, double x, double z) {}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {}

	// ------------------------------------------------------------------------------- Enojarse

	@Override
	public boolean hurt(DamageSource fuente, float danio) {
		boolean pego = super.hurt(fuente, danio);
		if (pego && !level().isClientSide && fuente.getEntity() instanceof LivingEntity atacante && atacante != this && isAlive()) {
			// Se enoja con el último que le pegó y le dispara enseguida (y después cada segundo).
			if (!atacante.getUUID().equals(enojadoCon)) {
				level().playSound(null, getX(), getY() + ALTO / 2, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4f, 0.6f);
				proximoDisparo = Math.min(proximoDisparo, 5);
			}
			enojadoCon = atacante.getUUID();
		}
		return pego;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo)) return;
		// El radio de /radios (o el normal).
		Double r = AjustesMob.radioDe(mundo.getServer(), getType());
		float radio = r == null ? RADIO_NORMAL : r.floatValue();
		if (radio() != radio) entityData.set(RADIO, radio);

		// Muerto: cuenta para escapar y después la supernova final.
		if (isDeadOrDying()) {
			tickMuerto(mundo, radio);
			return;
		}

		int estado = estado();
		if (estado == QUIETO) {
			// Enojado: le dispara a quien le pegó cada segundo.
			if (enojadoCon != null && --proximoDisparo <= 0) {
				proximoDisparo = CADA_DEVOLUCION;
				devolverGolpe(mundo, radio);
			}
			return;
		}
		int t = tiempo() + 1;
		entityData.set(TIEMPO, t);
		Vec3 centro = centro();
		if (estado == CARGANDO) {
			// Junta energía: chispas que se van al centro.
			for (int i = 0; i < 6; i++) {
				Vec3 desde = centro.add((random.nextDouble() - 0.5) * 10, (random.nextDouble() - 0.5) * 10, (random.nextDouble() - 0.5) * 10);
				Vec3 hacia = centro.subtract(desde).scale(0.12);
				mundo.sendParticles(ParticleTypes.FLAME, desde.x, desde.y, desde.z, 0, hacia.x, hacia.y, hacia.z, 1);
			}
			if (t % 20 == 0) {
				mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4f, 0.5f + t / 120f);
				devolverGolpe(mundo, radio);
			}
			if (t >= CARGA) explotar(mundo, centro, radio);
		} else if (estado == SUPERNOVA) {
			atrapar(mundo, centro, radio);
			if (t % 20 == 0) devolverGolpe(mundo, radio);
			if (t >= DURACION) {
				cambiar(QUIETO);
				enojadoCon = null;
			}
		}
	}

	/** El medio del Núcleo (el ojo dorado). */
	public Vec3 centro() {
		return position().add(0, ALTO / 2 * getScale(), 0);
	}

	/** Le dispara a quien le pegó (si está vivo, en el mismo mundo y dentro del radio) un rayo rojo como los de la nave. */
	private void devolverGolpe(ServerLevel mundo, float radio) {
		if (enojadoCon == null) return;
		// Si el que le pegó se murió o ya no está en este mundo, se calma.
		if (!(mundo.getEntity(enojadoCon) instanceof LivingEntity victima) || !victima.isAlive()) {
			enojadoCon = null;
			return;
		}
		if (victima instanceof Player p && (p.isCreative() || p.isSpectator())) return;
		Vec3 centro = centro(), destino = victima.position().add(0, victima.getBbHeight() / 2, 0);
		if (centro.distanceTo(destino) > radio) return;
		// Sale del ojo, un poco hacia adelante para no nacer dentro del cuerpo.
		Vec3 origen = centro.add(destino.subtract(centro).normalize().scale(2.2 * getScale()));
		RayoEntity rayo = new RayoEntity(mundo, this, origen).rojo((float) getAttributeValue(Attributes.ATTACK_DAMAGE));
		rayo.shoot(destino.x - origen.x, destino.y - origen.y, destino.z - origen.z, 2.2f, 0.5f);
		mundo.addFreshEntity(rayo);
		mundo.playSound(null, origen.x, origen.y, origen.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.9f + random.nextFloat() * 0.2f);
	}

	private void explotar(ServerLevel mundo, Vec3 centro, float radio) {
		efectosSupernova(mundo, centro, radio);
		atrapar(mundo, centro, radio);
	}

	/** Se guarda en qué está (así, si el servidor se apaga en medio de la cuenta, sigue donde estaba). */
	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Estado", estado());
		tag.putInt("Tiempo", tiempo());
		tag.putInt("Cuenta", cuenta());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(ESTADO, tag.getInt("Estado"));
		entityData.set(TIEMPO, tag.getInt("Tiempo"));
		if (tag.contains("Cuenta")) entityData.set(CUENTA, Math.max(20, tag.getInt("Cuenta")));
	}

	// ------------------------------------------------------------------------------- Muerte

	/** Al morir no desaparece: empieza la cuenta para escapar. */
	@Override
	public void die(DamageSource fuente) {
		super.die(fuente);
		if (!(level() instanceof ServerLevel mundo)) return;
		enojadoCon = null;
		entityData.set(CUENTA, Tiempito.ticks(mundo.getServer()));
		cambiar(ESCAPEN);
		Vec3 c = centro();
		mundo.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 8f, 0.5f);
		mundo.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 8f, 0.5f);
		contador(mundo, c, radio(), cuenta());
	}

	/** Cuánto dura la cuenta para escapar (ticks). */
	public int cuenta() {
		return entityData.get(CUENTA);
	}

	/**
	 * El contador rojo grande en el medio de la pantalla de todos los que están cerca, con los segundos que faltan; abajo
	 * dice si están adentro del radio (¡escapa!) o afuera (a salvo).
	 */
	private void contador(ServerLevel mundo, Vec3 centro, float radio, int faltanTicks) {
		int segundos = (faltanTicks + 19) / 20;
		for (ServerPlayer p : mundo.players()) {
			double d = p.position().add(0, p.getBbHeight() / 2, 0).distanceTo(centro);
			if (d > radio + 32 || p.isSpectator()) continue;
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(0, 30, 6));
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
					Component.literal(String.valueOf(segundos)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(d <= radio
					? Component.literal("¡Escapa del radio del Núcleo!").withStyle(ChatFormatting.RED)
					: Component.literal("A salvo").withStyle(ChatFormatting.GREEN)));
		}
	}

	private void tickMuerto(ServerLevel mundo, float radio) {
		// Por si murió sin pasar por die() (por ejemplo, un mundo guardado de antes): empieza la cuenta.
		if (estado() == QUIETO || estado() == CARGANDO) cambiar(ESCAPEN);
		int t = tiempo() + 1;
		entityData.set(TIEMPO, t);
		Vec3 centro = centro();
		int total = cuenta();
		if (estado() == ESCAPEN) {
			int quedan = (total - t + 19) / 20;
			// Junta energía cada vez más rápido.
			for (int i = 0; i < 4 + t / 20; i++) {
				Vec3 desde = centro.add((random.nextDouble() - 0.5) * 10, (random.nextDouble() - 0.5) * 10, (random.nextDouble() - 0.5) * 10);
				Vec3 hacia = centro.subtract(desde).scale(0.12);
				mundo.sendParticles(ParticleTypes.FLAME, desde.x, desde.y, desde.z, 0, hacia.x, hacia.y, hacia.z, 1);
			}
			if (t % 20 == 0) mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 6f, 0.6f + t / (float) total);
			// El contador rojo: cambia cada segundo.
			if ((total - t) % 20 == 0 && t < total) contador(mundo, centro, radio, total - t);
			// A los que están cerca: cuánto falta y si están adentro o afuera.
			if (t % 5 == 0) {
				for (ServerPlayer p : mundo.players()) {
					double d = p.position().add(0, p.getBbHeight() / 2, 0).distanceTo(centro);
					if (d > radio + 32 || p.isSpectator()) continue;
					Component aviso = d <= radio
							? Component.literal("¡Escapa! Explota en " + quedan + " s · faltan " + (int) Math.ceil(radio - d) + " bloques").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
							: Component.literal("A salvo · explota en " + quedan + " s").withStyle(ChatFormatting.GREEN);
					p.displayClientMessage(aviso, true);
				}
			}
			if (t >= total) {
				efectosSupernova(mundo, centro, radio);
				atrapar(mundo, centro, radio);
			}
		} else if (estado() == SUPERNOVA && t <= DURACION) {
			// Mientras dura la explosión, el que entre también queda atrapado.
			atrapar(mundo, centro, radio);
		}
	}

	/** Se queda hasta que explota y mientras tenga atrapados (como mucho 60 s más); después desaparece. */
	@Override
	protected void tickDeath() {
		++deathTime;
		if (level().isClientSide() || isRemoved()) return;
		boolean termino = estado() == SUPERNOVA && tiempo() >= DURACION;
		boolean tieneAtrapados = ATRAPADOS.values().stream().anyMatch(a -> a.nucleo().equals(getUUID()));
		if (termino && (!tieneAtrapados || tiempo() >= MAXIMO_ATRAPADOS)) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	private void efectosSupernova(ServerLevel mundo, Vec3 centro, float radio) {
		cambiar(SUPERNOVA);
		mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8f, 0.5f);
		mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 8f, 0.6f);
		mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.END_GATEWAY_SPAWN, SoundSource.HOSTILE, 8f, 0.7f);
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centro.x, centro.y, centro.z, 3, 1.5, 1.5, 1.5, 0);
		mundo.sendParticles(ParticleTypes.FLASH, centro.x, centro.y, centro.z, 4, 0, 0, 0, 0);
		// Un anillo de fuego que sale para afuera.
		for (int i = 0; i < 120; i++) {
			double a = i * Math.PI * 2 / 120;
			mundo.sendParticles(ParticleTypes.FLAME, centro.x, centro.y, centro.z, 0, Math.cos(a), 0.05, Math.sin(a), radio / 18.0);
		}
	}

	/** Los jugadores dentro del radio quedan atrapados (si no lo estaban ya). */
	private void atrapar(ServerLevel mundo, Vec3 centro, float radio) {
		for (ServerPlayer p : mundo.players()) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator() || ATRAPADOS.containsKey(p.getUUID())) continue;
			if (p.position().add(0, p.getBbHeight() / 2, 0).distanceTo(centro) > radio) continue;
			ATRAPADOS.put(p.getUUID(), new Atrapado(mundo.dimension(), p.position(), getUUID()));
			mundo.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5f, 0.6f);
		}
	}

	// ------------------------------------------------------------------------------- Atrapados

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(NucleoEntity::tickAtrapados);
	}

	private static void tickAtrapados(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Atrapado a = ATRAPADOS.get(p.getUUID());
			if (a == null) {
				soltar(p);
				continue;
			}
			ServerLevel mundo = p.serverLevel();
			Entity nucleo = mundo.dimension() == a.mundo() ? mundo.getEntity(a.nucleo()) : null;
			// (Un Núcleo muerto sigue atrapando hasta que desaparece: es la supernova final.)
			if (!p.isAlive() || p.isCreative() || p.isSpectator() || !(nucleo instanceof NucleoEntity n) || n.isRemoved()) {
				ATRAPADOS.remove(p.getUUID());
				soltar(p);
				continue;
			}
			// Quieto: sin caminar ni saltar, y si igual se movió (volando, empujado...), vuelve a su lugar.
			poner(p.getAttribute(Attributes.MOVEMENT_SPEED));
			poner(p.getAttribute(Attributes.JUMP_STRENGTH));
			poner(p.getAttribute(Attributes.FLYING_SPEED));
			if (p.position().distanceToSqr(a.lugar()) > 0.05) {
				p.teleportTo(mundo, a.lugar().x, a.lugar().y, a.lugar().z, p.getYRot(), p.getXRot());
			}
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
			if (p.tickCount % 4 == 0) {
				mundo.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.35f, 0.05f), 1.2f),
						p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.7, 0.4, 0);
			}
			// Cada medio segundo un golpe que no frena la armadura: rompe un tótem tras otro hasta que se muere.
			if (server.getTickCount() % CADA_GOLPE == 0) {
				p.invulnerableTime = 0;
				p.hurt(mundo.damageSources().indirectMagic(n, n), (float) n.getAttributeValue(Attributes.ATTACK_DAMAGE));
			}
		}
		// Los que se desconectaron.
		if (server.getTickCount() % 100 == 0) ATRAPADOS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
	}

	private static void poner(AttributeInstance atributo) {
		if (atributo != null && !atributo.hasModifier(QUIETO_ID)) {
			atributo.addTransientModifier(new AttributeModifier(QUIETO_ID, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void soltar(ServerPlayer p) {
		for (var holder : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.FLYING_SPEED)) {
			AttributeInstance a = p.getAttribute(holder);
			if (a != null && a.hasModifier(QUIETO_ID)) a.removeModifier(QUIETO_ID);
		}
	}

	/** ¿Este jugador está atrapado por una supernova? */
	public static boolean atrapado(Player p) {
		return ATRAPADOS.containsKey(p.getUUID());
	}

	/** Para las pruebas: se enoja ya, como si le hubieran pegado. */
	public void enojarseCon(LivingEntity quien) {
		enojadoCon = quien.getUUID();
		if (estado() == QUIETO) cambiar(CARGANDO);
	}
}
