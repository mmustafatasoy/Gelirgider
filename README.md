# 🏡 EYMEN GELİR

**EYMEN GELİR** — aile bütçesi için Türkçe, çevrimdışı çalışan gelir-gider takip uygulaması.

**Geliştirici:** Muhammet Atasoy · **Yayıncı:** Atasoy Yazılım · **Lisans:** [MIT](LICENSE)

## ✅ Android uygulaması v0.6.0 — Kira, Telefon, Gönderilen Paralar ve İmzalı Release Hazırlığı

Kotlin, Jetpack Compose ve Room kullanılarak ilk **gerçek Android uygulama kaynakları** yazıldı. GitHub Actions ile testler ve APK derlemesi başarıyla tamamlandı.

**Önceki v0.3.0 başarılı test APK:**
https://github.com/mmustafatasoy/Gelirgider/actions/runs/41676669873

**v0.4.0 önceki test APK:**
https://github.com/mmustafatasoy/Gelirgider/actions/runs/41678303239

**v0.5.0 önceki test APK:**
https://github.com/mmustafatasoy/Gelirgider/actions/runs/41680420362

**v0.6.0 başarılı TEST APK:**
https://github.com/mmustafatasoy/Gelirgider/actions/runs/41684805044

**İmzalı kararlı Release (henüz yayımlanmadı):** GitHub Secrets ayarları ve manuel yayın çalışması gerekir. [Adımlar](docs/SIGNED_RELEASE.md).

Derleme sayfasını açıp **Artifacts → EYMEN-GELIR-debug-apk** üzerinden ZIP'i indir; ZIP içindeki `app-debug.apk` dosyasını telefona yükle. GitHub artefakt indirmesi için giriş gerekebilir.

### ✅ Uygulamada bulunan özellikler
- Ana sayfa; seçilen ayın gelir, gider ve kalan bütçe bilgileri
- Animasyonlu yuvarlak/donut gelir-gider grafiği: gelir/gider payı ve merkezde kalan bütçe
- Gelişmiş soldan açılır menü: işlemler, raporlar, bütçe, hatırlatıcı, ayarlar, güvenlik
- İnternet gerektirmeyen **tek cihazlık yerel kullanıcı hesabı**, şifreli giriş ekranı (parola özeti PBKDF2-HMAC-SHA256)
- İlk kayıt sırasında **8 tek kullanımlık kurtarma kodu**, kodla geçici şifre, ardından zorunlu yeni şifre
- Hesap ekranından mevcut şifreyi girerek kurtarma kodlarını yeniden oluşturma ve oturumu kilitleme
- Yeni gelir / gider girişi: kategori, Türk lirası tutar, açıklama, tarih
- Yeni gider kategorileri: **Kira**, **Telefon - Turkcell**, **Telefon - Vodafone**, **Telefon - Türk Telekom**, Telefon - Diğer, **Gönderilen Para** (Market zaten mevcut)
- Gönderilen Para için **alıcı adı zorunlu**, alıcıya göre toplamlar ve aramalı transfer geçmişi
- Para gönderimleri aylık/yıllık gider hesaplarına eklenir; alıcı ve açıklama ZIP yedeklerinde saklanır
- İşlem listesini ay bazında ve gelir/gider filtresiyle görüntüleme
- İşlemleri düzenleme, onayla silme
- Elektrik, su, doğalgaz, bina aidatı, market, internet, ulaşım ve diğer kategoriler
- Kullanıcıya özel gelir/gider kategorisi oluşturma
- Aylık raporda kategorilerin gider payı; yıllık raporda 12 ayın gelir/gider/kalan özeti
- Room/SQLite ile telefona kalıcı kayıt; internet gerektirmeyen temel kullanım
- Ay seçimiyle ZIP dışa aktarma ve doğrulanan ZIP'den kullanıcı onaylı geri yükleme
- Fiş/fatura fotoğrafı ekleme, değiştirme, kaldırma; görselleri uygulama depolamasında saklama
- Fotoğrafları aylık ZIP içinde ayrı JPEG dosyaları ve SHA-256 doğrulamasıyla taşıma
- Kullanıcının klasör izniyle WorkManager üzerinden önceki tamamlanan ayı otomatik ZIP yedekleme
- **Kategori bütçeleri:** Her ay yenilenen aylık limitler, doluluk yüzdesi ve %80 / %100 uyarıları
- **Fatura/aidat hatırlatıcıları:** Elektrik, su, doğalgaz, bina aidatı ve özel giderler için her ay tekrar eden ödeme günü, isteğe bağlı bildirim izni, etkinleştir/pasifleştir ve sil
- **Gelişmiş raporlar:** Haftalık karşılaştırmalı gelir-gider çubukları, tüm yıl boyunca ortak ölçekli 12 aylık grafik
- **ZIP biçimi v3:** Aylık arşivde yeni bütçe limitleri ve ödeme hatırlatıcıları da saklanır; eski v1/v2 ZIP dosyaları okunur
- Hakkında, sürüm bilgisi ve GitHub Releases sürüm kontrolü
- Açık renkli tema ve hareketli uygulama simgesi

