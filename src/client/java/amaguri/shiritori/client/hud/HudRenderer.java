package amaguri.shiritori.client.hud;

import amaguri.shiritori.net.StatePayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** MC 側の描画。配置は {@link HudLayout} が決める。 */
public final class HudRenderer implements HudElement {
	private static volatile StatePayload state;

	public static void setState(StatePayload p) { state = p; }
	public static void clear() { state = null; }

	/** キー表示名を返す（変更されたキーに追従）。 */
	private final java.util.function.Supplier<String> keyLabel;

	public HudRenderer(java.util.function.Supplier<String> keyLabel) { this.keyLabel = keyLabel; }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		StatePayload p = state;
		Minecraft mc = Minecraft.getInstance();
		if (p == null || mc.player == null) return;               // 状態を受け取る前は出さない
		if (mc.gui.hud.isHidden()) return;                        // F1
		if (mc.getDebugOverlay().showDebugScreen()) return;       // F3

		Font font = mc.font;
		HudLayout.State s = new HudLayout.State(p.current(), p.historyNames(), p.historyIcons(),
			p.unlocked(), p.nextGoal(), p.cleared());
		HudLayout.Layout layout = HudLayout.build(s, keyLabel.get(), g.guiHeight(), font::width);
		HudLayout.Panel pn = layout.panel();

		// バニラのツールチップ枠（枠線・背景は padding 分だけ内側の矩形を渡す）
		TooltipRenderUtil.extractTooltipBackground(g,
			pn.x() + TooltipRenderUtil.PADDING_LEFT, pn.y() + TooltipRenderUtil.PADDING_TOP,
			pn.w() - TooltipRenderUtil.PADDING_LEFT - TooltipRenderUtil.PADDING_RIGHT,
			pn.h() - TooltipRenderUtil.PADDING_TOP - TooltipRenderUtil.PADDING_BOTTOM, null);

		for (HudLayout.Element e : layout.elements()) {
			switch (e.kind()) {
				case RULE -> g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + 1, e.color());
				case TEXT -> drawText(g, font, e);
				case ICON -> drawIcon(g, e);
			}
		}
	}

	private static void drawText(GuiGraphicsExtractor g, Font font, HudLayout.Element e) {
		int argb = 0xFF000000 | e.color();
		if (e.scale() == 1) {
			g.text(font, e.text(), e.x(), e.y(), argb, true);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(e.x(), e.y());
		g.pose().scale(e.scale(), e.scale());
		g.text(font, e.text(), 0, 0, argb, true);
		g.pose().popMatrix();
	}

	private static void drawIcon(GuiGraphicsExtractor g, HudLayout.Element e) {
		Identifier id = Identifier.tryParse(e.iconId());
		if (id == null) return;
		Item item = BuiltInRegistries.ITEM.getValue(id);
		ItemStack stack = new ItemStack(item);
		if (stack.isEmpty()) return;
		float k = e.w() / 16f;
		g.pose().pushMatrix();
		g.pose().translate(e.x(), e.y());
		g.pose().scale(k, k);
		g.fakeItem(stack, 0, 0);
		g.pose().popMatrix();
		if (e.dim()) g.fill(e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), 0xB0201830);
	}
}
