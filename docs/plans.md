# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）

# 画面の調整

- [x] ListView の行の高さを狭くする（AtlantaFX の dense スタイル）
- [x] ListView の行の色を交互に変える（AtlantaFX の striped スタイル）
- [x] ドリル間の区切りを見た目で判り易くする。決定: 各ドリルの先頭行に上線（border-top）を引く。見出し行は入れない（どの行もクリックで再生できるようにするため）
- [x] ターン番号の表示: 内部番号（Turn.number）は変えず、表示上の番号は Cue のターンを飛ばして数える。Cue の行には番号の代わりに「ドリル番号-Cue」（例: 1-Cue）と書く
- [x] Cue の行の文字色を薄くする
- [x] 再生位置に追従した選択行が仮想リストの可視範囲を超えたとき、自動的にスクロールして見えるようにする
- [x] 自動スクロールの改善: 再生中の行が可視範囲の先頭から7行目以内ならスクロールしない。外れたら、その行が7行目（6行上を残す）に来る位置までスクロールする。scrollTo だけだと再生中の行が一番上に来て、前の行に戻りにくいため
- [x] Cue の灰色をもっと薄くする（-color-fg-muted → -color-fg-subtle）
- [x] ドリルの区切り（上線）を緑色にする（-color-success-emphasis）
- [ ] ユニットリストを ListView から TableView にする（列の構成はテストリストで決める）
- [x] Play と Stop のボタンをアイコンにする（Ikonli 12.4.0、Material 2）
- [x] Cue の灰色をさらに薄くする: derive(-color-fg-subtle, 70%)（実機で確認して決定）
- [x] ドリルの区切りの上線を 2px、薄い青（-color-accent-muted）にする
- [x] Cue の文字色を上線と同じ -color-accent-muted にする
- [x] Unit 0.x の Cue 判定を長さのしきい値（0.8秒未満）から「等しい長さの対に属さない有音」に変える。Unit 0.5 は文が 0.72秒の対で、Cue と誤判定されて 1-1、1-2、2-Cue、3-Cue になっていたため
- [x] 選択ユニットが変更されたら、再生を中止し、再生中（選択中）のターンも解除する
- [x] images/icon.png をこのアプリのアイコンにする
  - テストリスト
    - [x] アイコン画像を src/main/resources/images/icon.png に置き、ウィンドウを表示するとステージのアイコンにその画像が設定されていることを確かめる（WindowManager.showWindow）
    - [x] jpackage の配布物のアイコンにも使う。images/icon.ico（ffmpeg で PNG から変換）、images/icon.icns（ユーザー提供）、Linux は icon.png。pom.xml の OS 別プロファイルで jpackage.icon プロパティに指定
  - 決定: アイコンパックは Material 2（ikonli-material2-pack 12.4.0）。Play は PLAY_ARROW、Stop は STOP
  - テストリスト
    - [x] Play ボタンと Stop ボタンは文字ではなくアイコン（Ikonli の FontIcon、Material 2 の PLAY_ARROW と STOP）を持ち、AtlantaFX のアイコンボタンのスタイル（BUTTON_ICON）である。既存のクリックのテスト（#play、#stop）が通ることも確かめる
- [ ] 選択中ファイルの表示欄では、冒頭の「ddd_」と末尾の「.mp3」を除いた名前（例: Unit 1.1_slow）を表示する
  - テストリスト
    - [ ] Unit.title() は、ファイル名の先頭の「数字_」と末尾の拡張子を除いた名前を返す。011_Unit 1.1_slow.mp3 → Unit 1.1_slow、001_Unit 0.1.mp3 → Unit 0.1。番号や拡張子がないファイル名はそのまま
    - [ ] ViewModel の選択中の表示は、ファイル名ではなくユニットの表示名（title）になる。未選択なら空
    - [ ] View の表示欄（ラベル）に表示名が出る
