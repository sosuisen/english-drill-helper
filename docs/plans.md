# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）


# 再生の操作

- [x] 再生ボタンを一時停止とのトグルにし、一時停止状態と再開を追加する
  - 設計案
    - AudioPlayer に pause() と resume() を足す。pause は位置を保ったまま止め、resume はそこから続ける。pause では PlaybackListener.stopped() を呼ばない（stopped は停止と末尾到達だけ）。MediaAudioPlayer は MediaPlayer.pause() / play() に対応させる
    - ViewModel は再生状態 PlaybackState { STOPPED, PLAYING, PAUSED } をプロパティとして公開する。play() を playOrPause() に置き換え、STOPPED なら再生、PLAYING なら一時停止、PAUSED なら再開。stop() は今までどおり停止（最終再生日時の記録は listener.stopped() 経由のまま）
    - 一時停止中は再生位置の表示と再生中の行はそのまま残す。一時停止中のターンのクリックはそのターンから再生（PLAYING）。一時停止中に別ユニットを選ぶと停止
    - View の再生ボタン（ID play）はそのままで、アイコンを状態に合わせて PLAY_ARROW（停止中・一時停止中）と PAUSE（再生中）に切り替える
  - テストリスト
    - [x] ViewModel: 停止中に playOrPause() すると再生が始まり、状態は PLAYING
    - [x] ViewModel: 再生中に playOrPause() するとプレイヤーの pause() が呼ばれ、状態は PAUSED。再生位置の表示と再生中の行は変わらない
    - [x] ViewModel: 一時停止中に playOrPause() するとプレイヤーの resume() が呼ばれ、状態は PLAYING
    - [x] ViewModel: 一時停止中に stop() するとプレイヤーの stop() が呼ばれ、状態は STOPPED。停止時刻は今までどおり記録される
    - [x] ViewModel: 再生が末尾に達して stopped() が来ると、状態は STOPPED
    - [x] ViewModel: 一時停止中にターンをクリックすると、そのターンの開始位置から再生が始まり（play が呼ばれる）、状態は PLAYING
    - [x] ViewModel: 一時停止中に別のユニットを選ぶと、プレイヤーが停止する
    - [x] View: 再生ボタンのアイコンは、停止中と一時停止中は PLAY_ARROW、再生中は PAUSE
    - [x] View: 再生中に再生ボタンを押すと一時停止し、もう一度押すと再開する
    - [x] MediaAudioPlayer: pause() と resume() は MediaPlayer の pause / play を呼ぶ（ヘッドレスでは MediaPlayer が動かないため、実機で確認。既存の MediaAudioPlayer のテスト方針に合わせる）

- [x] 停止の動作を変える。現在のターンを記録し、停止しても変えない。停止中に再生ボタンを押すと現在のターンの冒頭から再生する。初期状態の現在のターンは先頭ドリルの先頭ターン。ターンの再生が始まるごとに現在のターンは移ってゆく
  - 現状: 再生中のターン行（playingTurnRow）は停止しても消えず、ターン行の一覧が変わったときだけ空になる。再生ボタンは常にファイルの先頭から再生する
  - 設計案: playingTurnRow を「現在のターン」currentTurnRow に置き換える（再生中でも停止中でも、学習者がいる位置のターン）。ユニットのターン行ができた時点で先頭の行にし、再生位置の通知でターンが変わるたびに移す。ターンをクリックしたときは位置の通知を待たずにそのターンにする。停止では変えない。再生ボタン（STOPPED での playOrPause）は現在のターンの開始位置から再生する。ターン行が無いユニットでは空のままで、再生はファイルの先頭から
  - 画面: 現在のターンの行の強調（緑のバーと背景）は今の playing スタイルをそのまま使い、名前を current に改める。ユニットを選んだ直後から先頭の行が強調される（見た目の変化）
  - テストリスト
    - [x] ViewModel: ユニットを選んでターン行ができると、現在のターンは先頭ドリルの先頭ターン（ターン行の先頭）になる
    - [x] ViewModel: 再生位置が進んで別のターンに入ると、現在のターンはそのターンになる（既存の「再生中のターン行」のテストの書き換え）
    - [x] ViewModel: 再生位置が先頭ターンの開始より前（冒頭の無音）のときは、現在のターンは変わらない（既存の「空になる」テストの書き換え）
    - [x] ViewModel: ターンをクリックして再生すると、位置の通知を待たずに現在のターンはそのターンになる
    - [x] ViewModel: 停止しても現在のターンは変わらない
    - [x] ViewModel: 停止中に playOrPause() すると、現在のターンの開始位置から再生される（既存の「先頭から再生」のテストの書き換え）
    - [x] View: ユニットを選んだ直後、先頭のターン行に current スタイルが付く。再生位置が進むと移る（既存の playing スタイルのテストの書き換え）
  - 実機で見つかった不具合: 停止すると現在のターンが最後にクリックしたターンへ戻る。原因は MediaPlayer.stop() が再生位置を開始位置へ戻し、その位置が通知されること。MediaAudioPlayer の stop() で位置の購読を先に解除して直した（MediaPlayer が必要でテストできないため、ユーザーの許可を得てテスト無し。実機で確認）

