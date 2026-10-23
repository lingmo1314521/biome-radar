package com.lingmo.biomeradar.util;

import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * GUI 状态记忆：搜索词 / 模组筛选 / 玩家追踪 / 上次选中目标。
 * 存到 config/explorerscompass-ui.json，下次打开自动恢复。
 */
public class UiMemory {

	public static String search = "";
	public static String modFilter = null;
	public static int trackMode = 0;
	public static String trackTarget = "";
	public static String selectedKey = null;
	public static boolean dragonBalls = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("explorerscompass-ui.json");
	private static boolean loaded = false;

	private static class Data {
		String search;
		String modFilter;
		Integer trackMode;
		String trackTarget;
		String selectedKey;
		Boolean dragonBalls;
	}

	public static void ensureLoaded() {
		if (!loaded) {
			load();
			loaded = true;
		}
	}

	public static void load() {
		try {
			if (PATH.toFile().exists()) {
				final Data data = GSON.fromJson(java.nio.file.Files.readString(PATH), Data.class);
				if (data != null) {
					if (data.search != null) {
						search = data.search;
					}
					modFilter = data.modFilter;
					if (data.trackMode != null) {
						trackMode = data.trackMode;
					}
					if (data.trackTarget != null) {
						trackTarget = data.trackTarget;
					}
					selectedKey = data.selectedKey;
					if (data.dragonBalls != null) {
						dragonBalls = data.dragonBalls;
					}
				}
			}
		} catch (Exception e) {
			// 读失败保持默认
		}
	}

	public static void save() {
		try {
			final Data data = new Data();
			data.search = search;
			data.modFilter = modFilter;
			data.trackMode = trackMode;
			data.trackTarget = trackTarget;
			data.selectedKey = selectedKey;
			data.dragonBalls = dragonBalls;
			PATH.toFile().getParentFile().mkdirs();
			java.nio.file.Files.writeString(PATH, GSON.toJson(data));
		} catch (Exception e) {
			// 写失败不影响游戏
		}
	}
}
