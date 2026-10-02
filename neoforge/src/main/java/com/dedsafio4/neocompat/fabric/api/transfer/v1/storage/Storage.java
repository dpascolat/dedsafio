package com.dedsafio4.neocompat.fabric.api.transfer.v1.storage;

/** Sólo lo que usa el mod: "un almacén vacío" (para que las tolvas no saquen de un cofre con candado). */
public interface Storage<T> {
	Storage<?> VACIO = new Storage<Object>() {};

	@SuppressWarnings("unchecked")
	static <T> Storage<T> empty() {
		return (Storage<T>) VACIO;
	}
}
