package com.dedsafio4.hermandad;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Hermandades fundadas en el mundo. */
public class HermandadesData extends SavedData {
	public static final SavedData.Factory<HermandadesData> FACTORY =
			new SavedData.Factory<>(HermandadesData::new, HermandadesData::cargar, null);

	public static final class Hermandad {
		private final String nombre;
		private final UUID maestro;
		private final List<ManuscritoDatos.Inscrito> miembros;
		private long balance;
		private int nivelBanco = 1;
		private String motd = "";
		private int color = 0xFFFFFF;
		/** Los miembros con rango de Líder. */
		private final java.util.Set<UUID> lideres = new java.util.HashSet<>();
		/** Los anuncios del Tablón (el más nuevo primero). */
		private final List<HermandadTablonPayload.Anuncio> anuncios = new ArrayList<>();
		/** La Colección de Estandartes (hasta 27) y cuál es el activo (-1 si ninguno). */
		private final List<net.minecraft.world.item.ItemStack> estandartes = new ArrayList<>();
		private int activo = -1;
		/** Las últimas líneas del chat de la Hermandad (la más vieja primero). */
		private final List<HermandadChatPayload.Linea> chat = new ArrayList<>();

		public Hermandad(String nombre, UUID maestro, List<ManuscritoDatos.Inscrito> miembros) {
			this.nombre = nombre;
			this.maestro = maestro;
			this.miembros = new ArrayList<>(miembros);
		}

		public String nombre() { return nombre; }
		public UUID maestro() { return maestro; }
		public List<ManuscritoDatos.Inscrito> miembros() { return miembros; }
		public long balance() { return balance; }
		public int nivelBanco() { return nivelBanco; }
		public String motd() { return motd; }
		/** Color del nombre de la Hermandad (RGB). */
		public int color() { return color; }
		public List<HermandadChatPayload.Linea> chat() { return chat; }
		public java.util.Set<UUID> lideres() { return lideres; }
		public List<HermandadTablonPayload.Anuncio> anuncios() { return anuncios; }

		public boolean esLider(UUID jugador) {
			return lideres.contains(jugador);
		}

		/** El Maestro y los Líderes pueden publicar en el Tablón. */
		public boolean puedePublicar(UUID jugador) {
			return maestro.equals(jugador) || lideres.contains(jugador);
		}
		public List<net.minecraft.world.item.ItemStack> estandartes() { return estandartes; }
		public int activo() { return activo; }

		/** El estandarte que llevan de capa, o vacío. */
		public net.minecraft.world.item.ItemStack estandarteActivo() {
			return activo >= 0 && activo < estandartes.size() ? estandartes.get(activo) : net.minecraft.world.item.ItemStack.EMPTY;
		}

		public boolean esMiembro(UUID jugador) {
			return miembros.stream().anyMatch(m -> m.uuid().equals(jugador));
		}

		public String nombreMaestro() {
			return miembros.stream().filter(m -> m.uuid().equals(maestro))
					.map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
		}
	}

	private final List<Hermandad> hermandades = new ArrayList<>();

	public List<Hermandad> todas() {
		return hermandades;
	}

	public Optional<Hermandad> deJugador(UUID jugador) {
		return hermandades.stream().filter(h -> h.esMiembro(jugador)).findFirst();
	}

	public Optional<Hermandad> porNombre(String nombre) {
		return hermandades.stream().filter(h -> h.nombre().equalsIgnoreCase(nombre)).findFirst();
	}

	public boolean nombreUsado(String nombre) {
		return porNombre(nombre).isPresent();
	}

	public void agregar(Hermandad hermandad) {
		hermandades.add(hermandad);
		setDirty();
	}

	public void agregarMiembro(Hermandad hermandad, UUID uuid, String nombre) {
		hermandad.miembros.add(new ManuscritoDatos.Inscrito(uuid, nombre));
		setDirty();
	}

	public void setColor(Hermandad hermandad, int color) {
		hermandad.color = color & 0xFFFFFF;
		setDirty();
	}

	public static final int MAXIMO_ESTANDARTES = 27;

	public boolean guardarEstandarte(Hermandad hermandad, net.minecraft.world.item.ItemStack estandarte) {
		if (hermandad.estandartes.size() >= MAXIMO_ESTANDARTES) return false;
		hermandad.estandartes.add(estandarte.copyWithCount(1));
		setDirty();
		return true;
	}

	/** Lo saca de la colección (y lo devuelve); si era el activo, ya no hay activo. */
	public net.minecraft.world.item.ItemStack sacarEstandarte(Hermandad hermandad, int lugar) {
		if (lugar < 0 || lugar >= hermandad.estandartes.size()) return net.minecraft.world.item.ItemStack.EMPTY;
		net.minecraft.world.item.ItemStack sacado = hermandad.estandartes.remove(lugar);
		if (hermandad.activo == lugar) hermandad.activo = -1;
		else if (hermandad.activo > lugar) hermandad.activo--;
		setDirty();
		return sacado;
	}

	public void activarEstandarte(Hermandad hermandad, int lugar) {
		hermandad.activo = lugar >= 0 && lugar < hermandad.estandartes.size() ? lugar : -1;
		setDirty();
	}

	/** Cuántas líneas del chat se guardan. */
	public static final int LINEAS_CHAT = 100;

