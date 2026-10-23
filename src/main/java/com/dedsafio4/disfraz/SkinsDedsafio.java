package com.dedsafio4.disfraz;

import java.util.List;

/**
 * Los modelos del Skin Pack Dedsafío (hechos con Customizable Player Models): con /cambiarmob skin:<nombre> el
 * jugador se ve como ese modelo, con sus animaciones. Cada uno está en assets/dedsafio4/skins/<nombre>.json (las
 * piezas y las animaciones, convertidas del .cpmproject) y textures/entity/skins/<nombre>.png. Los hechos en
 * Blockbench están en skins/<nombre>.geo.json (con huesos armor_... para la armadura, ver SkinsGeo).
 */
public final class SkinsDedsafio {
	private SkinsDedsafio() {}

	public static final String ESPACIO = "skin";

	public static final List<String> NOMBRES = List.of("ajolote", "banquito", "churri", "diabli", "eon", "juansnutria", "mapache",
			"nec", "nec2", "nutria", "nutria2", "nutriaabeja", "nutriabetis", "nutriacalabaza", "nutriaconstructora", "nutriadetective", "nutriadj",
			"nutriaelegante", "nutriahoja", "nutriaia", "nutrianeitor", "nutriaplaya", "nutriawatona", "papuche",
			"papunutria", "parcenutria", "pe23", "ribbit", "sombrita", "unicornio");
}
