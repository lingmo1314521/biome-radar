package com.lingmo.biomeradar.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

/**
 * 龙珠模组任务所需的生物群系（以伪结构键接入现有搜索链路）。
 * 伪键统一用 "biome_" 前缀，区别于真实结构键。
 * 启动时探测 DragonMineZ 版本（2.1.3 vs 2.2），只注册当前版本真实存在的群系/tag。
 */
public class BiomeKeyUtils {

	/** 当前安装的 DragonMineZ 是否为 2.1.x 系列（tag 命名/群系与 2.2 不同） */
	private static boolean isDmzLegacy() {
		try {
			Optional<?> dmz = ModList.get().getModContainerById("dragonminez");
			if (dmz.isPresent()) {
				String ver = ModList.get().getModContainerById("dragonminez").get().getModInfo().getVersion().toString();
				return ver.startsWith("2.1.");
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	// 伪键 -> 真实 biome key（或 #tag）
	public static final Map<String, String> BIOME_TARGETS = new LinkedHashMap<>();
	static {
		final boolean legacy = isDmzLegacy();

		// ---- DMZ 两个版本共有的群系 ----
		BIOME_TARGETS.put("biome_dmz_rocky", "dragonminez:rocky");
		BIOME_TARGETS.put("biome_dmz_ajissa", "dragonminez:ajissa_plains");
		BIOME_TARGETS.put("biome_dmz_rivers", "dragonminez:namekian_rivers");
		BIOME_TARGETS.put("biome_dmz_sacredland", "dragonminez:sacred_land");
		BIOME_TARGETS.put("biome_dmz_sacredkai_hills", "dragonminez:sacredkai_hills");
		BIOME_TARGETS.put("biome_dmz_sacredkai_plains", "dragonminez:sacredkai_plains");
		BIOME_TARGETS.put("biome_dmz_sacredkai_rivers", "dragonminez:sacredkai_rivers");
		BIOME_TARGETS.put("biome_dmz_timechamber", "dragonminez:hyperbolic_time_chamber");
		BIOME_TARGETS.put("biome_dmz_otherworld", "dragonminez:other_world");
		// ---- 2.2 新增：三个魔界群系（2.1.3 没有）----
		if (!legacy) {
			BIOME_TARGETS.put("biome_dmz_demon1", "dragonminez:first_demon_world");
			BIOME_TARGETS.put("biome_dmz_demon2", "dragonminez:second_demon_world");
			BIOME_TARGETS.put("biome_dmz_demon3", "dragonminez:third_demon_world");
		}
		// ---- 任务相关的原版群系 ----
		BIOME_TARGETS.put("biome_mc_plains", "minecraft:plains");
		BIOME_TARGETS.put("biome_mc_desert", "minecraft:desert");
		BIOME_TARGETS.put("biome_mc_forest", "minecraft:forest");
		BIOME_TARGETS.put("biome_mc_snowy", "minecraft:snowy_plains");
		BIOME_TARGETS.put("biome_mc_swamp", "minecraft:swamp");
		BIOME_TARGETS.put("biome_mc_dripstone", "minecraft:dripstone_caves");
		BIOME_TARGETS.put("biome_mc_lush", "minecraft:lush_caves");
		// ---- 任务前置条件里用到的原版 tag 群系（河流/海洋/森林/积雪）----
		BIOME_TARGETS.put("biome_tag_mountain", "#minecraft:is_mountain");
		BIOME_TARGETS.put("biome_tag_beach", "#minecraft:is_beach");
		BIOME_TARGETS.put("biome_tag_river", "#minecraft:is_river");
		BIOME_TARGETS.put("biome_tag_ocean", "#minecraft:is_ocean");
		BIOME_TARGETS.put("biome_tag_forest", "#minecraft:is_forest");
		BIOME_TARGETS.put("biome_tag_snowy", "#forge:is_snowy");
		// ---- DMZ 自定义 tag：2.1.3 叫 is_plainslike，2.2 改名叫 is_plains ----
		BIOME_TARGETS.put("biome_tag_dmz_plains", legacy ? "#dragonminez:is_plainslike" : "#dragonminez:is_plains");
	}

	// 群系中文表：主名（2.2 汉化版/资源包译名）+ 另一版别名，用于无翻译键时兜底与中英搜索
	private static final Map<String, List<String>> BIOME_ZH = new LinkedHashMap<>();
	static {
		BIOME_ZH.put("dragonminez:rocky", Arrays.asList("岩石山地", "岩石地带"));
		BIOME_ZH.put("dragonminez:ajissa_plains", Arrays.asList("阿吉萨平原"));
		BIOME_ZH.put("dragonminez:namekian_rivers", Arrays.asList("那美克星河川", "那美克星河流"));
		BIOME_ZH.put("dragonminez:sacred_land", Arrays.asList("圣地"));
		BIOME_ZH.put("dragonminez:sacredkai_hills", Arrays.asList("界王神界丘陵"));
		BIOME_ZH.put("dragonminez:sacredkai_plains", Arrays.asList("界王神界平原"));
		BIOME_ZH.put("dragonminez:sacredkai_rivers", Arrays.asList("界王神界河川", "界王神界河流"));
		BIOME_ZH.put("dragonminez:hyperbolic_time_chamber", Arrays.asList("精神时光屋"));
		BIOME_ZH.put("dragonminez:other_world", Arrays.asList("冥界", "地府"));
		BIOME_ZH.put("dragonminez:first_demon_world", Arrays.asList("第一魔界"));
		BIOME_ZH.put("dragonminez:second_demon_world", Arrays.asList("第二魔界"));
		BIOME_ZH.put("dragonminez:third_demon_world", Arrays.asList("第三魔界"));
		BIOME_ZH.put("minecraft:dripstone_caves", Arrays.asList("滴水石洞窟"));
		BIOME_ZH.put("minecraft:lush_caves", Arrays.asList("繁茂洞窟"));
	}

	// 伪键 -> 中文（用于伪键条目的显示兜底）
	private static final Map<String, List<String>> PSEUDO_ZH = new LinkedHashMap<>();
	static {
		PSEUDO_ZH.put("biome_dmz_rocky", Arrays.asList("岩石山地", "岩石地带"));
		PSEUDO_ZH.put("biome_dmz_ajissa", Arrays.asList("阿吉萨平原"));
		PSEUDO_ZH.put("biome_dmz_rivers", Arrays.asList("那美克星河川", "那美克星河流"));
		PSEUDO_ZH.put("biome_dmz_sacredland", Arrays.asList("圣地"));
		PSEUDO_ZH.put("biome_dmz_sacredkai_hills", Arrays.asList("界王神界丘陵"));
		PSEUDO_ZH.put("biome_dmz_sacredkai_plains", Arrays.asList("界王神界平原"));
		PSEUDO_ZH.put("biome_dmz_sacredkai_rivers", Arrays.asList("界王神界河川", "界王神界河流"));
		PSEUDO_ZH.put("biome_dmz_timechamber", Arrays.asList("精神时光屋"));
		PSEUDO_ZH.put("biome_dmz_otherworld", Arrays.asList("冥界", "地府"));
		PSEUDO_ZH.put("biome_dmz_demon1", Arrays.asList("第一魔界"));
		PSEUDO_ZH.put("biome_dmz_demon2", Arrays.asList("第二魔界"));
		PSEUDO_ZH.put("biome_dmz_demon3", Arrays.asList("第三魔界"));
		PSEUDO_ZH.put("biome_mc_plains", Arrays.asList("平原"));
		PSEUDO_ZH.put("biome_mc_desert", Arrays.asList("沙漠"));
		PSEUDO_ZH.put("biome_mc_forest", Arrays.asList("森林"));
		PSEUDO_ZH.put("biome_mc_snowy", Arrays.asList("雪原"));
		PSEUDO_ZH.put("biome_mc_swamp", Arrays.asList("沼泽"));
		PSEUDO_ZH.put("biome_mc_dripstone", Arrays.asList("滴水石洞窟", "溶洞"));
		PSEUDO_ZH.put("biome_mc_lush", Arrays.asList("繁茂洞窟", "绿洞"));
		PSEUDO_ZH.put("biome_tag_mountain", Arrays.asList("山地"));
		PSEUDO_ZH.put("biome_tag_beach", Arrays.asList("沙滩"));
		PSEUDO_ZH.put("biome_tag_river", Arrays.asList("河流", "河川"));
		PSEUDO_ZH.put("biome_tag_ocean", Arrays.asList("海洋", "大海"));
		PSEUDO_ZH.put("biome_tag_forest", Arrays.asList("森林群系", "各类森林"));
		PSEUDO_ZH.put("biome_tag_snowy", Arrays.asList("积雪群系", "各类雪地"));
		PSEUDO_ZH.put("biome_tag_dmz_plains", Arrays.asList("龙珠平原", "那美克平原", "阿吉萨平原群"));
	}

	public static boolean isBiomeKey(ResourceLocation key) {
		return key != null && key.getPath().startsWith("biome_");
	}

	public static boolean isBiomeKey(String key) {
		return key != null && key.startsWith("biome_");
	}

	/** 伪键对应的目标字符串（biome key 或 #tag） */
	public static String getTarget(String pseudoKey) {
		return BIOME_TARGETS.get(pseudoKey);
	}

	/** 显示名：优先走龙珠模组/资源包的 biome 翻译键，其次内置表，最后英文 */
	public static String getDisplayName(String pseudoKey) {
		final String target = BIOME_TARGETS.get(pseudoKey);
		if (target == null) {
			return pseudoKey;
		}
		if (target.startsWith("#")) {
			// 标签群系
			final String tagPath = target.substring(1);
			final String langKey = "string.biomeradar.biome_tag_" + tagPath.replace(':', '_');
			final String name = I18n.get(langKey);
			if (!name.equals(langKey)) {
				return name;
			}
			final List<String> zh = PSEUDO_ZH.get(pseudoKey);
			return zh == null || zh.isEmpty() ? tagPath : zh.get(0);
		}
		final String langKey = "biome." + target.replace(':', '.');
		final String name = I18n.get(langKey);
		if (!name.equals(langKey)) {
			return name;
		}
		final List<String> zh = BIOME_ZH.get(target);
		return zh == null || zh.isEmpty() ? target : zh.get(0);
	}

	/** 中英搜索别名：英文 target/path + 中文主名 + 两版译名别名 + 拼音/首字母 */
	public static List<String> getSearchAliases(String pseudoKey) {
		final List<String> aliases = new ArrayList<>();
		final String target = BIOME_TARGETS.get(pseudoKey);
		if (target != null) {
			aliases.add(target);
			if (target.startsWith("#")) {
				aliases.add(target.substring(1));
			} else {
				aliases.add(target.substring(target.indexOf(":") + 1));
			}
			final List<String> zh = BIOME_ZH.get(target);
			if (zh != null) {
				aliases.addAll(zh);
			}
		}
		aliases.add(pseudoKey);
		final List<String> pzh = PSEUDO_ZH.get(pseudoKey);
		if (pzh != null) {
			aliases.addAll(pzh);
		}
		// 对所有中文别名追加全拼 + 首字母简写
		final List<String> withPinyin = new ArrayList<>(aliases.size() * 2);
		for (String a : aliases) {
			withPinyin.add(a);
			withPinyin.addAll(PinyinUtils.toPinyinForms(a));
		}
		return withPinyin;
	}

	/** 来源显示名 */
	public static String getSourceName(String pseudoKey) {
		final String target = BIOME_TARGETS.get(pseudoKey);
		if (target != null) {
			final String ns = target.startsWith("#") ? target.substring(1, target.indexOf(':')) : target.substring(0, target.indexOf(':'));
			if ("dragonminez".equals(ns)) {
				return "DragonMineZ";
			}
			if ("minecraft".equals(ns)) {
				return "Minecraft";
			}
			if ("forge".equals(ns)) {
				return "Forge";
			}
			return ns;
		}
		return pseudoKey.startsWith("biome_dmz") ? "DragonMineZ" : "Minecraft";
	}

	public static String getPseudoKey(ResourceLocation key) {
		return key.getPath();
	}
}
