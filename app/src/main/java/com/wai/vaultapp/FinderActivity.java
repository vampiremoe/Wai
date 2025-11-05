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
import android.net.Uri;
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

        logs = findViewById(R.id.logs);
        scrollView = findViewById(R.id.scroll_log);
        blastBtn = findViewById(R.id.blast);
        encryptBtn = findViewById(R.id.encrypt_btn);
        decryptBtn = findViewById(R.id.decrypt_btn);
        progressBar = findViewById(R.id.progress_bar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        getPermission();

        // Scan Vault
        blastBtn.setOnClickListener(v -> {
            if (!hasPermission()) {
                getPermission();
                return;
            }
            new Thread(() -> {
                disableButtons();
                scanVault();
                enableButtons();
            }).start();
        });

        // Decrypt
        decryptBtn.setOnClickListener(v -> {
            new Thread(() -> {
                disableButtons();
                processVault(false); // false = decrypt
                enableButtons();
            }).start();
        });

        // Encrypt
        encryptBtn.setOnClickListener(v -> {
            new Thread(() -> {
                disableButtons();
                processVault(true); // true = encrypt
                enableButtons();
            }).start();
        });
    }

    private void scanVault() {
        try {
            appendLog("Starting vault scan...");
            File vaultDir = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            if (!vaultDir.exists() || !vaultDir.isDirectory()) {
                appendLog("Cannot find Vault folder. Check permissions.");
                return;
            }

            File[] files = vaultDir.listFiles();
            if (files == null || files.length == 0) {
                appendLog("No files found in vault.");
                return;
            }

            int dbCount = 0;
            for (File f : files) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] bytes = new byte[10];
                    fis.read(bytes);
                    if (new String(bytes).startsWith("SQLite")) dbCount++;
                } catch (Exception ignored) {}
            }
            appendLog("Found " + dbCount + " vault databases.");
        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
        }
    }

    private void processVault(boolean encrypt) {
        try {
            File vaultDir = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            if (!vaultDir.exists()) {
                appendLog("Vault folder not found.");
                return;
            }

            File outBase = new File(Environment.getExternalStorageDirectory(), "waivault/" + (encrypt ? "encrypt" : "decrypt") + "/" + getDate());
            if (!outBase.exists()) outBase.mkdirs();

            File[] files = vaultDir.listFiles();
            if (files == null || files.length == 0) {
                appendLog("No files to process.");
                return;
            }

            for (File f : files) {
                String name = f.getName();
                String ext = "";
                int dot = name.lastIndexOf(".");
                if (dot != -1) ext = name.substring(dot + 1).toLowerCase();

                File outDir = new File(outBase, classify(ext));
                if (!outDir.exists()) outDir.mkdirs();

                String savedPath;
                if (encrypt) {
                    Encoder encoder = new Encoder();
                    savedPath = encoder.encodeAndSave(name, f.getAbsolutePath(), outDir.getAbsolutePath());
                } else {
                    Decoder decoder = new Decoder();
                    savedPath = decoder.decodeAndSave(name, f.getAbsolutePath(), "default", ext, outDir.getAbsolutePath());
                }

                if (savedPath != null) {
                    appendLog((encrypt ? "Encrypted: " : "Decrypted: ") + savedPath);
                    openMediaFile(savedPath); // optional, open automatically
                } else {
                    appendLog((encrypt ? "Encryption failed: " : "Decryption failed: ") + name);
                }
            }
        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
        }
    }

    private String classify(String ext) {
        if (ext.matches("jpg|jpeg|png|gif|bmp")) return "images";
        if (ext.matches("mp4|mkv|avi|mov")) return "videos";
        return "other";
    }

    private void openMediaFile(String path) {
        try {
            File f = new File(path);
            Uri uri = Uri.fromFile(f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (path.matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) intent.setDataAndType(uri, "image/*");
            else if (path.matches(".*\\.(mp4|mkv|avi|mov)$")) intent.setDataAndType(uri, "video/*");
            else intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Cannot open media: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            // TODO: implement theme change logic
            Toast.makeText(this, "Theme customization coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_themes) {
        // TODO: show theme selection dialog
        new AlertDialog.Builder(this)
            .setTitle("Select Theme")
            .setItems(new String[]{"Dark", "Light", "Kaki", "Custom"}, (dialog, which) -> {
                // handle theme change
                Snackbar.make(scrollView, "Theme changed (demo)", Snackbar.LENGTH_SHORT).show();
            })
            .show();
        return true;
    }
    return super.onOptionsItemSelected(item);
}
}
