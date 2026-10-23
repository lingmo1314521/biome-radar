package com.lingmo.biomeradar.gui;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuiWrapper {
	
	public static void openGUI(Level level, Player player, ItemStack stack) {
		Minecraft.getInstance().setScreen(new ExplorersCompassScreen(level, player, stack, (ExplorersCompassItem) stack.getItem(), ExplorersCompass.allowedStructureKeys));
	}

}
