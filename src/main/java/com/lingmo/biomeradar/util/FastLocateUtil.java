package com.lingmo.biomeradar.util;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 龙珠模组快速定位：反射调用 DragonMineZ 的 StructureLocator。
 * 龙珠模组的自定义结构放置（BiomeAwareUniquePlacement / FixedStructurePlacement /
 * UniqueNearSpawnPlacement）可以直接算出结构坐标，无需逐区块扫描。
 * 反射失败或未装龙珠模组时返回 null，由调用方回退到原搜索 worker。
 */
public class FastLocateUtil {

	private static final String LOCATOR_CLASS = "com.dragonminez.server.world.structure.helper.StructureLocator";
	private static Method locateMethod;
	private static boolean tried;

	/**
	 * 尝试用龙珠模组算法直接定位结构。返回 null 表示无法用快路径。
	 */
	public static BlockPos locate(ServerLevel level, ResourceLocation structureKey, BlockPos from) {
		if (!structureKey.getNamespace().equals("dragonminez")) {
			return null;
		}
		try {
			if (!tried) {
				try {
					Class<?> clazz = Class.forName(LOCATOR_CLASS);
					locateMethod = clazz.getMethod("locateStructure", ServerLevel.class, ResourceKey.class, BlockPos.class);
				} catch (Throwable t) {
					// 龙珠模组未加载或版本不兼容，标记后不再尝试
				}
				tried = true;
			}
			if (locateMethod == null) {
				return null;
			}
			final ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, structureKey);
			final Object result = locateMethod.invoke(null, level, key, from);
			if (result instanceof BlockPos) {
				return (BlockPos) result;
			}
		} catch (Throwable t) {
			// 任何异常都回退到原搜索
		}
		return null;
	}
}