	public void agregarChat(Hermandad hermandad, HermandadChatPayload.Linea linea) {
		hermandad.chat.add(linea);
		while (hermandad.chat.size() > LINEAS_CHAT) hermandad.chat.remove(0);
		setDirty();
	}

	public void quitarMiembro(Hermandad hermandad, UUID uuid) {
		hermandad.miembros.removeIf(m -> m.uuid().equals(uuid));
		hermandad.lideres.remove(uuid);
		setDirty();
	}

	public void setLider(Hermandad hermandad, UUID uuid, boolean lider) {
		if (lider) hermandad.lideres.add(uuid);
		else hermandad.lideres.remove(uuid);
		setDirty();
	}

	/** Cuántos anuncios se guardan. */
	public static final int MAXIMO_ANUNCIOS = 30;

	public void publicarAnuncio(Hermandad hermandad, HermandadTablonPayload.Anuncio anuncio) {
		hermandad.anuncios.add(0, anuncio);
		while (hermandad.anuncios.size() > MAXIMO_ANUNCIOS) hermandad.anuncios.remove(hermandad.anuncios.size() - 1);
		setDirty();
	}

	public boolean borrarAnuncio(Hermandad hermandad, int lugar) {
		if (lugar < 0 || lugar >= hermandad.anuncios.size()) return false;
		hermandad.anuncios.remove(lugar);
		setDirty();
		return true;
	}

	private static HermandadesData cargar(CompoundTag tag, HolderLookup.Provider registries) {
		HermandadesData data = new HermandadesData();
		for (Tag t : tag.getList("hermandades", Tag.TAG_COMPOUND)) {
			CompoundTag h = (CompoundTag) t;
			List<ManuscritoDatos.Inscrito> miembros = new ArrayList<>();
			for (Tag m : h.getList("miembros", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) m;
				miembros.add(new ManuscritoDatos.Inscrito(c.getUUID("uuid"), c.getString("nombre")));
			}
			Hermandad hermandad = new Hermandad(h.getString("nombre"), h.getUUID("maestro"), miembros);
			hermandad.balance = h.getLong("balance");
			hermandad.nivelBanco = Math.max(1, h.getInt("nivelBanco"));
			hermandad.motd = h.getString("motd");
			hermandad.color = h.contains("color") ? h.getInt("color") : 0xFFFFFF;
			for (Tag l : h.getList("lideres", Tag.TAG_INT_ARRAY)) {
				hermandad.lideres.add(net.minecraft.nbt.NbtUtils.loadUUID(l));
			}
			for (Tag a : h.getList("anuncios", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) a;
				hermandad.anuncios.add(new HermandadTablonPayload.Anuncio(c.getString("fecha"), c.getString("autor"), c.getString("texto")));
			}
			for (Tag e : h.getList("estandartes", Tag.TAG_COMPOUND)) {
				net.minecraft.world.item.ItemStack.parse(registries, e).ifPresent(hermandad.estandartes::add);
			}
			hermandad.activo = h.contains("activo") ? h.getInt("activo") : -1;
			if (hermandad.activo >= hermandad.estandartes.size()) hermandad.activo = -1;
			for (Tag l : h.getList("chat", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) l;
				hermandad.chat.add(new HermandadChatPayload.Linea(c.getString("hora"), c.getString("autor"), c.getString("texto")));
			}
			data.hermandades.add(hermandad);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag lista = new ListTag();
		for (Hermandad hermandad : hermandades) {
			CompoundTag h = new CompoundTag();
			h.putString("nombre", hermandad.nombre);
			h.putUUID("maestro", hermandad.maestro);
			h.putLong("balance", hermandad.balance);
			h.putInt("nivelBanco", hermandad.nivelBanco);
			h.putString("motd", hermandad.motd);
			h.putInt("color", hermandad.color);
			ListTag miembros = new ListTag();
			for (ManuscritoDatos.Inscrito m : hermandad.miembros) {
				CompoundTag c = new CompoundTag();
				c.putUUID("uuid", m.uuid());
				c.putString("nombre", m.nombre());
				miembros.add(c);
			}
			h.put("miembros", miembros);
			ListTag chat = new ListTag();
			for (HermandadChatPayload.Linea linea : hermandad.chat) {
				CompoundTag c = new CompoundTag();
				c.putString("hora", linea.hora());
				c.putString("autor", linea.autor());
				c.putString("texto", linea.texto());
				chat.add(c);
			}
			h.put("chat", chat);
			ListTag lideres = new ListTag();
			for (UUID l : hermandad.lideres) lideres.add(net.minecraft.nbt.NbtUtils.createUUID(l));
			h.put("lideres", lideres);
			ListTag anuncios = new ListTag();
			for (HermandadTablonPayload.Anuncio a : hermandad.anuncios) {
				CompoundTag c = new CompoundTag();
				c.putString("fecha", a.fecha());
				c.putString("autor", a.autor());
				c.putString("texto", a.texto());
				anuncios.add(c);
			}
			h.put("anuncios", anuncios);
			ListTag estandartes = new ListTag();
			for (net.minecraft.world.item.ItemStack e : hermandad.estandartes) estandartes.add(e.save(registries));
			h.put("estandartes", estandartes);
			h.putInt("activo", hermandad.activo);
			lista.add(h);
		}
		tag.put("hermandades", lista);
		return tag;
	}
}
