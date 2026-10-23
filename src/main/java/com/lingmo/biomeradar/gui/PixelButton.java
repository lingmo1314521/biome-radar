package com.lingmo.biomeradar.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 龙珠胶囊公司风格的橙金渐变按钮（参考 DragonMineZ / dmzclone 的按钮样式）：
 * 深色描边 + 橙金渐变底 + 米黄文字，悬停增亮，选中态描边加亮，不可用灰色。
 */
@OnlyIn(Dist.CLIENT)
public class PixelButton extends Button {

	private boolean highlighted;

	public PixelButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		this(x, y, width, height, message, onPress, false);
	}

	public PixelButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean highlighted) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.highlighted = highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		final int x = getX();
		final int y = getY();
		final int w = getWidth();
		final int h = getHeight();
		final boolean hover = this.isHoveredOrFocused();
		final boolean active = this.active;

		final int top = active ? (hover ? 0xFFF6A83E : 0xFFE8922F) : 0xFF7C6A55;
		final int bottom = active ? (hover ? 0xFFE08127 : 0xFFC96E1B) : 0xFF5D4E3F;
		final int border = !active ? 0xFF4A3D30 : (highlighted ? 0xFFFFD27A : (hover ? 0xFFFFD27A : 0xFF7A4E16));

		guiGraphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
		guiGraphics.fillGradient(x, y, x + w, y + h, top, bottom);
		guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, active ? 0x66FFE6B0 : 0x33FFFFFF);
		guiGraphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x553A1F08);

		final int textColor = active ? 0xFFFFF2CC : 0xFFBFB3A0;
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2 + (hover ? 1 : 0), textColor);
	}
}
