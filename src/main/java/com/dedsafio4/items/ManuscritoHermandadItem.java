package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.hermandad.ManuscritoDatos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/** Manuscrito para fundar una Hermandad: clic derecho abre el pergamino para inscribirse y crearla. */
public class ManuscritoHermandadItem extends Item {
	private static final Style ICONOS = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos"));
	private static final String ICONO_DEDITA = String.valueOf((char) 0xE002);

	private static final Style GRIS = Style.EMPTY.withColor(ChatFormatting.GRAY);
	private static final Style VERDE = Style.EMPTY.withColor(ChatFormatting.GREEN);
	private static final Style NARANJA = Style.EMPTY.withColor(ChatFormatting.GOLD);
	private static final Style CELESTE = Style.EMPTY.withColor(ChatFormatting.AQUA);
	private static final Style AMARILLO = Style.EMPTY.withColor(ChatFormatting.YELLOW);
	private static final Style VERDE_OLIVA = Style.EMPTY.withColor(TextColor.fromRgb(0xA8C66C));

	/** Lo completa el cliente: abre la pantalla del manuscrito para la mano indicada. */
	public static Consumer<InteractionHand> abrirPantalla = mano -> {};

	/** Inscritos necesarios para crear la Hermandad (3 el normal, 1 el de admin). */
	private final int minimoInscritos;
	private final boolean admin;

	public ManuscritoHermandadItem(Properties propiedades, int minimoInscritos, boolean admin) {
		super(propiedades);
		this.minimoInscritos = minimoInscritos;
		this.admin = admin;
	}

	/** true para el Manuscrito normal y para el de admin. */
	public static boolean es(ItemStack stack) {
		return stack.getItem() instanceof ManuscritoHermandadItem;
	}

	public static int minimoInscritos(ItemStack stack) {
		return stack.getItem() instanceof ManuscritoHermandadItem m ? m.minimoInscritos : ManuscritoDatos.MINIMO_INSCRITOS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		if (level.isClientSide()) abrirPantalla.accept(mano);
		return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), level.isClientSide());
	}

	@Override
	public Component getName(ItemStack stack) {
		return super.getName(stack).copy().withStyle(ChatFormatting.GREEN);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> lineas, TooltipFlag flag) {
		if (admin) {
			lineas.add(texto("Versión de admin: solo necesita " + minimoInscritos + " inscrito.",
					Style.EMPTY.withColor(ChatFormatting.RED)));
		}
		lineas.add(texto("Valor de compra: ", NARANJA).append(texto("1 ", Style.EMPTY.withColor(ChatFormatting.WHITE)))
				.append(texto(ICONO_DEDITA, ICONOS)).append(texto(" por 1", NARANJA)));
		lineas.add(Component.empty());

		lineas.add(texto("Utilízalo para crear tu propia", GRIS));
		lineas.add(texto("Hermandad. Las Hermandades", GRIS));
		lineas.add(texto("cuentan con:", GRIS));
		lineas.add(Component.empty());

		for (String ventaja : new String[]{"Banco Compartido", "Capas Personalizables", "Waypoints Propios",
				"Tablón de Anuncios", "Entre otras características."}) {
			lineas.add(texto("◆ " + ventaja, VERDE));
		}
		lineas.add(Component.empty());

		String miembros = minimoInscritos + (minimoInscritos == 1 ? " Miembro" : " Miembros");
		lineas.add(texto("Requiere que al menos ", GRIS).append(texto(miembros, NARANJA)));
		lineas.add(texto("firmen el Manuscrito para fundar", GRIS));
		lineas.add(texto("una Hermandad. Pulsa ", GRIS).append(texto("H", NARANJA)).append(texto(" para acceder", GRIS)));
		lineas.add(texto("a la Interfaz de Hermandades.", GRIS));
		lineas.add(Component.empty());

		lineas.add(texto("Una vez hayan firmado, el ", GRIS).append(texto("Maestro", CELESTE)));
		lineas.add(texto("de la Hermandad", CELESTE).append(texto(" debe utilizar", GRIS)));
		lineas.add(texto("el comando ", GRIS).append(texto("/guild found", NARANJA)).append(texto(" para", GRIS)));
		lineas.add(texto("crearla.", GRIS));
		lineas.add(Component.empty());

		lineas.add(texto("⚠ Atención: Solamente es necesario UN", AMARILLO));
		lineas.add(texto("Manuscrito para crear una Hermandad.", AMARILLO));
		lineas.add(Component.empty());

		lineas.add(texto("Puedes adquirirlo en las ", GRIS).append(texto("Nutrias", VERDE_OLIVA)));
		lineas.add(texto("de Piedra de la Montaña.", VERDE_OLIVA));
	}

	private static MutableComponent texto(String texto, Style estilo) {
		return Component.literal(texto).withStyle(estilo);
	}
}
