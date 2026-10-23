package com.lingmo.biomeradar.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

/**
 * 模组筛选界面：列出当前列表中出现过内容（结构/群系）的全部模组，点击只看该模组。
 */
@OnlyIn(Dist.CLIENT)
public class ModFilterScreen extends Screen {

	private static final ResourceLocation MENU_BIG = new ResourceLocation("biomeradar", "textures/gui/menu_big.png");
	private static final int PANEL_W = 141;
	private static final int PANEL_H = 213;

	private final List<String> modIds = new ArrayList<>();   // "__ALL__" 或 modid
	private final List<String> modNames = new ArrayList<>();
	private final ExplorersCompassScreen parent;

	public ModFilterScreen(ExplorersCompassScreen parent, List<ResourceLocation> allKeys) {
		super(Component.translatable("string.biomeradar.filter"));
		this.parent = parent;
		final Set<String> nsSet = new LinkedHashSet<>();
		for (ResourceLocation key : allKeys) {
			nsSet.add(keyNamespaceOf(key));
		}
		modIds.add("__ALL__");
		modNames.add(Component.translatable("string.biomeradar.modAll").getString());
		for (String ns : nsSet) {
			modIds.add(ns);
			Optional<? extends net.minecraftforge.fml.ModContainer> container = ModList.get().getModContainerById(ns);
			modNames.add(container.isPresent() ? container.get().getModInfo().getDisplayName() : ns);
		}
	}

	private static String keyNamespaceOf(ResourceLocation key) {
		if (com.lingmo.biomeradar.util.BiomeKeyUtils.isBiomeKey(key)) {
			final String path = key.getPath();
			if (path.startsWith("biome_dmz")) {
				return "dragonminez";
			}
			return "minecraft";
		}
		return key.getNamespace();
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

		for (int i = 0; i < modIds.size(); i++) {
			final int rowY = py + 36 + i * 20;
			if (rowY > py + PANEL_H - 20) {
				break;
			}
			final boolean hovered = mouseX >= px + 8 && mouseX <= px + 133 && mouseY >= rowY && mouseY <= rowY + 20;
			final boolean selected = (modIds.get(i).equals("__ALL__") && parent.getModFilter() == null)
					|| (!modIds.get(i).equals("__ALL__") && modIds.get(i).equals(parent.getModFilter()));
			if (hovered) {
				guiGraphics.fill(px + 8, rowY, px + 133, rowY + 20, 0x30FFFFFF);
			} else if (selected) {
				guiGraphics.fill(px + 8, rowY, px + 133, rowY + 20, 0x35FFFFFF);
			}
			guiGraphics.drawString(this.font, modNames.get(i), px + 14, rowY + 6, 0xFFFFFFFF);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			final int px = this.width / 2 - 70;
			final int py = this.height / 2 - 105;
			for (int i = 0; i < modIds.size(); i++) {
				final int rowY = py + 36 + i * 20;
				if (mouseX >= px + 8 && mouseX <= px + 133 && mouseY >= rowY && mouseY <= rowY + 20) {
					parent.setModFilter(modIds.get(i).equals("__ALL__") ? null : modIds.get(i));
					// 返回主界面而不是关闭整个界面
					this.minecraft.setScreen(parent);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
