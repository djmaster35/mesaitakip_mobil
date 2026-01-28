package com.example.mesaitakip;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private final Context mContext;
    private final WebView mWebView;
    private final DatabaseHelper dbHelper;

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
                        String colName = cursor.getColumnName(i);
                        if (colName != null) {
                            try {
                                switch (cursor.getType(i)) {
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        rowObject.put(colName, cursor.getLong(i));
                                        break;
                                    case Cursor.FIELD_TYPE_FLOAT:
                                        rowObject.put(colName, cursor.getDouble(i));
                                        break;
                                    case Cursor.FIELD_TYPE_STRING:
                                        rowObject.put(colName, cursor.getString(i));
                                        break;
                                    case Cursor.FIELD_TYPE_NULL:
                                        rowObject.put(colName, JSONObject.NULL);
                                        break;
                                }
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                    }
                    jsonArray.put(rowObject);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in query: " + e.getMessage(), e);
            return "{\"error\": \"" + escapeJs(e.getMessage()) + "\"}";
        } finally {
            if (cursor != null) cursor.close();
        }
        return jsonArray.toString();
    }

    @JavascriptInterface
    public long execute(String sql, String argsJson) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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

            if (sql.trim().toUpperCase().startsWith("INSERT")) {
                db.execSQL(sql, args != null ? args : new Object[0]);
                cursor = db.rawQuery("SELECT last_insert_rowid()", null);
                long lastId = -1;
                if (cursor.moveToFirst()) {
                    lastId = cursor.getLong(0);
                }
                return lastId;
            } else {
                db.execSQL(sql, args);
                return 1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in execute: " + e.getMessage(), e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void googleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).startGoogleSignIn(mGoogleSignInClient);
        }
    }

    public void onGoogleSignInSuccess(GoogleSignInAccount account) {
        String email = escapeJs(account.getEmail());
        String displayName = escapeJs(account.getDisplayName());
        String id = escapeJs(account.getId());
        sendToJs("onGoogleLoginSuccess('" + email + "', '" + displayName + "', '" + id + "')");
    }

    public void onGoogleSignInFailure(int statusCode) {
        sendToJs("onGoogleLoginFailure(" + statusCode + ")");
    }

    @JavascriptInterface
    public void backupToCloud() {
        new Thread(new BackupTask(mContext, mWebView)).start();
    }

    @JavascriptInterface
    public void restoreFromCloud() {
        new Thread(new RestoreTask(mContext, mWebView, dbHelper)).start();
    }

    @JavascriptInterface
    public String getAppSha1() {
        try {
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = info.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = info.signatures;
            }

            for (Signature signature : signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    String append = Integer.toHexString(0xFF & digest[i]);
                    if (append.length() == 1) hexString.append("0");
                    hexString.append(append.toUpperCase());
                    if (i < digest.length - 1) hexString.append(":");
                }
                return hexString.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SHA1", e);
        }
        return "Not found";
    }

    private void sendToJs(String js) {
        mWebView.post(new JsLoader(mWebView, js));
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("'", "\\'");
    }
}
