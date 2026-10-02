package com.dedsafio4.neocompat.fabric.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Un evento al estilo Fabric: se le registran oyentes y Puente los llama desde el evento de NeoForge que corresponde. */
public final class Event<T> {
	private final List<T> oyentes = new CopyOnWriteArrayList<>();

	public void register(T oyente) {
		oyentes.add(oyente);
	}

	public List<T> oyentes() {
		return oyentes;
	}
}
