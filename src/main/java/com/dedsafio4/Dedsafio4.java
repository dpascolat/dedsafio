package com.dedsafio4;

import com.dedsafio4.pociones.ModPociones;
import com.dedsafio4.banco.Banco;
import com.dedsafio4.bloques.ModBloques;
import com.dedsafio4.dimension.ModGeneracion;
import com.dedsafio4.dimension.Portales;
import com.dedsafio4.eburia.Eburia;
import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import com.dedsafio4.robots.SinAldeanosViejos;
import com.dedsafio4.banco.BancoComandos;
import com.dedsafio4.banco.DeditasPayload;
import com.dedsafio4.cambios.CambiosComandos;
import com.dedsafio4.candados.CandadoCodigoPayload;
import com.dedsafio4.candados.CandadoPantallaPayload;
import com.dedsafio4.candados.CandadosListaPayload;
import com.dedsafio4.candados.Candados;
import com.dedsafio4.cambios.CambiosEventos;
import com.dedsafio4.cambios.MomentoReviil;
import com.dedsafio4.cielo.Cielo;
import com.dedsafio4.cielo.CieloPayload;
import com.dedsafio4.hermandad.HermandadAccionPayload;
import com.dedsafio4.hermandad.HermandadInfoPayload;
import com.dedsafio4.hermandad.Hermandades;
import com.dedsafio4.hermandad.HermandadesJugadoresPayload;
import com.dedsafio4.hermandad.ManuscritoAccionPayload;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.marcas.BloqueoMinimapa;
import com.dedsafio4.marcas.Marcas;
import com.dedsafio4.marcas.MarcasPayload;
import com.dedsafio4.nave.ModEntidades;
import com.dedsafio4.temporizador.Temporizador;
import com.dedsafio4.temporizador.TemporizadorPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dedsafio4 implements ModInitializer {
	public static final String MOD_ID = "dedsafio4";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntidades.registrar();
		ModBloques.registrar();
		com.dedsafio4.banco.ModCajero.registrar();
		com.dedsafio4.subastas.ModVitrina.registrar();
		com.dedsafio4.puertas.ModPuertas.registrar();
		com.dedsafio4.boveda.ModBoveda.registrar();
		com.dedsafio4.meteoritos.ModMeteoritos.registrar();
		com.dedsafio4.marcos.ModMarcos.registrar();
		com.dedsafio4.casino.ModCasino.registrar();
		com.dedsafio4.cinematica.Cinematica.registrar();
		com.dedsafio4.disfraz.Disfraces.registrar();
		com.dedsafio4.bestias.AnuncioMob.registrar();
		com.dedsafio4.aviso.Aviso.registrar();
		com.dedsafio4.bestias.CreeperAmarilloEntity.registrar();
		NombreOculto.registrar();
		com.dedsafio4.dimension.Limbo.registrar();
		com.dedsafio4.bloques.EntregaMisionBlock.registrar();
		com.dedsafio4.ruleta.RuletaPiso.registrar();
		com.dedsafio4.revivir.Revivir.registrar();
		com.dedsafio4.correo.Correo.registrar();
		com.dedsafio4.cambios.DificultadCambio.registrar();
		com.dedsafio4.nave.PartesNave.registrar();
		com.dedsafio4.baneos.Baneos.registrar();
		com.dedsafio4.dactylos.ModDactylos.registrar();
		com.dedsafio4.herobrine.ModHerobrine.registrar();
		com.dedsafio4.trex.ModTRex.registrar();
		com.dedsafio4.qumara.ModQumara.registrar();
		com.dedsafio4.eclipse.Eclipse.registrar();
		com.dedsafio4.cofres.ModCofres.registrar();
		com.dedsafio4.robots.ModRobots.registrar();
		ModGeneracion.registrar();
		Portales.registrar();
		com.dedsafio4.dimension.Organos.registrar();
		com.dedsafio4.despegue.ModDespegue.registrar();
		Eburia.registrar();
		com.dedsafio4.items.Linternas.registrar();
		com.dedsafio4.items.TotemFrericoItem.registrar();
		com.dedsafio4.items.TotemGlebanoideItem.registrar();
		com.dedsafio4.items.TotemLimboItem.registrar();
		com.dedsafio4.items.TotemIdoloItem.registrar();
		com.dedsafio4.items.CorazonItem.registrar();
		com.dedsafio4.items.FrutoQuiuItem.registrar();
		com.dedsafio4.almas.Almas.registrar();
		com.dedsafio4.items.PildoraItem.registrar();
		VenenoPrimitivo.registrar();
		ModPociones.registrar();
		ModItems.registrar();

		// Servidor → cliente
		PayloadTypeRegistry.playS2C().register(DeditasPayload.TYPE, DeditasPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HermandadInfoPayload.TYPE, HermandadInfoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.dedsafio4.hermandad.HermandadTablonPayload.TYPE,
				com.dedsafio4.hermandad.HermandadTablonPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.dedsafio4.hermandad.HermandadEstandartesPayload.TYPE,
				com.dedsafio4.hermandad.HermandadEstandartesPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.dedsafio4.hermandad.HermandadChatPayload.TYPE,
				com.dedsafio4.hermandad.HermandadChatPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HermandadesJugadoresPayload.TYPE, HermandadesJugadoresPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CieloPayload.TYPE, CieloPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TemporizadorPayload.TYPE, TemporizadorPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MarcasPayload.TYPE, MarcasPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.dedsafio4.marcas.MinimapaPayload.TYPE, com.dedsafio4.marcas.MinimapaPayload.CODEC);
		com.dedsafio4.catalogo.Catalogo.registrar();
		com.dedsafio4.catalogo.Misiones.registrar();
		com.dedsafio4.ruleta.Ruleta.registrar();
		com.dedsafio4.bulag.Bulag.registrar();
		com.dedsafio4.despegue.Tunel.registrar();
		com.dedsafio4.momentito.MomentitoEntity.registrar();
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
				com.dedsafio4.bestias.CreeperPastelEntity.PastelPayload.TYPE, com.dedsafio4.bestias.CreeperPastelEntity.PastelPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CandadoPantallaPayload.TYPE, CandadoPantallaPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CandadosListaPayload.TYPE, CandadosListaPayload.CODEC);

		// Cliente → servidor
		PayloadTypeRegistry.playC2S().register(ManuscritoAccionPayload.TYPE, ManuscritoAccionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(HermandadAccionPayload.TYPE, HermandadAccionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CandadoCodigoPayload.TYPE, CandadoCodigoPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CandadoCodigoPayload.TYPE, (payload, context) ->
				context.server().execute(() -> Candados.codigoEscrito(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(ManuscritoAccionPayload.TYPE, (payload, context) ->
				Hermandades.alAccion(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(HermandadAccionPayload.TYPE, (payload, context) ->
				Hermandades.alAccionInterfaz(context.player(), payload));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BancoComandos.registrar(dispatcher);
			com.dedsafio4.subastas.Subastas.registrar(dispatcher);
			BloqueoMinimapa.registrarComando(dispatcher);
			CambiosComandos.registrar(dispatcher);
			Hermandades.registrarComandos(dispatcher);
			Cielo.registrarComandos(dispatcher);
			Temporizador.registrarComandos(dispatcher);
			MomentoReviil.registrarComandos(dispatcher);
			com.dedsafio4.eclipse.Eclipse.registrarComandos(dispatcher);
			Marcas.registrarComandos(dispatcher);
			com.dedsafio4.qumara.BossComandos.registrar(dispatcher);
			AdminComandos.registrar(dispatcher);
			AyudaComando.registrar(dispatcher);
			com.dedsafio4.almas.Almas.registrarComandos(dispatcher);
			com.dedsafio4.estructuras.Estructuras.registrar(dispatcher);
			com.dedsafio4.casino.CasinoPremios.registrarComandos(dispatcher);
			com.dedsafio4.cinematica.Cinematica.registrarComandos(dispatcher);
			com.dedsafio4.disfraz.Disfraces.registrarComandos(dispatcher);
			com.dedsafio4.bestias.AnuncioMob.registrarComandos(dispatcher);
			com.dedsafio4.aviso.Aviso.registrarComandos(dispatcher);
			com.dedsafio4.bestias.AlmitaEntity.registrarComandos(dispatcher);
			NombreOculto.registrarComandos(dispatcher);
			com.dedsafio4.dimension.Limbo.registrarComandos(dispatcher);
			com.dedsafio4.ruleta.RuletaPiso.registrarComandos(dispatcher);
			com.dedsafio4.revivir.Revivir.registrarComandos(dispatcher);
			com.dedsafio4.cambios.DificultadCambio.registrarComandos(dispatcher);
			com.dedsafio4.catalogo.Catalogo.registrarComandos(dispatcher);
			com.dedsafio4.ruleta.Ruleta.registrarComandos(dispatcher);
			com.dedsafio4.bulag.Bulag.registrarComandos(dispatcher);
			com.dedsafio4.despegue.Tunel.registrarComandos(dispatcher);
			com.dedsafio4.momentito.MomentitoEntity.registrarComandos(dispatcher);
		});

		// Al entrar, cada jugador recibe el estado actual de todo lo que se muestra en su pantalla.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Banco.sincronizar(handler.player);
			Hermandades.sincronizarJugador(handler.player);
			Cielo.sincronizar(handler.player);
			com.dedsafio4.eclipse.Eclipse.sincronizar(handler.player);
			Temporizador.sincronizar(handler.player);
			Marcas.sincronizar(handler.player);
			BloqueoMinimapa.alEntrar(handler.player);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Temporizador.tick(server);
			MomentoReviil.tick(server);
			BloqueoMinimapa.tick(server);
		});

		MensajesConexion.registrar();
		CambiosEventos.registrar();
		Candados.registrar();
		SinAldeanosViejos.registrar();

		// Los Wardens quedan en la lista de mobs de la oscuridad profunda; SpawnPlacementsMixin
		// solo los deja aparecer con "/cambio wardens 1".
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DEEP_DARK),
				MobCategory.MONSTER, EntityType.WARDEN, 1, 1, 1);
		// Blaze en todo el Nether: sólo se permiten fuera de las fortalezas con "blaze 1" (SpawnPlacementsMixin).
		BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(),
				MobCategory.MONSTER, EntityType.BLAZE, 20, 1, 2);
		// Bogged en todo el Overworld: fuera de los pantanos sólo con "esqueleto_bogged 1" y de noche (SpawnPlacementsMixin).
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
				MobCategory.MONSTER, EntityType.BOGGED, 50, 1, 2);
		// Stray en todo el Overworld: fuera de la nieve sólo con "esqueleto_stray 1" y de noche (SpawnPlacementsMixin).
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
				MobCategory.MONSTER, EntityType.STRAY, 50, 1, 2);
		// Zombis en todo el Nether: sólo aparecen con "zombi 3" (SpawnPlacementsMixin), sin importar la luz.
		BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(),
				MobCategory.MONSTER, EntityType.ZOMBIE, 100, 2, 4);
		// El Flashbang aparece en la oscuridad, en cualquier parte del Overworld.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
				MobCategory.MONSTER, ModEntidades.FLASHBANG, 12, 1, 1);
		// El Wraith aparece en la oscuridad, en cualquier parte del Overworld (solo después de /mob wraith).
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
				MobCategory.MONSTER, ModEntidades.WRAITH, 8, 1, 1);
		// El Creeper Pastel aparece en cualquier parte del Overworld.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
				MobCategory.MONSTER, ModEntidades.CREEPER_PASTEL, 15, 1, 1);
		// El Creeper Nuclear aparece en la nieve.
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES,
						Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.GROVE, Biomes.SNOWY_BEACH, Biomes.FROZEN_RIVER),
				MobCategory.MONSTER, ModEntidades.CREEPER_NUCLEAR, 40, 1, 1);
		// Las Bombas Warden aparecen solas en la Oscuridad Profunda.
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DEEP_DARK),
				MobCategory.MONSTER, ModEntidades.BOMBA_WARDEN, 30, 1, 2);
	}
}
