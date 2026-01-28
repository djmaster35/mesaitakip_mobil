package com.example.mesaitakip;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.util.Collections;

public class BackupTask implements Runnable {
    private static final String TAG = "BackupTask";
    private final Context context;
    private final WebView webView;

    public BackupTask(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    @Override
    public void run() {
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            if (account == null) {
                postToJs(webView, "alert('Lütfen önce Google ile giriş yapın.')");
                return;
            }

            Drive service = getDriveService(context, account);
            java.io.File dbFile = context.getDatabasePath("mesaitakip.db");

            FileList result = service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = 'mesaitakip_backup.db'")
                    .execute();

            File metadata = new File();
            metadata.setName("mesaitakip_backup.db");
            FileContent content = new FileContent("application/x-sqlite3", dbFile);

            if (result.getFiles() == null || result.getFiles().isEmpty()) {
                metadata.setParents(Collections.singletonList("appDataFolder"));
                service.files().create(metadata, content).execute();
            } else {
                String fileId = result.getFiles().get(0).getId();
                service.files().update(fileId, null, content).execute();
            }

            postToJs(webView, "alert('Yedekleme başarıyla tamamlandı!')");
        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            postToJs(webView, "alert('Yedekleme hatası: " + escapeJs(e.getMessage()) + "')");
        }
    }

    private Drive getDriveService(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Mesai Takip")
                .build();
    }

    private void postToJs(WebView webView, String js) {
        if (webView != null) {
            webView.post(new JsLoader(webView, js));
        }
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("'", "\\'");
    }
}