### 🚧 Sıradaki işler
- ZIP yedeklerini parola ile şifreleme ve yedekleme geçmişi
- Fiş/fatura fotoğrafı ekleme
- Sürüm güncellemesi için daha gelişmiş sistem bildirimleri ve imzalı üretim sürümleri
- Kalıcı imzalama anahtarıyla güvenilir sürüm güncelleme; Play Store hazırlığı

### ⚠️ Test APK'sı hakkında önemli not
Bu bir **debug/test APK** dosyasıdır, Google Play'e sunulmuş bir sürüm değildir. Her GitHub Actions derlemesi farklı geçici debug anahtarıyla imzalanabilir, bu yüzden yeni test APK'sı eski test APK'sının üzerine kurulamayabilir. **Test edilmeden gerçek finans verilerinizin tek kopyasını bu uygulamada tutmayın.** Uygulamayı silmek, yerel veritabanını da silebilir.

## 🔧 Teknik bilgiler
- Uygulama kimliği: `com.eymen.gelir`
- Kotlin / Jetpack Compose / Material 3
- Room 2.8.5 + KSP; tutarlar kuruş cinsinden `Long` olarak kaydedilir
- `minSdk 26` / `targetSdk 36` / `compileSdk 36`
- GitHub Actions iş akışı: `.github/workflows/android.yml`
- GitHub sürüm denetimi: `https://api.github.com/repos/mmustafatasoy/Gelirgider/releases/latest`
- Dağıtım: Android test APK'sı (GitHub Actions). Henüz resmî Release yayımlanmadı.

## 🎨 Varlıklar ve belgeler
- [CHANGELOG.md](CHANGELOG.md): tüm sürümlerin değişiklik geçmişi
- `assets/icons/svg/`: 38 bağımsız SVG simgesi
- `previews/update-notification.html`: tasarım amaçlı güncelleme bildirim kutusu
- `docs/DESIGN_GUIDE.md`: tema ve arayüz
- `docs/PROJECT_PLAN.md`: geliştirme yol haritası
- `docs/BACKUP_AND_PRIVACY.md`: planlanan yedekleme / gizlilik
- `docs/UPDATE_SYSTEM.md`: sürüm denetimi tasarımı

## ⚖️ Lisans ve imza
MIT lisanslı açık kaynak proje. © 2026 **Muhammet Atasoy**.

**Muhammet Atasoy**  
*Atasoy Yazılım*  
https://atasoyyazilim.com

## 📦 v0.3.0 Yedekleme Merkezi
Ayarlar → Yedekleme Merkezi → ayı seç → ZIP olarak dışa aktar. ZIP seçilip doğrulandıktan sonra mevcut kayıtlar silinmeden, aynı işlem UUID'leri atlanarak geri yüklenir. **ZIP varsayılan olarak şifresizdir. Otomatik yedekleme kullanıcı onayı ve klasör izni ile çalışır.** İlk v0.1.0 test APK'sı ZIP özelliğini içermez; geçici debug imzaları farklı olabileceğinden uygulamayı kaldırmadan önce veri güvenliğini sağlayın.

## 🧾 v0.3.0 — Fotoğraflar ve otomatik yedek
İşlem ekleme/düzenleme formundaki **Fiş/Fatura Ekle** ile telefondan resim seçilir; görsel uygulamanın özel deposuna güvenli boyuta getirilerek kopyalanır. Aylık yedek dosyası artık bunları da içerir. **Eski v0.2.0 ZIP dosyaları okunabilir.** Ayarlar → Otomatik Aylık Yedekleme seçeneğinde erişilebilir bir klasör seçildikten sonra Android WorkManager, ay değiştikten sonra tamamlanan önceki ayı uygun zamanda yedeklemeye çalışır; kesin saat garantisi yoktur. Fotoğraflar ZIP içinde **şifrelenmez**.

## 🎯 v0.4.0 — Kullanım
**Raporlar → Aylık → Aylık Kategori Bütçeleri:** Harcama kategorisi seçip aylık TL limiti gir. Kategorinin gider toplamı seçili ayın gerçek kayıtlarından hesaplanır. Limitin %80'ine ulaşınca uyarı, %100'de aşım uyarısı çıkar.

**Ayarlar → Fatura ve Aidat Hatırlatıcıları:** Kategori, açıklama ve her ayın ödeme gününü seç. Android 13 ve sonrasında bildirim izni verilmediyse telefon bildirimi çıkmaz. WorkManager Android'in çalışma şartlarına göre günlük kontrol yapar; kesin saat garantisi yoktur. 31 gibi kısa aylara uymayan ödeme günleri ayın son gününe uyarlanır.

**ZIP yedekleri:** v0.4.0 biçim sürümü 3; gelir-gider kayıtları, fiş görselleri, kategoriler, bütçe limitleri ve hatırlatıcı tercihleri birlikte aktarılır. Geri yükleme mevcut işlemleri silmeden yalnızca eksikleri ekler; mevcut kategori bütçeleri korunur. Yedekler hâlâ şifresizdir.

