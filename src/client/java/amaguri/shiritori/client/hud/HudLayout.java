package amaguri.shiritori.client.hud;

import amaguri.shiritori.core.ShiritoriGame.Milestone;

import java.util.ArrayList;
import java.util.List;

/**
 * 「状態 → 描画位置の一覧」を作る純Java部分（MC非依存）。
 * 座標は GUI 座標（スケール適用後のピクセル）。描画は {@link HudRenderer} が担当する。
 *
 * 単体テスト:
 *   javac -d /tmp/hudtest src/main/java/amaguri/shiritori/core/*.java src/client/java/amaguri/shiritori/client/hud/HudLayout.java
 *   java -cp /tmp/hudtest amaguri.shiritori.client.hud.HudLayout
 */
public final class HudLayout {
	public static final int PANEL_WIDTH = 124;
	public static final int MARGIN_LEFT = 4;
	public static final int PAD = 6;
	public static final int ICON = 12;
	public static final int ROW = 14;
	public static final int LINE = 10;
	public static final int MAX_HISTORY = 3;

	public static final int GOLD = 0xFFAA00, YELLOW = 0xFFFF55, WHITE = 0xFFFFFF, GRAY = 0xAAAAAA,
		DARK = 0x555555, GREEN = 0x55FF55, AQUA = 0x55FFFF, RULE_COLOR = 0x553F3F5F;

	/** 文字列の幅（スケール1）を返す。MC側では Font::width を渡す。 */
	public interface TextMeasure { int width(String s); }

	/** 表示に必要な状態（StatePayload から詰め替える）。 */
	public record State(String current, List<String> historyNames, List<String> historyIcons,
						List<String> unlocked, String nextGoal, boolean cleared) {}

	public enum Kind { TEXT, ICON, RULE }

	/**
	 * 描画要素1つ。TEXT: text/color/scale。ICON: iconId（item ID）, dim（暗くする）, size。RULE: 横線（w 幅）。
	 * w, h は占有サイズ（枠はみ出し検査用）。
	 */
	public record Element(Kind kind, int x, int y, int w, int h, String text, int color, int scale,
						  String iconId, boolean dim) {}

	public record Panel(int x, int y, int w, int h) {
		public int right() { return x + w; }
		public int bottom() { return y + h; }
	}

	public record Layout(Panel panel, List<Element> elements) {}

	private HudLayout() {}

