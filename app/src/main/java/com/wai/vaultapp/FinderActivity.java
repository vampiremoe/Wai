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
                if (dot != -1) ext = name.substring(dot +
                .setItems(themes, (dialog, which) -> {
                    Snackbar.make(scrollView, "Theme " + themes[which] + " applied!", Snackbar.LENGTH_SHORT).show();
                    // Save in preferences and apply colors dynamically
                })
                .show();
    }
}
