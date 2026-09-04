# Plans

TDDの作業用todo。使い捨て。

## 決められた音声フォルダで始める

- [x] D:\Dropbox\英語のハノン_210407 を開いてファイルリストを取得する（App.start で走査して DrillViewModel に渡す）
  - [x] フォルダを開くと、その中の音声ファイルの一覧がファイル名順に得られる（FileSystemAudioFolderScanner）
  - [x] 決められた音声フォルダは App の定数 AUDIO_FOLDER に置く
  - [x] 一覧は DrillViewModel に渡す（DrillViewModel(List<Path>)、getAudioFiles()）
- [x] mp3とm4aのみリストアップする
- [x] 指定の音楽ファイルフォルダがないときは、専用の非チェック例外を投げる（AudioFolderScanException）
- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）
- [x] ファイル拡張子は大文字にも対応する

### 気づき（Greenの過程で見つけたもの。テストリストに入れるかはユーザーが判断）
- AppTest は App.start で実フォルダ D:\Dropbox\英語のハノン_210407 を走査するため、このフォルダがないマシンでは AudioFolderScanException で失敗する。「専用の非チェック例外に対する処理」で扱う
