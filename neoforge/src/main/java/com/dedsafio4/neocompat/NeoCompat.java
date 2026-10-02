package com.dedsafio4.neocompat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** (Sólo cliente.) Reemplaza a MenuScreens.register, que en NeoForge va en RegisterMenuScreensEvent. */
public final class NeoCompat {
	private NeoCompat() {}

	static final List<Consumer<RegisterMenuScreensEvent>> PANTALLAS = new ArrayList<>();

	public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void pantallaDeMenu(MenuType<? extends M> tipo,
			MenuScreens.ScreenConstructor<M, U> constructor) {
		PANTALLAS.add(e -> e.register(tipo, constructor));
	}
}
