package com.dedsafio4.neocompat;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * En NeoForge las inyecciones del mod no son obligatorias (NeoForge cambia partes de Minecraft y alguna puede no
 * encajar). Para enterarse cuáles no encajaron: después de aplicar cada mixin, busca los métodos de inyección del mod
 * que nadie llama en la clase y los avisa en el log ("INYECCIÓN FALLIDA").
 */
public class DiagnosticoMixins implements IMixinConfigPlugin {
	private static final Logger LOG = LoggerFactory.getLogger("dedsafio4/neoforge");
	private static final String[] PREFIJOS = {"handler$", "redirect$", "modify$", "localvar$", "constant$", "wrapOperation$",
			"modifyExpressionValue$", "modifyReturnValue$", "wrapWithCondition$", "modifyReceiver$", "wrapMethod$", "args$"};

	@Override
	public void onLoad(String paquete) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String clase, String mixin) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> mias, Set<String> otras) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String clase, ClassNode nodo, String mixin, IMixinInfo info) {}

	@Override
	public void postApply(String clase, ClassNode nodo, String mixin, IMixinInfo info) {
		try {
			Set<String> llamados = new HashSet<>();
			for (MethodNode m : nodo.methods) {
				for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
					if (i instanceof MethodInsnNode llamada && llamada.owner.equals(nodo.name)) llamados.add(llamada.name);
					if (i instanceof InvokeDynamicInsnNode indy) {
						for (Object arg : indy.bsmArgs) if (arg instanceof Handle h && h.getOwner().equals(nodo.name)) llamados.add(h.getName());
					}
				}
			}
			for (MethodNode m : nodo.methods) {
				if (!m.name.contains("dedsafio4$") || llamados.contains(m.name)) continue;
				for (String prefijo : PREFIJOS) {
					if (m.name.startsWith(prefijo)) {
						LOG.warn("INYECCIÓN FALLIDA: {} en {} ({})", m.name, clase, mixin);
						break;
					}
				}
			}
		} catch (Throwable t) {
			LOG.warn("No se pudo revisar {}: {}", mixin, t.toString());
		}
	}
}
