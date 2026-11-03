# 🏡 EYMEN GELİR

**EYMEN GELİR** — aile bütçesi için Türkçe, çevrimdışı çalışan gelir-gider takip uygulaması.

**Geliştirici:** Muhammet Atasoy · **Yayıncı:** Atasoy Yazılım · **Lisans:** [MIT](LICENSE)

## ✅ Android uygulaması (v0.2.0 — aylık ZIP yedekleme)

Kotlin, Jetpack Compose ve Room kullanılarak ilk **gerçek Android uygulama kaynakları** yazıldı. GitHub Actions ile testler ve APK derlemesi başarıyla tamamlandı.

**Başarılı derleme ve test APK'sı:**
https://github.com/mmustafatasoy/Gelirgider/actions/runs/41671869636

Derleme sayfasını açıp **Artifacts → EYMEN-GELIR-debug-apk** üzerinden ZIP'i indir; ZIP içindeki `app-debug.apk` dosyasını telefona yükle. GitHub artefakt indirmesi için giriş gerekebilir.

### ✅ Uygulamada bulunan özellikler
- Ana sayfa; seçilen ayın gelir, gider ve kalan bütçe bilgileri
- Yeni gelir / gider girişi: kategori, Türk lirası tutar, açıklama, tarih
- İşlem listesini ay bazında ve gelir/gider filtresiyle görüntüleme
- İşlemleri düzenleme, onayla silme
- Elektrik, su, doğalgaz, bina aidatı, market, internet, ulaşım ve diğer kategoriler
- Kullanıcıya özel gelir/gider kategorisi oluşturma
- Aylık raporda kategorilerin gider payı; yıllık raporda 12 ayın gelir/gider/kalan özeti
- Room/SQLite ile telefona kalıcı kayıt; internet gerektirmeyen temel kullanım
- Ay seçimiyle ZIP dışa aktarma ve doğrulanan ZIP'den kullanıcı onaylı geri yükleme
- Hakkında, sürüm bilgisi ve GitHub Releases sürüm kontrolü
- Açık renkli tema ve hareketli uygulama simgesi

### 🚧 Sıradaki işler
- Otomatik aylık ZIP yedekleme, şifreli yedekleme
- Fiş/fatura fotoğrafı ekleme
- Tekrarlayan ödemeler ve planlı gider bildirimleri
- Bütçe hedefleri, daha kapsamlı grafikler
- Resmî Android bildirimleri ve periyodik güncelleme denetimi
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

## 📦 v0.2.0 Yedekleme Merkezi
Ayarlar → Yedekleme Merkezi → ayı seç → ZIP olarak dışa aktar. ZIP seçilip doğrulandıktan sonra mevcut kayıtlar silinmeden, aynı işlem UUID'leri atlanarak geri yüklenir. **ZIP varsayılan olarak şifresizdir ve otomatik oluşturulmaz.** İlk v0.1.0 test APK'sı ZIP özelliğini içermez; geçici debug imzaları farklı olabileceğinden uygulamayı kaldırmadan önce veri güvenliğini sağlayın.
