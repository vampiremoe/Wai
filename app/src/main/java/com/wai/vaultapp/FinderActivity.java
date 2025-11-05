package com.wai.vaultapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FinderActivity extends AppCompatActivity {

    TextView logs;
    Button blastBtn, encryptBtn, decryptBtn;
    ScrollView scrollView;
    ProgressBar progressBar;

    private static final int REQ_PERM = 1;
    private Decoder decoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finder);
        getPermission();

        logs = findViewById(R.id.logs);
        scrollView = findViewById(R.id.scroll_log);
        blastBtn = findViewById(R.id.blast);
        encryptBtn = findViewById(R.id.encrypt_btn);
        decryptBtn = findViewById(R.id.decrypt_btn);
        progressBar = findViewById(R.id.progress_bar);
        decoder = new Decoder();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        blastBtn.setOnClickListener(v -> {
            if (!hasPermission()) {
                getPermission();
                return;
            }
            new Thread(() -> {
                disableButtons();
                initBlast();
                enableButtons();
            }).start();
        });

        encryptBtn.setOnClickListener(v -> {
            new Thread(() -> {
                disableButtons();
                initEncrypt();
                enableButtons();
            }).start();
        });

        decryptBtn.setOnClickListener(v -> {
            new Thread(() -> {
                disableButtons();
                initDecrypt();
                enableButtons();
            }).start();
        });
    }

    private void initBlast() {
        try {
            appendLog("Starting vault scan...");
            File file = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            File whereSave = new File(Environment.getExternalStorageDirectory(), "waivault/decrypt/" + getDate());
            whereSave.mkdirs();

            File[] fileList = file.listFiles();
            if (fileList == null) {
                appendLog("Cannot find Vault folder.");
                return;
            }

            String dbPath = null;
            for (File f : fileList) {
                if (f.isDirectory() || f.getAbsolutePath().endsWith("journal")) continue;
                if (f.getName().endsWith(".db")) dbPath = f.getAbsolutePath();
            }

            if (dbPath == null) {
                appendLog("Cannot find Vault database.");
                return;
            }

            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);

            if (cursor.getCount() == 0) {
                appendLog("No items found.");
                return;
            }

            appendLog("Found " + cursor.getCount() + " items.");
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String type = cursor.getString(3).toLowerCase();
                File folder = new File(whereSave, type);
                if (!folder.exists()) folder.mkdirs();

                String savedPath = decoder.decodeAndSave(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(0),
                        cursor.getString(3),
                        folder.getAbsolutePath()
                );

                if (savedPath == null) appendLog("Failed: " + cursor.getString(1));
                else appendLog("Saved: " + savedPath);
            }

            cursor.close();
            db.close();
        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
        }
    }

    private void initEncrypt() {
        File source = new File(Environment.getExternalStorageDirectory(), "waivault/decrypt");
        File target = new File(Environment.getExternalStorageDirectory(), "waivault/encrypt");
        target.mkdirs();
        for (File typeFolder : source.listFiles()) {
            if (!typeFolder.isDirectory()) continue;
            File outType = new File(target, typeFolder.getName());
            outType.mkdirs();
            for (File f : typeFolder.listFiles()) {
                String path = decoder.encodeAndSave(f.getAbsolutePath(), outType.getAbsolutePath());
                if (path != null) appendLog("Encrypted: " + path);
            }
        }
    }

    private void initDecrypt() {
        File source = new File(Environment.getExternalStorageDirectory(), "waivault/encrypt");
        File target = new File(Environment.getExternalStorageDirectory(), "waivault/decrypt");
        target.mkdirs();
        for (File typeFolder : source.listFiles()) {
            if (!typeFolder.isDirectory()) continue;
            File outType = new File(target, typeFolder.getName());
            outType.mkdirs();
            for (File f : typeFolder.listFiles()) {
                String path = decoder.decodeAndSave(f.getName(), f.getAbsolutePath(), "password", "", outType.getAbsolutePath());
                if (path != null) appendLog("Decrypted: " + path);
            }
        }
    }

    private void disableButtons() {
        runOnUiThread(() -> {
            blastBtn.setEnabled(false);
            encryptBtn.setEnabled(false);
            decryptBtn.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        });
    }

    private void enableButtons() {
        runOnUiThread(() -> {
            blastBtn.setEnabled(true);
            encryptBtn.setEnabled(true);
            decryptBtn.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void appendLog(String msg) {
        runOnUiThread(() -> {
            logs.append(msg + "\n");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission() {
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQ_PERM);
        }
    }

    public static String getDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }

    // Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_theme) {
            Toast.makeText(this, "Theme selection coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
