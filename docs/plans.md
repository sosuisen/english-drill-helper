# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）


# 再生の操作

- [ ] 再生ボタンを一時停止とのトグルにし、一時停止状態と再開を追加する
  - 設計案
    - AudioPlayer に pause() と resume() を足す。pause は位置を保ったまま止め、resume はそこから続ける。pause では PlaybackListener.stopped() を呼ばない（stopped は停止と末尾到達だけ）。MediaAudioPlayer は MediaPlayer.pause() / play() に対応させる
    - ViewModel は再生状態 PlaybackState { STOPPED, PLAYING, PAUSED } をプロパティとして公開する。play() を playOrPause() に置き換え、STOPPED なら再生、PLAYING なら一時停止、PAUSED なら再開。stop() は今までどおり停止（最終再生日時の記録は listener.stopped() 経由のまま）
    - 一時停止中は再生位置の表示と再生中の行はそのまま残す。一時停止中のターンのクリックはそのターンから再生（PLAYING）。一時停止中に別ユニットを選ぶと停止
    - View の再生ボタン（ID play）はそのままで、アイコンを状態に合わせて PLAY_ARROW（停止中・一時停止中）と PAUSE（再生中）に切り替える
  - テストリスト
    - [ ] ViewModel: 停止中に playOrPause() すると再生が始まり、状態は PLAYING
    - [ ] ViewModel: 再生中に playOrPause() するとプレイヤーの pause() が呼ばれ、状態は PAUSED。再生位置の表示と再生中の行は変わらない
    - [ ] ViewModel: 一時停止中に playOrPause() するとプレイヤーの resume() が呼ばれ、状態は PLAYING
    - [ ] ViewModel: 一時停止中に stop() するとプレイヤーの stop() が呼ばれ、状態は STOPPED。停止時刻は今までどおり記録される
    - [ ] ViewModel: 再生が末尾に達して stopped() が来ると、状態は STOPPED
    - [ ] ViewModel: 一時停止中にターンをクリックすると、そのターンの開始位置から再生が始まり（play が呼ばれる）、状態は PLAYING
    - [ ] ViewModel: 一時停止中に別のユニットを選ぶと、プレイヤーが停止する
    - [ ] View: 再生ボタンのアイコンは、停止中と一時停止中は PLAY_ARROW、再生中は PAUSE
    - [ ] View: 再生中に再生ボタンを押すと一時停止し、もう一度押すと再開する
    - [ ] MediaAudioPlayer: pause() と resume() は MediaPlayer の pause / play を呼ぶ（ヘッドレスでは MediaPlayer が動かないため、実機で確認。既存の MediaAudioPlayer のテスト方針に合わせる）
