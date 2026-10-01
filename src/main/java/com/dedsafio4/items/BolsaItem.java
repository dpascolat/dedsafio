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
 * Bolsa Primitiva: una bolsa atada con el mismo espacio que un cofre simple (27 lugares).
 * Con click derecho se abre; lo que tiene adentro se guarda en la bolsa misma.
 */
public class BolsaItem extends Item {
	public static final int ESPACIOS = 27;

	public BolsaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack bolsa = jugador.getItemInHand(mano);
		if (jugador instanceof ServerPlayer servidor) {
			// El lugar del inventario donde está la bolsa: mientras está abierta no se puede mover.
			int lugar = mano == InteractionHand.MAIN_HAND ? jugador.getInventory().selected : Inventory.SLOT_OFFHAND;
			servidor.openMenu(new SimpleMenuProvider((id, inventario, quien) -> new Menu(id, inventario, bolsa, lugar),
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
		texto.add(Component.literal("Utilízala para guardar objetos sin").withColor(GRIS));
		texto.add(Component.literal("ocupar lugares de tu inventario.").withColor(GRIS));
	}

	/** El menú de la bolsa: igual que el de un cofre simple, para que se vea con la pantalla de siempre. */
	private static final class Menu extends ChestMenu {
		private final ItemStack bolsa;
		private final int lugar;

		Menu(int id, Inventory inventario, ItemStack bolsa, int lugar) {
			this(id, inventario, bolsa, lugar, cargar(bolsa));
		}

		private Menu(int id, Inventory inventario, ItemStack bolsa, int lugar, SimpleContainer contenido) {
			super(MenuType.GENERIC_9x3, id, inventario, contenido, 3);
			this.bolsa = bolsa;
			this.lugar = lugar;
			// Cada cambio se guarda en la bolsa al momento.
			contenido.addListener(c -> guardar(bolsa, (SimpleContainer) c));
			// No se puede meter una bolsa adentro de otra.
			for (int i = 0; i < ESPACIOS; i++) {
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

		private static SimpleContainer cargar(ItemStack bolsa) {
			SimpleContainer contenido = new SimpleContainer(ESPACIOS);
			NonNullList<ItemStack> cosas = NonNullList.withSize(ESPACIOS, ItemStack.EMPTY);
			bolsa.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(cosas);
			for (int i = 0; i < ESPACIOS; i++) contenido.setItem(i, cosas.get(i));
			return contenido;
		}

		private static void guardar(ItemStack bolsa, SimpleContainer contenido) {
			bolsa.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contenido.getItems()));
		}

		/** El lugar del inventario del jugador en este menú (el inventario va después de los 27 de la bolsa). */
		private int lugarEnMenu() {
			if (lugar == Inventory.SLOT_OFFHAND) return -1;   // la mano secundaria no está en el menú
			return ESPACIOS + 27 + lugar;                     // la barra va al final
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
