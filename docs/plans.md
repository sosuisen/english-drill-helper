# Plans

TDDの作業用todo。使い捨て。

### 気づき（テストリストに入れるかはユーザーが判断）

- 起動時に253ファイル（約530MB）を同期でハッシュしている。体感で遅ければ Task でバックグラウンド化する
- クリック音のようなスパイクが長い無音を分断すると、文の境界が消える。今の教材には見つからなかったが、必要になったら「最小有音長」パラメータ（例: 100ms未満の有音を無音に吸収）を足す
- mp3 のエンコーダ遅延: javasound-mp3 は LAME タグを読まないので、デコード結果の先頭に約25ms、末尾にパディングの余分が残る（実測: 10.5秒の音声で2358サンプル = 約53ms）。再生は JavaFX Media で行うので、検出側と再生側で遅延の扱いが違う可能性がある。ターンのクリック再生で再生位置が数十ms ずれていないかを確認する

# 例外処理

- [ ] 専用の非チェック例外（AudioFolderScanException）に対する処理（今はやらない）

# 画面の調整

- [ ] 見栄え2: ターン行の役割をアイコンで示す。[Key] の代わりに鍵のアイコン、Cue の代わりに耳や吹き出しのアイコン（Material 2）。文字より目で追いやすくする
  - 設計案: TurnRow のラベルから「[Key]」を外し、セルの graphic に役割のアイコンを置く。Key は Material 2 の VPN_KEY（鍵）、Cue は ANNOUNCEMENT（吹き出しに「!」。希望の chat_info は Material 2 パックに無いため、ユーザーがこれを選んだ）、Answer と SENTENCE はアイコンなし。アイコンの色は Cue の文字色と同じ薄い青（-color-accent-muted）。ラベルは「1-1」「1-Cue」のまま
  - テストリスト
    - [ ] TurnRow: Key sentence のラベルは「1-1」になる（[Key] を付けない。既存のラベルのテストの書き換え）
    - [ ] View: Key の行のセルには鍵のアイコン（VPN_KEY）、Cue の行のセルには吹き出しのアイコン（ANNOUNCEMENT）が graphic として付く。Answer の行には graphic がない
- [x] 見栄え3: 再生中の行を、選択色ではなく Nord の青系アクセントで左端にバーを付けるなどして強調し、再生位置が一目で分かるようにする
  - 設計案: ListView の選択色はそのまま使わず、再生中の行のセルに style class「playing」を付け、unit.css で左端に 3px の青いバー（-color-accent-emphasis）と薄い青の背景（-color-accent-subtle）を描く。選択（クリック）と再生中は別の見え方になる
  - 決定: クリックで選んだ行（選択）と再生中の行は分ける。再生位置に合わせて選択を動かすのはやめ、再生中の行には playing スタイルだけを付ける。自動スクロールは再生中の行に追従させたまま
  - テストリスト
    - [x] View: 再生位置が通知されると、再生中のターンのセルに「playing」スタイルが付き、他の行には付かない。再生位置が進むと前の行から外れる
    - [x] unit.css に .list-cell.playing の規則（左端のバーと背景）がある（スタイルシートの内容の確認）
- [x] 見栄え4: Play/Stop の横に「01:23 / 03:10」の経過時間ラベルを置いて再生位置を表示する（決定: ラベルのみ。プログレスバーは付けない）
  - 設計案: ViewModel が再生位置の文字列（positionText）を公開する。形式は「mm:ss / mm:ss」で、前が再生位置、後がユニット全体の長さ（セグメント列の末尾の終了時刻）。未選択なら空、選択直後は「00:00 / 総時間」。再生位置は PlaybackListener の positionChanged で更新する
  - テストリスト
    - [x] ViewModel: ユニットを選ぶと、位置の文字列が「00:00 / 総時間」になる（5組の合成セグメント = 55.0秒なら「00:00 / 00:55」）。総時間がセグメント列の末尾から求まることを確かめる
    - [x] ViewModel: 再生位置が通知されると、前半がその位置（例: 83秒 → 01:23）になる。秒は切り捨て
    - [x] ViewModel: 選択を外すと、位置の文字列は空になる
    - [x] View: Play/Stop の並びの右に位置のラベル（ID position）があり、ViewModel の位置の文字列に接続されている
- [x] Card と中のリストの間の余白をなくす。Card の内側の余白（CSS の 1em / 0.75em）を、画面のスタイルシート（styles/unit.css）の flush クラスで 0 にし、右ペインの見出しとボタンにだけ余白を持たせる。外側の HBox の余白は 15 → 8
  - テストリスト
    - [x] View: 左右の Card に flush スタイルが付き、画面の Scene に styles/unit.css が読み込まれていることを確かめる
- [x] 現在開いているフォルダのパスをユニット一覧の Card の header に、現在開いているユニットの表示名を drillPane の Card の header に出す（Card の header を使うので Card は続ける）
  - テストリスト
    - [x] ViewModel は音声フォルダのパスをコンストラクタで受け取り、表示用の文字列として公開する
    - [x] View: unitPane の header にフォルダのパスが表示される
    - [x] View: drillPane の header に選択中ユニットの表示名（ID selectedUnitTitle、TITLE_3）が表示され、body には Play/Stop とターン一覧が残る
- [x] 再生ボタンの左側の余白を戻す（Card の body を flush にしたためボタンが枠に付いた）。Play/Stop の並び（ID playback）に左右 8px の余白を付ける
- [x] unitPane の header（音声フォルダのパス）にも AtlantaFX のスタイルを適用して、drillPane の見出しと揃える。決定: 見出しと同じ TITLE_3
- [x] unitPane の header（音声フォルダのパス）の文字色をかなり薄くする。決定: Cue の文字と同じ色（-color-accent-muted、上線とも同じ）にする。TEXT_SUBTLE では足りない
- [x] クリックで行を選択して再生したときは、位置調整のスクロールをしない。スクロールは、再生中の行が自動で（選択した行から先へ）移動したときだけ
  - 設計案: TurnListScroll.firstIndexToShow に選択中の行のインデックスを渡し、再生中の行が選択中の行と同じならスクロールしない（空を返す）。View は ListView の選択を渡す
  - テストリスト
    - [x] TurnListScroll: 再生中の行が選択中の行と同じなら、可視範囲の外でもスクロールしない（空）
    - [x] TurnListScroll: 再生中の行が選択中の行と違えば、これまでの規則（7行目以内なら維持、外れたら6行上を残す）で決まる
