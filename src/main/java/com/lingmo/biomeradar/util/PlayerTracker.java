package com.lingmo.biomeradar.util;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * 玩家追踪状态与队伍判定（客户端）。
 * 追踪模式：0=不追踪 1=全部玩家 2=指定玩家。
 * 同计分板队伍（DMZ 队伍系统挂在 scoreboard 上）显示绿色，其他玩家红色。
 */
public class PlayerTracker {

	public static final int MODE_NONE = 0;
	public static final int MODE_ALL = 1;
	public static final int MODE_SPECIFIC = 2;

	/** 服务端同步的在线玩家信息 */
	public record TrackedPlayer(String name, double x, double z, String dimension, String team) {}

	private static int mode = MODE_NONE;
	private static String targetName = "";
	private static java.util.Map<String, TrackedPlayer> tracked = new java.util.HashMap<>();

	public static void updateTracked(java.util.List<TrackedPlayer> list) {
		tracked = new java.util.HashMap<>();
		for (TrackedPlayer p : list) {
			tracked.put(p.name(), p);
		}
	}

	public static java.util.Map<String, TrackedPlayer> getTracked() {
		return tracked;
	}

	/** 从记忆文件恢复追踪状态（首次使用时调用） */
	public static void restoreFromMemory() {
		UiMemory.ensureLoaded();
		mode = UiMemory.trackMode;
		targetName = UiMemory.trackTarget;
	}

	public static int getMode() {
		return mode;
	}

	public static void setMode(int newMode) {
		mode = newMode;
		UiMemory.ensureLoaded();
		UiMemory.trackMode = newMode;
		UiMemory.save();
	}

	public static String getTargetName() {
		return targetName;
	}

	public static void setTargetName(String name) {
		targetName = name == null ? "" : name;
		UiMemory.ensureLoaded();
		UiMemory.trackTarget = targetName;
		UiMemory.save();
	}

	/** 当前追踪模式是否追踪给定玩家 */
	public static boolean shouldTrack(Player me, Player other) {
		if (other == me || other == null || !other.isAlive()) {
			return false;
		}
		if (mode == MODE_ALL) {
			return true;
		}
		if (mode == MODE_SPECIFIC) {
			return targetName.equals(other.getName().getString());
		}
		return false;
	}

	/** 是否同一队伍（DMZ 队伍 = 计分板队伍）：同队绿色，不同队/无队红色 */
	public static boolean isSameTeam(Player me, Player other) {
		final Team myTeam = me.getTeam();
		final Team otherTeam = other.getTeam();
		return myTeam != null && otherTeam != null && myTeam.equals(otherTeam);
	}

	public static List<? extends Player> getVisiblePlayers() {
		final Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return java.util.Collections.emptyList();
		}
		return mc.level.players();
	}
}
