package com.existingeevee.simmerjockey;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.dhanantry.scapeandrunparasites.entity.monster.infected.EntityInfHuman;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeModContainer;

public class ItemDebugTool extends Item {

	public ItemDebugTool() {
		super();
		this.setRegistryName("debugtool");
		this.setTranslationKey("debugtool");
		this.setMaxStackSize(1);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (debugFunction(worldIn, playerIn)) {
			playerIn.getCooldownTracker().setCooldown(this, 5);
			return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
		}

		if (worldIn.isRemote) {
			if (playerIn.isSneaking()) {
				List<Entity> entities = worldIn.getEntitiesWithinAABBExcludingEntity(playerIn, playerIn.getEntityBoundingBox().expand(5.0D, 5.0D, 5.0D).expand(-5, -5, -5));
				Pair<Entity, Double> dval = Pair.of(null, Double.MAX_VALUE);
				for (Entity e : entities) {
					double distance = e.getPositionVector().distanceTo(playerIn.getPositionVector());
					if (distance < dval.getRight()) {
						dval = Pair.of(e, distance);
					}
				}
				if (dval.getLeft() != null) {
					playerIn.sendMessage(new TextComponentString(dval.getLeft().serializeNBT().toString()));
				}
			} else {
				playerIn.sendMessage(new TextComponentString(playerIn.getHeldItem(handIn.equals(EnumHand.MAIN_HAND) ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND).serializeNBT().toString()));
			}

		}

		playerIn.getCooldownTracker().setCooldown(this, 5);
		return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
	}

	protected boolean debugFunction(World worldIn, EntityPlayer playerIn) { // this is used by me to test stuff.
		if (worldIn.isRemote)
			return true;
		List<Entity> entities = worldIn.getEntitiesWithinAABBExcludingEntity(playerIn, playerIn.getEntityBoundingBox().expand(5.0D, 5.0D, 5.0D).expand(-5, -5, -5));
		Pair<Entity, Double> dval = Pair.of(null, Double.MAX_VALUE);
		for (Entity e : entities) {
			double distance = e.getPositionVector().distanceTo(playerIn.getPositionVector());
			if (distance < dval.getRight()) {
				dval = Pair.of(e, distance);
			}
		}
		if (dval.getLeft() instanceof EntityInfHuman) {
			ForgeModContainer.zombieBabyChance *= 10000;
			Simmerjockey.infHumanSpawnHook((EntityInfHuman) dval.getLeft(), worldIn.getDifficultyForLocation(dval.getLeft().getPosition()));
			ForgeModContainer.zombieBabyChance /= 10000;
		}
		return true;
	}
}
