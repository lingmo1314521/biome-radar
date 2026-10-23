package com.lingmo.biomeradar.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lingmo.biomeradar.ExplorersCompass;
import com.lingmo.biomeradar.items.ExplorersCompassItem;
import com.lingmo.biomeradar.network.CompassSearchForNextPacket;
import com.lingmo.biomeradar.network.CompassSearchPacket;
import com.lingmo.biomeradar.network.TeleportPacket;
import com.lingmo.biomeradar.sorting.ISorting;
import com.lingmo.biomeradar.sorting.NameSorting;
import com.lingmo.biomeradar.util.CompassState;
import com.lingmo.biomeradar.util.StructureUtils;
import com.lingmo.biomeradar.util.UiMemory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ExplorersCompassScreen extends Screen {

	private static final ResourceLocation MENU_BIG = new ResourceLocation("biomeradar", "textures/gui/menu_big.png");
	private static final int PANEL_W = 141;
	private static final int PANEL_H = 213;

	// 龙珠模组任务群系伪键
	private static final List<ResourceLocation> BIOME_KEYS = new ArrayList<>();
	static {
		for (String key : com.lingmo.biomeradar.util.BiomeKeyUtils.BIOME_TARGETS.keySet()) {
			BIOME_KEYS.add(new ResourceLocation("biomeradar", key));
		}
	}

	private Level level;
	private Player player;
	private List<ResourceLocation> allowedStructureKeys;
	private List<ResourceLocation> structureKeysMatchingSearch;
	private ItemStack stack;
	private ExplorersCompassItem explorersCompass;
	private ResourceLocation foundStructureKey;
	private Button teleportButton;
	private Button trackButton;
	private Button dragonBallButton;
	private Button filterButton;
	private Button trackLocationButton;
	private Button cancelTrackButton;
	private Button cancelButton;
	private TransparentTextField searchTextField;
	private StructureSearchList selectionList;
	private String modFilter; // null=全部模组，否则为模组 id

	public ExplorersCompassScreen(Level level, Player player, ItemStack stack, ExplorersCompassItem explorersCompass, List<ResourceLocation> allowedStructureKeys) {
		super(Component.translatable("string.biomeradar.selectStructure"));
		this.level = level;
		this.player = player;
		this.stack = stack;
		this.explorersCompass = explorersCompass;

		// 恢复上次的模组筛选
		UiMemory.ensureLoaded();
		modFilter = UiMemory.modFilter;

		this.allowedStructureKeys = new ArrayList<ResourceLocation>(allowedStructureKeys);
		structureKeysMatchingSearch = new ArrayList<ResourceLocation>(getVisibleKeys());

		if (explorersCompass.getState(stack) == CompassState.FOUND) {
			foundStructureKey = explorersCompass.getStructureKey(stack);
		}
	}

	@Override
	public void onClose() {
		saveMemory();
		super.onClose();
	}

	@Override
	public void removed() {
		// 任何关闭方式（Esc/取消/传送/双击搜索）都会触发，保证记忆不丢
		saveMemory();
		super.removed();
	}

	private void saveMemory() {
		if (searchTextField != null) {
			UiMemory.search = searchTextField.getValue();
		}
		UiMemory.modFilter = modFilter;
		UiMemory.selectedKey = (selectionList != null && selectionList.hasSelection())
				? selectionList.getSelected().getStructureKey().toString() : null;
		UiMemory.save();
	}

	@Override
	public boolean mouseScrolled(double scroll1, double scroll2, double scroll3) {
		return selectionList.mouseScrolled(scroll1, scroll2, scroll3);
	}

	@Override
	protected void init() {
		setupWidgets();
	}

	@Override
	public void tick() {
		searchTextField.tick();
		// 选中任意目标即可传送/追踪（未搜索的龙珠结构会走服务端快路径直接定位）
		teleportButton.active = selectionList.hasSelection();
		trackLocationButton.active = selectionList.hasSelection();

		// Check if sync packet has been received
		if (ExplorersCompass.synced) {
			removeWidget(selectionList);
			allowedStructureKeys = new ArrayList<ResourceLocation>(ExplorersCompass.allowedStructureKeys);
			refreshVisibleKeys();
			ResourceLocation toSelect = foundStructureKey;
			if (UiMemory.selectedKey != null) {
				final ResourceLocation memKey = ResourceLocation.tryParse(UiMemory.selectedKey);
				if (memKey != null) {
					toSelect = memKey;
				}
			}
			selectionList = new StructureSearchList(this, minecraft, toSelect, getLeftPanelX() + 8, getPanelY() + 44, 128, 141, 47);
			addRenderableWidget(selectionList);

			teleportButton.visible = ExplorersCompass.canTeleport;

			ExplorersCompass.synced = false;
		}
	}

	private int getLeftPanelX() {
		return this.width / 2 - 145;
	}

	private int getRightPanelX() {
		return this.width / 2 + 2;
	}

	private int getPanelY() {
		return this.height / 2 - 105;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		// 龙珠模组菜单风格：深色渐变背景（游戏世界可见）
		int top = 0xC0 << 24 | 0x101010;
		int bottom = 0xD0 << 24 | 0x101010;
		guiGraphics.fillGradient(0, 0, this.width, this.height, top, bottom);

		// 左面板（结构列表）
		final int leftX = getLeftPanelX();
		// 右面板（操作按钮）
		final int rightX = getRightPanelX();
		final int panelY = getPanelY();
		guiGraphics.blit(MENU_BIG, leftX, panelY, 0, 0, PANEL_W, PANEL_H, 256, 256);
		guiGraphics.blit(MENU_BIG, leftX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
		guiGraphics.blit(MENU_BIG, rightX, panelY, 0, 0, PANEL_W, PANEL_H, 256, 256);
		guiGraphics.blit(MENU_BIG, rightX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);

		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		// 金色标题
		guiGraphics.drawCenteredString(font, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), leftX + 70, panelY + 17, 0xFFFFD700);
		guiGraphics.drawCenteredString(font, Component.translatable("string.biomeradar.operations").withStyle(net.minecraft.ChatFormatting.BOLD), rightX + 70, panelY + 17, 0xFFFFD700);
	}

	@Override
	public boolean keyPressed(int par1, int par2, int par3) {
		boolean ret = super.keyPressed(par1, par2, par3);
		if (searchTextField.isFocused()) {
			processSearchTerm();
			return true;
		}
		// 未聚焦时按回车直接搜索选中目标（替代搜索按钮）
		if ((par1 == GLFW.GLFW_KEY_ENTER || par1 == GLFW.GLFW_KEY_KP_ENTER) && selectionList != null && selectionList.hasSelection()) {
			searchForStructure(selectionList.getSelected().getStructureKey());
			return true;
		}
		return ret;
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		boolean ret = super.charTyped(typedChar, keyCode);
		if (searchTextField.isFocused()) {
			processSearchTerm();
			return true;
		}
		return ret;
	}

	public void searchForStructure(ResourceLocation key) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		ExplorersCompass.network.sendToServer(new CompassSearchPacket(key, false));
		minecraft.setScreen(null);
	}

	public void searchForGroup(ResourceLocation key) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		ExplorersCompass.network.sendToServer(new CompassSearchPacket(key, true));
		minecraft.setScreen(null);
	}

	public void searchForNext() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		ExplorersCompass.network.sendToServer(new CompassSearchForNextPacket());
		minecraft.setScreen(null);
	}

	public void teleport() {
		// 携带选中的目标键：服务端会优先用已找到坐标，未找到的龙珠结构走快路径直接定位
		final ResourceLocation target = selectionList.hasSelection() ? selectionList.getSelected().getStructureKey() : null;
		ExplorersCompass.network.sendToServer(new TeleportPacket(target));
		minecraft.setScreen(null);
	}

	public void processSearchTerm() {
		structureKeysMatchingSearch = new ArrayList<ResourceLocation>();
		String searchTerm = searchTextField.getValue().toLowerCase();
		for (ResourceLocation key : getVisibleKeys()) {
			if (searchTerm.startsWith("@")) {
				if (StructureUtils.getPrettyStructureSource(key).toLowerCase().contains(searchTerm.substring(1))) {
					structureKeysMatchingSearch.add(key);
				}
			} else if (matchesSearchTerm(key, searchTerm)) {
				structureKeysMatchingSearch.add(key);
			}
		}
		selectionList.refreshList(true);
	}

	/** 中英双匹配：英文 key/path、中文显示名、旧版/资源包译名别名都参与搜索 */
	private boolean matchesSearchTerm(ResourceLocation key, String searchTerm) {
		// 英文路径（rocky / elder_guru / biome_dmz_rivers）
		if (key.getPath().toLowerCase().contains(searchTerm)) {
			return true;
		}
		// 当前中文/英文显示名
		final String prettyName = StructureUtils.getPrettyStructureName(key);
		if (prettyName.toLowerCase().contains(searchTerm)) {
			return true;
		}
		// 显示名的全拼 + 首字母简写（搜 "pingyuan"/"py" 能命中"平原"）
		for (String py : com.lingmo.biomeradar.util.PinyinUtils.toPinyinForms(prettyName)) {
			if (py.contains(searchTerm)) {
				return true;
			}
		}
		// 群系伪键：英文 target + 两版译名别名
		if (com.lingmo.biomeradar.util.BiomeKeyUtils.isBiomeKey(key)) {
			for (String a : com.lingmo.biomeradar.util.BiomeKeyUtils.getSearchAliases(key.getPath())) {
				if (a.toLowerCase().contains(searchTerm)) {
					return true;
				}
			}
			return false;
		}
		// 结构别名（旧版/资源包译名）
		for (String a : StructureUtils.getStructureSearchAliases(key)) {
			if (a.toLowerCase().contains(searchTerm)) {
				return true;
			}
		}
		return false;
	}

	public Player getPlayer() {
		return player;
	}

	public List<ResourceLocation> sortStructures() {
		Collections.sort(structureKeysMatchingSearch, new NameSorting());
		return structureKeysMatchingSearch;
	}

	public String getModFilter() {
		return modFilter;
	}

	public void setModFilter(String ns) {
		if (java.util.Objects.equals(modFilter, ns)) {
			return;
		}
		modFilter = ns;
		refreshVisibleKeys();
		if (selectionList != null) {
			selectionList.refreshList(true);
		}
	}

	private void setupWidgets() {
		clearWidgets();
		final int rightX = getRightPanelX() + 20;
		final int panelY = getPanelY();

		// 追踪地点：对当前选中的目标发起搜索/追踪（不传送）
		trackLocationButton = addRenderableWidget(new PixelButton(rightX, panelY + 40, 101, 16, Component.translatable("string.biomeradar.trackLocation"), (onPress) -> {
			if (selectionList != null && selectionList.hasSelection()) {
				searchForStructure(selectionList.getSelected().getStructureKey());
			}
		}));
		trackLocationButton.active = false;

		teleportButton = addRenderableWidget(new PixelButton(rightX, panelY + 64, 101, 16, Component.translatable("string.biomeradar.teleport"), (onPress) -> {
			teleport();
		}));
		teleportButton.visible = ExplorersCompass.canTeleport;
		teleportButton.active = false;

		trackButton = addRenderableWidget(new PixelButton(rightX, panelY + 88, 101, 16, Component.translatable("string.biomeradar.trackPlayer"), (onPress) -> {
			minecraft.setScreen(new PlayerTrackScreen(this));
		}));

		// 龙珠探测开关（仅增强雷达显示）：可与其他追踪/搜索叠加
		final boolean enhanced = explorersCompass.getEnhanced(stack);
		dragonBallButton = addRenderableWidget(new PixelButton(rightX, panelY + 112, 101, 16, getDragonBallLabel(), (onPress) -> {
			UiMemory.ensureLoaded();
			UiMemory.dragonBalls = !UiMemory.dragonBalls;
			UiMemory.save();
			dragonBallButton.setMessage(getDragonBallLabel());
		}));
		dragonBallButton.visible = enhanced;

		// 取消当前追踪：清除已找到/搜索中的目标，HUD 不再显示
		cancelTrackButton = addRenderableWidget(new PixelButton(rightX, panelY + (enhanced ? 136 : 112), 101, 16, Component.translatable("string.biomeradar.cancelTrack"), (onPress) -> {
			explorersCompass.setInactive(stack, player);
			minecraft.setScreen(null);
		}));
		cancelTrackButton.visible = stack.getItem() == ExplorersCompass.explorersCompass
				&& explorersCompass.getState(stack) != com.lingmo.biomeradar.util.CompassState.INACTIVE;

		// 取消
		cancelButton = addRenderableWidget(new PixelButton(rightX, panelY + (enhanced ? 160 : 136), 101, 16, Component.translatable("gui.cancel"), (onPress) -> {
			minecraft.setScreen(null);
		}));

		final int leftX = getLeftPanelX();
		// 搜索框 + 右侧筛选按钮（筛选当前列表出现的所有模组），紧凑同行；搜索词从记忆恢复
		searchTextField = new TransparentTextField(font, leftX + 17, panelY + 32, 64, 16, Component.translatable("string.biomeradar.search"));
		searchTextField.setValue(UiMemory.search);
		addRenderableWidget(searchTextField);
		filterButton = addRenderableWidget(new PixelButton(leftX + 85, panelY + 32, 40, 16, Component.translatable("string.biomeradar.filter"), (onPress) -> {
			minecraft.setScreen(new ModFilterScreen(this, new ArrayList<ResourceLocation>(getAllKeys())));
		}));

		// 恢复上次选中的目标（优先于已找到目标）
		ResourceLocation toSelect = foundStructureKey;
		if (UiMemory.selectedKey != null) {
			final ResourceLocation memKey = ResourceLocation.tryParse(UiMemory.selectedKey);
			if (memKey != null) {
				toSelect = memKey;
			}
		}
		// 列表缩短（3 行 × 47 高）：比原版 150 短，比上一版 132 略长，底部不贴面板边框
		selectionList = new StructureSearchList(this, minecraft, toSelect, leftX + 8, panelY + 44, 128, 141, 47);
		addRenderableWidget(selectionList);
	}

	private Component getDragonBallLabel() {
		UiMemory.ensureLoaded();
		return Component.translatable("string.biomeradar.dragonRadar").append(Component.literal(": " + (UiMemory.dragonBalls ? Component.translatable("string.biomeradar.on").getString() : Component.translatable("string.biomeradar.off").getString())));
	}

	private List<ResourceLocation> getAllKeys() {
		final List<ResourceLocation> all = new ArrayList<ResourceLocation>(allowedStructureKeys);
		all.addAll(BIOME_KEYS);
		return all;
	}

	private List<ResourceLocation> getVisibleKeys() {
		final List<ResourceLocation> visible = new ArrayList<ResourceLocation>(getAllKeys());
		if (modFilter != null) {
			visible.removeIf(key -> !matchesFilter(key));
		}
		return visible;
	}

	/** 键的真实归属模组：群系伪键按目标群系/标签判断，结构按命名空间 */
	private String getKeyNamespace(ResourceLocation key) {
		if (com.lingmo.biomeradar.util.BiomeKeyUtils.isBiomeKey(key)) {
			final String path = key.getPath();
			if (path.startsWith("biome_dmz")) {
				return "dragonminez";
			}
			return "minecraft";
		}
		return key.getNamespace();
	}

	private boolean matchesFilter(ResourceLocation key) {
		if (modFilter == null) {
			return true;
		}
		return getKeyNamespace(key).equals(modFilter);
	}

	private void refreshVisibleKeys() {
		structureKeysMatchingSearch = new ArrayList<ResourceLocation>(getVisibleKeys());
	}

}
