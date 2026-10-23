package com.lingmo.biomeradar.util;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * 读取龙珠模组（DragonMineZ）客户端缓存的龙珠位置（RadarRenderEvent 静态字段）。
 * 龙珠模组每 1 秒同步一次龙珠数据到客户端，这里反射读取，未装龙珠模组时返回空列表。
 */
public class DragonBallHud {

	private static final String CLASS_NAME = "com.dragonminez.client.events.RadarRenderEvent";
	private static Field earthField;
	private static Field namekField;
	private static boolean tried;

	@SuppressWarnings("unchecked")
	public static List<BlockPos> getEarthPositions() {
		return readField("clientEarthPositions");
	}

	@SuppressWarnings("unchecked")
	public static List<BlockPos> getNamekPositions() {
		return readField("clientNamekPositions");
	}

	private static List<BlockPos> readField(String fieldName) {
		try {
			if (!tried) {
				try {
					final Class<?> clazz = Class.forName(CLASS_NAME);
					earthField = clazz.getDeclaredField("clientEarthPositions");
					earthField.setAccessible(true);
					namekField = clazz.getDeclaredField("clientNamekPositions");
					namekField.setAccessible(true);
				} catch (Throwable t) {
					// 龙珠模组未加载或版本不兼容
				}
				tried = true;
			}
			final Field f = fieldName.equals("clientEarthPositions") ? earthField : namekField;
			if (f == null) {
				return Collections.emptyList();
			}
			final Object value = f.get(null);
			if (value instanceof List) {
				return (List<BlockPos>) value;
			}
		} catch (Throwable t) {
			// 读取失败返回空
		}
		return Collections.emptyList();
	}
}
