# EYMEN GELİR — Geliştirme Planı
## Tamamlananlar
- [x] Simge paketi, açık tema, ana ekran, kategori yönetimi
- [x] Room ile çevrimdışı gelir/gider kayıtları ve açıklamalar
- [x] Aylık ve yıllık gelir-gider raporları
- [x] Hakkında, sürüm bilgisi, GitHub sürüm kontrolü
- [x] Aylık ZIP dışa aktarma: SHA-256 bütünlük kontrolü
- [x] ZIP'den onaylı, mükerrerleri atlayan, silmeden geri yükleme
- [x] ZIP formatı birim testleri
- [x] v0.4.0: Aylık kategori bütçeleri ve %80 / %100 limit uyarıları
- [x] v0.4.0: Tekrarlayan ödeme günü hatırlatıcıları ve isteğe bağlı telefon bildirimi
- [x] v0.4.0: Haftalık grafikler ve yıllık ortak ölçek
- [x] v0.4.0: Room 2→3 veri kaybı olmayan migration, ZIP v3 plan verileri

- [x] v0.5.0: Animasyonlu yuvarlak gelir-gider karşılaştırması
- [x] v0.5.0: Gelişmiş yan menü ve hesap güvenliği ekranı
- [x] v0.5.0: Sunucusuz yerel kayıt/giriş, tuzlu PBKDF2 parola özeti
- [x] v0.5.0: 8 tek kullanımlık kurtarma kodu ve geçici şifreyle zorunlu sıfırlama
- [x] v0.5.0: Şifre doğrulamalı kod yenileme ve oturum kilidi

## Sonraki aşamalar
- [x] Kullanıcı klasör izniyle otomatik aylık ZIP kontrolü (WorkManager)
- [ ] Şifreli ZIP arşivi
- [x] Fiş/fatura fotoğrafı ekleme ve aylık ZIP içinde taşıma
- [ ] Harcama uyarılarının isteğe bağlı sistem bildirimi; otomatik fatura kaydı
- [ ] Kalıcı imzalama anahtarı / güncellenebilir resmî sürüm
- [ ] Gerçek cihaz ve geri yükleme uçtan uca testleri

- [ ] SQLite ve ZIP arşivinin gerçek uçtan uca şifrelenmesi, anahtar kurtarma tasarımı
- [ ] Yerel hesap/finans kurtarma davranışının gerçek cihaz testleri
