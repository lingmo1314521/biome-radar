package com.lingmo.biomeradar.client;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.CompassState;
import com.lingmo.biomeradar.util.ItemUtils;
import com.lingmo.biomeradar.util.DragonBallHud;
import com.lingmo.biomeradar.util.PlayerTracker;
import com.lingmo.biomeradar.util.UiMemory;
import com.lingmo.biomeradar.util.StructureUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import org.joml.Quaternionf;

/**
 * 群系雷达 HUD：复用 DragonMineZ 龙珠雷达的雷达盘贴图与渲染逻辑。
 * 手持雷达时在屏幕右下角绘制雷达盘，目标在范围内显示红点，范围外显示方向箭头，
 * 雷达下方显示目标名称与距离。
 */
@OnlyIn(Dist.CLIENT)
public class ExplorersCompassOverlay implements IGuiOverlay {

    public static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation RADAR_TEX = new ResourceLocation("biomeradar:textures/gui/radar.png");
    private static final int TEXTURE_W = 121;
    private static final int TEXTURE_H = 146;
    private static final int RADAR_CENTER_X_OFFSET = 61;
    private static final int RADAR_CENTER_Y_OFFSET = 87;
    private static final int DOT_RADIUS = 50;
    // 与龙珠模组雷达一致：范围内显示红点，范围外只显示边缘方向箭头（避免远目标被缩到盘心看起来"很近"）
    private static final int MAX_RANGE = 1000;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (mc.player != null && mc.level != null && !mc.options.hideGui && !mc.options.renderDebug && (mc.screen == null || mc.screen instanceof ChatScreen)) {
            // 首次渲染时从记忆恢复玩家追踪状态
            PlayerTracker.restoreFromMemory();
            final Player player = mc.player;
            final ItemStack stack = ItemUtils.getHeldItem(player, ExplorersCompass.explorersCompass);
            if (stack != null && stack.getItem() instanceof ExplorersCompassItem) {
                final ExplorersCompassItem compass = (ExplorersCompassItem) stack.getItem();
                // 左右手模式（照龙珠模组）：主手 → 右下，副手 → 左下
                final boolean inMainHand = player.getMainHandItem() == stack;
                final int centerX = inMainHand ? screenWidth - TEXTURE_W - 10 : 10;
                final int centerY = screenHeight - TEXTURE_H - 10;
                guiGraphics.blit(RADAR_TEX, centerX, centerY, 0, 0, TEXTURE_W, TEXTURE_H, 256, 256);

                if (compass.getState(stack) == CompassState.FOUND) {
                    renderTarget(guiGraphics, player, compass, stack, centerX, centerY);
                } else if (compass.getState(stack) == CompassState.SEARCHING) {
                    drawText(guiGraphics, centerX, centerY, I18n.get("string.biomeradar.searching"), StructureUtils.getPrettyStructureName(compass.getStructureKey(stack)));
                } else if (compass.getState(stack) == CompassState.NOT_FOUND) {
                    // 未找到时提示该地点所在的维度
                    final String dimHint = getDimensionHint(compass, stack);
                    if (dimHint.isEmpty()) {
                        drawText(guiGraphics, centerX, centerY, I18n.get("string.biomeradar.notFound"), StructureUtils.getPrettyStructureName(compass.getStructureKey(stack)));
                    } else {
                        drawText3(guiGraphics, centerX, centerY, I18n.get("string.biomeradar.notFound"), StructureUtils.getPrettyStructureName(compass.getStructureKey(stack)), I18n.get("string.biomeradar.locatedIn") + ": " + dimHint);
                    }
                }

                // 玩家追踪 + 龙珠探测：与目的地追踪互不冲突，任意状态下同时显示
                renderTrackedPlayers(guiGraphics, player, centerX, centerY);
                renderDragonBalls(guiGraphics, player, centerX, centerY, compass.getEnhanced(stack));
            }
        }
    }

    private void renderTarget(GuiGraphics gui, Player player, ExplorersCompassItem compass, ItemStack stack, int centerX, int centerY) {
        // 跨维度校验：目标在其他维度时不显示红点与距离（该维度不存在此结构）
        final String foundDim = compass.getFoundDimension(stack);
        final String currentDim = player.level().dimension().location().toString();
        if (!foundDim.isEmpty() && !foundDim.equals(currentDim)) {
            drawText(gui, centerX, centerY, StructureUtils.getPrettyStructureName(compass.getStructureKey(stack)), I18n.get("string.biomeradar.otherDimension") + ": " + getPrettyDimensionName(foundDim));
            return;
        }

        final int foundX = compass.getFoundStructureX(stack);
        final int foundZ = compass.getFoundStructureZ(stack);
        final double dx = foundX + 0.5D - player.getX();
        final double dz = foundZ + 0.5D - player.getZ();
        final double dist = Math.sqrt(dx * dx + dz * dz);

        final double angleToTarget = Math.atan2(dz, dx);
        final double playerYaw = Math.toRadians(player.getYRot()) + (Math.PI / 2);
        final double finalAngle = angleToTarget - playerYaw;
        final double renderAngle = finalAngle - (Math.PI / 2);

        final int radarCenterX = centerX + RADAR_CENTER_X_OFFSET;
        final int radarCenterY = centerY + RADAR_CENTER_Y_OFFSET;

        if (dist <= MAX_RANGE) {
            final double scaledDist = (dist / MAX_RANGE) * DOT_RADIUS;
            final int dotX = (int) (radarCenterX + (scaledDist * Math.cos(renderAngle)));
            final int dotY = (int) (radarCenterY + (scaledDist * Math.sin(renderAngle)));
            // 目的地：橙红色球形圆点，与龙珠金黄/玩家红绿区分
            gui.blit(DOT_ORANGE, dotX - 3, dotY - 3, 0, 0, 6, 6, 6, 6);
        } else {
            final int arrowX = (int) (radarCenterX + (DOT_RADIUS * Math.cos(renderAngle)));
            final int arrowY = (int) (radarCenterY + (DOT_RADIUS * Math.sin(renderAngle)));
            final float rotation = (float) renderAngle + (float) (Math.PI / 2);
            gui.pose().pushPose();
            gui.pose().translate(arrowX, arrowY, 0);
            gui.pose().mulPose(new Quaternionf().rotationZ(rotation));
            gui.pose().translate(-3.0f, -3.0f, 0);
            gui.blit(DOT_ORANGE, 0, 0, 0, 0, 6, 6, 6, 6);
            gui.pose().popPose();
        }

        final String distance = String.valueOf(StructureUtils.getHorizontalDistanceToLocation(player, foundX, foundZ));
        drawText(gui, centerX, centerY, StructureUtils.getPrettyStructureName(compass.getStructureKey(stack)), I18n.get("string.biomeradar.distance") + ": " + distance);
    }

    private static final ResourceLocation DOT_GREEN = new ResourceLocation("biomeradar:textures/gui/dot_green.png");
    private static final ResourceLocation DOT_RED = new ResourceLocation("biomeradar:textures/gui/dot_red.png");
    private static final ResourceLocation DOT_GOLD = new ResourceLocation("biomeradar:textures/gui/dot_gold.png");
    private static final ResourceLocation DOT_ORANGE = new ResourceLocation("biomeradar:textures/gui/dot_orange.png");

    private void renderTrackedPlayers(GuiGraphics gui, Player player, int centerX, int centerY) {
        if (PlayerTracker.getMode() == PlayerTracker.MODE_NONE) {
            return;
        }
        final String currentDim = player.level().dimension().location().toString();
        final String myTeam = player.getTeam() == null ? "" : player.getTeam().getName();
        final java.util.List<int[]> dots = new java.util.ArrayList<>();   // {x, y, tex:0绿 1红}
        final java.util.List<int[]> arrows = new java.util.ArrayList<>(); // {x, y, rotDeg, tex}
        for (PlayerTracker.TrackedPlayer other : PlayerTracker.getTracked().values()) {
            if (!other.dimension().equals(currentDim)) {
                continue; // 其他维度的玩家不显示
            }
            if (other.name().equals(player.getName().getString())) {
                continue; // 自己
            }
            if (PlayerTracker.getMode() == PlayerTracker.MODE_SPECIFIC && !PlayerTracker.getTargetName().equals(other.name())) {
                continue;
            }
            final double dx = other.x() - player.getX();
            final double dz = other.z() - player.getZ();
            final double dist = Math.sqrt(dx * dx + dz * dz);

            final double angleToTarget = Math.atan2(dz, dx);
            final double playerYaw = Math.toRadians(player.getYRot()) + (Math.PI / 2);
            final double finalAngle = angleToTarget - playerYaw;
            final double renderAngle = finalAngle - (Math.PI / 2);

            final boolean sameTeam = !other.team().isEmpty() && other.team().equals(myTeam);
            final int radarCenterX = centerX + RADAR_CENTER_X_OFFSET;
            final int radarCenterY = centerY + RADAR_CENTER_Y_OFFSET;

            if (dist <= MAX_RANGE) {
                final double scaledDist = (dist / MAX_RANGE) * DOT_RADIUS;
                final int dotX = (int) (radarCenterX + (scaledDist * Math.cos(renderAngle)));
                final int dotY = (int) (radarCenterY + (scaledDist * Math.sin(renderAngle)));
                dots.add(new int[] { dotX, dotY, sameTeam ? 0 : 1 });
            } else {
                final int arrowX = (int) (radarCenterX + (DOT_RADIUS * Math.cos(renderAngle)));
                final int arrowY = (int) (radarCenterY + (DOT_RADIUS * Math.sin(renderAngle)));
                arrows.add(new int[] { arrowX, arrowY, (int) Math.round(Math.toDegrees(renderAngle) + 90.0D), sameTeam ? 0 : 1 });
            }
        }
        // 小圆球之间推开，避免三格以内的玩家重叠
        pushApart(dots, 4);
        for (int[] d : dots) {
            gui.blit(d[2] == 0 ? DOT_GREEN : DOT_RED, d[0] - 2, d[1] - 2, 0, 0, 5, 5, 5, 5);
        }
        for (int[] a : arrows) {
            gui.pose().pushPose();
            gui.pose().translate(a[0], a[1], 0);
            gui.pose().mulPose(new Quaternionf().rotationZ((float) Math.toRadians(a[2])));
            gui.pose().translate(-2.5f, -2.5f, 0);
            gui.blit(a[3] == 0 ? DOT_GREEN : DOT_RED, 0, 0, 0, 0, 5, 5, 5, 5);
            gui.pose().popPose();
        }
    }

    /** 简单斥力推挤：点间距小于 minDist 时互相推开，迭代两轮 */
    private void pushApart(java.util.List<int[]> points, int minDist) {
        final int minDistSq = minDist * minDist;
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < points.size(); i++) {
                for (int j = i + 1; j < points.size(); j++) {
                    final int[] a = points.get(i);
                    final int[] b = points.get(j);
                    final int ddx = b[0] - a[0];
                    final int ddy = b[1] - a[1];
                    final int dsq = ddx * ddx + ddy * ddy;
                    if (dsq < minDistSq && dsq > 0) {
                        final double dist = Math.sqrt(dsq);
                        final double push = (minDist - dist) / 2.0D;
                        final double nx = ddx / dist;
                        final double ny = ddy / dist;
                        a[0] -= (int) Math.round(nx * push);
                        a[1] -= (int) Math.round(ny * push);
                        b[0] += (int) Math.round(nx * push);
                        b[1] += (int) Math.round(ny * push);
                    }
                }
            }
        }
    }

    /** 增强雷达的龙珠探测：主世界显示地球龙珠，那美克星显示那美克星龙珠 */
    private void renderDragonBalls(GuiGraphics gui, Player player, int centerX, int centerY, boolean enhanced) {
        if (!enhanced) {
            return;
        }
        UiMemory.ensureLoaded();
        if (!UiMemory.dragonBalls) {
            return;
        }
        final String dim = player.level().dimension().location().toString();
        final java.util.List<BlockPos> positions;
        if (dim.equals("minecraft:overworld")) {
            positions = DragonBallHud.getEarthPositions();
        } else if (dim.equals("dragonminez:namek")) {
            positions = DragonBallHud.getNamekPositions();
        } else {
            return;
        }
        final int radarCenterX = centerX + RADAR_CENTER_X_OFFSET;
        final int radarCenterY = centerY + RADAR_CENTER_Y_OFFSET;
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            final double dx = pos.getX() + 0.5D - player.getX();
            final double dz = pos.getZ() + 0.5D - player.getZ();
            final double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
            final double angleToTarget = Math.atan2(dz, dx);
            final double playerYaw = Math.toRadians(player.getYRot()) + (Math.PI / 2);
            final double finalAngle = angleToTarget - playerYaw;
            final double renderAngle = finalAngle - (Math.PI / 2);
            // 距离越近越亮，越远越暗（保持金色圆点样式）
            final double brightFrac = Math.max(0.0D, Math.min(1.0D, 1.0D - (dist / 300.0D)));
            final float shade = (float) (0.85D + 0.15D * brightFrac);
            if (dist <= MAX_RANGE) {
                final double scaledDist = (dist / MAX_RANGE) * DOT_RADIUS;
                final int dotX = (int) (radarCenterX + (scaledDist * Math.cos(renderAngle)));
                final int dotY = (int) (radarCenterY + (scaledDist * Math.sin(renderAngle)));
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(shade, shade, shade, 1.0F);
                gui.blit(DOT_GOLD, dotX - 2, dotY - 2, 0, 0, 5, 5, 5, 5);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                final int arrowX = (int) (radarCenterX + (DOT_RADIUS * Math.cos(renderAngle)));
                final int arrowY = (int) (radarCenterY + (DOT_RADIUS * Math.sin(renderAngle)));
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(shade, shade, shade, 1.0F);
                gui.blit(DOT_GOLD, arrowX - 2, arrowY - 2, 0, 0, 5, 5, 5, 5);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        // 最近龙珠：方位 + 精确到 1 格的格数（金色文本，放在雷达盘底部贴图内，照龙珠模组 proximity 位置）
        if (nearest != null) {
            final double dx = nearest.getX() + 0.5D - player.getX();
            final double dz = nearest.getZ() + 0.5D - player.getZ();
            final double angle = Math.atan2(dz, dx);
            final String dir = getCompassDirection(angle);
            final int distBlocks = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            final String text = dir + " " + distBlocks + (isZh() ? "格" : "m");
            final int textX = centerX + (TEXTURE_W / 2) - (mc.font.width(text) / 2);
            // 放在雷达盘上方最顶行，与目的地文字不重叠、不压雷达
            final int textY = centerY - 44;
            gui.drawString(mc.font, text, textX, textY, 0xFFFFE27A, true);
        }
    }

    /** 绝对方位（东南西北）：atan2 角度 0=东(+X)、90=南(+Z)、180=西、-90=北(-Z) */
    private String getCompassDirection(double angle) {
        final double deg = Math.toDegrees(angle);
        final String[] dirs = isZh()
            ? new String[]{ "东", "东南", "南", "西南", "西", "西北", "北", "东北" }
            : new String[]{ "E", "SE", "S", "SW", "W", "NW", "N", "NE" };
        int idx = (int) Math.floor((deg + 22.5D) / 45.0D);
        idx = ((idx % 8) + 8) % 8;
        return dirs[idx];
    }

    private static boolean isZh() {
        return net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh");
    }

    private void drawText(GuiGraphics guiGraphics, int centerX, int centerY, String line1, String line2) {
        // 文字放在雷达盘上方，避免贴到屏幕底部被裁切
        final int textX = centerX + TEXTURE_W / 2;
        final int textY = centerY - 24;
        guiGraphics.drawCenteredString(mc.font, line1, textX, textY, 0xFFFFFF);
        if (!line2.isEmpty()) {
            guiGraphics.drawCenteredString(mc.font, line2, textX, textY + 10, 0xFFFFFF);
        }
    }

    private void drawText3(GuiGraphics guiGraphics, int centerX, int centerY, String line1, String line2, String line3) {
        final int textX = centerX + TEXTURE_W / 2;
        final int textY = centerY - 34;
        guiGraphics.drawCenteredString(mc.font, line1, textX, textY, 0xFFFFFF);
        guiGraphics.drawCenteredString(mc.font, line2, textX, textY + 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(mc.font, line3, textX, textY + 20, 0xFFFFFF);
    }

    private String getDimensionHint(ExplorersCompassItem compass, ItemStack stack) {
        final ResourceLocation key = compass.getStructureKey(stack);
        if (key == null) {
            return "";
        }
        if (com.lingmo.biomeradar.util.BiomeKeyUtils.isBiomeKey(key)) {
            return I18n.get("string.biomeradar.overworld");
        }
        final java.util.List<ResourceLocation> dims = ExplorersCompass.dimensionKeysForAllowedStructureKeys != null ? ExplorersCompass.dimensionKeysForAllowedStructureKeys.get(key) : null;
        if (dims == null || dims.isEmpty()) {
            return "";
        }
        return StructureUtils.dimensionKeysToString(dims);
    }

    private String getPrettyDimensionName(String dimLocation) {
        return StructureUtils.getDimensionDisplayName(dimLocation);
    }

}
