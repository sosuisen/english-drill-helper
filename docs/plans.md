# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）

# 画面の調整

- [x] ユニットリストを ListView から TableView にする。列は File（ファイル名）と Last played（最終再生日時）の2列（決定）
  - テストリスト
    - [x] View の ID units は TableView で、items が ViewModel のユニット一覧に接続されている（ListView のテストの置き換え）
    - [x] 列は File と Last played の2列で、File 列にファイル名、Last played 列に最終再生日時（yyyy-MM-dd HH:mm、未再生は空）が表示される（セルのテストの置き換え）
    - [x] 行をクリックすると、そのユニットが選択される（既存のクリックのテストが通る）
    - [x] dense と striped のスタイルは TableView にも付く（既存のスタイルのテストが通る）
    - [x] File 列は残りの幅を使い、Last played 列は日時が収まる固定幅にする（列幅の方針。重要なデザイン上の決定なのでテストを残す）
- [x] 見栄え良くするアイデアを出す。採用: 1（ユニット名を見出しに）、5（左右のペインを Card に）、8（Windows のメニューバー拡張）を先に、2（役割のアイコン）、3（再生中の行の強調）、4（再生位置の表示）を後に。除外: 6（相対時刻）、7（進捗のチェック列）、9（Nord Dark 切り替え）
- [x] 見栄え1: 選択中のユニット名を大きめの見出し（AtlantaFX の TITLE_3）にし、その下に Play/Stop を並べる
  - [x] View: ユニット名のラベルに TITLE_3 のスタイルが付いていることを確かめる
- [x] 見栄え5: 左右のペインを AtlantaFX の Card にして余白を揃える
  - [x] View: ユニット一覧を含む左ペイン（ID unitPane）と、見出し・ボタン・ターン一覧を含む右ペイン（ID drillPane）が Card であることを確かめる
- [x] Windows のメニューバー拡張（JavaFX のプレビュー機能 StageStyle.EXTENDED と HeaderBar）を使う。参考: sss-music-player プロジェクト。-Djavafx.enablePreview=true を surefire、javafx-maven-plugin、jpackage の3箇所に書く（JavaFX-MVVM.md）
  - テストリスト
    - [x] UnitView は stageStyle() で EXTENDED を宣言する（WindowManager がその様式でウィンドウを開く既存の仕組みを使う）
    - [x] 画面の上部に HeaderBar（ID headerBar）があり、その中にアプリ名「English Drill Helper」が表示される
    - [x] WindowManagerTest の期待を DECORATED から EXTENDED に変える
    - [x] -Djavafx.enablePreview=true を surefire、javafx-maven-plugin、jpackage の3箇所に書き、README にプレビュー機能の注意を書く（準備・文書）
- [x] アプリの既定のウィンドウ幅を3割大きくする（480 → 624）
