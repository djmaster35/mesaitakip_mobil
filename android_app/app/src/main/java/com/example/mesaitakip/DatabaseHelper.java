package com.example.mesaitakip;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mesaitakip.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Kullanıcılar tablosu
        db.execSQL("CREATE TABLE kullanicilar (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE," +
                "password TEXT," +
                "adsoyad TEXT," +
                "is_admin INTEGER DEFAULT 0," +
                "is_banned INTEGER DEFAULT 0" +
                ");");

        // Haftalar tablosu
        db.execSQL("CREATE TABLE haftalar (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hafta_baslangic TEXT," +
                "hafta_araligi TEXT," +
                "calisan_adi TEXT," +
                "user_id INTEGER," +
                "FOREIGN KEY(user_id) REFERENCES kullanicilar(id)" +
                ");");

        // Mesai kayıtları tablosu
        db.execSQL("CREATE TABLE mesai_kayitlari (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hafta_id INTEGER," +
                "tarih TEXT," +
                "aciklama TEXT," +
                "saat TEXT," +
                "is_resmi_tatil INTEGER DEFAULT 0," +
                "FOREIGN KEY(hafta_id) REFERENCES haftalar(id)" +
                ");");

        // Varsayılan bir admin hesabı ekleyelim
        db.execSQL("INSERT INTO kullanicilar (username, password, adsoyad, is_admin) VALUES ('admin', 'admin', 'Sistem Yöneticisi', 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS mesai_kayitlari");
        db.execSQL("DROP TABLE IF EXISTS haftalar");
        db.execSQL("DROP TABLE IF EXISTS kullanicilar");
        onCreate(db);
    }
}
