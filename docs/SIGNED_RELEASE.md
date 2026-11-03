# EYMEN GELİR — İmzalı GitHub Releases yönergesi

Android güncellemelerinde APK'ların **aynı kalıcı imzalama anahtarı** ile imzalanması gerekir. İmzalama anahtarı kaybolursa bu kimlikle gelecekte normal güncelleme dağıtılamaz.

## 1. Anahtarın saklanması
- Android Studio veya Java `keytool` ile 10.000 gün geçerli imzalama anahtarı (`.jks`) oluşturun.
- Özel anahtarı, şifrelerini, base64 içeriğini README, Issues veya herkese açık GitHub deposuna **asla** eklemeyin.
- Anahtar dosyasını ve şifrelerini kayıplara karşı erişimi kısıtlı en az iki güvenli konumda saklayın.

## 2. GitHub Secrets
`https://github.com/mmustafatasoy/Gelirgider/settings/secrets/actions` sayfasında şu dört *Repository secret* oluşturun:

| GitHub secret | Değer |
|---|---|
| `EYMEN_KEYSTORE_BASE64` | Anahtar dosyasının tek satırlık base64 kodlaması |
| `EYMEN_KEYSTORE_PASSWORD` | Anahtar deposu şifresi |
| `EYMEN_KEY_ALIAS` | İmzalama anahtarının alias değeri |
| `EYMEN_KEY_PASSWORD` | Anahtar parolası (store parolası ile aynı olabilir) |

Örneğin bilgisayarda base64: `base64 -w 0 eymen-release.jks` (Linux). Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("eymen-release.jks"))`.

## 3. İmzalı kararlı yayın
1. GitHub → **Actions** → **EYMEN GELİR — Signed Stable Release**.
2. **Run workflow** → branch `main` → onay `PUBLISH`.
3. İş akışı test, APK oluşturma, Android imza doğrulama ve SHA-256 bütünlük hesaplaması yapar.
4. Hepsi başarılıysa `v0.6.0` etiketiyle **pre-release olmayan, latest işaretli kararlı** GitHub Release yayımlanır.
5. Yayım sonrasında: `https://github.com/mmustafatasoy/Gelirgider/releases/tag/v0.6.0`.

**Durum:** GitHub Secrets verilmediği sürece yayımlama işlemi başarısız olur; **kararlı sürüm hazır veya yayımlandı diye sunulmaz**.

## 4. Önceki APK'lar ve veri güvenliği
Eski GitHub Actions debug APK'ları geçici debug anahtarlarıyla imzalıdır. Bu anahtarlar yeni release anahtarından farklıdır. Dolayısıyla güncelleme olarak üzerlerine kurulamazlar. Önce gerçek kayıtlarınızın ve varsa fişlerin ZIP yedeğini uygulamadan oluşturup farklı yerde saklayın. Kaldırma işlemi yerel finans verilerini ve yerel hesap bilgilerini silebilir. ZIP mali verileri geri yükleyebilir, ancak kullanıcı adı/şifre/kurtarma kodlarını yedeklemez.

## 5. Her yeni sürümde
- **Aynı** `.jks` dosyasını ve alias değerini kullanın.
- `versionCode` artırın ve `versionName` yenileyin.
- Gizli anahtarı, parolayı veya base64 içeriğini git deposuna koymayın.
- Cihaz kurulumu, Room migration, parola kurtarma, ZIP geri yükleme ve WorkManager bildirimlerini gerçek Android telefon üzerinde test edin.

© Muhammet Atasoy · Atasoy Yazılım.
