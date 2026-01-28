# Google ile Giriş ve Bulut Yedekleme Kurulum Kılavuzu

Uygulamanın Google özelliklerinin (Giriş ve Drive Yedekleme) çalışması için Google Cloud Console üzerinden bir proje oluşturmanız ve yapılandırmanız gerekmektedir.

## 1. Google Cloud Projesi Oluşturun
1. [Google Cloud Console](https://console.cloud.google.com/) adresine gidin.
2. Yeni bir proje oluşturun.
3. **APIs & Services > Library** kısmına gidin ve şu API'leri etkinleştirin:
   - **Google Drive API**
   - **Google Auth Library**

## 2. OAuth İzin Ekranını Yapılandırın
1. **APIs & Services > OAuth consent screen** kısmına gidin.
2. "External" seçeneğini seçin.
3. Uygulama adını, destek e-postasını ve iletişim bilgilerini girin.
4. "Scopes" kısmına şu kapsamları ekleyin:
   - `.../auth/drive.appdata` (Uygulama verilerine erişim)
   - `.../auth/drive.file` (Dosya oluşturma/güncelleme)
   - `openid`, `email`, `profile`

## 3. Android Kimlik Bilgileri (Credentials) Oluşturun
1. **APIs & Services > Credentials** kısmına gidin.
2. **Create Credentials > OAuth client ID** seçeneğine tıklayın.
3. Application type olarak **Android** seçin.
4. **Package name** kısmına `com.example.mesaitakip` yazın.
5. **SHA-1 certificate fingerprint** bilgisini eklemeniz gerekecek.
   - Bilgisayarınızda terminali açın ve şu komutu çalıştırın:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
     (Windows'ta dosya yolu farklı olabilir: `%USERPROFILE%\.android\debug.keystore`)
   - Çıkan SHA-1 değerini kopyalayıp Google Cloud Console'a yapıştırın.

## 4. Uygulamayı Derleyin ve Test Edin
- Bu işlemlerden sonra APK build edip cihazınıza yüklediğinizde Google ile giriş yapabilir ve verilerinizi yedekleyebilirsiniz.
- **Önemli:** Eğer uygulama yayınlanacaksa (Release), release keystore için de ayrı bir SHA-1 eklemeniz gerekir.
