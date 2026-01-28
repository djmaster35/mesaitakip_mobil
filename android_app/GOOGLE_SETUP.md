# Google ile Giriş ve Bulut Yedekleme Kurulum Kılavuzu

Uygulamanın Google özelliklerinin (Giriş ve Drive Yedekleme) çalışması için Google Cloud Console üzerinden bir proje oluşturmanız ve yapılandırmanız gerekmektedir. **Hata kodu 10 (DEVELOPER_ERROR)** alıyorsanız, aşağıdaki adımları dikkatlice kontrol edin.

## 1. Google Cloud Projesi Oluşturun
1. [Google Cloud Console](https://console.cloud.google.com/) adresine gidin.
2. Yeni bir proje oluşturun.
3. **APIs & Services > Library** kısmına gidin ve şu API'yi etkinleştirin:
   - **Google Drive API**

## 2. OAuth İzin Ekranını Yapılandırın (OAuth Consent Screen)
1. **APIs & Services > OAuth consent screen** kısmına gidin.
2. "User Type" olarak **External** seçin.
3. Uygulama bilgilerini girin (Uygulama adı, e-posta vb.).
4. **Scopes (Kapsamlar)** kısmına şu kapsamları ekleyin:
   - `.../auth/drive.appdata` (Uygulama verilerine erişim)
   - `openid`, `email`, `profile`
5. **Test Users** kısmına kendi e-posta adresinizi mutlaka ekleyin. (Uygulama "Testing" modundayken sadece buradaki kullanıcılar giriş yapabilir).

## 3. Android Kimlik Bilgileri (Credentials) Oluşturun
1. **APIs & Services > Credentials** kısmına gidin.
2. **Create Credentials > OAuth client ID** seçeneğine tıklayın.
3. Application type: **Android**.
4. **Package name**: `com.example.mesaitakip` (Tam olarak bu olmalı).
5. **SHA-1 certificate fingerprint**:
   - Bu değerin yanlış olması **Hata 10**'un en yaygın sebebidir.
   - Android Studio'da sağ taraftaki **Gradle** sekmesini açın.
   - `app > Tasks > android > signingReport` yolunu izleyip çalıştırın.
   - Alt kısmdaki konsolda **SHA1** değerini göreceksiniz. Bunu kopyalayın.
   - Alternatif (Terminal):
     - Windows: `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android`
     - Mac/Linux: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
6. Kaydedin.

## 4. Hata Kodu 10 Çözüm Özeti
Eğer hala Hata 10 alıyorsanız:
1. Google Cloud Console'daki **Package Name**'in `com.example.mesaitakip` olduğundan emin olun.
2. **SHA-1** değerini `signingReport` çıktısından alarak güncelleyin (Bazen bilgisayarınızdaki debug key beklediğinizden farklı olabilir).
3. **OAuth Consent Screen** ayarlarında **Test Users** kısmına giriş yaptığınız e-postayı eklediğinizden emin olun.
4. Değişikliklerin Google sunucularında aktif olması için 5-10 dakika bekleyin.

## 5. Uygulamayı Derleyin
- Bu işlemlerden sonra uygulamayı tekrar cihazınıza yüklediğinizde giriş yapabileceksiniz.
