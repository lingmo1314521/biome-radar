package com.lingmo.biomeradar.gui;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 龙珠风格结构列表（借鉴 dmzclone）：
 * - 行悬停/选中用柔和半透明白高亮，选中行左侧一条金色指示条
 * - 滚动条：右侧常驻浅色滑块 + 深色细轨（可按住拖动）
 */
@OnlyIn(Dist.CLIENT)
public class StructureSearchList extends ObjectSelectionList<StructureSearchEntry> {

	private static final int TRACK_X_OFF = 125;   // 轨道 x = getLeft() + TRACK_X_OFF
	private static final int SCROLLBAR_HOT = 0xFFEAF7FF;
	private static final int SCROLLBAR_COLD = 0xFFD7E8DE;
	private static final int SCROLLBAR_ACCENT_HOT = 0xFF7CFDD6;
	private static final int SCROLLBAR_ACCENT_COLD = 0xFF9FB8AE;

	private final ExplorersCompassScreen parentScreen;
	private boolean scrollDragging = false;
	private long lastClickTime = 0L;
	private StructureSearchEntry lastClickEntry = null;

	public StructureSearchList(ExplorersCompassScreen parentScreen, Minecraft mc, ResourceLocation structureKeyToSelect, int x, int y, int width, int height, int slotHeight) {
		super(mc, width, height, y, y + height, slotHeight);
		this.parentScreen = parentScreen;
        setLeftPos(x);
		refreshList(structureKeyToSelect);
	}

	@Override
	protected int getScrollbarPosition() {
		return getLeft() + TRACK_X_OFF;
	}

	@Override
	public int getRowWidth() {
		// 行宽略小于列表宽，留出右侧滚动条空间
		return getWidth() - 8;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 显式裁剪到列表可视区域：滚动出去的条目文字不会画出面板/边框
        guiGraphics.enableScissor(getRowLeft() - 1, getTop(), getRight(), getBottom());
        // Render backgrounds
        for (int i = 0; i < getItemCount(); ++i) {
            if (getRowBottom(i) >= getTop() && getRowTop(i) <= getBottom()) {
                StructureSearchEntry entry = getEntry(i);
                boolean hovering = isMouseOver(mouseX, mouseY) && Objects.equals(getEntryAtPosition(mouseX, mouseY), entry);
                int fillColor;
                if (!entry.isEnabled()) {
                    fillColor = 0x0AFFFFFF;
                } else if (entry == getSelected()) {
                    fillColor = 0x2AFFFFFF;
                } else if (hovering) {
                    fillColor = 0x16FFFFFF;
                } else {
                    fillColor = 0x00000000;
                }
                if (fillColor != 0x00000000) {
                    guiGraphics.fill(getRowLeft(), getRowTop(i), getRight() - 8, getRowBottom(i), fillColor);
                }
                // 选中行：左侧金色指示条（替代边框）
                if (entry == getSelected()) {
                    final int top = getRowTop(i);
                    final int bottom = getRowBottom(i);
                    guiGraphics.fill(getRowLeft(), top + 2, getRowLeft() + 2, bottom - 2, 0xFFF6A83E);
                }
            }
        }
        // Render entries
        for (int i = 0; i < getItemCount(); ++i) {
            int top = getRowTop(i);
            int bottom = getRowBottom(i);
            if (bottom >= getTop() && top <= getBottom()) {
                StructureSearchEntry entry = getEntry(i);
                boolean isHovering = isMouseOver(mouseX, mouseY) && Objects.equals(getEntryAtPosition(mouseX, mouseY), entry);
                entry.render(guiGraphics, i, top, getRowLeft(), getRowWidth(), itemHeight, mouseX, mouseY, isHovering, partialTicks);
            }
        }
        guiGraphics.disableScissor();
        renderScrollBar(guiGraphics, mouseX, mouseY);
	}

	/** 龙珠风格滚动条：深色细轨 + 浅色滑块（照 dmzclone） */
	private void renderScrollBar(GuiGraphics g, int mouseX, int mouseY) {
		int trackX = getScrollbarPosition();
		int top = getTop();
		int listH = getBottom() - getTop();
		g.fill(trackX, top + 2, trackX + 3, top + listH - 2, 0x66202020);
		g.fill(trackX, top + 2, trackX + 1, top + listH - 2, 0x55BFE3C6);
		float max = getMaxScroll();
		if (max <= 0.01F) {
			return;
		}
		float frac = (float) listH / (float) getMaxPosition();
		int thumbH = Math.max(16, Math.round(listH * frac));
		int thumbY = top + Math.round((listH - thumbH) * ((float) getScrollAmount() / max));
		boolean hot = scrollDragging || (mouseX >= trackX - 2 && mouseX <= trackX + 4 && mouseY >= thumbY && mouseY <= thumbY + thumbH);
		g.fill(trackX - 1, thumbY, trackX + 4, thumbY + thumbH, hot ? SCROLLBAR_HOT : SCROLLBAR_COLD);
		g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, hot ? SCROLLBAR_ACCENT_HOT : SCROLLBAR_ACCENT_COLD);
	}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 点击滑块：开始拖动（照 dmzclone scrollDragging 机制）
            if (getMaxScroll() > 0 && overScrollBar(mouseX, mouseY)) {
                scrollDragging = true;
                setScrollFromMouse(mouseY);
                return true;
            }
            // 双击列表行直接搜索该目标（替代右侧搜索按钮）
            final StructureSearchEntry entry = getEntryAtPosition(mouseX, mouseY);
            final long now = net.minecraft.Util.getMillis();
            if (entry != null && entry.isEnabled()) {
                if (entry == lastClickEntry && now - lastClickTime < 250L) {
                    lastClickEntry = null;
                    lastClickTime = 0L;
                    parentScreen.searchForStructure(entry.getStructureKey());
                    return true;
                }
                lastClickEntry = entry;
                lastClickTime = now;
            } else {
                lastClickEntry = null;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollDragging) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollDragging && button == 0) {
            scrollDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

	private boolean overScrollBar(double mx, double my) {
		int trackX = getScrollbarPosition();
		return mx >= trackX - 2 && mx <= trackX + 6 && my >= getTop() && my <= getBottom();
	}

	private void setScrollFromMouse(double my) {
		int top = getTop();
		int listH = getBottom() - getTop();
		float frac = (float) (my - top) / listH;
		float max = getMaxScroll();
		setScrollAmount(Mth.clamp(max * frac, 0.0D, max));
	}

    @Override
    public void setSelected(StructureSearchEntry entry) {
        if (entry == null || entry.isEnabled()) {
            super.setSelected(entry);
        }
    }

    public void refreshList(ResourceLocation structureKeyToSelect) {
		clearEntries();
		for (ResourceLocation key : parentScreen.sortStructures()) {
			StructureSearchEntry entry = new StructureSearchEntry(this, key, parentScreen.getPlayer());
			addEntry(entry);
			if (key.equals(structureKeyToSelect)) {
				setSelected(entry);
			}
		}
		setScrollAmount(0);
	}

	public void refreshList(boolean maintainSelection) {
		ResourceLocation select = maintainSelection && hasSelection() ? getSelected().getStructureKey() : null;
		refreshList(select);
	}

	public boolean hasSelection() {
		return getSelected() != null;
	}

	public ExplorersCompassScreen getParentScreen() {
		return parentScreen;
	}

}
