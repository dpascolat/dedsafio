package com.dedsafio4.robots;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * El botón de la Dedita en el comercio del Aldeano: gastando Deditas (azules, las tienes que tener encima) repone
 * todos sus comercios. Cada vez que se repone ese aldeano cuesta una Dedita más (1, 2, 3...).
 */
public final class ReponerComercio {
	private ReponerComercio() {}

	/**
	 * Del servidor al jugador: cuánto cuesta reponer a este aldeano (-1: no se puede, no es un Aldeano) y, para cada
	 * comercio, cuántos segundos faltan para que vuelva (-1: no está agotado).
	 */
	public record CostoPayload(int costo, java.util.List<Integer> segundos) implements CustomPacketPayload {
		public static final Type<CostoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "comercio_costo"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CostoPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, CostoPayload::costo, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CostoPayload::segundos,
				CostoPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Del jugador al servidor: tocó la Dedita. */
	public record ReponerPayload() implements CustomPacketPayload {
		public static final Type<ReponerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "comercio_reponer"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ReponerPayload> CODEC = StreamCodec.unit(new ReponerPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(CostoPayload.TYPE, CostoPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ReponerPayload.TYPE, ReponerPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ReponerPayload.TYPE, (payload, context) ->
				context.server().execute(() -> reponer(context.player())));
	}

	/** El aldeano con el que está comerciando el jugador (o null). */
	private static AldeanoRobotEntity aldeanoDe(ServerPlayer jugador) {
		if (!(jugador.containerMenu instanceof MerchantMenu)) return null;
		for (AldeanoRobotEntity a : jugador.serverLevel().getEntitiesOfClass(AldeanoRobotEntity.class, jugador.getBoundingBox().inflate(16))) {
			if (a.getTradingPlayer() == jugador) return a;
		}
		return null;
	}

	private static int deditas(ServerPlayer jugador) {
		int n = 0;
		var inv = jugador.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.DEDITA)) n += inv.getItem(i).getCount();
		return n;
	}

	private static void reponer(ServerPlayer jugador) {
		AldeanoRobotEntity aldeano = aldeanoDe(jugador);
		if (aldeano == null) return;
		int costo = aldeano.costoReponer();
		if (deditas(jugador) < costo) {
			jugador.displayClientMessage(Component.literal("Necesitas " + costo + (costo == 1 ? " Dedita" : " Deditas") + " para reponer los comercios.")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		// Se cobran del inventario.
		int falta = costo;
		var inv = jugador.getInventory();
		for (int i = 0; i < inv.getContainerSize() && falta > 0; i++) {
			if (!inv.getItem(i).is(ModItems.DEDITA)) continue;
			int saca = Math.min(falta, inv.getItem(i).getCount());
			inv.getItem(i).shrink(saca);
			falta -= saca;
		}
		aldeano.reponerPagando();
		jugador.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
		jugador.displayClientMessage(Component.literal("¡Comercios repuestos!").withStyle(ChatFormatting.GREEN), true);
		aldeano.avisarEstado();
	}
}
