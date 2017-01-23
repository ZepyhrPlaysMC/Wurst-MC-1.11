/*
 * Copyright � 2014 - 2017 | Wurst-Imperium | All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.features.mods;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import tk.wurst_client.events.listeners.RenderListener;
import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.features.Feature;
import tk.wurst_client.utils.BlockUtils;
import tk.wurst_client.utils.InventoryUtils;
import tk.wurst_client.utils.RenderUtils;

@Mod.Info(description = "Allows you to see chests through walls.",
	name = "ChestESP",
	tags = "ChestFinder, chest esp, chest finder",
	help = "Mods/ChestESP")
@Mod.Bypasses
public class ChestEspMod extends Mod implements UpdateListener, RenderListener
{
	private final ArrayList<AxisAlignedBB> basicNew = new ArrayList<>();
	private final ArrayList<AxisAlignedBB> basicEmpty = new ArrayList<>();
	private final ArrayList<AxisAlignedBB> basicNotEmpty = new ArrayList<>();
	
	private final ArrayList<AxisAlignedBB> trappedNew = new ArrayList<>();
	private final ArrayList<AxisAlignedBB> trappedEmpty = new ArrayList<>();
	private final ArrayList<AxisAlignedBB> trappedNotEmpty = new ArrayList<>();
	
	private final ArrayList<AxisAlignedBB> specialEnder = new ArrayList<>();
	private final ArrayList<Entity> specialCart = new ArrayList<>();
	
	private int totalChests;
	
	private TileEntityChest openChest;
	private final LinkedHashSet<TileEntityChest> emptyChests =
		new LinkedHashSet<>();
	private final LinkedHashSet<TileEntityChest> nonEmptyChests =
		new LinkedHashSet<>();
	
	@Override
	public Feature[] getSeeAlso()
	{
		return new Feature[]{wurst.mods.autoStealMod, wurst.mods.itemEspMod,
			wurst.mods.searchMod, wurst.mods.xRayMod};
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + totalChests + " chests]";
	}
	
	@Override
	public void onEnable()
	{
		emptyChests.clear();
		nonEmptyChests.clear();
		
		wurst.events.add(UpdateListener.class, this);
		wurst.events.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		wurst.events.remove(UpdateListener.class, this);
		wurst.events.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// clear lists
		basicNew.clear();
		basicEmpty.clear();
		basicNotEmpty.clear();
		trappedNew.clear();
		trappedEmpty.clear();
		trappedNotEmpty.clear();
		specialEnder.clear();
		specialCart.clear();
		
		for(TileEntity tileEntity : mc.world.loadedTileEntityList)
		{
			// normal chests
			if(tileEntity instanceof TileEntityChest)
			{
				TileEntityChest chest = (TileEntityChest)tileEntity;
				
				// ignore other block in double chest
				if(chest.adjacentChestXPos != null
					|| chest.adjacentChestZPos != null)
					continue;
				
				// get hitbox
				AxisAlignedBB bb = BlockUtils.getBoundingBox(chest.getPos());
				
				// larger box for double chest
				if(chest.adjacentChestXNeg != null)
					bb = bb.union(BlockUtils
						.getBoundingBox(chest.adjacentChestXNeg.getPos()));
				else if(chest.adjacentChestZNeg != null)
					bb = bb.union(BlockUtils
						.getBoundingBox(chest.adjacentChestZNeg.getPos()));
				
				boolean trapped = chest.getChestType() == BlockChest.Type.TRAP;
				
				// add to appropriate list
				if(emptyChests.contains(chest))
				{
					if(trapped)
						trappedEmpty.add(bb);
					else
						basicEmpty.add(bb);
					
				}else if(nonEmptyChests.contains(chest))
				{
					if(trapped)
						trappedNotEmpty.add(bb);
					else
						basicNotEmpty.add(bb);
					
				}else
				{
					if(trapped)
						trappedNew.add(bb);
					else
						basicNew.add(bb);
				}
				
				continue;
			}
			
			// ender chests
			if(tileEntity instanceof TileEntityEnderChest)
			{
				AxisAlignedBB bb = BlockUtils.getBoundingBox(
					((TileEntityEnderChest)tileEntity).getPos());
				specialEnder.add(bb);
			}
		}
		
		// minecarts
		for(Entity entity : mc.world.loadedEntityList)
			if(entity instanceof EntityMinecartChest)
				specialCart.add(entity);
			
		// chest counter
		totalChests = basicNew.size() + basicEmpty.size() + basicNotEmpty.size()
			+ trappedNew.size() + trappedEmpty.size() + trappedNotEmpty.size()
			+ specialEnder.size() + specialCart.size();
	}
	
	@Override
	public void onRender()
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		GL11.glTranslated(-mc.getRenderManager().renderPosX,
			-mc.getRenderManager().renderPosY,
			-mc.getRenderManager().renderPosZ);
		
		// TODO: interpolation for minecarts
		
		GL11.glColor4f(0, 1, 0, 0.25F);
		basicNew.forEach((bb) -> RenderUtils.drawSolidBox(bb));
		specialCart.forEach((e) -> RenderUtils.drawSolidBox(e.boundingBox));
		
		GL11.glColor4f(0, 1, 0, 0.5F);
		basicNew.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		basicEmpty.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		basicNotEmpty.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		basicNotEmpty.forEach((bb) -> RenderUtils.drawCrossBox(bb));
		specialCart.forEach((e) -> RenderUtils.drawOutlinedBox(e.boundingBox));
		
		GL11.glColor4f(1, 0.5F, 0, 0.25F);
		trappedNew.forEach((bb) -> RenderUtils.drawSolidBox(bb));
		
		GL11.glColor4f(1, 0.5F, 0, 0.5F);
		trappedNew.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		trappedEmpty.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		trappedNotEmpty.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		trappedNotEmpty.forEach((bb) -> RenderUtils.drawCrossBox(bb));
		
		GL11.glColor4f(0, 1, 1, 0.25F);
		specialEnder.forEach((bb) -> RenderUtils.drawSolidBox(bb));
		
		GL11.glColor4f(0, 1, 1, 0.5F);
		specialEnder.forEach((bb) -> RenderUtils.drawOutlinedBox(bb));
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	public void openChest(BlockPos pos)
	{
		TileEntity tileEntity = mc.world.getTileEntity(pos);
		if(tileEntity instanceof TileEntityChest)
		{
			openChest = (TileEntityChest)tileEntity;
			if(openChest.adjacentChestXPos != null)
				openChest = openChest.adjacentChestXPos;
			if(openChest.adjacentChestZPos != null)
				openChest = openChest.adjacentChestZPos;
		}
	}
	
	public void closeChest(Container chest)
	{
		if(openChest == null)
			return;
		
		boolean empty = true;
		for(int i = 0; i < chest.inventorySlots.size() - 36; i++)
			if(!InventoryUtils
				.isEmptySlot(chest.inventorySlots.get(i).getStack()))
			{
				empty = false;
				break;
			}
		
		if(empty)
		{
			if(!emptyChests.contains(openChest))
				emptyChests.add(openChest);
			
			nonEmptyChests.remove(openChest);
		}else
		{
			if(!nonEmptyChests.contains(openChest))
				nonEmptyChests.add(openChest);
			
			emptyChests.remove(openChest);
		}
		
		openChest = null;
	}
}