	public static Layout build(State s, String keyLabel, int screenHeight, TextMeasure m) {
		int px = MARGIN_LEFT;
		int innerL = px + PAD;
		int innerW = PANEL_WIDTH - PAD * 2;
		int innerR = innerL + innerW;
		List<Element> out = new ArrayList<>();
		int y = 0; // パネル内の相対 y（あとで全体を平行移動する）

		y += 5;
		out.add(text(innerL, y, "しりとり", GOLD, 1, m));
		y += LINE + 3;

		// いまの文字（2倍）
		out.add(text(innerL, y + 6, "いまの文字", WHITE, 1, m));
		if (s.cleared()) {
			String t = "クリア！";
			int w = m.width(t);
			out.add(new Element(Kind.TEXT, innerR - w, y + 6, w, 9, t, GREEN, 1, null, false));
		} else {
			String c = s.current() == null || s.current().isEmpty() ? "？" : s.current();
			String t = "「" + c + "」";
			int w = m.width(t) * 2;
			out.add(new Element(Kind.TEXT, innerR - w, y, w, 18, t, YELLOW, 2, null, false));
		}
		y += 20;
		out.add(rule(innerL, y, innerW));
		y += 4;

		// これまで（最大3語・古い順）
		out.add(text(innerL, y, "これまで", GRAY, 1, m));
		y += LINE + 1;
		List<String> names = s.historyNames() == null ? List.of() : s.historyNames();
		List<String> icons = s.historyIcons() == null ? List.of() : s.historyIcons();
		int from = Math.max(0, names.size() - MAX_HISTORY);
		if (names.size() == from) {
			out.add(text(innerL, y + 2, "まだなし", DARK, 1, m));
			y += ROW;
		}
		for (int i = from; i < names.size(); i++) {
			String icon = i < icons.size() ? icons.get(i) : null;
			if (icon != null && !icon.isEmpty()) {
				out.add(new Element(Kind.ICON, innerL, y, ICON, ICON, null, WHITE, 1, icon, false));
			}
			int tx = innerL + ICON + 4;
			out.add(clipped(tx, y + 2, names.get(i), WHITE, innerR - tx, m));
			y += ROW;
		}
		y += 1;
		out.add(rule(innerL, y, innerW));
		y += 4;

		// 関門4つ
		out.add(text(innerL, y, "関門", GRAY, 1, m));
		y += LINE + 1;
		for (Milestone ms : Milestone.values()) {
			boolean done = s.cleared() || (s.unlocked() != null && s.unlocked().contains(ms.name()));
			boolean next = !done && ms.name().equals(s.nextGoal());
			int color = done ? GREEN : next ? AQUA : DARK;
			out.add(new Element(Kind.ICON, innerL, y, ICON, ICON, null, WHITE, 1, gateIcon(ms), !done && !next));
			int tx = innerL + ICON + 4;
			String mark = done ? "✓" : next ? "▶" : "";
			int markW = mark.isEmpty() ? 0 : m.width(mark);
			int avail = innerR - tx - (markW == 0 ? 0 : markW + 3);
			out.add(clipped(tx, y + 2, ms.head + " " + ms.display, color, avail, m));
			if (markW > 0) {
				out.add(new Element(Kind.TEXT, innerR - markW, y + 2, markW, 9, mark, color, 1, null, false));
			}
			y += ROW;
		}
		y += 1;
		out.add(rule(innerL, y, innerW));
		y += 4;

		// 操作ヒント
		out.add(clipped(innerL, y, "[" + keyLabel + "] 手のアイテムで答える", GRAY, innerW, m));
		y += LINE + 5;

		int py = Math.max(2, (screenHeight - y) / 2);
		List<Element> moved = new ArrayList<>(out.size());
		for (Element e : out) {
			moved.add(new Element(e.kind(), e.x(), e.y() + py, e.w(), e.h(), e.text(), e.color(), e.scale(), e.iconId(), e.dim()));
		}
		return new Layout(new Panel(px, py, PANEL_WIDTH, y), moved);
	}

	static String gateIcon(Milestone ms) {
		return switch (ms) {
			case NETHER_PORTAL -> "minecraft:obsidian";
			case BLAZE_POWDER -> "minecraft:blaze_powder";
			case ENDER_EYE -> "minecraft:ender_eye";
			case DRAGON -> "minecraft:dragon_head";
		};
	}

	private static Element text(int x, int y, String t, int color, int scale, TextMeasure m) {
		return new Element(Kind.TEXT, x, y, m.width(t) * scale, 9 * scale, t, color, scale, null, false);
	}

	private static Element rule(int x, int y, int w) {
		return new Element(Kind.RULE, x, y, w, 1, null, RULE_COLOR, 1, null, false);
	}

	/** maxW に収まるよう、必要なら末尾を「…」で切る。 */
	private static Element clipped(int x, int y, String t, int color, int maxW, TextMeasure m) {
		String shown = t;
		if (m.width(shown) > maxW) {
			int end = shown.length();
			while (end > 0 && m.width(shown.substring(0, end) + "…") > maxW) end--;
			shown = shown.substring(0, end) + "…";
		}
		return new Element(Kind.TEXT, x, y, m.width(shown), 9, shown, color, 1, null, false);
	}

	// ---- 自己テスト ----

	/** 全角9・半角6 の簡易幅（MC の日本語フォントに近い）。 */
	private static int fakeWidth(String s) {
		int w = 0;
		for (char c : s.toCharArray()) w += c < 0x100 ? 6 : 9;
		return w;
	}

