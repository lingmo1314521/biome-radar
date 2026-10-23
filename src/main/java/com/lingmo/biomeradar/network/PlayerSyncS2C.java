package com.lingmo.biomeradar.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.lingmo.biomeradar.util.PlayerTracker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：广播所有在线玩家的位置与队伍（每 1 秒一次）。
 * 客户端据此渲染追踪红/绿点，不依赖区块内实体加载，任意距离都能追踪。
 */
public class PlayerSyncS2C {

	private final List<PlayerTracker.TrackedPlayer> players;

	public PlayerSyncS2C(List<PlayerTracker.TrackedPlayer> players) {
		this.players = players;
	}

	public PlayerSyncS2C(FriendlyByteBuf buf) {
		players = new ArrayList<>();
		final int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			players.add(new PlayerTracker.TrackedPlayer(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readUtf(), buf.readUtf()));
		}
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeVarInt(players.size());
		for (PlayerTracker.TrackedPlayer p : players) {
			buf.writeUtf(p.name());
			buf.writeDouble(p.x());
			buf.writeDouble(p.z());
			buf.writeUtf(p.dimension());
			buf.writeUtf(p.team());
		}
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerTracker.updateTracked(players)));
		ctx.get().setPacketHandled(true);
	}
}
