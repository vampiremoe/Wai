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
        decryptBtn = findViewById(R.id.decrypt_btn);
        encryptBtn = findViewById(R.id.encrypt_btn);
        progressBar = findViewById(R.id.progress_bar);

        // Apply button text color & shadow
        blastBtn.setTextColor(getResources().getColor(android.R.color.black));
        decryptBtn.setTextColor(getResources().getColor(android.R.color.black));
        encryptBtn.setTextColor(getResources().getColor(android.R.color.black));

        // Decrypt button kaki background
        decryptBtn.setBackgroundTintList(getResources().getColorStateList(R.color.kaki));

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
                initBlast("/storage/emulated/0/waivault/scan");
                enableButtons();
            }).start();
        });

        decryptBtn.setOnClickListener(v -> askDecryptMode());
        encryptBtn.setOnClickListener(v -> Snackbar.make(scrollView, "Encryption feature coming soon", Snackbar.LENGTH_SHORT).show());
    }

    private void askDecryptMode() {
        new AlertDialog.Builder(this)
                .setTitle("Decrypt Options")
                .setMessage("Decrypt other than QAVault?")
                .setPositiveButton("Yes", (d, w) -> Snackbar.make(scrollView, "Custom decrypt flow coming soon", Snackbar.LENGTH_SHORT).show())
                .setNegativeButton("No", (d, w) -> new Thread(() -> {
                    disableButtons();
                    initBlast("/storage/emulated/0/waivault/decrypt");
                    enableButtons();
                }).start())
                .show();
    }

    private void initBlast(String baseFolder) {
        try {
            appendLog("Starting vault scan...");
            File file = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            File whereSave = new File(baseFolder + "/" + getDate());
            if (!whereSave.exists()) whereSave.mkdirs();
            Log.i("FindX", file.getAbsolutePath());

            File[] fileList = file.listFiles();
            if (fileList == null) {
                appendLog("Cannot find Vault folder. Check permissions.");
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
                appendLog("No items found in database.");
                return;
            }

            appendLog("Found " + cursor.getCount() + " item(s).");
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                appendLog("Processing " + (i + 1) + " of " + cursor.getCount() + "...");
                
                // Determine output folder by type
                String type = cursor.getString(3).toLowerCase();
                String subFolder = "other";
                if (type.contains("image")) subFolder = "image";
                else if (type.contains("video")) subFolder = "video";
                File outFolder = new File(whereSave, subFolder);
                if (!outFolder.exists()) outFolder.mkdirs();

                boolean success = decoder.decodeAndSave(cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(0),
                        cursor.getString(3),
                        outFolder.getAbsolutePath());

                if (!success) appendLog("Failed: " + cursor.getString(1));
                else appendLog("Saved: " + cursor.getString(1));
            }

            appendLog("\nFinished. Files saved in: " + whereSave.getAbsolutePath());
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

    // Optional: open media with default player
    private void openMedia(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "*/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // Toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_themes) {
            Toast.makeText(this, "Themes customization coming soon!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
