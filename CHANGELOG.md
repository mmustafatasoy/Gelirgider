# EYMEN GELİR — Sürüm Geçmişi

## v0.4.0 (3 Kasım 2026)
- Aylık tekrar eden kategori bütçeleri ve limit kullanım oranları.
- Kategori harcaması bütçenin %80'ine ve %100'üne ulaşınca uygulama içinde uyarı.
- Fatura, doğalgaz, su, aidat ve özel giderlere yönelik aylık ödeme günü hatırlatıcıları.
- Android 13+ için kullanıcı onaylı bildirim izni ve WorkManager ile günlük ödeme günü kontrolü.
- Haftalık gelir-gider grafikleri ve 12 aylık grafikte ortak ölçek.
- Room veritabanı v3: eski v1→v2→v3 verileri koruyan şema geçişi.
- ZIP biçimi v3: kategori bütçeleri ve hatırlatıcı ayarları da yedeklenir; v1/v2 okuma korunur.
- Birim testleri, Android debug APK derlemesi ve GitHub Actions yükleme: BAŞARILI.
- Test APK: https://github.com/mmustafatasoy/Gelirgider/actions/runs/41678303239

## v0.3.0
- Fiş/fatura fotoğrafı ekleme ve ZIP yedeklerinde saklama.
- Kullanıcı seçtiği klasöre WorkManager ile otomatik aylık ZIP yedeklemesi.
- Başarılı test APK: https://github.com/mmustafatasoy/Gelirgider/actions/runs/41676669873

## v0.2.0
- Manuel aylık ZIP dışa aktarma ve mevcut kayıtları silmeden geri yükleme.
- Başarılı test APK: https://github.com/mmustafatasoy/Gelirgider/actions/runs/41673813639

## v0.1.0
- Açık temalı Android uygulaması, Room kayıtları, kategoriler, aylık/yıllık rapor.
- Başarılı test APK: https://github.com/mmustafatasoy/Gelirgider/actions/runs/41671869636

**Uyarı:** Bu sürümlerin tamamı debug APK'dır. GitHub Actions debug imzaları birbirinden farklı olabilir. Eski APK'yı silerseniz uygulamanın dahili verileri silinebilir. Önce ZIP yedeği alın ve başka bir yerde saklayın. Resmî sabit anahtarla imzalanmış Release APK henüz oluşturulmadı.

© 2026 Muhammet Atasoy · Atasoy Yazılım · MIT.
