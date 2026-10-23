package com.lingmo.biomeradar.worker;

import java.util.List;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.config.ConfigHandler;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.BiomeKeyUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * 生物群系搜索 worker：从玩家位置开始按环形逐圈扫描区块，
 * 每个区块中心采样一次群系，匹配目标群系 key 或 tag。
 */
public class BiomeSearchWorker implements WorldWorkerManager.IWorker {

	private final String managerId;
	private final ServerLevel level;
	private final Player player;
	private final ItemStack stack;
	private final BlockPos startPos;
	private final ResourceLocation pseudoKey;
	private final List<BlockPos> prevPos;

	private final ResourceKey<Biome> targetBiome;
	private final TagKey<Biome> targetTag;
	private final boolean isTag;

	private int samples;
	private int ring;
	private int segment;
	private boolean finished;

	public BiomeSearchWorker(ServerLevel level, Player player, ItemStack stack, BlockPos startPos, List<BlockPos> prevPos, ResourceLocation pseudoKey, String managerId) {
		this.level = level;
		this.player = player;
		this.stack = stack;
		this.startPos = startPos;
		this.prevPos = prevPos;
		this.pseudoKey = pseudoKey;
		this.managerId = managerId;

		final String target = BiomeKeyUtils.getTarget(pseudoKey.getPath());
		this.isTag = target != null && target.startsWith("#");
		if (isTag) {
			final String tagStr = target.substring(1);
			final int sep = tagStr.indexOf(':');
			this.targetTag = TagKey.create(Registries.BIOME, new ResourceLocation(tagStr.substring(0, sep), tagStr.substring(sep + 1)));
			this.targetBiome = null;
		} else {
			final int sep = target.indexOf(':');
			this.targetBiome = ResourceKey.create(Registries.BIOME, new ResourceLocation(target.substring(0, sep), target.substring(sep + 1)));
			this.targetTag = null;
		}
	}

	public void start() {
		if (!stack.isEmpty() && stack.getItem() == ExplorersCompass.explorersCompass) {
			if (ConfigHandler.GENERAL.maxRadius.get() > 0) {
				ExplorersCompass.LOGGER.info("BiomeSearchWorker " + managerId + ": searching for biome " + pseudoKey + " with max radius " + ConfigHandler.GENERAL.maxRadius.get());
				WorldWorkerManager.addWorker(this);
			} else {
				fail();
			}
		}
	}

	@Override
	public boolean hasWork() {
		if (finished || prevPos.size() > ConfigHandler.GENERAL.maxNextSearches.get()) {
			return false;
		}
		// 最大搜索半径（米），换算为区块圈数
		final int maxChunkRing = ConfigHandler.GENERAL.maxRadius.get() / 16 + 1;
		if (ring > maxChunkRing) {
			fail();
			return false;
		}
		return true;
	}

	@Override
	public boolean doWork() {
		if (ring == 0) {
			// 先检查起点区块
			if (checkChunk(startPos.getX() >> 4, startPos.getZ() >> 4)) {
				return true;
			}
			ring = 1;
			return false;
		}

		// 每 tick 扫当前圈的一个边（segment），边长为 ring
		final int r = ring;
		final int seg = segment;
		for (int i = 0; i < r; i++) {
			int bx;
			int bz;
			switch (seg) {
				case 0: bx = startPos.getX() + r * 16 + i * 16; bz = startPos.getZ() + r * 16; break;        // 上边
				case 1: bx = startPos.getX() + r * 16; bz = startPos.getZ() - i * 16 - 16; break;           // 右边
				case 2: bx = startPos.getX() - i * 16 - 16; bz = startPos.getZ() - r * 16; break;           // 下边
				default: bx = startPos.getX() - r * 16; bz = startPos.getZ() + i * 16; break;               // 左边
			}
			if (checkChunk(bx >> 4, bz >> 4)) {
				return true;
			}
		}

		segment++;
		if (segment >= 4) {
			segment = 0;
			ring++;
		}
		return false;
	}

	private boolean checkChunk(int chunkX, int chunkZ) {
		if (shouldIgnore(new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8))) {
			return false;
		}
		samples++;
		try {
			final ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES);
			final Holder<Biome> holder = chunk.getNoiseBiome((chunkX * 16 + 8) >> 2, 16, (chunkZ * 16 + 8) >> 2);
			final boolean matched;
			if (isTag) {
				matched = holder.tags().anyMatch(tag -> tag.equals(targetTag));
			} else {
				final Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
				matched = holder.is(registry.getResourceKey(holder.value()).orElse(targetBiome)) || registry.getResourceKey(holder.value()).map(key -> key.equals(targetBiome)).orElse(false);
			}
			if (matched) {
				final int foundX = chunkX * 16 + 8;
				final int foundZ = chunkZ * 16 + 8;
				if (!stack.isEmpty() && stack.getItem() == ExplorersCompass.explorersCompass) {
					((ExplorersCompassItem) stack.getItem()).succeed(stack, pseudoKey, false, foundX, foundZ, prevPos, samples, ConfigHandler.GENERAL.displayCoordinates.get(), level.dimension().location().toString());
				}
				finished = true;
				ExplorersCompass.LOGGER.info("BiomeSearchWorker " + managerId + ": found biome " + pseudoKey + " at " + foundX + ", " + foundZ + " after " + samples + " samples");
				return true;
			}
		} catch (Exception e) {
			ExplorersCompass.LOGGER.debug("BiomeSearchWorker " + managerId + ": chunk check failed at " + chunkX + ", " + chunkZ);
		}
		return false;
	}

	private boolean shouldIgnore(BlockPos pos) {
		for (BlockPos p : prevPos) {
			if (Math.abs(p.getX() - pos.getX()) < 16 && Math.abs(p.getZ() - pos.getZ()) < 16) {
				return true;
			}
		}
		return false;
	}

	private void fail() {
		if (!stack.isEmpty() && stack.getItem() == ExplorersCompass.explorersCompass) {
			((ExplorersCompassItem) stack.getItem()).fail(stack, pseudoKey, ConfigHandler.GENERAL.maxRadius.get(), samples);
		}
		finished = true;
	}
}
