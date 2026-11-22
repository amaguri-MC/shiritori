# 指示書 S: サーバー側（状態・通信・登録・しりとり棒）
先に agent/tasks/COMMON.md と agent/CONTRACT.md を読む。
目的: CONTRACT.md の「辞書 Readings」「サーバー」「通信」「アイテム」を実装する。
担当範囲: src/main/java/amaguri/shiritori/ 直下（ShiritoriMod.java, Readings.java, ShiritoriState.java, ShiritoriService.java, ModItems.java など）と net/、src/main/resources/data/shiritori/、src/main/resources/assets/shiritori/（items・models・textures・lang）。core/ と mixin/ は触らない（mixin が必要なら司令塔に相談せず不確かな点に書く）
入力: agent/CONTRACT.md、core/*.java、/home/claude/apidump
成果物: 上記ファイル。しりとり棒のテクスチャ 16x16 PNG（Python で描く。木の棒に小さな吹き出し or ひらがなの札。縁取りは黒一色にせず隣の色を暗くした色）と、それを 16 倍に拡大した agent/tasks/S/stick_preview.png
制約:
- SavedData は 26.2 の形（SavedDataType + Codec 等）を apidump で確認して書く
- モブのとどめ判定はサーバーのみ。直接攻撃（DamageSource の攻撃者が ServerPlayer、メインハンドがしりとり棒）
- 履歴アイコンのスポーンエッグは「<entity id>_spawn_egg」がアイテム登録にあるかで判定
- テスト用に `/shiritori reset` `/shiritori set <文字>` `/shiritori status` コマンド（権限2）も作る（set は game を作り直してよい: new ShiritoriGame(文字, 空, 空, 空, false)）
- readings.json はまだ無い。無いときは空として動くこと
合格基準:
- [ ] CONTRACT.md のクラス名・メソッド名・payload の形が一字一句そのまま
- [ ] 使った MC/Fabric API がすべて apidump に存在（使った主なシグネチャを agent/tasks/S/api_used.md に列挙）
- [ ] 結果ごとのメッセージ・ウィザー200tick・全員同期・ログイン時同期
- [ ] stick_preview.png があり、縁が黒一色でない
返答: 「完了 / 成果物 / 不確かな点」を3行以内
