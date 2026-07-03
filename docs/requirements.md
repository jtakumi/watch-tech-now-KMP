# 要件定義書｜技術記事リーダーアプリ（Flutter版）

| 項目 | 内容 |
|---|---|
| ドキュメント名 | 技術記事リーダーアプリ 要件定義書 |
| バージョン | 1.0 |
| 対象プラットフォーム | Android / iOS |
| 採用フレームワーク | Flutter |
| 作成日 | 2026-07-03 |

---

## 1. 概要

### 1.1 目的
ZennおよびQiitaに投稿された技術記事を、単一のモバイルアプリから横断的に閲覧・検索できるようにする。両サービスのAPIを利用し、Android・iOS両対応のクロスプラットフォームアプリをFlutterで実装する。

### 1.2 背景
- Zenn・Qiitaはそれぞれ独立したアプリ／Webでの閲覧が基本であり、横断閲覧の手段が乏しい。
- 「いいね」等の書き込み機能を要件から除外することで、OAuth認証を不要とし、読み取り専用の公開APIのみで完結する構成とする。

### 1.3 用語定義
| 用語 | 定義 |
|---|---|
| 記事 | Zenn / Qiita に投稿された技術記事1件 |
| ソース | 記事の提供元サービス（Zenn または Qiita） |
| 追加読み込み | 一覧末尾到達時に次ページを取得する無限スクロール |

---

## 2. スコープ

### 2.1 対象範囲（In Scope）
- Zenn / Qiita の記事一覧表示
- 記事の全文閲覧
- キーワードによる記事検索
- 一覧の追加読み込み（無限スクロール / ページネーション）
- Android・iOS両対応

### 2.2 対象外（Out of Scope）
- いいね・LGTM・ストック等の**書き込み系操作**（本要件で明示的に除外）
- ユーザーログイン / OAuth認証
- 記事の投稿・編集
- コメント投稿
- プッシュ通知
- オフライン永続キャッシュ（初期リリースでは対象外、将来拡張候補）

---

## 3. 技術スタック

| 分類 | 採用技術 | 補足 |
|---|---|---|
| 言語 | Dart | Flutter標準 |
| フレームワーク | Flutter (stable) | Android / iOS共通UI |
| HTTP通信 | `dio` または `http` | dio推奨（インターセプタ・リトライが容易） |
| JSONパース | `json_serializable` + `freezed` | 型安全なモデル生成 |
| 状態管理 | `riverpod`（推奨）または `bloc` | 非同期状態の扱いが明快 |
| 無限スクロール | `infinite_scroll_pagination` | 定番。ページング状態を内包 |
| 本文表示（Qiita） | `flutter_widget_from_html` または `webview_flutter` | rendered_body(HTML)描画 |
| 本文表示（Zenn） | `webview_flutter` | 記事ページを直接表示 |
| Markdown表示（任意） | `flutter_markdown` | Qiitaのbody(Markdown)描画時 |
| 画像表示 | `cached_network_image` | サムネイル・アバター |
| ルーティング | `go_router` | 画面遷移 |

---

## 4. 機能要件

### 4.1 記事一覧表示（FR-01）
| 項目 | 内容 |
|---|---|
| 概要 | Zenn / Qiita の記事を一覧表示する |
| 表示項目 | タイトル、投稿者名、投稿者アイコン、いいね数（表示のみ）、投稿日、ソース種別（Zenn/Qiita）、絵文字/サムネイル |
| ソース切替 | タブまたはフィルタで Zenn / Qiita / 全件 を切り替え可能 |
| 並び順 | 新着順（デフォルト）。可能な範囲でトレンド順に対応 |

> 注: いいね「数」は記事メタ情報として**表示のみ**行い、いいね「操作」は提供しない。

### 4.2 記事閲覧（FR-02）
| 項目 | 内容 |
|---|---|
| 概要 | 一覧から選択した記事の本文を表示する |
| Qiita | APIの `rendered_body`(HTML) をHTMLウィジェットまたはWebViewで描画。`body`(Markdown)をネイティブ描画する選択肢も可 |
| Zenn | 記事ページURLをWebViewで表示（非公式APIに本文HTMLが安定して含まれないため、WebView表示を基本とする） |
| 外部リンク | 元記事をブラウザで開くボタンを設置 |

### 4.3 記事検索（FR-03）
| 項目 | 内容 |
|---|---|
| 概要 | キーワードで記事を検索する |
| Qiita | `GET /api/v2/items?query={keyword}&page={n}&per_page=20` |
| Zenn | 非公式検索エンドポイント（`/api/search`）を使用。**遮断・仕様変更のリスクありとして扱う**（後述 リスク R-01） |
| 入力 | 検索バーにキーワード入力、実行で結果一覧を表示 |
| 空状態 | 結果0件時に専用の空状態UIを表示 |

### 4.4 追加読み込み（FR-04）
| 項目 | 内容 |
|---|---|
| 概要 | 一覧末尾スクロール時に次ページを自動取得する |
| Qiita | `page` パラメータをインクリメント（`per_page=20`目安） |
| Zenn | レスポンスの `next_page` を用いて次ページ取得。`next_page` が null で終端判定 |
| UI | 末尾にローディングインジケータ表示、失敗時はリトライ導線 |
| 実装 | `infinite_scroll_pagination` の `PagingController` で共通化 |

---

## 5. 外部API仕様

