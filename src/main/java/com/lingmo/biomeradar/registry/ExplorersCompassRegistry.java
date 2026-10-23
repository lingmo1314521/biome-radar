package com.lingmo.biomeradar.registry;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@EventBusSubscriber(modid = ExplorersCompass.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ExplorersCompassRegistry {

	@SubscribeEvent
	public static void registerItems(RegisterEvent e) {
		e.register(ForgeRegistries.Keys.ITEMS, helper -> {
			ExplorersCompass.explorersCompass = new ExplorersCompassItem();
            helper.register(new ResourceLocation(ExplorersCompass.MODID, ExplorersCompassItem.NAME), ExplorersCompass.explorersCompass);
        });
		e.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, helper -> {
			helper.register(new ResourceLocation(ExplorersCompass.MODID, "enhanced_radar"), new com.lingmo.biomeradar.recipe.EnhancedRadarRecipe.Serializer());
		});
	}

}