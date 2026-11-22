# 検品レポート（独立検品・修正なし）

判定: **PASS**（直す必要のある欠陥なし。下に軽微な指摘と司令塔向けの所見あり）

対象: S（サーバー・通信・アイテム）、G（関門 Mixin）、C（クライアント・HUD）。apidump（javap -p）で裏取り。メソッド本体は apidump に無いので、バニラ内部の呼び出し関係（例: InventoryMenu.slotsChanged → CraftingMenu.slotChangedCraftingGrid）は既知の 1.21.2 以降の実装を前提にした。

## 1. Bキー → 登録 → 同期
- ShiritoriClient.java:25-32 キー `key.shiritori.answer`（B、カテゴリ `key.category.shiritori.main`=「しりとり」、lang 両方あり）。END_CLIENT_TICK で consumeClick → `ClientPlayNetworking.send(new AnswerPayload())`。
- ShiritoriMod.java:43-45 `serverboundPlay` に AnswerPayload、`clientboundPlay` に StatePayload。向き OK。ID `shiritori:answer` / `shiritori:state` OK。
- 受信: `ServerPlayNetworking.registerGlobalReceiver`（Fabric の play 受信はサーバースレッドで実行）→ onAnswer（ShiritoriMod.java:97）。空手は無視、しりとり棒は灰色「使えない」。`item.getDescriptionId()` と ITEM レジストリIDで submitWord。
- ShiritoriService.submitWord（:32-74）: Readings.get → 無ければ灰色「〇〇はしりとりに使えない」（ペナルティなし）→ game.submit → 結果ごとのメッセージ（文言は CONTRACT どおり）。WRONG_HEAD/ALREADY_USED は `MobEffects.WITHER, 200, 0`（ウィザーⅠ・10秒）。OK/CLEARED/GAME_OVER で `sync(server)`（PlayerLookup.all へ送信）。
- `ServerPlayer.sendSystemMessage(Component, boolean)` は apidump の ServerPlayer に宣言あり（server/level.txt:744）。
- StatePayload の形・フィールド順は CONTRACT と一字一句一致。履歴は直近3語・古い順。

## 2. しりとり棒・配布・ログイン同期・保存
- onDeath（ShiritoriMod.java:109-118）: AFTER_DEATH、攻撃者 ServerPlayer かつ directEntity==player かつメインハンドが棒。プレイヤーとドラゴンは除外。モブ名は `entity.minecraft.*` キー、アイコンは `<id>_spawn_egg` があればそれ、無ければ name_tag。
- 棒: `sword(ToolMaterial.WOOD, 3.0F, -2.4F)` = 木の剣相当。レシピ棒＋紙（不定形）OK。テクスチャ 16x16・純黒なし、stick_preview.png 256x256 あり。
- JOIN（:48-55）: markGiven（UUID を SavedData に保存）で初回だけ配布、インベントリ満杯ならドロップ。その後 `sync(player)`。
- ShiritoriState: current/used_keys/used_names/unlocked/cleared/icons/given をすべて Codec で保存・復元。復元時に ShiritoriGame を再構築（updateUnlocks も走る）。putIcon/clearIcons/markGiven/setGame で setDirty。submit 後も setDirty。
- readings.json: 2046 件（block 1300 / item 577 / entity 169）、キー形式 OK。

