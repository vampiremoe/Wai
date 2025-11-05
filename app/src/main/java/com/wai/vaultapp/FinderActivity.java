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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
                scanVault();
                enableButtons();
            }).start();
        });

        encryptBtn.setOnClickListener(v -> Snackbar.make(scrollView, "Encrypt feature coming soon", Snackbar.LENGTH_SHORT).show());
        decryptBtn.setOnClickListener(v -> Snackbar.make(scrollView, "Decrypt feature coming soon", Snackbar.LENGTH_SHORT).show());
    }

    private void scanVault() {
        try {
            appendLog("Scanning vault...");
            File sourceDir = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            if (!sourceDir.exists()) {
                appendLog("Vault folder not found.");
                return;
            }

            File destBase = new File(Environment.getExternalStorageDirectory(), "waivault");
            File encryptDir = new File(destBase, "encrypt");
            File decryptDir = new File(destBase, "decrypt");

            String[] types = {"image", "video", "other"};
            for (String t : types) {
                new File(encryptDir, t).mkdirs();
                new File(decryptDir, t).mkdirs();
            }

            File[] files = sourceDir.listFiles();
            if (files == null) {
                appendLog("No files found in vault folder.");
                return;
            }

            Decoder decoder = new Decoder();

            for (File f : files) {
                if (f.isDirectory()) continue;

                String ext = "";
                int dot = f.getName().lastIndexOf('.');
                if (dot >= 0) ext = f.getName().substring(dot + 1).toLowerCase();

                String typeFolder;
                if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif")) {
                    typeFolder = "image";
                } else if (ext.equals("mp4") || ext.equals("avi") || ext.equals("mov")) {
                    typeFolder = "video";
                } else {
                    typeFolder = "other";
                }

                String savedPath = decoder.decodeAndSave(f.getName(), f.getAbsolutePath(), "default", ext, new File(encryptDir, typeFolder).getAbsolutePath());
                if (savedPath != null) {
                    appendLog("Saved: " + savedPath);
                    // Optional: Open viewer if image/video
                    if (typeFolder.equals("image") || typeFolder.equals("video")) {
                        openFile(savedPath, typeFolder);
                    }
                } else {
                    appendLog("Failed: " + f.getName());
                }
            }
            appendLog("Vault scan completed.");
        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
        }
    }

    private void openFile(String path, String type) {
        try {
            File file = new File(path);
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (type.equals("image")) intent.setDataAndType(uri, "image/*");
            else if (type.equals("video")) intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            appendLog("Cannot open file: " + path);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_themes) {
            String[] themes = {"Dark", "Light", "Kaki"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Theme")
                    .setItems(themes, (dialog, which) -> {
                        Toast.makeText(this, "Selected: " + themes[which], Toast.LENGTH_SHORT).show();
                        // TODO: Apply theme
                    }).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