**Güvenlik:** Test APK'sı imzası değişirse eski APK'nın üzerine kurulamayabilir. Eski uygulamayı silmeden önce verilerinizi erişilebilir ayrı bir klasöre yedekleyin. Birim testlerini geçmiş APK, gerçek cihazda kapsamlı test edildiği anlamına gelmez.


## 🔐 v0.5.0 — Yerel hesap ve parola kurtarma

1. İlk açılışta **Kayıt Ol**: kullanıcı adı (3–32 karakter), şifre (en az 10 karakter) ve tekrarını gir.
2. Uygulamanın oluşturduğu **8 ayrı kurtarma kodunu** (24 hex karakter / 96 bit) güvenli bir yere kaydet; kodlar **yalnızca bir kez gösterilir**.
3. Sonraki başlatmalarda **Giriş Yap** ekranında kullanıcı adı ve şifrenle oturum aç.
4. Şifren unutulursa **Şifremi Unuttum → kullanıcı adı + kullanılmamış kurtarma kodu**. Kod tüketilir, uygulama rastgele geçici şifre gösterir.
5. Geçici şifreni ve yeni kalıcı şifreni gir. Bundan sonra **yalnızca yeni şifrenle** giriş yaparsın.
6. Oturum açıkken Ayarlar → Hesap ve Güvenlik bölümünden mevcut şifreyle **yeni kurtarma kodları** oluşturabilirsin; eskiler geçersizleşir.

**Önemli güvenlik sınırları:** Bu sistem **sunucusuz ve tek cihazlı bir uygulama kilididir**; bankacılık düzeyinde veri şifrelemesi değildir. Android Room/SQLite veritabanı ve dışa aktarılan ZIP yedekleri **şifreli değildir**; telefonu başka uygulamalar, root erişimi veya erişilebilir ZIP kopyaları üzerinden korumaz. Kullanıcı adı ve şifre sunucuya gitmez; yalnızca tuzlanmış özetleri cihazda tutulur. **Kurtarma kodlarının hepsi kaybolursa ve şifre unutulursa sunucusuz olarak hesap kurtarılamaz.** Uygulamayı silince yerel hesap ve veriler silinebilir. Manuel ZIP finans yedekleri hesap/şifre bilgilerini içermez. Güncelleme denetimi, hesap girişinden ayrı bir GitHub bağlantısı kullanabilir.

**Sürüm uyumluluğu:** v0.4.0 finans veritabanı korunur; yeni oturum kilidi ilk açılışta mevcut yerel verilerin önüne eklenir. APK imzası sabitlenmediğinden debug sürüm güncellemesi başarısız olabilir; önce dışarıdaki ZIP yedeğini kontrol et.


## 💸 v0.6.0 — Kira, telefon ve para gönderme
Gider Ekle ekranından Kira, Telefon - Turkcell, Telefon - Vodafone, Telefon - Türk Telekom ve Market kategorilerini seçerek kayıt oluşturabilirsiniz. **Gönderilen Para** kategorisinde kime gönderdiğinizi yazmak zorunludur. Sol menüde **Gönderilen Paralar** bölümünden kişiye göre toplamları, açıklamayı, tarihi ve tekil gönderimleri görebilirsiniz.

## 🔏 Kararlı GitHub Releases ve dijital imza
Bu açık depoda **kalıcı bir Android imzalama anahtarı tutulmaz**. Üretim imzalama, GitHub Actions repository secrets ayarlarında oluşturulan ve aynı kimlikle gelecekte de saklanacak dört gizli değer ile yapılır: `EYMEN_KEYSTORE_BASE64`, `EYMEN_KEYSTORE_PASSWORD`, `EYMEN_KEY_ALIAS`, `EYMEN_KEY_PASSWORD`. **Bunlar kurulmadan v0.6.0 kararlı sürüm yayımlanamaz**; derleme yalnızca debug APK çıkarır.

`.github/workflows/release.yml`, yönetici **PUBLISH** onayıyla testler çalıştırır, Google Android imzalama yapılandırması ile release APK üretir, imzayı doğrular, SHA256 oluşturur ve `v0.6.0` tag'iyle GitHub Releases'e kararlı (prerelease olmayan) sürüm yayımlar.

**İmzalama anahtarını GitHub'a açık dosya olarak koymayın.** Anahtarın güvenli bir yedeğini alın; kaybederseniz kullanıcıların mevcut kurulumlarını aynı uygulama olarak güncelleyemezsiniz. Önceki geçici debug APK'lar, yeni release anahtarının imzasıyla uyumlu olmayacağından **üzerine kurulamaz**. Kullanıcı verilerinin ZIP yedeğini silmeden önce kontrol edin. GitHub Releases URL'si, yalnızca yayın işlemi gerçekten tamamlandıktan sonra paylaşılır.