## 3. 関門
- ポータル（PortalShapeMixin）: `createPortalBlocks(LevelAccessor)` HEAD。canLightPortal=false なら cancel し、近くのプレイヤーに「まだ『ね』に来ていない！」。portalRegistersNow なら submitMilestone(NETHER_PORTAL)。ゲームモードは見ていない（要件どおり）。火自体は残るが膜は張られない（CONTRACT どおり）。
- クラフト: CraftingGridMixin（CraftingMenu.slotChangedCraftingGrid RETURN。作業台と 2x2 が共用）で完成品スロットを空にし、remote slot とパケットも空で送る → shift クリック・レシピ本で置いたときも空。CrafterBlockMixin（getPotentialResults RETURN）で Optional.empty → クラフターの排出とメニュー表示の両方を止める。判定は完成品のアイテム種類（Gate.isLockedCraft）。
- ドラゴン: EnderDragon.hurt(ServerLevel, EnderDragonPart, DamageSource, float)・EnderDragon.hurtServer・EnderDragonPart.hurtServer の3入口を HEAD で止める。「今の文字では倒せない！」は 40tick に1回まで。死亡は tickDeath HEAD で dragonDeathTime==0 の1回だけ submitMilestone(DRAGON)、サーバー側のみ。2体目以降は cleared → INVALID で何も起きない。

## 4. Mixin の宣言確認（apidump で再確認）
| 対象 | 宣言クラス | 結果 |
|---|---|---|
| PortalShape.createPortalBlocks(LevelAccessor) / field bottomLeft | PortalShape | 宣言あり |
| CraftingMenu.slotChangedCraftingGrid(6引数, protected static) | CraftingMenu | 宣言あり（InventoryMenu には同名なし） |
| CrafterBlock.getPotentialResults(ServerLevel, CraftingInput) | CrafterBlock | 宣言あり |
| EnderDragon.hurt(…Part…) / hurtServer / tickDeath / dragonDeathTime | EnderDragon | 宣言あり（hurt は private 版と区別して記述子指定済み） |
| EnderDragonPart.hurtServer（public final） | EnderDragonPart | 宣言あり |
mixins.json に5つすべて登録済み。client mixins は空。

## 5. HUD
- 状態は受信時に `context.client().execute` で保持、切断で clear。null の間は描画しない。F1 は `mc.gui.hud.isHidden()`（Hud.isHidden/toggle が宣言あり）、F3 は `getDebugOverlay().showDebugScreen()`（宣言あり）。
- 構成: いまの文字2倍・これまで3語（本物のアイテム描画 fakeItem）・関門4つ（済=緑✓ / 次=水色▶ / 未=暗い＋アイコン暗転）・`[キー名] 手のアイテムで答える`。左・縦中央・バニラのツールチップ枠。
- HudLayout 自己テストをこちらで再実行: `HudLayout self-test OK (panel Panel[x=4, y=146, w=124, h=187], elements 25)`。

## 6. タイトル・コマンド
- GAME_OVER: 全員にタイトル「ゲームオーバー」＋サブタイトル「『ん』で終わった…『り』からやり直し」、状態とアイコンはリセット済み。CLEARED: 全員に「しりとりクリア！」。
- `/shiritori reset|set <文字>|status`、権限 LEVEL_GAMEMASTERS(2)。set は new ShiritoriGame(c, 空, 空, 空, false)。

## 軽微な指摘（今回の FAIL 理由ではない）
1. ShiritoriState.java:30 の `DataFixTypes.LEVEL` を使うと、MC バージョンを上げたときに level.dat 用のデータ修正がこのデータにも適用される。null（データ修正なし）の方が安全。
2. EnderDragonMixin.java:30-34 は全部のダメージを止めるので、ロック中は `/kill` でもドラゴンを消せない（テストがやりにくい）。
3. CraftingGridMixin は格子が変わったときだけ再判定する。完成品を表示したまま GAME_OVER でロックに戻ると、格子に触るまで取り出せてしまう（ごくまれ）。

## 司令塔向けの所見（core / CONTRACT 側。作業者の欠陥ではない）
- Kana.matches は濁点を無視するので、ネザーゲート（ねざーげーと）を登録して「と」になった時点で DRAGON が解禁され、canHurtDragon も true になる。HUD でもドラゴンが ✓ になる。意図した仕様か確認してほしい。
- hud_preview2.png の下段（「ぶ」でブレイズパウダーが ▶）と core の動き（「ぶ」に着いた時点で解禁され ✓、▶ はエンダーアイ）が合っていない。実装は CONTRACT（unlocked / nextGoal）どおり。
