package com.lingmo.biomeradar.client;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.util.CompassState;
import com.lingmo.biomeradar.util.PlayerTracker;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ExplorersCompass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ExplorersCompassClient {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ExplorersCompass.explorersCompass, new ResourceLocation("angle"), new ClampedItemPropertyFunction() {
                @OnlyIn(Dist.CLIENT)
                private double rotation;
                @OnlyIn(Dist.CLIENT)
                private double rota;
                @OnlyIn(Dist.CLIENT)
                private long lastUpdateTick;

                @OnlyIn(Dist.CLIENT)
                @Override
                public float unclampedCall(ItemStack stack, ClientLevel world, LivingEntity entityLiving, int seed) {
                    if (entityLiving == null && !stack.isFramed()) {
                        return 0.0F;
                    } else {
                        final boolean entityExists = entityLiving != null;
                        final Entity entity = (Entity) (entityExists ? entityLiving : stack.getFrame());
                        if (world == null && entity.level() instanceof ClientLevel) {
                            world = (ClientLevel) entity.level();
                        }

                        double rotation = entityExists ? (double) entity.getYRot() : getFrameRotation((ItemFrame) entity);
                        rotation = rotation % 360.0D;
                        double adjusted = Math.PI - ((rotation - 90.0D) * 0.01745329238474369D - getAngle(world, entity, stack));

                        // 猎人追踪式：不摆动，直接即时指向目标
                        final float f = (float) (adjusted / (Math.PI * 2D));
                        return Mth.positiveModulo(f, 1.0F);
                    }
                }

                @OnlyIn(Dist.CLIENT)
                private double wobble(ClientLevel world, double amount) {
                    if (world.getGameTime() != lastUpdateTick) {
                        lastUpdateTick = world.getGameTime();
                        double d0 = amount - rotation;
                        d0 = Mth.positiveModulo(d0 + Math.PI, Math.PI * 2D) - Math.PI;
                        d0 = Mth.clamp(d0, -1.0D, 1.0D);
                        rota += d0 * 0.1D;
                        rota *= 0.8D;
                        rotation += rota;
                    }

                    return rotation;
                }

                @OnlyIn(Dist.CLIENT)
                private double getFrameRotation(ItemFrame itemFrame) {
                    Direction direction = itemFrame.getDirection();
                    int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
                    return (double)Mth.wrapDegrees(180 + direction.get2DDataValue() * 90 + itemFrame.getRotation() * 45 + i);
                }

                @OnlyIn(Dist.CLIENT)
                private double getAngle(ClientLevel world, Entity entity, ItemStack stack) {
                    if (stack.getItem() == ExplorersCompass.explorersCompass) {
                        ExplorersCompassItem compassItem = (ExplorersCompassItem) stack.getItem();
                        // 1) 追踪地点：指向已找到的目标（优先级最高）
                        if (compassItem.getState(stack) == CompassState.FOUND) {
                            final BlockPos pos = new BlockPos(compassItem.getFoundStructureX(stack), 0, compassItem.getFoundStructureZ(stack));
                            return Math.atan2((double) pos.getZ() - entity.position().z(), (double) pos.getX() - entity.position().x());
                        }
                        // 2) 玩家追踪：指向被追踪玩家（指定玩家或同维度最近玩家）
                        if (PlayerTracker.getMode() != PlayerTracker.MODE_NONE) {
                            final java.util.Map<String, PlayerTracker.TrackedPlayer> tracked = PlayerTracker.getTracked();
                            PlayerTracker.TrackedPlayer target = null;
                            if (PlayerTracker.getMode() == PlayerTracker.MODE_SPECIFIC) {
                                target = tracked.get(PlayerTracker.getTargetName());
                            } else {
                                double best = Double.MAX_VALUE;
                                final String dim = entity.level().dimension().location().toString();
                                for (PlayerTracker.TrackedPlayer tp : tracked.values()) {
                                    if (!tp.dimension().equals(dim)) {
                                        continue;
                                    }
                                    final double d = (tp.x() - entity.position().x()) * (tp.x() - entity.position().x())
                                            + (tp.z() - entity.position().z()) * (tp.z() - entity.position().z());
                                    if (d < best) {
                                        best = d;
                                        target = tp;
                                    }
                                }
                            }
                            if (target != null) {
                                return Math.atan2(target.z() - entity.position().z(), target.x() - entity.position().x());
                            }
                        }
                        // 3) 无任何追踪：指南针模式（指向世界出生点）
                        final BlockPos pos = world.getSharedSpawnPos();
                        return Math.atan2((double) pos.getZ() - entity.position().z(), (double) pos.getX() - entity.position().x());
                    }
                    return 0.0D;
                }
            });
        });
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), "explorers_compass", new ExplorersCompassOverlay());
    }

}