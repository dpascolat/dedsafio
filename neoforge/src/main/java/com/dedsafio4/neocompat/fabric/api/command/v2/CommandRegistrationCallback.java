package com.dedsafio4.neocompat.fabric.api.command.v2;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public interface CommandRegistrationCallback {
	Event<CommandRegistrationCallback> EVENT = new Event<>();

	void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment);
}
