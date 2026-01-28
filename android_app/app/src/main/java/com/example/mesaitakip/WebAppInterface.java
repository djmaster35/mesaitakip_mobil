package com.example.mesaitakip;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private Context mContext;
    private WebView mWebView;
    private DatabaseHelper dbHelper;

    WebAppInterface(Context c, WebView webView) {
        mContext = c;
        mWebView = webView;
        dbHelper = new DatabaseHelper(c);
    }

    @JavascriptInterface
    public String query(String sql, String argsJson) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = null;
        try {
            String[] args = null;
            if (argsJson != null && !argsJson.equals("[]")) {
                JSONArray argsArray = new JSONArray(argsJson);
                args = new String[argsArray.length()];
                for (int i = 0; i < argsArray.length(); i++) {
                    args[i] = argsArray.getString(i);
                }
            }

            cursor = db.rawQuery(sql, args);
            if (cursor.moveToFirst()) {
                do {
                    int totalColumn = cursor.getColumnCount();
                    JSONObject rowObject = new JSONObject();
                    for (int i = 0; i < totalColumn; i++) {
                        if (cursor.getColumnName(i) != null) {
                            try {
                                switch (cursor.getType(i)) {
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        rowObject.put(cursor.getColumnName(i), cursor.getLong(i));
                                        break;
                                    case Cursor.FIELD_TYPE_FLOAT:
                                        rowObject.put(cursor.getColumnName(i), cursor.getDouble(i));
                                        break;
                                    case Cursor.FIELD_TYPE_STRING:
                                        rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                                        break;
                                    case Cursor.FIELD_TYPE_BLOB:
                                        // Blobs are not handled for simplicity
                                        break;
                                    case Cursor.FIELD_TYPE_NULL:
                                        rowObject.put(cursor.getColumnName(i), JSONObject.NULL);
                                        break;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    jsonArray.put(rowObject);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in query: " + e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } finally {
            if (cursor != null) cursor.close();
        }
        return jsonArray.toString();
    }

    @JavascriptInterface
    public long execute(String sql, String argsJson) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String[] args = null;
            if (argsJson != null && !argsJson.equals("[]")) {
                JSONArray argsArray = new JSONArray(argsJson);
                args = new String[argsArray.length()];
                for (int i = 0; i < argsArray.length(); i++) {
                    args[i] = argsArray.getString(i);
                }
            }

            if (sql.trim().toUpperCase().startsWith("INSERT")) {
                db.execSQL(sql, args != null ? args : new Object[0]);
                // To get last insert ID, we might need another query
                Cursor cursor = db.rawQuery("SELECT last_insert_rowid()", null);
                long lastId = -1;
                if (cursor.moveToFirst()) {
                    lastId = cursor.getLong(0);
                }
                cursor.close();
                return lastId;
            } else {
                db.execSQL(sql, args);
                return 1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in execute: " + e.getMessage(), e);
            return -1;
        }
    }

    @JavascriptInterface
    public void showToast(String toast) {
        android.widget.Toast.makeText(mContext, toast, android.widget.Toast.LENGTH_SHORT).show();
    }

    // --- Google Auth ---

    @JavascriptInterface
    public void googleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_FILE))
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
        ((MainActivity) mContext).startGoogleSignIn(mGoogleSignInClient);
    }

    public void onGoogleSignInSuccess(GoogleSignInAccount account) {
        final String email = account.getEmail();
        final String displayName = account.getDisplayName();
        final String id = account.getId();
        sendToJs("onGoogleLoginSuccess('" + email + "', '" + displayName + "', '" + id + "')");
    }

    public void onGoogleSignInFailure(int statusCode) {
        sendToJs("onGoogleLoginFailure(" + statusCode + ")");
    }

    // --- Google Drive Backup ---

    private class BackupTask implements Runnable {
        @Override
        public void run() {
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
                if (account == null) {
                    sendToJs("alert('Lütfen önce Google ile giriş yapın.')");
                    return;
                }

                Drive googleDriveService = getDriveService(account);
                java.io.File dbFile = mContext.getDatabasePath("mesaitakip.db");

                // Drive'da mevcut dosyayı ara
                FileList result = googleDriveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = 'mesaitakip_backup.db'")
                        .execute();

                File fileMetadata = new File();
                fileMetadata.setName("mesaitakip_backup.db");
                FileContent mediaContent = new FileContent("application/x-sqlite3", dbFile);

                if (result.getFiles().isEmpty()) {
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    googleDriveService.files().create(fileMetadata, mediaContent).execute();
                } else {
                    String fileId = result.getFiles().get(0).getId();
                    googleDriveService.files().update(fileId, null, mediaContent).execute();
                }

                sendToJs("alert('Yedekleme başarıyla tamamlandı!')");
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                sendToJs("alert('Yedekleme hatası: " + e.getMessage() + "')");
            }
        }
    }

    @JavascriptInterface
    public void backupToCloud() {
        new Thread(new BackupTask()).start();
    }

    private class RestoreTask implements Runnable {
        @Override
        public void run() {
            try {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
                if (account == null) {
                    sendToJs("alert('Lütfen önce Google ile giriş yapın.')");
                    return;
                }

                Drive googleDriveService = getDriveService(account);
                FileList result = googleDriveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = 'mesaitakip_backup.db'")
                        .execute();

                if (result.getFiles().isEmpty()) {
                    sendToJs("alert('Bulutta yedek bulunamadı.')");
                    return;
                }

                String fileId = result.getFiles().get(0).getId();
                java.io.File dbFile = mContext.getDatabasePath("mesaitakip.db");

                // Veritabanını kapat
                dbHelper.close();

                try (OutputStream outputStream = new FileOutputStream(dbFile)) {
                    googleDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                }

                sendToJs("alert('Geri yükleme tamamlandı. Uygulama yenileniyor.'); location.reload();");
            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                sendToJs("alert('Geri yükleme hatası: " + e.getMessage() + "')");
            }
        }
    }

    @JavascriptInterface
    public void restoreFromCloud() {
        new Thread(new RestoreTask()).start();
    }

    private Drive getDriveService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                mContext, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Mesai Takip")
                .build();
    }

    private class JsLoader implements Runnable {
        private String js;
        JsLoader(String js) { this.js = js; }
        @Override
        public void run() {
            mWebView.loadUrl("javascript:" + js);
        }
    }

    private void sendToJs(final String js) {
        mWebView.post(new JsLoader(js));
    }
}
