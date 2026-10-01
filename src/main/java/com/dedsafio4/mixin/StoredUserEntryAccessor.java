package com.dedsafio4.mixin;

import net.minecraft.server.players.StoredUserEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para leer a quién corresponde cada entrada de la lista de baneados (Minecraft lo tiene escondido). */
@Mixin(StoredUserEntry.class)
public interface StoredUserEntryAccessor {
	@Invoker("getUser")
	Object dedsafio4$usuario();
}
