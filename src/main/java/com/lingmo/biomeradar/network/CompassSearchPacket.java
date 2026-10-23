package com.lingmo.biomeradar.network;

import java.util.function.Supplier;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.ItemUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class CompassSearchPacket {

	private ResourceLocation structureOrGroupKey;
	private boolean isGroup;

	public CompassSearchPacket() {}

	public CompassSearchPacket(ResourceLocation structureOrGroupKey, boolean isGroup) {
		this.structureOrGroupKey = structureOrGroupKey;
		this.isGroup = isGroup;
	}

	public CompassSearchPacket(FriendlyByteBuf buf) {
		structureOrGroupKey = buf.readResourceLocation();
		isGroup = buf.readBoolean();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeResourceLocation(structureOrGroupKey);
		buf.writeBoolean(isGroup);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(ctx.get().getSender(), ExplorersCompass.explorersCompass);
			if (!stack.isEmpty()) {
				final ExplorersCompassItem explorersCompass = (ExplorersCompassItem) stack.getItem();
				explorersCompass.searchForStructure((ServerLevel) ctx.get().getSender().level(), ctx.get().getSender(), ctx.get().getSender().blockPosition(), structureOrGroupKey, isGroup, stack);
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
