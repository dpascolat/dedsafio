package com.dedsafio4.marcas;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Guardado en el mundo: si los admins pueden usar el minimapa. Por defecto no: nadie lo usa. */
public class MinimapaData extends SavedData {
	public static final SavedData.Factory<MinimapaData> FACTORY =
			new SavedData.Factory<>(MinimapaData::new, MinimapaData::cargar, null);

	private boolean adminsLoUsan;

	public boolean adminsLoUsan() {
		return adminsLoUsan;
	}

	public void setAdminsLoUsan(boolean valor) {
		adminsLoUsan = valor;
		setDirty();
	}

	private static MinimapaData cargar(CompoundTag tag, HolderLookup.Provider registros) {
		MinimapaData data = new MinimapaData();
		data.adminsLoUsan = tag.getBoolean("admins");
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
		tag.putBoolean("admins", adminsLoUsan);
		return tag;
	}
}
