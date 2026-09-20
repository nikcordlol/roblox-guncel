# Roblox Güncel 📲

Roblox Türkiye'de engelli olduğu için Play Store'dan güncellenemiyor. Bu uygulama,
**Roblox'un en son Android sürümünü otomatik bulup** (APKMirror üzerinden) tek dokunuşla
indirip kurulum ekranını açıyor. Kuzenlerin seni sormasına son! 🎉

## Ne yapıyor?

- Açılışta telefonuna yüklü Roblox sürümünü okur ve son sürümle karşılaştırır.
- Son sürümü sırayla şu kaynaklardan bulur (biri çalışmazsa diğerine geçer):
  1. **AppBrain** 2. **APKCombo** 3. **APKMirror RSS** 4. **Google Play**
- **"Son sürümü indir"** → APKMirror'daki release sayfasını uygulama içinde açar.
  Oradaki "DOWNLOAD APK" butonuna basınca indirme otomatik yakalanır, indirilir
  ve bittiğinde kurulum ekranı açılır.
- **"Tarayıcıda aç"** → İstemezsen aynı sayfayı Chrome'da açar (senin eski yöntemin).
- APK'yı bu uygulama barındırmaz; her zaman APKMirror'un resmî, imza doğrulamalı
  kopyasını indirir.

## APK nasıl üretilir? (Android Studio gerekmez!)

Proje, GitHub Actions ile **otomatik derlenir**:

1. Bu klasörü GitHub'da yeni bir repoya yükle (GitHub Desktop → `Add local repository` → `Publish repository`).
2. GitHub'da **Actions** sekmesine gir; `APK Derle` otomatik çalışır, bitince derleme sayfasındaki **Artifacts → apk** altında `app-release.apk` olur.
3. **Kalıcı link istiyorsan** (kuzenlere atmak için en güzeli):
   - Repoda **Releases → New release** de.
   - Tag olarak `v1` yaz, **Publish release**'e bas.
   - Actions tekrar çalışır ve APK otomatik olarak o release'e eklenir.
   - Kuzenlere vereceğin link: `https://github.com/<kullanıcı-adı>/<repo>/releases/latest`

## Kuzenler nasıl kurar?

1. APK'yı indirir.
2. İlk kurulumda Android "Bilinmeyen uygulamalar" izni ister → **İzin ver** (tek seferlik).
3. İndirme bitince kurulum ekranı kendiliğinden açılır.
4. Play Protect "Taranmamış uygulama" derse **Yükle/De yükle** demek yeterli.

## Önemli notlar

- **İmza:** `release.keystore` repoda durur; böylece her APK aynı imzayla çıkar ve
  güncellemeler eski sürümün üzerine sorunsuz kurulur. Şifreyi değiştirirsen,
  yayınlanmış APK'ların üstüne yeni APK kuramazsın (önce kaldırmak gerekir).
- APKMirror bir "robot doğrulaması" çıkararsa "Tarayıcıda aç" butonunu kullan.
- Roblox'a bağlanmak için VPN vs. bu uygulamanın kapsamı dışındadır; bu uygulama
  yalnızca son sürüm APK'yı bulup kurmayı kolaylaştırır.

## Elle derlemek istersen

- Android Studio'yla aç (JDK 17 + Gradle 8.9).
- Ya da terminalden: `gradle assembleRelease`
- İmzalı APK: `app/build/outputs/apk/release/app-release.apk`

## Proje yapısı

```
app/src/main/java/com/robloxguncel/app/
  MainActivity.kt      -> sürüm karşılaştırma + ana ekran
  VersionFetcher.kt    -> çok kaynaklı sürüm bulucu
  WebViewActivity.kt   -> APKMirror sayfası + indirme yakalama
  InstallHelper.kt     -> kurulum ekranına yönlendirme
tools/make_keystore.py -> imza dosyasını üreten script
.github/workflows/     -> otomatik derleme
```
