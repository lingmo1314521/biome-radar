package com.lingmo.biomeradar.recipe;

import com.lingmo.biomeradar.items.ExplorersCompassItem;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * 增强雷达配方：群系雷达 + 地球雷达芯片 + 那美克星芯片 + 处理器 ×2 → 带 Enhanced 标记的群系雷达。
 */
public class EnhancedRadarRecipe extends ShapelessRecipe {

	public EnhancedRadarRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
		super(id, group, category, result, ingredients);
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		final ItemStack result = super.getResultItem(registryAccess);
		ExplorersCompassItem.setEnhanced(result, true);
		return result;
	}

	@Override
	public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
		final ItemStack result = super.assemble(container, registryAccess);
		ExplorersCompassItem.setEnhanced(result, true);
		return result;
	}

	public static class Serializer extends ShapelessRecipe.Serializer {
		@Override
		public EnhancedRadarRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json) {
			final ShapelessRecipe base = super.fromJson(id, json);
			return new EnhancedRadarRecipe(id, base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
		}
	}
}
