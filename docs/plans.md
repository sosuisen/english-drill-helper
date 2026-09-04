# Plans

TDDの作業用todo。使い捨て。

## 最終再生日時を確認できる

- [x] 再生の停止を検出し、その時点での時刻を取得
- [x] 音声ファイルの指紋（内容全体のSHA-256、小文字16進）を計算する
- [x] 最終再生日時がSQLiteに記録される
- [ ] リストに最終再生日時が記録される

### 気づき（Greenの過程で見つけたもの。テストリストに入れるかはユーザーが判断）

- App の DB パスが ~/.english-drill-helper/drill.db にベタ書き。AppTest が実際のユーザーホームに DB を作ってしまう。ADR-002 のとおり edh.drill.db で上書きできるようにし、AppTest は @TempDir を指すようにする
- 指紋の付与が App の中にある。ADR-002 では走査時に付与するとしているので、FileSystemAudioFolderScanner が Fingerprinter を受け取り List<AudioFile> を返す形に寄せる
- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）
