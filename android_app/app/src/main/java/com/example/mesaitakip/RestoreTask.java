package com.example.mesaitakip;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public class RestoreTask implements Runnable {
    private static final String TAG = "RestoreTask";
    private final Context context;
    private final WebView webView;
    private final DatabaseHelper dbHelper;

    public RestoreTask(Context context, WebView webView, DatabaseHelper dbHelper) {
        this.context = context;
        this.webView = webView;
        this.dbHelper = dbHelper;
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
            FileList result = service.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = 'mesaitakip_backup.db'")
                    .execute();

            if (result.getFiles() == null || result.getFiles().isEmpty()) {
                postToJs(webView, "alert('Bulutta yedek bulunamadı.')");
                return;
            }

            String fileId = result.getFiles().get(0).getId();
            java.io.File dbFile = context.getDatabasePath("mesaitakip.db");

            dbHelper.close();

            OutputStream output = null;
            try {
                output = new FileOutputStream(dbFile);
                service.files().get(fileId).executeMediaAndDownloadTo(output);
            } finally {
                if (output != null) {
                    try { output.close(); } catch (IOException e) { /* ignore */ }
                }
            }

            postToJs(webView, "alert('Geri yükleme tamamlandı. Uygulama yenileniyor.'); location.reload();");
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            postToJs(webView, "alert('Geri yükleme hatası: " + escapeJs(e.getMessage()) + "')");
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
