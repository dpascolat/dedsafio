package com.dedsafio4.lagartos;

import com.dedsafio4.nave.ModEntidades;
import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * La lanza de hueso que tira el Reptisaurio con Lanza: vuela como un tridente, lastima el doble del golpe del que la
 * tira (6 = 3 corazones; si se le cambia el daño con /dañomob, la lanza también cambia) y deja el
 * Veneno Primitivo. No se puede juntar; clavada, desaparece a los 2 segundos. Se dibuja con la lanza del modelo del
 * lancero (LanzaLanzadaRenderer).
 */
public class LanzaLanzadaEntity extends AbstractArrow {
	public LanzaLanzadaEntity(EntityType<? extends LanzaLanzadaEntity> tipo, Level level) {
		super(tipo, level);
		this.pickup = Pickup.DISALLOWED;
		this.setBaseDamage(6.0);
	}

	public LanzaLanzadaEntity(Level level, LivingEntity quien) {
		this(ModEntidades.LANZA_LANZADA, level);
		this.setOwner(quien);
		this.setBaseDamage(quien.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 2);
		this.setPos(quien.getX(), quien.getEyeY() - 0.2, quien.getZ());
	}

	@Override
	protected void onHitEntity(EntityHitResult golpe) {
		super.onHitEntity(golpe);
		if (!level().isClientSide && golpe.getEntity() instanceof LivingEntity victima && victima.isAlive()) VenenoPrimitivo.aplicar(victima);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && this.inGroundTime > 40) discard();
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.BONE);
	}
}
