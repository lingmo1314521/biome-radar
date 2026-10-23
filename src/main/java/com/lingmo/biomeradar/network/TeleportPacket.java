package com.lingmo.biomeradar.network;

import java.util.function.Supplier;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.config.ConfigHandler;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.CompassState;
import com.lingmo.biomeradar.util.FastLocateUtil;
import com.lingmo.biomeradar.util.ItemUtils;
import com.lingmo.biomeradar.util.PlayerUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class TeleportPacket {

	private String structureKey = null;

	public TeleportPacket() {}

	public TeleportPacket(ResourceLocation key) {
		this.structureKey = key == null ? null : key.toString();
	}

	public TeleportPacket(FriendlyByteBuf buf) {
		structureKey = buf.readBoolean() ? buf.readUtf() : null;
	}

	public void fromBytes(FriendlyByteBuf buf) {
		structureKey = buf.readBoolean() ? buf.readUtf() : null;
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBoolean(structureKey != null);
		if (structureKey != null) {
			buf.writeUtf(structureKey);
		}
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			final ItemStack stack = ItemUtils.getHeldItem(ctx.get().getSender(), ExplorersCompass.explorersCompass);
			if (!stack.isEmpty()) {
				final ExplorersCompassItem explorersCompass = (ExplorersCompassItem) stack.getItem();
				final ServerPlayer player = ctx.get().getSender();
				if (ConfigHandler.GENERAL.allowTeleport.get() && PlayerUtils.canTeleport(player.getServer(), player)) {
					BlockPos targetPos = null;
					String targetDim = null;
					if (explorersCompass.getState(stack) == CompassState.FOUND) {
						final String foundDim = explorersCompass.getFoundDimension(stack);
						if (!foundDim.isEmpty() && !foundDim.equals(player.level().dimension().location().toString())) {
							// 跨维度禁止传送
							ExplorersCompass.LOGGER.warn("Player " + player.getDisplayName().getString() + " tried to teleport across dimensions.");
							return;
						}
						targetPos = new BlockPos(explorersCompass.getFoundStructureX(stack), 0, explorersCompass.getFoundStructureZ(stack));
						targetDim = foundDim;
					} else if (structureKey != null) {
						// 未搜索：龙珠结构走快路径直接定位并传送
						final ResourceLocation key = ResourceLocation.tryParse(structureKey);
						if (key != null && key.getNamespace().equals("dragonminez") && player.level() instanceof ServerLevel) {
							final BlockPos direct = FastLocateUtil.locate((ServerLevel) player.level(), key, player.blockPosition());
							if (direct != null) {
								targetPos = new BlockPos(direct.getX(), 0, direct.getZ());
								targetDim = player.level().dimension().location().toString();
							}
						}
					}
					if (targetPos != null) {
						final int y = findValidTeleportHeight(player.level(), targetPos.getX(), targetPos.getZ());
						player.stopRiding();
						player.connection.teleport(targetPos.getX(), y, targetPos.getZ(), player.getYRot(), player.getXRot());
						if (!player.isFallFlying()) {
							player.setDeltaMovement(player.getDeltaMovement().x(), 0, player.getDeltaMovement().z());
							player.setOnGround(true);
						}
					}
				} else {
					ExplorersCompass.LOGGER.warn("Player " + player.getDisplayName().getString() + " tried to teleport but does not have permission.");
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
	
	private int findValidTeleportHeight(Level level, int x, int z) {
		int upY = level.getSeaLevel();
		int downY = level.getSeaLevel();
		while ((!level.isOutsideBuildHeight(upY) || !level.isOutsideBuildHeight(downY)) && !(isValidTeleportPosition(level, new BlockPos(x, upY, z)) || isValidTeleportPosition(level, new BlockPos(x, downY, z)))) {
			upY++;
			downY--;
		}
		BlockPos upPos = new BlockPos(x, upY, z);
		BlockPos downPos = new BlockPos(x, downY, z);
		if (isValidTeleportPosition(level, upPos)) {
			return upY;
		}
		if (isValidTeleportPosition(level, downPos)) {
			return downY;
		}
		return 256;
	}
	
	private boolean isValidTeleportPosition(Level level, BlockPos pos) {
		return isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
	}
	
	private boolean isFree(Level level, BlockPos pos) {
		return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.FIRE) || level.getBlockState(pos).liquid() || level.getBlockState(pos).canBeReplaced();
	}

}
