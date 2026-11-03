# EYMEN GELİR — Aylık ZIP yedekleme (v0.3.0)

## Kullanım
Ayarlar → Yedekleme Merkezi → ayı seç → **Seçilen Ayı ZIP Olarak Kaydet**. Android'in dosya seçicisinden uygun konumu seçin. Geri yükleme için ZIP seçin, özetini inceleyin ve onaylayın.

## Teknik güvenlik
- Room veritabanı ana kaynaktır; gelir/gider kayıtları kuruş cinsinden tam sayıdır.
- ZIP `manifest.json`, `records.json` ve varsa `receipts/<UUID>.jpg` dosyalarını içerir. Eski v1 dosyaları desteklenir.
- Manifest biçim sürümü 2, ay, saat dilimi, kayıt sayısı ile kayıtlar ve fiş fotoğrafları için SHA-256 özetleri içerir.
- En fazla 12 MiB açılmış içerik, 20.000 işlem, 400 kategori kabul edilir.
- ZIP dosya adları beyaz listeyle doğrulanır; arşiv dosya sistemine açılmaz.
- Mevcut işlemler silinmez. Aynı UUID'li kayıtlar atlanır. Room transaction ile işlem bütünlüğü korunur.
- Genel depolama izni istenmez: Android dosya seçici kullanılır.

## Sınırlar
- **Otomatik aylık yedek:** Kullanıcı Android klasör seçicisinde izin verdikten sonra WorkManager önceki tamamlanan ayı uygun zamanda yedekler. Kesin gün/saat garantisi yoktur.
- **ZIP şifreli değil**; başkalarının erişebileceği yerde tutmayın.
- Fiş/fatura görselleri uygulama özel deposuna JPEG olarak kopyalanır ve ZIP içinde yedeklenir. 2 MiB/görsel, 20 MiB/açılmış ZIP limitleri vardır.
- Debug APK imzaları derlemeler arasında değişebilir. Önceki v0.1.0 test APK'sında ZIP yedekleme yoktu; uygulamayı kaldırmak verileri silebilir.
