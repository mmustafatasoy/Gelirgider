# EYMEN GELİR

**EYMEN GELİR**, aile bütçesinin aylık ve yıllık gelir/gider takibi için geliştirilen Türkçe, çevrimdışı öncelikli Android uygulaması projesidir.

> **Geliştirme durumu:** v0.1 tasarım ve proje iskeleti. Henüz APK yayımlanmadı; arayüz örneklerindeki rakamlar gerçek finans verisi değildir.

## Planlanan özellikler
- Gelir ve gider kaydetme, düzenleme, silme; tarih, kategori, açıklama ve tutar
- Elektrik, su, doğalgaz, bina aidatı, market, internet, ulaşım ve özel kategoriler
- 12 aylık yıllık gelir/gider tablosu ve aylık raporlar
- Bütçe hedefleri, kategori dağılımları ve grafikler
- Room/SQLite ile çevrimdışı veri saklama, fiş/fatura ekleme
- Aylık ZIP yedekleri, verileri dışa aktarma ve geri yükleme
- Hakkında, sürüm bilgisi, güncelleme kontrolü ve bildirim ekranı
- Açık tema; animasyonlu ve erişilebilir simgeler

## Dosyalar
- `assets/icons/svg/` — **38 ayrı düzenlenebilir SVG simgesi**
- `docs/PROJECT_PLAN.md` — uygulama geliştirme planı
- `docs/UPDATE_SYSTEM.md` — güncelleme bildirimi ve güvenli yükseltme tasarımı
- `docs/BACKUP_AND_PRIVACY.md` — ZIP yedekleme, geri yükleme ve gizlilik
- `previews/update-notification.html` — güncelleme kutusunun yalnızca **tasarım önizlemesi**

Ayrıca simgelerin PNG 128/256/512 sürümlerini içeren ayrı ZIP paket hazırlanmıştır; GitHub deposunda şu an SVG kaynakları bulunur.

## Teknik yaklaşım
Kotlin, Jetpack Compose/Material 3, Room/SQLite, Storage Access Framework, GitHub Releases sürüm denetimi. Bildirim ve gerçek güncelleme işlemleri Android kodlama aşamasında eklenecektir.

## Lisans
MIT. Bkz. [LICENSE](LICENSE).

## Geliştirici
**Muhammet Atasoy**  
**Atasoy Yazılım**  
https://atasoyyazilim.com

© 2026 Muhammet Atasoy.
