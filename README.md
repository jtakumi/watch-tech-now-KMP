# Watch Tech Now (Kotlin Multiplatform)

Zenn と Qiita の技術記事を横断して閲覧するアプリの Kotlin Multiplatform / Compose Multiplatform プロジェクトです。

## 対象プラットフォーム

- Android 8.0（API 26）以上
- iOS 14.0 以上
- Kotlin ターゲット: `androidTarget` / `iosArm64` / `iosSimulatorArm64`

Compose Multiplatform 1.11.1 の公式サポート範囲は iOS 14 以上のため、要件を満たします。

## 必要な環境

- JDK 17
- Android Studio（Android SDK 36）
- Xcode 16 以降
- Kotlin Multiplatform IDE plugin（推奨）

Android SDK の場所が自動検出されない場合は、プロジェクト直下に次の `local.properties` を作成してください。

```properties
sdk.dir=/path/to/Android/sdk
```

## Android

デバッグ APK をビルドします。

```shell
./gradlew :composeApp:assembleDebug
```

Android Studio で `composeApp` の Android 実行構成を選択すると、実機またはエミュレータで起動できます。

## iOS

Kotlin/Native の Simulator framework をビルドします。

```shell
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

続いて `iosApp/iosApp.xcodeproj` を Xcode で開き、署名チームと Simulator を選択して `iosApp` スキームを実行します。Xcode の Build Phase が `ComposeApp` framework を自動的に生成・埋め込みします。

## テスト

```shell
./gradlew :composeApp:testDebugUnitTest
```

## セキュリティ

Android Manifest で `android:usesCleartextTraffic="false"` を指定しています。iOS は App Transport Security の既定値を維持しており、任意の HTTP 通信を許可する例外は設定していません。
