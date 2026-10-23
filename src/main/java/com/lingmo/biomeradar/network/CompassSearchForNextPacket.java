package com.lingmo.biomeradar.network;

import java.util.function.Supplier;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.ItemUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class CompassSearchForNextPacket {

	public CompassSearchForNextPacket() {}

	public CompassSearchForNextPacket(FriendlyByteBuf buf) {}

	public void toBytes(FriendlyByteBuf buf) {}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(ctx.get().getSender(), ExplorersCompass.explorersCompass);
			if (!stack.isEmpty()) {
				final ExplorersCompassItem explorersCompass = (ExplorersCompassItem) stack.getItem();
				explorersCompass.searchForNextStructure((ServerLevel) ctx.get().getSender().level(), ctx.get().getSender(), ctx.get().getSender().blockPosition(), stack);
			}
		});
		ctx.get().setPacketHandled(true);
	}

}
