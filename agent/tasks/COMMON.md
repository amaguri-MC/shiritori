# 共通ルール（しりとりサバイバルMod）
- リポ: /home/claude/shiritori（Fabric / Minecraft 26.2 / Java 25 / modid=shiritori / パッケージ amaguri.shiritori）
- ここではビルドできない（Gradle不可）。26.2 の API は /home/claude/apidump を grep して必ず裏を取る（minecraft/ と fabric/ に javap -p の出力。files/ にバニラの lang・レシピJSON）。推測でメソッド名を書かない
- ルール本体は src/main/java/amaguri/shiritori/core/（Kana, ShiritoriGame）。MC非依存。勝手に変えない
- 担当間の約束は agent/CONTRACT.md。そこに無いクラス・メソッドを他担当に期待しない
- 担当範囲のファイル以外は触らない。git の commit / push はしない（司令塔がやる）
- Mixin: @Shadow / @Inject 先はターゲットクラス自身が宣言しているメンバーだけ（apidump で確認）。mixin を足したら json に登録
- 画面に出る文字は日本語。lang は en_us.json にも日本語を入れる
- 返答は3行以内（完了 / 成果物 / 不確かな点）