# その他
- [x] アプリ名を English Drill Playerにする。
  - 名前が現れる場所（調査済み）
    - 表示名「English Drill Helper」: UnitView の TITLE（ウィンドウタイトルと HeaderBar）、App の Javadoc、pom の description、README と storymap の見出し、ADR-001 と ADR-002 の本文。テストは UnitViewTest（HeaderBar の文字）、WindowManagerTest（ウィンドウの検索）、AppTest（タイトルの対応表）
    - 技術名「english-drill-helper」: pom の artifactId と jpackage の name、SqliteDatabase の既定の DB フォルダ ~/.english-drill-helper（schema.sql のコメント、SqliteDatabaseTest、ADR-002 にも記載）、AppTest の一時フォルダ名、prd.md の dirPath
    - 略称「edh」: システムプロパティ edh.drill.db と edh.survey.folder（README、テスト、DrillBookSurveyTest）
  - 設計案: 表示名は「English Drill Player」に変える。artifactId と jpackage の name は english-drill-player に変える（インストーラの名前になる）。ADR の本文は当時の記録なので変えず、脚注で改名を記す
  - 決定（ユーザー）: DB フォルダは ~/.english-drill-player に変える（今までの記録の移行はしない）。ADR-002 の記載を改定する。システムプロパティの接頭辞は edh から edp に変える（edp.drill.db、edp.survey.folder）。README と起動オプションも書き換える
  - テストリスト
    - [x] SqliteDatabase: システムプロパティ edp.drill.db が未指定のとき、既定の DB は ~/.english-drill-player/drill.db である（SqliteDatabaseTest の書き換え）
    - [x] SqliteDatabase: システムプロパティ edp.drill.db で DB の場所を指定できる（既存テストの書き換え。edh は読まない）
    - [x] DrillBookSurveyTest: 実フォルダ検証は edp.survey.folder で有効になる（テスト自身の書き換え）
    - [x] View: HeaderBar とウィンドウタイトルの文字が「English Drill Player」である（UnitViewTest、WindowManagerTest、AppTest の書き換え）
    - [x] 文書と pom: README、storymap、pom の description と artifactId、jpackage の name を改名する（テストなし。mvn package で確認）

# バグ
- [x] ターンリストの下のほうをクリックすると、スクロールバーが上に戻ってしまう。
  - 原因: MediaAudioPlayer は currentTimeProperty を subscribe しており、subscribe は購読した瞬間に現在値（0）を通知する。ターンから再生を始めるとまず位置 0 が届き、再生中の行が先頭のターンになって一覧が先頭へスクロールする。その後に本当の位置が届いて再生中の行はクリックした行（＝選択行）に戻るが、選択行にはスクロールしない規則なので一覧は先頭のまま残る
  - 設計案: ViewModel は、再生を始めた開始位置より前の位置の通知を無視する。開始位置より前の位置はその再生のものではありえない
  - テストリスト
    - [x] ViewModel: ターンから再生を始めた直後に開始位置より前の位置（0）が通知されても、再生中のターン行と再生位置の表示は変わらない
