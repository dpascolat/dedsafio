package com.dedsafio4.items;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * Bolsas: la Bolsa Primitiva (3 filas, 27 lugares, como un cofre simple) y el Saco (4 filas, 36 lugares: 9 más).
 * Con click derecho se abre; lo que tiene adentro se guarda en la bolsa misma.
 */
public class BolsaItem extends Item {
	/** Cuántas filas de 9 tiene. */
	private final int filas;
	/** "a" para la bolsa, "o" para el saco (en la descripción: "Utilízala" / "Utilízalo"). */
	private final String terminacion;

	public BolsaItem(Properties propiedades) {
		this(propiedades, 3, "a");
	}

	public BolsaItem(Properties propiedades, int filas, String terminacion) {
		super(propiedades);
		this.filas = filas;
		this.terminacion = terminacion;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack bolsa = jugador.getItemInHand(mano);
		if (jugador instanceof ServerPlayer servidor) {
			// El lugar del inventario donde está la bolsa: mientras está abierta no se puede mover.
			int lugar = mano == InteractionHand.MAIN_HAND ? jugador.getInventory().selected : Inventory.SLOT_OFFHAND;
			servidor.openMenu(new SimpleMenuProvider((id, inventario, quien) -> new Menu(id, inventario, bolsa, lugar, filas),
					bolsa.getHoverName()));
			level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1f, 0.8f);
		}
		return InteractionResultHolder.sidedSuccess(bolsa, level.isClientSide);
	}

	/** Si la bolsa se quema o se rompe tirada en el piso, deja caer lo que tenía. */
	@Override
	public void onDestroyed(ItemEntity entidad) {
		ItemContainerContents contenido = entidad.getItem().get(DataComponents.CONTAINER);
		if (contenido != null) {
			entidad.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
			ItemUtils.onContainerDestroyed(entidad, contenido.nonEmptyItemsCopy());
		}
	}

	private static final int VIOLETA = 0xC77DF0, GRIS = 0xC6C6C6;

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId())
				.withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(VIOLETA));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, java.util.List<Component> texto,
								net.minecraft.world.item.TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Utilíza" + (terminacion.equals("o") ? "lo" : "la") + " para guardar objetos sin").withColor(GRIS));
		texto.add(Component.literal("ocupar lugares de tu inventario.").withColor(GRIS));
	}

	/** El menú de la bolsa: igual que el de un cofre, para que se vea con la pantalla de siempre. */
	private static final class Menu extends ChestMenu {
		private final ItemStack bolsa;
		private final int lugar;
		private final int espacios;

		Menu(int id, Inventory inventario, ItemStack bolsa, int lugar, int filas) {
			this(id, inventario, bolsa, lugar, filas, cargar(bolsa, filas * 9));
		}

		private Menu(int id, Inventory inventario, ItemStack bolsa, int lugar, int filas, SimpleContainer contenido) {
			super(filas == 4 ? MenuType.GENERIC_9x4 : MenuType.GENERIC_9x3, id, inventario, contenido, filas);
			this.bolsa = bolsa;
			this.lugar = lugar;
			this.espacios = filas * 9;
			// Cada cambio se guarda en la bolsa al momento.
			contenido.addListener(c -> guardar(bolsa, (SimpleContainer) c));
			// No se puede meter una bolsa adentro de otra.
			for (int i = 0; i < espacios; i++) {
				Slot viejo = this.slots.get(i);
				Slot nuevo = new Slot(contenido, i, viejo.x, viejo.y) {
					@Override
					public boolean mayPlace(ItemStack pila) {
						return !(pila.getItem() instanceof BolsaItem);
					}
				};
				nuevo.index = i;
				this.slots.set(i, nuevo);
			}
		}

		private static SimpleContainer cargar(ItemStack bolsa, int espacios) {
			SimpleContainer contenido = new SimpleContainer(espacios);
			NonNullList<ItemStack> cosas = NonNullList.withSize(espacios, ItemStack.EMPTY);
			bolsa.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(cosas);
			for (int i = 0; i < espacios; i++) contenido.setItem(i, cosas.get(i));
			return contenido;
		}

		private static void guardar(ItemStack bolsa, SimpleContainer contenido) {
			bolsa.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contenido.getItems()));
		}

		/** El lugar del inventario del jugador en este menú (el inventario va después de los lugares de la bolsa). */
		private int lugarEnMenu() {
			if (lugar == Inventory.SLOT_OFFHAND) return -1;   // la mano secundaria no está en el menú
			return espacios + 27 + lugar;                     // la barra va al final
		}

		@Override
		public void clicked(int lugarClick, int boton, ClickType tipo, Player jugador) {
			// La bolsa abierta no se puede agarrar, mover ni cambiar con los números o con F.
			if (lugarClick >= 0 && lugarClick == lugarEnMenu()) return;
			if (tipo == ClickType.SWAP && (boton == lugar || (boton == 40 && lugar == Inventory.SLOT_OFFHAND))) return;
			super.clicked(lugarClick, boton, tipo, jugador);
		}

		@Override
		public ItemStack quickMoveStack(Player jugador, int lugarClick) {
			if (lugarClick == lugarEnMenu()) return ItemStack.EMPTY;
			return super.quickMoveStack(jugador, lugarClick);
		}

		@Override
		public boolean stillValid(Player jugador) {
			ItemStack enLugar = lugar == Inventory.SLOT_OFFHAND ? jugador.getOffhandItem() : jugador.getInventory().getItem(lugar);
			return enLugar == bolsa;
		}

		@Override
		public void removed(Player jugador) {
			super.removed(jugador);
			if (!jugador.level().isClientSide) {
				jugador.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
						SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 1f, 0.8f);
			}
		}
	}
}
