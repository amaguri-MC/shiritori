# 指示書 C: クライアント（Bキー・HUD）
先に agent/tasks/COMMON.md と agent/CONTRACT.md を読む。
目的: CONTRACT.md の「クライアント」を実装する。見た目は agent/ui/hud_preview2.png（ユーザー承認待ちの案。左サイド・バニラのツールチップ枠）に合わせる。
担当範囲: src/client/java/amaguri/shiritori/client/、src/client/resources/
入力: agent/CONTRACT.md、agent/ui/hud_preview2.png、/home/claude/apidump（minecraft-client/ にクライアント側）
成果物: 上記。HUD の描画は「状態→描画位置の一覧」を作る純Java部分（HudLayout など）と、MC 描画部分に分ける
制約:
- StatePayload / AnswerPayload は S 担当が net/ に作る。CONTRACT の形を前提に使う（自分では作らない）
- 26.2 の HUD 登録・キー登録・GuiGraphics 相当の API は apidump で確認（推測禁止）
- 状態を受け取る前は HUD を出さない。F1 で隠れる。デバッグ画面(F3)表示中は出さない
- 文字は日本語。キー名「しりとりで答える」、カテゴリ「しりとり」
- HudLayout は javac 単体で動く自己テスト（main）を付け、長い名前（ダークオークの原木）でも枠からはみ出さないことを確かめる
合格基準:
- [ ] プレビューと同じ構成（いまの文字2倍・これまで3語・関門4つの状態表示・操作ヒント）
- [ ] 使った API がすべて apidump に存在（agent/tasks/C/api_used.md）
- [ ] HudLayout の自己テストが通る（実行結果を貼る）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
