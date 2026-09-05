# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）

# 画面の調整

- [ ] ユニットリストを ListView から TableView にする。列は File（ファイル名）と Last played（最終再生日時）の2列（決定）
  - テストリスト
  - 決定: アイコンパックは Material 2（ikonli-material2-pack 12.4.0）。Play は PLAY_ARROW、Stop は STOP
  - テストリスト
  - テストリスト
  - テストリスト
- [ ] 見栄え良くするアイデアを出す（アイデアはユーザーと相談）
- [ ] Windows のメニューバー拡張（JavaFX のプレビュー機能 StageStyle.EXTENDED と HeaderBar）を使う。参考: sss-music-player プロジェクト。-Djavafx.enablePreview=true を surefire、javafx-maven-plugin、jpackage の3箇所に書く（JavaFX-MVVM.md）
