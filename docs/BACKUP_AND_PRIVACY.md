# Kayıt ve yedek stratejisi
- Ana kaynak Android Room SQLite veritabanı: gelirler, giderler, kategoriler, hedefler ve belge bağlantıları.
- Aylık ZIP: manifest.json (şema/sürüm/checksum), records.json (tutar/kategori/tarih/açıklama), attachments/ (opsiyonel fişler).
- Android Storage Access Framework ile kullanıcının seçtiği klasöre dışa aktarma.
- Otomatik yedek kullanıcı izniyle, kalıcı yazma yetkisi var ise.
- Güvenli geri yüklemede checksum, ZIP içeriği, boyut limiti, zip-slip kontrolleri ve veri birleştirme/onay akışı.
- Uygulama silinince dahili özel veriler silinebilir; ayrı telefon/harici kopya önerilir.
- ZIP varsayılan olarak şifreli değildir. İleride şifreli yedek düşünülüyor.