	private static void check(boolean ok, String msg) {
		if (!ok) throw new AssertionError(msg);
	}

	private static void checkInside(Layout l, String label) {
		Panel p = l.panel();
		for (Element e : l.elements()) {
			check(e.x() >= p.x() + PAD, label + ": 左にはみ出し " + e);
			check(e.x() + e.w() <= p.right() - PAD, label + ": 右にはみ出し " + e);
			check(e.y() >= p.y() && e.y() + e.h() <= p.bottom(), label + ": 上下にはみ出し " + e);
		}
	}

	public static void main(String[] args) {
		TextMeasure m = HudLayout::fakeWidth;

		// 1) 長い名前（プレビュー相当の通常状態）
		State longName = new State("ご",
			List.of("ダークオークの原木", "クロスボウ", "マグマキューブ"),
			List.of("minecraft:dark_oak_log", "minecraft:crossbow", "minecraft:magma_cube_spawn_egg"),
			List.of("NETHER_PORTAL"), "BLAZE_POWDER", false);
		Layout l1 = build(longName, "B", 480, m);
		checkInside(l1, "長い名前");
		check(l1.elements().stream().filter(e -> e.kind() == Kind.ICON).count() == 3 + 4, "アイコン数");
		check(l1.panel().y() > 0 && l1.panel().bottom() <= 480, "縦中央");

		// 2) 異常に長い名前は「…」で切れて枠内
		String huge = "あ".repeat(40);
		State hugeName = new State("あ", List.of(huge), List.of("minecraft:apple"), List.of(), "NETHER_PORTAL", false);
		Layout l2 = build(hugeName, "B", 480, m);
		checkInside(l2, "巨大な名前");
		check(l2.elements().stream().anyMatch(e -> e.text() != null && e.text().endsWith("…")), "省略記号");

		// 3) 履歴が5語来ても直近3語だけ
		State many = new State("ご", List.of("a1", "a2", "a3", "a4", "a5"),
			List.of("minecraft:apple", "minecraft:apple", "minecraft:apple", "minecraft:apple", "minecraft:apple"),
			List.of(), "NETHER_PORTAL", false);
		Layout l3 = build(many, "B", 480, m);
		check(l3.elements().stream().filter(e -> e.kind() == Kind.ICON && !e.dim()).count() == 3 + 1, "履歴は3語まで");
		checkInside(l3, "履歴5語");

		// 4) 履歴なし・クリア後・キー名が長い
		Layout l4 = build(new State("り", List.of(), List.of(), List.of(), "NETHER_PORTAL", false), "B", 480, m);
		checkInside(l4, "履歴なし");
		State cleared = new State("ど", List.of("ドラゴン"), List.of("minecraft:dragon_head"),
			List.of("NETHER_PORTAL", "BLAZE_POWDER", "ENDER_EYE", "DRAGON"), "", true);
		Layout l5 = build(cleared, "右Shift+B", 480, m);
		checkInside(l5, "クリア後");
		check(l5.elements().stream().filter(e -> "✓".equals(e.text())).count() == 4, "関門4つ済");
		check(l5.elements().stream().noneMatch(e -> e.kind() == Kind.ICON && e.dim()), "クリア後は暗くしない");

		// 5) 状態表示: 済=✓ 次=▶ 未=暗い
		check(l1.elements().stream().filter(e -> "✓".equals(e.text())).count() == 1, "済1");
		check(l1.elements().stream().filter(e -> "▶".equals(e.text())).count() == 1, "次1");
		check(l1.elements().stream().filter(e -> e.kind() == Kind.ICON && e.dim()).count() == 2, "未2");

		// 6) 小さい画面でも壊れない
		checkInside(build(longName, "B", 120, m), "小画面");

		System.out.println("HudLayout self-test OK (panel " + l1.panel() + ", elements " + l1.elements().size() + ")");
	}
}
