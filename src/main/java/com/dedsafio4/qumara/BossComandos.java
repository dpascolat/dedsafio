package com.dedsafio4.qumara;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * /boss 1 ai       → Qumara la maneja una IA (para practicar): toma la Qumara más cercana, o hace aparecer
 *                    una 35 bloques adelante. En modo IA ataca a todos los que están en supervivencia,
 *                    admins incluidos.
 * /boss 1 jugador  → vuelve a ser para manejar (la IA la deja).
 * /boss 1 gente N  → la vida de la Qumara más cercana pasa a ser N veces la normal (N jugadores: N × 1000).
 *                    Se mantiene el porcentaje de vida que tenía, así las etapas siguen igual.
 */
public final class BossComandos {
	private BossComandos() {}

	private static final double BUSCAR = 160;

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String boss : new String[]{"boss", "Boss"}) {
			LiteralArgumentBuilder<CommandSourceStack> uno = Commands.literal("1");
			for (String s : new String[]{"ai", "Ai", "AI", "ia", "IA"}) uno.then(Commands.literal(s).executes(c -> aplicar(c.getSource(), true)));
			for (String s : new String[]{"jugador", "Jugador", "off"}) uno.then(Commands.literal(s).executes(c -> aplicar(c.getSource(), false)));
			for (String s : new String[]{"gente", "Gente"}) {
				uno.then(Commands.literal(s).then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100))
						.executes(c -> gente(c.getSource(), IntegerArgumentType.getInteger(c, "cantidad")))));
			}
			dispatcher.register(Commands.literal(boss).requires(s -> s.hasPermission(2)).then(uno));
		}
	}

	/** La vida de la Qumara más cercana: la normal multiplicada por la cantidad de gente. */
	private static int gente(CommandSourceStack fuente, int cantidad) {
		QumaraEntity q = masCercana(fuente);
		if (q == null) {
			fuente.sendFailure(Component.literal("No hay ninguna Qumara cerca."));
			return 0;
		}
		AttributeInstance vida = q.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) return 0;
		float porcentaje = q.getHealth() / q.getMaxHealth();
		vida.setBaseValue(QumaraEntity.VIDA_BASE * cantidad);
		q.setHealth(q.getMaxHealth() * porcentaje);
		fuente.sendSuccess(() -> Component.literal("Qumara ahora tiene " + (int) (QumaraEntity.VIDA_BASE * cantidad)
				+ " de vida (" + cantidad + (cantidad == 1 ? " persona" : " personas") + ").").withColor(0xF0418F), true);
		return cantidad;
	}

	/** La Qumara viva más cercana (hasta 160 bloques), o null. */
	private static QumaraEntity masCercana(CommandSourceStack fuente) {
		ServerLevel mundo = fuente.getLevel();
		Vec3 pos = fuente.getPosition();
		QumaraEntity q = null;
		double mejor = BUSCAR * BUSCAR;
		for (QumaraEntity e : mundo.getEntitiesOfClass(QumaraEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(BUSCAR),
				e -> e.isAlive() && !e.derrotada())) {
			double d = e.distanceToSqr(pos);
			if (d < mejor) {
				mejor = d;
				q = e;
			}
		}
		return q;
	}

	private static int aplicar(CommandSourceStack fuente, boolean ia) {
		ServerLevel mundo = fuente.getLevel();
		Vec3 pos = fuente.getPosition();
		QumaraEntity q = null;
		double mejor = BUSCAR * BUSCAR;
		for (QumaraEntity e : mundo.getEntitiesOfClass(QumaraEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(BUSCAR),
				e -> e.isAlive() && !e.derrotada())) {
			double d = e.distanceToSqr(pos);
			if (d < mejor) {
				mejor = d;
				q = e;
			}
		}
		if (q == null) {
			if (!ia) {
				fuente.sendFailure(Component.literal("No hay ninguna Qumara cerca."));
				return 0;
			}
			// Una nueva, 35 bloques adelante, mirando hacia vos.
			float yaw = fuente.getRotation().y;
			Vec3 adelante = Vec3.directionFromRotation(0, yaw).scale(35);
			double x = pos.x + adelante.x, z = pos.z + adelante.z;
			int y = mundo.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
			q = ModQumara.QUMARA.create(mundo);
			if (q == null) return 0;
			q.moveTo(x, y, z, Mth.wrapDegrees(yaw + 180), 0);
			q.yBodyRot = q.yHeadRot = q.getYRot();
			mundo.addFreshEntity(q);
		}
		q.setIa(ia);
		if (ia) {
			Entity jinete = q.getControllingPassenger();
			if (jinete != null) jinete.stopRiding();
			fuente.sendSuccess(() -> Component.literal("Qumara ahora la maneja la IA: ¡a practicar! (ataca a todos los que están en supervivencia, admins también)")
					.withColor(0xF0418F), true);
		} else {
			fuente.sendSuccess(() -> Component.literal("Qumara vuelve a ser para manejar (la IA la dejó).").withColor(0xF0418F), true);
		}
		return 1;
	}
}
