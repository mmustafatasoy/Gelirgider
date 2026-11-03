# EYMEN GELİR — Aylık ZIP yedekleme (v0.2.0)

## Kullanım
Ayarlar → Yedekleme Merkezi → ayı seç → **Seçilen Ayı ZIP Olarak Kaydet**. Android'in dosya seçicisinden uygun konumu seçin. Geri yükleme için ZIP seçin, özetini inceleyin ve onaylayın.

## Teknik güvenlik
- Room veritabanı ana kaynaktır; gelir/gider kayıtları kuruş cinsinden tam sayıdır.
- ZIP iki dosya içerir: `manifest.json` ve `records.json`.
- Manifest biçim sürümü 1, ay, saat dilimi, kayıt sayısı ve SHA-256 özeti içerir.
- En fazla 12 MiB açılmış içerik, 20.000 işlem, 400 kategori kabul edilir.
- ZIP dosya adları beyaz listeyle doğrulanır; arşiv dosya sistemine açılmaz.
- Mevcut işlemler silinmez. Aynı UUID'li kayıtlar atlanır. Room transaction ile işlem bütünlüğü korunur.
- Genel depolama izni istenmez: Android dosya seçici kullanılır.

## Sınırlar
- **Her ay otomatik yedek yok**; manuel dışa aktarılır.
- **ZIP şifreli değil**; başkalarının erişebileceği yerde tutmayın.
- Fiş/fatura fotoğrafları henüz eklenmediğinden yedekte bulunmaz.
- Debug APK imzaları derlemeler arasında değişebilir. Önceki v0.1.0 test APK'sında ZIP yedekleme yoktu; uygulamayı kaldırmak verileri silebilir.
