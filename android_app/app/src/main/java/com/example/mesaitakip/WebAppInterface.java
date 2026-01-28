package com.example.mesaitakip;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebAppInterface {
    Context mContext;
    DatabaseHelper dbHelper;

    WebAppInterface(Context c) {
        mContext = c;
        dbHelper = new DatabaseHelper(c);
    }

    @JavascriptInterface
    public String query(String sql, String[] args) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = null;
        try {
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
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } finally {
            if (cursor != null) cursor.close();
        }
        return jsonArray.toString();
    }

    @JavascriptInterface
    public long execute(String sql, String[] args) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if (sql.trim().toUpperCase().startsWith("INSERT")) {
                db.execSQL(sql, args);
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
            return -1;
        }
    }

    @JavascriptInterface
    public void showToast(String toast) {
        android.widget.Toast.makeText(mContext, toast, android.widget.Toast.LENGTH_SHORT).show();
    }
}
