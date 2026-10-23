package com.lingmo.biomeradar.server;

import java.util.ArrayList;
import java.util.List;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.network.PlayerSyncS2C;
import com.lingmo.biomeradar.util.PlayerTracker;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

/**
 * 服务端向所有在线玩家广播玩家位置与队伍，供群系雷达的玩家追踪使用（任意距离可追踪）。
 * 每 1 秒心跳一次；玩家加入/离开世界时立即补发一次，保证新玩家马上出现在雷达上。
 */
@EventBusSubscriber(modid = ExplorersCompass.MODID)
public class PlayerSyncServer {

	private static int tickCount;

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		final MinecraftServer server = event.getServer();
		if (server == null || server.getPlayerList().getPlayers().isEmpty()) {
			return;
		}
		tickCount++;
		if (tickCount % 20 != 0) {
			return;
		}
		broadcast(server);
	}

	@SubscribeEvent
	public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
		final MinecraftServer server = event.getEntity().getServer();
		if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
			broadcast(server);
		}
	}

	@SubscribeEvent
	public static void onPlayerLeave(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		final MinecraftServer server = event.getEntity().getServer();
		if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
			broadcast(server);
		}
	}

	private static void broadcast(MinecraftServer server) {
		final List<PlayerTracker.TrackedPlayer> list = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			final String team = p.getTeam() == null ? "" : p.getTeam().getName();
			list.add(new PlayerTracker.TrackedPlayer(p.getName().getString(), p.getX(), p.getZ(),
					p.level().dimension().location().toString(), team));
		}
		final PlayerSyncS2C packet = new PlayerSyncS2C(list);
		ExplorersCompass.network.send(PacketDistributor.ALL.noArg(), packet);
	}
}
