package com.wai.vaultapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FinderActivity extends AppCompatActivity {

    TextView logs;
    Button blastBtn, encryptBtn, decryptBtn;
    ScrollView scrollView;

    private static final int REQ_PERM = 1;
    private static final int REQ_PICK_FILES = 2;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        blastBtn.setOnClickListener(v -> {
            if (!hasPermission()) {
                getPermission();
                return;
            }
            new Thread(() -> {
                initBlast();
                runOnUiThread(() -> enableButtons());
            }).start();
        });

        encryptBtn.setOnClickListener(v -> {
            if (!hasPermission()) {
                getPermission();
                return;
            }
            openFilePicker();
        });

        decryptBtn.setOnClickListener(v -> askDecryptMode());
    }

    private void askDecryptMode() {
        new AlertDialog.Builder(this)
                .setTitle("Decrypt Options")
                .setMessage("Would you like to decrypt other than QAVault?")
                .setPositiveButton("Yes", (d, w) -> openFilePicker())
                .setNegativeButton("No", (d, w) -> {
                    new Thread(() -> {
                        initBlast();
                        runOnUiThread(this::enableButtons);
                    }).start();
                })
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select files"), REQ_PICK_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FILES && resultCode == RESULT_OK && data != null) {
            ArrayList<Uri> uris = new ArrayList<>();
            if (data.getData() != null) uris.add(data.getData());
            else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++)
                    uris.add(data.getClipData().getItemAt(i).getUri());
            }

            if (!uris.isEmpty()) {
                disableButtons();
                new Thread(() -> {
                    int success = 0, fail = 0;
                    for (Uri uri : uris) {
                        if (processAndEncryptUri(uri)) success++;
                        else fail++;
                    }
                    int s = success, f = fail;
                    runOnUiThread(() -> {
                        enableButtons();
                        Snackbar.make(scrollView,
                                "Encrypted: " + s + " | Failed: " + f,
                                Snackbar.LENGTH_LONG).show();
                    });
                }).start();
            }
        }
    }

    private boolean processAndEncryptUri(Uri uri) {
        try {
            String displayName = getDisplayNameForUri(uri);
            if (displayName == null) displayName = "unknown";
            File temp = new File(getCacheDir(),
                    "tmp_" + System.currentTimeMillis() + "_" + displayName);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }

            String namePart = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String outName = namePart + "_" + getDate() + ".enc";
            File root = new File(Environment.getExternalStorageDirectory(), "waidecryptor/encrypted");
            if (!root.exists()) root.mkdirs();
            File outFile = new File(root, outName);

            boolean ok = Encoder.encryptReverse(temp, outFile);
            temp.delete();

            appendLog(ok ? "Encrypted: " + outName : "Failed: " + displayName);
            return ok;
        } catch (Exception e) {
            appendLog("Error: " + e.getMessage());
            return false;
        }
    }

    private String getDisplayNameForUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String result = cursor.getString(nameIndex);
            cursor.close();
            return result;
        }
        return null;
    }

    // --- Original functions (simplified here for space) ---
    private void initBlast() { appendLog("Vault scan logic here..."); }

    private void appendLog(String msg) {
        runOnUiThread(() -> {
            logs.append(msg + "\n");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private boolean hasPermission() {
        boolean w = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean r = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return w && r;
    }

    private void getPermission() {
        ArrayList<String> req = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (!req.isEmpty())
            ActivityCompat.requestPermissions(this, req.toArray(new String[0]), REQ_PERM);
    }

    private void disableButtons() {
        runOnUiThread(() -> {
            blastBtn.setEnabled(false);
            encryptBtn.setEnabled(false);
            decryptBtn.setEnabled(false);
        });
    }

    private void enableButtons() {
        runOnUiThread(() -> {
            blastBtn.setEnabled(true);
            encryptBtn.setEnabled(true);
            decryptBtn.setEnabled(true);
        });
    }

    private String getDate() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }
}
