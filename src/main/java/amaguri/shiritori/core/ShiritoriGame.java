package amaguri.shiritori.core;

import java.util.*;

/**
 * しりとりの状態とルール（MC非依存・全プレイヤー共有の1本）。
 * サーバーはこのクラスだけを通して状態を変える。
 */
public final class ShiritoriGame {
	public static final char START = 'り';

	/** 関門。head に一度たどり着くと unlocked になる（リセットまで続く）。 */
	public enum Milestone {
		NETHER_PORTAL('ね', "shiritori:nether_gate", "ネザーゲート", "ねざーげーと"),
		BLAZE_POWDER('ぶ', "item.minecraft.blaze_powder", "ブレイズパウダー", "ぶれいずぱうだー"),
		ENDER_EYE('え', "item.minecraft.ender_eye", "エンダーアイ", "えんだーあい"),
		DRAGON('ど', "shiritori:dragon", "ドラゴン", "どらごん");

		public final char head; public final String key, display, reading;
		Milestone(char head, String key, String display, String reading) {
			this.head = head; this.key = key; this.display = display; this.reading = reading;
		}
	}

	public enum Result {
		/** 登録できた。 */ OK,
		/** ドラゴンを登録してクリア。 */ CLEARED,
		/** 頭の文字が違う（ペナルティ対象）。 */ WRONG_HEAD,
		/** 使用済み（ペナルティ対象）。 */ ALREADY_USED,
		/** 「ん」で終わった→リセット済み。 */ GAME_OVER,
		/** 読みが無い・クリア後など（ペナルティなし）。 */ INVALID;
		public boolean isPenalty() { return this == WRONG_HEAD || this == ALREADY_USED; }
	}

	private char current = START;
	private final LinkedHashMap<String, String> used = new LinkedHashMap<>(); // key -> 表示名（順番どおり）
	private final EnumSet<Milestone> unlocked = EnumSet.noneOf(Milestone.class);
	private boolean cleared;

	public ShiritoriGame() { updateUnlocks(); }

	/** 保存データから復元する。 */
	public ShiritoriGame(char current, List<String> usedKeys, List<String> usedNames, Collection<Milestone> unlocked, boolean cleared) {
		this.current = current;
		for (int i = 0; i < usedKeys.size(); i++) used.put(usedKeys.get(i), i < usedNames.size() ? usedNames.get(i) : usedKeys.get(i));
		this.unlocked.addAll(unlocked);
		this.cleared = cleared;
		updateUnlocks();
	}

	/**
	 * 言葉を出す。key は重複判定用（翻訳キーなど）、display は表示名、reading はひらがな読み。
	 */
	public Result submit(String key, String display, String reading) {
		if (cleared || reading == null || !Kana.isValidReading(reading)) return Result.INVALID;
		if (used.containsKey(key)) return Result.ALREADY_USED;
		if (!Kana.matches(Kana.head(reading), current)) return Result.WRONG_HEAD;
		if (Kana.endsWithN(reading)) {
			if (key.equals(Milestone.DRAGON.key)) {
				used.put(key, display);
				current = 'ん';
				cleared = true;
				return Result.CLEARED;
			}
			reset();
			return Result.GAME_OVER;
		}
		used.put(key, display);
		current = Kana.tail(reading);
		updateUnlocks();
		return Result.OK;
	}

	public Result submit(Milestone m) { return submit(m.key, m.display, m.reading); }

	/** 「り」からやり直し（使用済み・解禁・クリアも消す）。 */
	public void reset() {
		current = START; used.clear(); unlocked.clear(); cleared = false; updateUnlocks();
	}

	private void updateUnlocks() {
		for (Milestone m : Milestone.values()) if (Kana.matches(m.head, current)) unlocked.add(m);
	}

	public char current() { return current; }
	public boolean isUnlocked(Milestone m) { return unlocked.contains(m); }
	public Set<Milestone> unlocked() { return Collections.unmodifiableSet(unlocked); }
	public boolean isCleared() { return cleared; }
	public boolean isUsed(String key) { return used.containsKey(key); }
	public List<String> usedKeys() { return new ArrayList<>(used.keySet()); }
	public List<String> usedNames() { return new ArrayList<>(used.values()); }
	public String lastWord() { String l = null; for (String v : used.values()) l = v; return l; }

	/** ポータルに火をつけてよいか。 */
	public boolean canLightPortal() { return isUnlocked(Milestone.NETHER_PORTAL); }
	/** 今ポータルを点けたら「ネザーゲート」が登録されるか。 */
	public boolean portalRegistersNow() { return !cleared && Kana.matches(Milestone.NETHER_PORTAL.head, current) && !isUsed(Milestone.NETHER_PORTAL.key); }
	public boolean canCraftBlazePowder() { return isUnlocked(Milestone.BLAZE_POWDER); }
	public boolean canCraftEnderEye() { return isUnlocked(Milestone.ENDER_EYE); }
	/** ドラゴンにダメージが通るか（今の文字がど/と）。 */
	public boolean canHurtDragon() { return !cleared && Kana.matches(Milestone.DRAGON.head, current); }

	/** 次の目標（まだたどり着いていない最初の関門）。全部済みなら DRAGON、クリア後は null。 */
	public Milestone nextGoal() {
		if (cleared) return null;
		for (Milestone m : Milestone.values()) if (!unlocked.contains(m)) return m;
		return Milestone.DRAGON;
	}
}