### 5.1 Qiita API（公式 v2）
| 項目 | 内容 |
|---|---|
| 記事一覧 | `GET https://qiita.com/api/v2/items?page={n}&per_page={m}` |
| 検索 | `GET https://qiita.com/api/v2/items?query={keyword}` |
| 本文 | レスポンスに `body`(Markdown) / `rendered_body`(HTML) を含む |
| 認証 | 不要（読み取りのみ） |
| レート制限 | 未認証: 60リクエスト/時。認証時: 1000リクエスト/時 |
| 対応方針 | 未認証で実装。制限緩和が必要になった場合のみアクセストークン利用を検討 |

### 5.2 Zenn API（非公式）
| 項目 | 内容 |
|---|---|
| 記事一覧 | `GET https://zenn.dev/api/articles?order=latest&page={n}` |
| ページング | レスポンス `next_page` で次ページ判定（1ページ最大48件程度） |
| 検索 | `GET https://zenn.dev/api/search?q={keyword}`（非公式・非保証） |
| 認証 | 不要 |
| 対応方針 | **公式サポートがないため、いつでも変更・遮断され得る前提で実装**。Repository層で抽象化し、失敗時はWebViewフォールバックを用意 |

---

## 6. 非機能要件

| ID | 分類 | 要件 |
|---|---|---|
| NFR-01 | 対応OS | Android 8.0(API 26)以上 / iOS 14以上を目安 |
| NFR-02 | パフォーマンス | 一覧初回表示は通信成功後2秒以内を目標。スクロールは60fps維持 |
| NFR-03 | ネットワーク | 通信失敗時にエラー表示とリトライ導線を提供 |
| NFR-04 | 保守性 | ソース（Zenn/Qiita）を抽象化し、片方のAPI変更が他方に影響しない設計 |
| NFR-05 | 可用性 | Zenn非公式APIの障害時も、Qiita側は独立して稼働 |
| NFR-06 | セキュリティ | 認証情報を扱わない（トークン保存なし）。HTTPS通信のみ |
| NFR-07 | 法務 | Zenn非公式API利用について、各サービスの利用規約を公開前に確認 |

---

## 7. アーキテクチャ

### 7.1 レイヤ構成
```
Presentation (UI / Widget)
        │
State Management (Riverpod)
        │
UseCase / Service
        │
Repository（インターフェース）
   ├─ QiitaRepositoryImpl   → Qiita API v2
   └─ ZennRepositoryImpl    → Zenn 非公式API / WebViewフォールバック
        │
DataSource (dio クライアント)
```

### 7.2 設計方針
- **Repository抽象化**: `ArticleRepository` インターフェースを定義し、Zenn / Qiitaの実装を差し替え可能にする。
- **共通データモデル**: Zenn / Qiitaのレスポンスを共通の `Article` モデルにマッピングし、UI層はソースを意識しない。
- **ページング共通化**: ソースごとのページ取得ロジックを `PagingController` の背後に隠蔽する。

### 7.3 共通データモデル（例）
```dart
class Article {
  final String id;
  final ArticleSource source; // zenn / qiita
  final String title;
  final String url;
  final String authorName;
  final String authorAvatarUrl;
  final int likeCount;        // 表示のみ
  final DateTime publishedAt;
  final String? emoji;        // Zenn
  final String? renderedBody; // Qiita（本文HTML、取得できる場合）
}
```

---

## 8. 画面一覧

| 画面ID | 画面名 | 概要 |
|---|---|---|
| SCR-01 | 記事一覧 | ソースタブ + 無限スクロール一覧 |
| SCR-02 | 検索 | 検索バー + 検索結果一覧（無限スクロール） |
| SCR-03 | 記事詳細 | Qiita: HTML/Markdown描画、Zenn: WebView |
| SCR-04 | エラー / 空状態 | 通信失敗・0件時の共通表示 |

---

## 9. リスクと対応

| ID | リスク | 影響 | 対応策 |
|---|---|---|---|
| R-01 | Zenn非公式APIの仕様変更・遮断 | Zenn機能全停止 | Repository抽象化＋WebViewフォールバック。監視とフェイルセーフ |
| R-02 | Zenn検索エンドポイントの非保証 | 検索がZennで機能しない | Qiitaのみ検索対応にフォールバック可能な設計 |
| R-03 | Qiitaレート制限（未認証60/時） | 高頻度利用で429 | キャッシュ活用・リクエスト間引き。必要時トークン導入 |
| R-04 | 利用規約への抵触 | ストア公開不可 | 公開前に両サービスの規約を確認 |
| R-05 | WebView上のOSごとの挙動差 | 表示崩れ | `webview_flutter` の設定を両OSで検証 |

---

## 10. 開発フェーズ（想定）

| フェーズ | 内容 |
|---|---|
| Phase 1 (MVP) | Qiita一覧・詳細・検索・追加読み込み（公式APIで安定実装） |
| Phase 2 | Zenn一覧・追加読み込み・WebView詳細を追加 |
| Phase 3 | ソース横断表示、UI改善、エラーハンドリング強化 |
| Phase 4（任意） | オフラインキャッシュ、トレンド並び替え、ブックマーク（ローカル保存） |

---

## 11. 未確定事項 / 要決定

- [ ] Zennの「いいね数表示」を残すか、Zennでは非表示にするか
- [ ] Qiita本文をネイティブMarkdown描画にするか、WebView(HTML)にするか
- [ ] ソース横断一覧を初期リリースに含めるか、Phase 3送りにするか
- [ ] ローカルブックマーク機能を要件に加えるか
