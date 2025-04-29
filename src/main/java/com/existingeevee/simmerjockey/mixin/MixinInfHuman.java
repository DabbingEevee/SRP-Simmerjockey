package com.existingeevee.simmerjockey.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dhanantry.scapeandrunparasites.entity.monster.infected.EntityInfHuman;
import com.existingeevee.simmerjockey.IInfHumanExt;
import com.existingeevee.simmerjockey.Simmerjockey;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

@Mixin(EntityInfHuman.class)
public class MixinInfHuman extends EntityLiving implements IInfHumanExt {

	@Unique
	private static final DataParameter<Boolean> IS_CHILD = EntityDataManager.createKey(EntityInfHuman.class, DataSerializers.BOOLEAN);

	public MixinInfHuman(World worldIn) {
		super(worldIn);
	}

	@Inject(method = "entityInit()V", at = @At("TAIL"))
	void simmerjockey$entityInit(CallbackInfo ci) {
		this.dataManager.register(IS_CHILD, false);
	}

	@Unique
	@Override
	public void setChildState(boolean state) {
		this.dataManager.set(IS_CHILD, state);
		setChildSize(isChild());
	}

	@Unique
	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		if (IS_CHILD.equals(key)) {
			setChildSize(isChild());
		}

		super.notifyDataManagerChange(key);
	}

	@Inject(method = "writeEntityToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
	public void simmerjockey$writeEntityToNBT(NBTTagCompound compound, CallbackInfo ci) {
		compound.setBoolean("stupidfuckingchild", isChild());
	}

	@Inject(method = "readEntityFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
	public void simmerjockey$readEntityFromNBT(NBTTagCompound compound, CallbackInfo ci) {
		super.readEntityFromNBT(compound);
		this.setChildState(compound.getBoolean("stupidfuckingchild"));
	}

	@Unique
	@Override
	public boolean isChild() {
		return this.dataManager.get(IS_CHILD);
	}

	@Unique
	@Override
	public void setChildSize(boolean isChild) {
		if (!isChild) {
			super.setSize(0.6f, 1.95f);
		} else {
			super.setSize(0.3f, 0.975f);
		}
	}

	@Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
	public void simmerjockey$getEyeHeight(CallbackInfoReturnable<Float> ci) {
		if (this.isChild()) {
			ci.setReturnValue(ci.getReturnValueF() - 0.81F);
		}
	}

	@Inject(method = "onInitialSpawn(Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;", at = @At("RETURN"))
	public void simmerjockey$onInitialSpawn(final DifficultyInstance difficulty, final IEntityLivingData livingdata, CallbackInfoReturnable<IEntityLivingData> ci) {
		Simmerjockey.infHumanSpawnHook((EntityInfHuman) (Object) this, difficulty);
	}

}
