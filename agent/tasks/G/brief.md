# 指示書 G: 関門（ポータル・クラフト・ドラゴン）
先に agent/tasks/COMMON.md と agent/CONTRACT.md を読む。
目的: CONTRACT.md の「関門」を Mixin / Fabric イベントで実装する。
担当範囲: src/main/java/amaguri/shiritori/mixin/、src/main/java/amaguri/shiritori/gate/、src/main/resources/shiritori.mixins.json
入力: agent/CONTRACT.md、core/*.java、/home/claude/apidump
成果物: 上記
制約:
- 状態は ShiritoriState.get(server).game()、登録は ShiritoriService.submitMilestone、メッセージは player.displayClientMessage(Component, true) で（S 担当が並行して作る。CONTRACT のシグネチャどおりに呼ぶ）
- ブレイズパウダー・エンダーアイの判定は完成品のアイテム種類で（レシピIDに頼らない）。作業台・インベントリ(2x2)・クラフター(Crafter ブロック)の3経路を塞ぐ。shift クリックでも出ないこと
- ポータル: 火打石・ファイヤーチャージ・自然発火など、枠が作られる入口1か所で判定する（PortalShape 周辺を apidump で調べる）。ゲームモード問わず
- ドラゴン: EnderDragon と、ドラゴンの部位（EnderDragonPart）経由のダメージの両方。死亡の検出は 1 回だけ submitMilestone
- @Inject/@Shadow の対象がターゲットクラス自身で宣言されているか apidump で確認し、確認結果を agent/tasks/G/mixin_check.md に表で書く
合格基準:
- [ ] 3つの関門がすべて実装され、ロック中は作れない／倒せない
- [ ] mixin_check.md の全行が「宣言あり」
- [ ] mixins.json に全 mixin が登録
返答: 「完了 / 成果物 / 不確かな点」を3行以内
