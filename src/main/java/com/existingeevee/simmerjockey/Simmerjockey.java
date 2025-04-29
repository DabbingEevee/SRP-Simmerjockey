package com.existingeevee.simmerjockey;

import java.util.List;
import java.util.UUID;

import com.dhanantry.scapeandrunparasites.client.model.entity.infected.ModelInfHuman;
import com.dhanantry.scapeandrunparasites.entity.monster.crude.EntityInhooS;
import com.dhanantry.scapeandrunparasites.entity.monster.infected.EntityInfHuman;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.world.DifficultyInstance;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = Simmerjockey.MODID)
public class Simmerjockey {
	public static final String MODID = "simmerjockey";

	@Mod.EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		System.out.println("SIMMER JOCKEY!  - sim adventurer probably");

		// IDC i want my debug tool grrrr
		if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
			ForgeRegistries.ITEMS.register(new ItemDebugTool());
		}

		// Register the main class to event bus
		MinecraftForge.EVENT_BUS.register(Simmerjockey.class);
	}

	protected static final AttributeModifier CHILD_SPEED = new AttributeModifier(UUID.nameUUIDFromBytes("child".getBytes()), "child", 0.75, 1);
	protected static final AttributeModifier RIDDEN_SPEED = new AttributeModifier(UUID.nameUUIDFromBytes("ridden".getBytes()), "ridden", 1.25, 1);

	@SubscribeEvent
	public static void onLivingUpdate(LivingUpdateEvent ev) {
		// Get the entity. simple enough.
		EntityLivingBase entity = ev.getEntityLiving();

		// If the entity is a sim human
		if (entity instanceof IInfHumanExt) {

			// get the attribute instance
			IAttributeInstance attr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

			// Store the states
			boolean isChild = entity.isChild();
			boolean hasModifier = attr.hasModifier(CHILD_SPEED);

			// if it is a child and lacking the modifier, add it. if its not a child and has the modifier, remove it.
			if (!isChild && hasModifier) {
				attr.removeModifier(CHILD_SPEED);
			} else if (isChild && !hasModifier) {
				attr.applyModifier(CHILD_SPEED);
			}

		} else if (entity instanceof EntityInhooS) { // If the entity is a small inc form

			// Get riding entities
			List<Entity> passengers = entity.getPassengers();

			// get the attribute instance
			IAttributeInstance attr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

			// Store the states
			boolean riddenByChild = !passengers.isEmpty() && passengers.get(0) instanceof IInfHumanExt && ((EntityLivingBase) passengers.get(0)).isChild();
			boolean hasModifier = attr.hasModifier(RIDDEN_SPEED);

			// if it is being ridden by a child and lacking the modifier, add it. if its not being ridden by a child and has the modifier, remove it.
			if (!riddenByChild && hasModifier) {
				attr.removeModifier(RIDDEN_SPEED);
			} else if (riddenByChild && !hasModifier) {
				attr.applyModifier(RIDDEN_SPEED);
			}
		}
	}

	public static void infHumanSpawnHook(EntityInfHuman entity, DifficultyInstance difficulty) {
		// this is the same probability as a vanilla chicken jockey:tm: spawning
		if (entity.world.rand.nextFloat() < 0.05D * ForgeModContainer.zombieBabyChance) {
			// make the infected human a child
			((IInfHumanExt) entity).setChildState(true);

			// spawn the "chicken" in
			EntityInhooS formerChicken = new EntityInhooS(entity.world);
			formerChicken.setCustomNameTag("Former Chicken");
			formerChicken.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, 0.0F);
			formerChicken.onInitialSpawn(difficulty, (IEntityLivingData) null);
			entity.world.spawnEntity(formerChicken);

			// TODO make baby human faster and buff the chicken

			// force the human to ride it
			entity.startRiding(formerChicken);
		}
	}

	@SideOnly(Side.CLIENT)
	public static boolean simHumanRenderHook(ModelInfHuman model, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		// We only care if its a child. otherwise let vanilla srp handle rendering
		if (model.isChild) {
			// For the sake of rendering without a recursive mess, we set this to false temporarily
			model.isChild = false;

			// move the little fucker down since he a little short
			GlStateManager.translate(0, 0.75, 0);

			// push the matrix
			GlStateManager.pushMatrix();

			// scale it down
			GlStateManager.scale(0.5, 0.5, 0.5);

			// render the model without the head
			model.jointH.isHidden = true;
			model.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
			model.jointH.isHidden = false;

			// unpush the matrix. were done with rendering the small bits of the model
			GlStateManager.popMatrix();

			// render the head at regular size
			model.jointH.render(scale);

			// set this back to avoid issues
			model.isChild = true;
			return true;
		}
		return false;
	}

	@SideOnly(Side.CLIENT)
	public static void simHumanRotationHook(ModelInfHuman model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
		// These are not actually reset fsr.
		model.jointRL.rotateAngleY = 0;
		model.jointLL.rotateAngleY = 0;

		// They look up a little too much. Move it down a notch
		if (model.isChild) {
			model.jointH.rotateAngleX += Math.PI / 5;
		}

		// Only if it is riding smt
		if (model.isRiding) {
			// Move the left leg up and to the left
			model.jointLL.rotateAngleX = (float) (-Math.PI / 2);
			model.jointLL.rotateAngleY = (float) (-Math.PI / 10);

			// Move the right leg up and to the right
			model.jointRL.rotateAngleX = (float) (-Math.PI / 2);
			model.jointRL.rotateAngleY = (float) (Math.PI / 10);

			// make the right arm not shake as much and move it up
			model.jointRA.rotateAngleX /= 3;
			model.jointRA.rotateAngleX -= Math.PI / 2;

			// make the left arm not shake as much and move it up
			model.jointLA.rotateAngleX /= 3;
			model.jointLA.rotateAngleX -= Math.PI / 2;
		}
	}
}
