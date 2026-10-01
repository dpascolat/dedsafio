package com.dedsafio4.nave;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Function;

/**
 * Una parte de la nave (cabina, motor o alerones) como ítem: se ve con su modelo 3D (GeckoLib), en el inventario, en
 * la mano, en el piso y en los marcos. Por ahora no hace nada más.
 */
public class ParteNaveItem extends Item implements GeoItem {
	/**
	 * Lo pone el cliente al arrancar: arma el dibujo de cada parte (un GeoRenderProvider de GeckoLib; acá va como
	 * Object porque esta parte del mod también corre en el servidor, que no dibuja nada).
	 */
	public static Function<ParteNaveItem, Object> dibujo;

	/** El nombre del modelo: geo/item/<modelo>.geo.json. */
	public final String modelo;
	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	private Object proveedor;

	/** El color del nombre y la descripción (puede no tener). */
	private final int colorNombre;
	private final java.util.function.Consumer<java.util.List<net.minecraft.network.chat.Component>> descripcion;

	public ParteNaveItem(String modelo, Properties propiedades) {
		this(modelo, propiedades, com.dedsafio4.items.DescritoItem.CELESTE, null);
	}

	public ParteNaveItem(String modelo, Properties propiedades, int colorNombre,
						 java.util.function.Consumer<java.util.List<net.minecraft.network.chat.Component>> descripcion) {
		super(propiedades);
		this.modelo = modelo;
		this.colorNombre = colorNombre;
		this.descripcion = descripcion;
	}

	@Override
	public void appendHoverText(net.minecraft.world.item.ItemStack item, TooltipContext contexto,
								java.util.List<net.minecraft.network.chat.Component> texto, net.minecraft.world.item.TooltipFlag bandera) {
		if (descripcion == null) return;
		texto.add(net.minecraft.network.chat.Component.empty());
		descripcion.accept(texto);
	}

	/** El nombre en color (celeste las partes), como en el catálogo (la G). */
	@Override
	public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack item) {
		return super.getName(item).copy().withStyle(com.dedsafio4.items.DescritoItem.color(colorNombre));
	}

	@Override
	public Object getRenderProvider() {
		if (proveedor == null && dibujo != null) proveedor = dibujo.apply(this);
		return proveedor;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
