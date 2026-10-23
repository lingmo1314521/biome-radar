package com.lingmo.biomeradar.gui;

import java.util.ArrayList;
import java.util.List;

import com.lingmo.biomeradar.util.PlayerTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 追踪玩家选择界面：取消追踪 / 追踪全部玩家 / 指定玩家。
 * 同队伍玩家绿色显示，其他玩家红色显示。
 */
@OnlyIn(Dist.CLIENT)
public class PlayerTrackScreen extends Screen {

	private static final ResourceLocation MENU_BIG = new ResourceLocation("biomeradar", "textures/gui/menu_big.png");
	private static final int PANEL_W = 141;
	private static final int PANEL_H = 213;

	private final List<String> options = new ArrayList<>();
	private final List<Integer> optionColors = new ArrayList<>();
	private final ExplorersCompassScreen parent;

	public PlayerTrackScreen(ExplorersCompassScreen parent) {
		super(Component.translatable("string.biomeradar.trackPlayer"));
		this.parent = parent;
		buildOptions();
	}

	private void buildOptions() {
		final Player me = Minecraft.getInstance().player;
		options.add("__NONE__");
		optionColors.add(0xFFFFF2CC);
		options.add("__ALL__");
		optionColors.add(0xFFFFF2CC);
		for (Player p : PlayerTracker.getVisiblePlayers()) {
			if (p == me) {
				continue;
			}
			options.add(p.getName().getString());
			optionColors.add(PlayerTracker.isSameTeam(me, p) ? 0xFF40E040 : 0xFFE04040);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		final int top = 0xC0 << 24 | 0x101010;
		final int bottom = 0xD0 << 24 | 0x101010;
		guiGraphics.fillGradient(0, 0, this.width, this.height, top, bottom);

		final int px = this.width / 2 - 70;
		final int py = this.height / 2 - 105;
		guiGraphics.blit(MENU_BIG, px, py, 0, 0, PANEL_W, PANEL_H, 256, 256);
		guiGraphics.blit(MENU_BIG, px + 17, py + 10, 142, 22, 107, 21, 256, 256);
		guiGraphics.drawCenteredString(this.font, this.title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), px + 70, py + 17, 0xFFFFD700);

		for (int i = 0; i < options.size(); i++) {
			final int rowY = py + 36 + i * 20;
			if (rowY > py + PANEL_H - 20) {
				break;
			}
			final boolean hovered = mouseX >= px + 8 && mouseX <= px + 133 && mouseY >= rowY && mouseY <= rowY + 20;
			if (hovered) {
				guiGraphics.fill(px + 8, rowY, px + 133, rowY + 20, 0x30FFFFFF);
			}
			final String label = switch (options.get(i)) {
				case "__NONE__" -> Component.translatable("string.biomeradar.trackOff").getString();
				case "__ALL__" -> Component.translatable("string.biomeradar.trackAll").getString();
				default -> options.get(i);
			};
			guiGraphics.drawString(this.font, label, px + 14, rowY + 6, optionColors.get(i));
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			final int px = this.width / 2 - 70;
			final int py = this.height / 2 - 105;
			for (int i = 0; i < options.size(); i++) {
				final int rowY = py + 36 + i * 20;
				if (mouseX >= px + 8 && mouseX <= px + 133 && mouseY >= rowY && mouseY <= rowY + 20) {
					final String sel = options.get(i);
					if (sel.equals("__NONE__")) {
						PlayerTracker.setMode(PlayerTracker.MODE_NONE);
					} else if (sel.equals("__ALL__")) {
						PlayerTracker.setMode(PlayerTracker.MODE_ALL);
						PlayerTracker.setTargetName("");
					} else {
						PlayerTracker.setMode(PlayerTracker.MODE_SPECIFIC);
						PlayerTracker.setTargetName(sel);
					}
					// 返回主界面而不是关闭整个界面
					this.minecraft.setScreen(parent);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
