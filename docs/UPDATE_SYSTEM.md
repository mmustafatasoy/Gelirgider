# Güncelleme bildirimi tasarımı
- Ayarlar > Hakkında: uygulamanın gerçek versionName/versionCode bilgileri.
- Ayarlar > Güncellemeleri Denetle: https://api.github.com/repos/mmustafatasoy/Gelirgider/releases/latest üzerinden HTTPS ile sürüm bilgisi.
- GitHub Release API erişilemezse yerel uygulama kullanılabilir kalır.
- Yeni sürüm bulunursa sürüm, değişiklik notları ve indirme bağlantısı gösterilir.
- İndir butonu kullanıcıyı gerçek Release sayfasına götürür, arka planda APK kurulmaz.
- Android bildirim izni ve kullanıcı tercihleri gözetilir.
- Veri kaybını önlemek için Room migration ve paket imza tutarlılığı test edilir.
- preivews/update-notification.html yalnızca yerel tasarım gösterimidir; uygulamada henüz gerçek otomatik güncelleme yoktur.
