package com.existingeevee.simmerjockey.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dhanantry.scapeandrunparasites.client.model.entity.infected.ModelInfHuman;
import com.existingeevee.simmerjockey.Simmerjockey;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

@Mixin(ModelInfHuman.class)
public class MixinModelInfHuman extends ModelBase {
	// RenderInfHuman ModelZombie
	@Inject(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
	public void simmerjockey$render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
		if (Simmerjockey.simHumanRenderHook((ModelInfHuman) (Object) this, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale))
			ci.cancel();
	}

	@Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
	public void simmerjockey$setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
		Simmerjockey.simHumanRotationHook((ModelInfHuman) (Object) this, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
	}

}
