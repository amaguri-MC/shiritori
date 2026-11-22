# 担当間の約束（変えるときは司令塔に相談）

## ルール本体（司令塔作・済）
- core/Kana, core/ShiritoriGame（submit / canLightPortal / portalRegistersNow / canCraftBlazePowder / canCraftEnderEye / canHurtDragon / nextGoal / Milestone）

## 辞書（司令塔が統合）
- src/main/resources/shiritori/readings.json
  `{ "item.minecraft.apple": {"name": "リンゴ", "reading": "りんご"}, ... }`（キーは翻訳キー。item./block./entity.）
- `amaguri.shiritori.Readings`（サーバー担当が作る）
  - `static Readings.Entry get(String translationKey)` → `record Entry(String name, String reading)`、無ければ null
  - クラスパスから1回だけ読み込む

## サーバー（担当 S）
- `amaguri.shiritori.ShiritoriState`（SavedData・ワールドに1つ・全員共有）
  - `static ShiritoriState get(MinecraftServer server)`
  - `ShiritoriGame game()`
  - 履歴アイコン: key ごとにアイテムID文字列を保存（`void putIcon(String key, String itemId)`）
- `amaguri.shiritori.ShiritoriService`
  - `static ShiritoriGame.Result submitWord(ServerPlayer player, String key, String iconItemId)`
     … Readings で名前と読みを引き、game().submit。結果ごとにメッセージ・ペナルティ・全員へ同期まで全部やる
  - `static void submitMilestone(MinecraftServer server, ServerPlayer playerOrNull, ShiritoriGame.Milestone m)`
     … ネザーゲート点火・ドラゴン撃破用。同じく後処理まで全部
  - `static void sync(MinecraftServer server)` / `static void sync(ServerPlayer player)`
- ペナルティ（WRONG_HEAD / ALREADY_USED）: ウィザーⅠを10秒（200tick）
- GAME_OVER: 全員にタイトル「ゲームオーバー」＋サブタイトル「『ん』で終わった…『り』からやり直し」。状態はリセット済み
- CLEARED: 全員にタイトル「しりとりクリア！」
- 登録成功: 本人のアクションバーに緑で「リンゴ（りんご）を登録！ 次は「ご」」、全員のチャットに「<プレイヤー> リンゴ → 次は「ご」」
- ミス: 本人のアクションバーに赤で「頭の文字が違う！（今は「ご」）ウィザー！」/「〇〇はもう使った！ ウィザー！」
- 読みが無いアイテム: 本人のアクションバーに灰色「〇〇はしりとりに使えない」（ペナルティなし）
- アイコン: アイテムは自分のID、モブはスポーンエッグ（無ければ minecraft:name_tag）、ネザーゲート=minecraft:obsidian、ドラゴン=minecraft:dragon_head

## 通信（担当 S が作る。クライアント担当 C はこの形を前提に書く）
- `amaguri.shiritori.net.AnswerPayload` C2S、フィールド無し。ID `shiritori:answer`。サーバーはメインハンドのアイテムで submitWord（空なら無視、しりとり棒自体は「使えない」扱い）
- `amaguri.shiritori.net.StatePayload` S2C、ID `shiritori:state`
  `record StatePayload(String current, List<String> historyNames, List<String> historyIcons, List<String> unlocked, String nextGoal, boolean cleared)`
  - current: 今の文字1字（表示用。例「ご」）。historyNames/Icons: 直近3語（古い順）。unlocked: Milestone.name() の一覧。nextGoal: Milestone.name() か ""（クリア後）
  - ログイン時と状態が変わるたびに全員へ送る
- 両方とも `ShiritoriMod.onInitialize` で PayloadTypeRegistry に登録（S が担当）

## アイテム（担当 S）
- `shiritori:shiritori_stick`「しりとり棒」。攻撃力は木の剣相当。これでとどめを刺したモブの名前で submitWord
- レシピ: 棒＋紙（不定形）。初参加時に1本配る

## 関門（担当 G）
- ポータル点火: canLightPortal() が false なら火がついても枠を作らない＋アクションバー「まだ『ね』に来ていない！」。portalRegistersNow() なら点火成功時に submitMilestone(NETHER_PORTAL)
- ブレイズパウダー / エンダーアイ: 解禁前は作業台・インベントリ・クラフターの完成品が出ない
- エンダードラゴン: canHurtDragon() が false ならダメージ無効（たまに「今の文字では倒せない！」をアクションバー）。死亡時に submitMilestone(DRAGON)

## クライアント（担当 C）
- Bキー（変更可、カテゴリ「しりとり」）で AnswerPayload を送る
- HUD: 画面左・縦中央。agent/ui/hud_preview2.png の見た目（バニラのツールチップ枠、いまの文字は2倍、これまで3語、関門4つ: 済=緑✓/次=水色▶/未=暗い、下に「[B] 手のアイテムで答える」）。アイコンは本物のアイテム描画
