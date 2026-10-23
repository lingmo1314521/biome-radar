package com.lingmo.biomeradar.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.config.ConfigHandler;
import com.lingmo.biomeradar.util.BiomeKeyUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public class StructureUtils {

	public static Map<ResourceLocation, ResourceLocation> getStructureKeysToTypeKeys(ServerLevel level) {
		Map<ResourceLocation, ResourceLocation> structureKeysToStructureKeys = new HashMap<ResourceLocation, ResourceLocation>();
		for (Structure structure : getStructureRegistry(level)) {
			structureKeysToStructureKeys.put(getKeyForStructure(level, structure), getTypeForStructure(level, structure));
		}
		return structureKeysToStructureKeys;
	}

	public static ResourceLocation getTypeForStructure(ServerLevel level, Structure structure) {
		Registry<StructureSet> registry = getStructureSetRegistry(level);
		for (StructureSet set : registry) {
			for (StructureSelectionEntry entry : set.structures()) {
				if (entry.structure().get().equals(structure)) {
					return registry.getKey(set);
				}
			}
		}
		return new ResourceLocation(ExplorersCompass.MODID, "none");
	}

	public static List<ResourceLocation> getStructuresForGroup(ServerLevel level, ResourceLocation groupKey) {
		List<ResourceLocation> structureKeys = new ArrayList<ResourceLocation>();
		Registry<StructureSet> registry = getStructureSetRegistry(level);
		StructureSet set = registry.get(groupKey);
		if (set != null) {
			for (StructureSelectionEntry entry : set.structures()) {
				ResourceLocation structureKey = getKeyForStructure(level, entry.structure().get());
				if (structureKey != null) {
					structureKeys.add(structureKey);
				}
			}
		}
		return structureKeys;
	}

	public static ResourceLocation getKeyForStructure(ServerLevel level, Structure structure) {
		return getStructureRegistry(level).getKey(structure);
	}

	public static Structure getStructureForKey(ServerLevel level, ResourceLocation key) {
		return getStructureRegistry(level).get(key);
	}
	
	public static Holder<Structure> getHolderForStructure(ServerLevel level, Structure structure) {
		Optional<ResourceKey<Structure>> optional = getStructureRegistry(level).getResourceKey(structure);
		if (optional.isPresent()) {
			return getStructureRegistry(level).getHolderOrThrow(optional.get());
		}
		return null;
	}

	public static List<ResourceLocation> getAllowedStructureKeys(ServerLevel level) {
		final List<ResourceLocation> structures = new ArrayList<ResourceLocation>();
		for (Structure structure : getStructureRegistry(level)) {
			if (structure != null && getKeyForStructure(level, structure) != null && !structureIsBlacklisted(level, structure)) {
				structures.add(getKeyForStructure(level, structure));
			}
		}
		return structures;
	}

	public static boolean structureIsBlacklisted(ServerLevel level, Structure structure) {
		final List<String> structureBlacklist = ConfigHandler.GENERAL.structureBlacklist.get();
		for (String structureKey : structureBlacklist) {
			if (getKeyForStructure(level, structure).toString().matches(convertToRegex(structureKey))) {
				return true;
			}
		}
		return false;
	}

	public static List<ResourceLocation> getGeneratingDimensionKeys(ServerLevel serverLevel, Structure structure) {
		final List<ResourceLocation> dimensions = new ArrayList<ResourceLocation>();
		for (ServerLevel level : serverLevel.getServer().getAllLevels()) {
			ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
			Set<Holder<Biome>> biomeSet = chunkGenerator.getBiomeSource().possibleBiomes();
			if (!structure.biomes().stream().noneMatch(biomeSet::contains)) {
				dimensions.add(level.dimension().location());
			}
		}
		return dimensions;
	}

	public static ListMultimap<ResourceLocation, ResourceLocation> getGeneratingDimensionsForAllowedStructures(ServerLevel serverLevel) {
		ListMultimap<ResourceLocation, ResourceLocation> dimensionsForAllowedStructures = ArrayListMultimap.create();
		for (ResourceLocation structureKey : getAllowedStructureKeys(serverLevel)) {
			Structure structure = getStructureForKey(serverLevel, structureKey);
			dimensionsForAllowedStructures.putAll(structureKey, getGeneratingDimensionKeys(serverLevel, structure));
		}
		return dimensionsForAllowedStructures;
	}

	public static int getXpLevelsForStructure(ServerLevel serverLevel, ResourceLocation structureKey) {
		int xpLevels = ConfigHandler.GENERAL.defaultXpLevels.get();
		final Map<String, Integer> xpLevelOverrides = parseXpLevelOverridesConfig();
		for (String structureRegex : xpLevelOverrides.keySet()) {
			if (structureKey.toString().matches(convertToRegex(structureRegex))) {
				xpLevels = xpLevelOverrides.get(structureRegex);
				if (xpLevels > 3) {
					xpLevels = 3;
				}
				break;
			}
		}
		return xpLevels;
	}

	public static Map<String, Integer> parseXpLevelOverridesConfig() {
		final List<String> xpLevelOverrides = ConfigHandler.GENERAL.perStructureXpLevels.get();
		Map<String, Integer> parsedOverrides = new HashMap<String, Integer>();
		for (String override : xpLevelOverrides) {
			String[] split = override.split(",");
			if (split.length != 2) {
				continue;
			}
			String structureRegex = split[0];
			String xpLevelsStr = split[1];
			try {
				int xpLevels = Integer.valueOf(xpLevelsStr);
				parsedOverrides.put(structureRegex, xpLevels);
			} catch (NumberFormatException e) {
				continue;
			}
		}
		return parsedOverrides;
	}

	public static Map<ResourceLocation, Integer> getXpLevelsForAllowedStructures(ServerLevel serverLevel) {
		final Map<ResourceLocation, Integer> xpLevels = new HashMap<ResourceLocation, Integer>();
		for (ResourceLocation structureKey : getAllowedStructureKeys(serverLevel)) {
			xpLevels.put(structureKey, getXpLevelsForStructure(serverLevel, structureKey));
		}
		return xpLevels;
	}

	public static int getHorizontalDistanceToLocation(Player player, int x, int z) {
		return getHorizontalDistanceToLocation(player.blockPosition(), x, z);
	}

	public static int getHorizontalDistanceToLocation(BlockPos startPos, int x, int z) {
		return (int) Mth.sqrt((float) startPos.distSqr(new BlockPos(x, startPos.getY(), z)));
	}

	@OnlyIn(Dist.CLIENT)
	public static String getPrettyStructureName(ResourceLocation key) {
		if (key == null) {
			return "";
		}
		// 生物群系伪键：直接显示群系名（优先龙珠模组的 biome 翻译键）
		if (BiomeKeyUtils.isBiomeKey(key)) {
			return BiomeKeyUtils.getDisplayName(key.getPath());
		}
		String name = key.toString();
		if (ConfigHandler.CLIENT.translateStructureNames.get()) {
			name = I18n.get(Util.makeDescriptionId("structure", key));
		}
		if (name.equals(Util.makeDescriptionId("structure", key)) || !ConfigHandler.CLIENT.translateStructureNames.get()) {
			name = key.toString();
			if (name.contains(":")) {
				name = name.substring(name.indexOf(":") + 1);
			}
			name = WordUtils.capitalize(name.replace('_', ' '));
		}
		return name;
	}

	@OnlyIn(Dist.CLIENT)
	public static String getPrettyStructureSource(ResourceLocation key) {
		if (key == null) {
			return "";
		}
		if (BiomeKeyUtils.isBiomeKey(key)) {
			return BiomeKeyUtils.getSourceName(key.getPath());
		}
		String registryEntry = key.toString();
		String modid = registryEntry.substring(0, registryEntry.indexOf(":"));
		if (modid.equals("minecraft")) {
			return net.minecraft.client.resources.language.I18n.get("string.biomeradar.modVanilla");
		}
		Optional<? extends ModContainer> sourceContainer = ModList.get().getModContainerById(modid);
		if (sourceContainer.isPresent()) {
			return sourceContainer.get().getModInfo().getDisplayName();
		}
		return modid;
	}

	@OnlyIn(Dist.CLIENT)
	public static String dimensionKeysToString(List<ResourceLocation> dimensions) {
		Set<String> dimensionNames = new HashSet<String>();
		dimensions.forEach((key) -> dimensionNames.add(getDimensionName(key)));
		return String.join(", ", dimensionNames);
	}

	/** 常见维度 → [中文名, 英文名] */
	private static final java.util.Map<String, String[]> DIMENSION_ZH = new java.util.HashMap<>();
	static {
		DIMENSION_ZH.put("minecraft:overworld", new String[]{"地球", "Overworld"});
		DIMENSION_ZH.put("minecraft:the_nether", new String[]{"地狱", "The Nether"});
		DIMENSION_ZH.put("minecraft:the_end", new String[]{"末地", "The End"});
		DIMENSION_ZH.put("dragonminez:namek", new String[]{"娜美克星", "Namek"});
		DIMENSION_ZH.put("dragonminez:demon_realm", new String[]{"魔界", "Demon Realm"});
		DIMENSION_ZH.put("dragonminez:otherworld", new String[]{"冥界", "Otherworld"});
		DIMENSION_ZH.put("dragonminez:sacredkaiplanet", new String[]{"界王星", "Kai Planet"});
		DIMENSION_ZH.put("dragonminez:time_chamber", new String[]{"精神时光屋", "Hyperbolic Time Chamber"});
	}

	/** 原版结构组 → [中文名, 英文名] */
	private static final java.util.Map<String, String[]> GROUP_ZH = new java.util.HashMap<>();
	static {
		GROUP_ZH.put("minecraft:nether_fossils", new String[]{"下界化石群", "Nether Fossils"});
		GROUP_ZH.put("minecraft:nether_complexes", new String[]{"下界要塞群", "Nether Fortresses"});
		GROUP_ZH.put("minecraft:ruined_portals", new String[]{"废弃传送门群", "Ruined Portals"});
		GROUP_ZH.put("minecraft:ocean_ruins", new String[]{"海底废墟群", "Ocean Ruins"});
		GROUP_ZH.put("minecraft:stronghold", new String[]{"要塞", "Stronghold"});
		GROUP_ZH.put("minecraft:ancient_cities", new String[]{"远古城市群", "Ancient Cities"});
		GROUP_ZH.put("minecraft:buried_treasures", new String[]{"埋藏的宝藏群", "Buried Treasures"});
		GROUP_ZH.put("minecraft:mineshafts", new String[]{"废弃矿井群", "Mineshafts"});
		GROUP_ZH.put("minecraft:desert_pyramids", new String[]{"沙漠神殿群", "Desert Pyramids"});
		GROUP_ZH.put("minecraft:igloos", new String[]{"雪屋群", "Igloos"});
		GROUP_ZH.put("minecraft:jungle_temples", new String[]{"丛林神庙群", "Jungle Temples"});
		GROUP_ZH.put("minecraft:swamp_huts", new String[]{"沼泽小屋群", "Swamp Huts"});
		GROUP_ZH.put("minecraft:pillager_outposts", new String[]{"掠夺者前哨站群", "Pillager Outposts"});
		GROUP_ZH.put("minecraft:villages", new String[]{"村庄群", "Villages"});
		GROUP_ZH.put("minecraft:woodland_mansions", new String[]{"林地府邸群", "Woodland Mansions"});
		GROUP_ZH.put("minecraft:shipwrecks", new String[]{"沉船群", "Shipwrecks"});
		GROUP_ZH.put("minecraft:monuments", new String[]{"海底神殿群", "Ocean Monuments"});
		GROUP_ZH.put("minecraft:mansions", new String[]{"林地府邸群", "Woodland Mansions"});
		GROUP_ZH.put("minecraft:strongholds", new String[]{"要塞", "Strongholds"});
		GROUP_ZH.put("minecraft:end_cities", new String[]{"末地城群", "End Cities"});
		GROUP_ZH.put("minecraft:trail_ruins", new String[]{"古迹废墟群", "Trail Ruins"});
	}

	/** 结构组显示名：优先双语映射，其次走结构名翻译 */
	@OnlyIn(Dist.CLIENT)
	public static String getGroupDisplayName(ResourceLocation key) {
		if (key == null) {
			return "";
		}
		final String[] names = GROUP_ZH.get(key.toString());
		if (names != null) {
			return isZh() ? names[0] : names[1];
		}
		return getPrettyStructureName(key);
	}

	/** 结构搜索别名：与当前显示名不同的旧版/资源包译名，中英搜索都能命中 */
	private static final java.util.Map<String, java.util.List<String>> STRUCTURE_ALIASES = new java.util.HashMap<>();
	static {
		putAlias("elder_guru", "大长老的家", "大长老住宅");
		putAlias("cell_arena", "沙鲁竞技场");
		putAlias("gero_lab", "格罗博士的研究所");
		putAlias("oldkai_pillar", "老界王神的神殿");
		putAlias("piccolo_house", "比克的住处");
		putAlias("roshi_house", "龟仙屋");
		putAlias("rrtower", "红缎带军团塔", "红缎带塔");
		putAlias("trunks_ship", "特兰克斯的飞船");
		putAlias("vegeta_pod", "贝吉塔的太空舱");
		putAlias("village_ajissa", "阿吉萨村");
		putAlias("village_sacred", "圣村");
		// DMZ 任务里出现的其余关键地点
		putAlias("baba_palace", "巴巴宫", "占卜巴巴的宫殿");
		putAlias("babidi", "巴比迪飞船", "巴比迪的太空船");
		putAlias("frieza_ship", "弗利萨飞船", "弗利萨的太空船");
		putAlias("goku_house", "悟空之家", "悟空的房子", "悟饭的家");
		putAlias("kamilookout", "神仙的神殿", "神神殿", "卡琳塔顶上的神殿");
		putAlias("timechamber", "精神时光屋入口", "时光屋入口");
		putAlias("yamcha_house", "雅木茶的家", "饮茶之家");
	}

	private static void putAlias(String path, String... aliases) {
		STRUCTURE_ALIASES.put(path, java.util.Arrays.asList(aliases));
	}

	/** 结构搜索别名列表（不含英文 path 与当前显示名，它们已单独参与匹配）；自动追加全拼与首字母 */
	public static java.util.List<String> getStructureSearchAliases(ResourceLocation key) {
		final java.util.List<String> list = STRUCTURE_ALIASES.get(key.getPath());
		if (list == null) {
			return java.util.Collections.emptyList();
		}
		final java.util.List<String> withPinyin = new java.util.ArrayList<>();
		for (String a : list) {
			withPinyin.add(a);
			withPinyin.addAll(PinyinUtils.toPinyinForms(a));
		}
		return withPinyin;
	}

	/** 维度显示名：优先双语映射，其次 lang 翻译键，最后英文原名 */
	@OnlyIn(Dist.CLIENT)
	public static String getDimensionDisplayName(String dimLocation) {
		final String[] names = DIMENSION_ZH.get(dimLocation);
		if (names != null) {
			return isZh() ? names[0] : names[1];
		}
		final String key = "dimension." + dimLocation.replace(':', '.');
		final String translated = I18n.get(key);
		if (!translated.equals(key)) {
			return translated;
		}
		return dimLocation;
	}

	@OnlyIn(Dist.CLIENT)
	private static boolean isZh() {
		return net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh");
	}

	@OnlyIn(Dist.CLIENT)
	private static String getDimensionName(ResourceLocation dimensionKey) {
		return getDimensionDisplayName(dimensionKey.toString());
	}

	private static Registry<Structure> getStructureRegistry(ServerLevel level) {
		return level.registryAccess().registryOrThrow(Registries.STRUCTURE);
	}

	private static Registry<StructureSet> getStructureSetRegistry(ServerLevel level) {
		return level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
	}

	private static String convertToRegex(String glob) {
		String regex = "^";
		for (char i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			if (c == '*') {
				regex += ".*";
			} else if (c == '?') {
				regex += ".";
			} else if (c == '.') {
				regex += "\\.";
			} else {
				regex += c;
			}
		}
		regex += "$";
		return regex;
	}

}