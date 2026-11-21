package amaguri.shiritori.core;

/** しりとり用のかな処理（MC非依存）。読みはひらがな＋「ー」だけで書く。 */
public final class Kana {
	private Kana() {}

	private static final String SMALL = "ぁぃぅぇぉっゃゅょゎゕゖ";
	private static final String BIG   = "あいうえおつやゆよわかけ";
	private static final String VOICED   = "がぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽゔ";
	private static final String UNVOICED = "かきくけこさしすせそたちつてとはひふへほはひふへほう";

	/** カタカナをひらがなにする（ーはそのまま）。 */
	public static String toHiragana(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			if (c >= 'ァ' && c <= 'ヶ') b.append((char) (c - 0x60));
			else b.append(c);
		}
		return b.toString();
	}

	/** 小さい字を大きくする（表示用の文字。濁点は残す）。 */
	public static char enlarge(char c) {
		c = toHiragana(String.valueOf(c)).charAt(0);
		int i = SMALL.indexOf(c);
		return i >= 0 ? BIG.charAt(i) : c;
	}

	/** 照合用：小さい字を大きく、濁点・半濁点を外す。 */
	public static char normalize(char c) {
		c = enlarge(c);
		int i = VOICED.indexOf(c);
		return i >= 0 ? UNVOICED.charAt(i) : c;
	}

	/** 最初の文字（表示用）。ーは飛ばす。無ければ 0。 */
	public static char head(String reading) {
		String r = toHiragana(reading);
		for (int i = 0; i < r.length(); i++) if (r.charAt(i) != 'ー') return enlarge(r.charAt(i));
		return 0;
	}

	/** 最後の文字（表示用）。末尾のーは無視、小さい字は大きく。無ければ 0。 */
	public static char tail(String reading) {
		String r = toHiragana(reading);
		for (int i = r.length() - 1; i >= 0; i--) if (r.charAt(i) != 'ー') return enlarge(r.charAt(i));
		return 0;
	}

	public static boolean endsWithN(String reading) {
		return tail(reading) == 'ん';
	}

	/** 2文字がしりとりとしてつながるか（濁点・小文字の違いは無視）。 */
	public static boolean matches(char a, char b) {
		return a != 0 && b != 0 && normalize(a) == normalize(b);
	}

	/** 読みとして正しい形か（ひらがなとーだけ、1文字以上）。 */
	public static boolean isValidReading(String s) {
		if (s == null || s.isEmpty()) return false;
		for (char c : s.toCharArray()) if (!(c == 'ー' || (c >= 'ぁ' && c <= 'ゖ'))) return false;
		return head(s) != 0;
	}
}
