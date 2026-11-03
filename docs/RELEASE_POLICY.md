# EYMEN GELİR — YAYIN POLİTİKASI

**Kullanıcı kararı:** APK indirmeleri **yalnızca GitHub Releases** bölümünden, **kararlı / production release** olarak sunulur.

- Debug APK geliştirme/test amaçlı dahili varyanttır. **İndirme bağlantısı olarak yayımlanmaz** ve kullanıcıya tavsiye edilmez.
- Uygulamanın `versionCode` değeri her sürümde artar. GitHub Release etiketi `versionName` ile eşleşir.
- APK kalıcı üretim imzalama sertifikasıyla imzalanır, Android `apksigner` ile doğrulanır.
- Aynı sürüm etiketini sessizce değiştirip yeniden yüklemek yasaktır; yeni sürüm için yeni versiyon çıkarılır.
- `.jks` özel imza anahtarı ve parolalar GitHub deposuna yüklenmez. Yalnızca GitHub Actions Secrets üzerinden kullanılır.
- Testler başarısızsa, imzalama sırları eksikse veya imza doğrulanamıyorsa **Release yayımlanmaz**.
- GitHub Actions Artifacts'teki dosyalar son kullanıcı dağıtımı değildir.
- Kullanıcıya ancak Releases sayfasında gerçekten var olan indirme bağlantısı gösterilir.

**Mevcut teknik sınırlama:** GitHub bağlantısı imzalama sırlarını ayarlama ve Release binary asset yükleme eylemini sağlamıyorsa, yalnızca GitHub deposu ve iş akışı hazırlanabilir; resmi Release varmış gibi gösterilemez.

**İndirme sayfası:** https://github.com/mmustafatasoy/Gelirgider/releases
