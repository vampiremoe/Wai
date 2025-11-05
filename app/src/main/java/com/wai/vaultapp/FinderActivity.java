package com.wai.vaultapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FinderActivity extends AppCompatActivity {

    TextView logs;
    Button blastBtn, encryptBtn, decryptBtn;
    ScrollView scrollView;
    ProgressBar progressBar;

    private static final int REQ_PERM = 1;

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

        decryptBtn.setOnClickListener(v -> askDecryptMode());
        encryptBtn.setOnClickListener(v -> Snackbar.make(scrollView, "Encryption feature not yet migrated", Snackbar.LENGTH_SHORT).show());
    }

    private void askDecryptMode() {
        new AlertDialog.Builder(this)
                .setTitle("Decrypt Options")
                .setMessage("Would you like to decrypt other than QAVault?")
                .setPositiveButton("Yes", (d, w) -> Snackbar.make(scrollView, "Custom decrypt flow coming soon", Snackbar.LENGTH_SHORT).show())
                .setNegativeButton("No", (d, w) -> new Thread(() -> {
                    disableButtons();
                    initBlast();
                    enableButtons();
                }).start())
                .show();
    }

    private void initBlast() {
        try {
            appendLog("Starting vault scan...");
            File file = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            File whereSave = new File(Environment.getExternalStorageDirectory(), "waivault/decrypt");
            if (!whereSave.exists()) whereSave.mkdirs();
            Log.i("FindX", file.getAbsolutePath());

            File[] fileList = file.listFiles();
            if (fileList == null) {
                appendLog("Cannot find Vault folder. Please check permissions.");
                return;
            }

            String dbPath = null;
            for (File f : fileList) {
                if (f.isDirectory() || f.getAbsolutePath().endsWith("journal")) continue;
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] bytes = new byte[10];
                    fis.read(bytes);
                    if (new String(bytes).startsWith("SQLite")) {
                        dbPath = f.getAbsolutePath();
                        break;
                    }
                } catch (Exception ignored) {}
            }

            if (dbPath == null) {
                appendLog("Cannot find Vault database.");
                return;
            }

            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);
            if (cursor.getCount() == 0) {
                appendLog("No images or videos found in database.");
                return;
            }

            appendLog("Found " + cursor.getCount() + " item(s) in the vault.");
            Decoder decoder = new Decoder();

            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                appendLog("Decoding " + (i + 1) + " of " + cursor.getCount() + "...");
                String savedPath = decoder.decodeAndSave(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(0),
                        cursor.getString(3),
                        whereSave.getAbsolutePath()
                );
                if (savedPath != null) {
                    appendLog("Saved: " + savedPath);
                } else {
                    appendLog("Decoding failed: " + cursor.getString(1));
                }
            }
            appendLog("\nDecoding finished. All files saved in:\n" + whereSave.getAbsolutePath());
            cursor.close();
            db.close();

        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
        }
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

    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(new Date());
    }
}
