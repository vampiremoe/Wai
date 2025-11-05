package com.wai.vaultapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

public class FinderActivity extends AppCompatActivity {

    TextView logs;
    Button button;
    Button encryptButton;
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
        logs.setTextColor(Color.BLACK);
        button = findViewById(R.id.blast);
        encryptButton = findViewById(R.id.encrypt_btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasPermission()){
                    getPermission();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        initBlast();
                        enableButton();
                    }
                }).start();
            }
        });

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasPermission()){
                    getPermission();
                    return;
                }
                openFilePicker();
            }
        });

        BroadcastReceiver brLog = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String[] msg = intent.getStringArrayExtra("log_content");
                switch (Integer.valueOf(msg[0])){
                    case 1:
                        button.setEnabled(false);
                        encryptButton.setEnabled(false);
                        break;
                    case 2:
                        button.setEnabled(true);
                        encryptButton.setEnabled(true);
                        break;
                    default:
                        try{
                            Log.i("Finder",msg[1]);
                            logs.append(msg[1]+"\n");
                            scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });
                        } catch (Exception ex){}
                        break;
                }
            }
        };
        registerReceiver(brLog, new IntentFilter("com.wai.vaultapp.log_u"));
    }

    private void openFilePicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select files to encrypt"), REQ_PICK_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_PICK_FILES && resultCode == RESULT_OK) {
            final ArrayList<Uri> uris = new ArrayList<>();
            if (data == null) {
                return;
            }
            if (data.getData() != null) {
                uris.add(data.getData());
            } else {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        uris.add(data.getClipData().getItemAt(i).getUri());
                    }
                }
            }
            if (uris.size() > 0) {
                // run encryption on background thread
                disableButton();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final ArrayList<String> savedPaths = new ArrayList<>();
                        for (Uri u : uris) {
                            try {
                                String saved = processAndEncryptUri(u);
                                if (saved != null) savedPaths.add(saved);
                            } catch (Exception e) {
                                appendLog("Encryption failed for a file: " + e.getMessage());
                            }
                        }
                        // show aggregate UI notifications on UI thread if you prefer; currently each file shows its own dialog/toast inside processAndEncryptUri
                        enableButton();
                    }
                }).start();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processAndEncryptUri(Uri uri) throws IOException {
        // copy content:// uri to temp file
        String displayName = getDisplayNameForUri(uri);
        if (displayName == null) displayName = "unknown";
        File temp = new File(getCacheDir(), "tmp_" + System.currentTimeMillis() + "_" + displayName);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
        } catch (Exception e){
            appendLog("Failed to copy selected file: " + displayName);
            if (temp.exists()) temp.delete();
            return;
        }

        // determine type
        String type = getContentResolver().getType(uri);
        String category = "other";
        if (type != null) {
            if (type.startsWith("image")) category = "image";
            else if (type.startsWith("video")) category = "video";
        } else {
            // fallback to extension
            String lower = displayName.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".bmp")) category = "image";
            else if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".webm")) category = "video";
        }

        // create target dirs: /storage/emulated/0/waidecryptor/encrypted/{image,video,other}
        File root = new File(Environment.getExternalStorageDirectory(), "waidecryptor");
        File encDir = new File(root, "encrypted/" + category);
        if (!encDir.exists()) encDir.mkdirs();

        // construct output filename
        String outName = displayName + ".enc";
        File outFile = new File(encDir, outName);
        for (int i=0; outFile.exists(); i++){
            outFile = new File(encDir, "(" + i + ") " + outName);
        }

        boolean ok = Encoder.encryptReverse(temp, outFile);

        // remove temp
        temp.delete();

        final String savedPath = outFile.getAbsolutePath();
        if (ok) {
            appendLog("Encrypted and saved: " + savedPath);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FinderActivity.this, "Saved: " + savedPath, Toast.LENGTH_SHORT).show();
                    new AlertDialog.Builder(FinderActivity.this)
                            .setTitle("File encrypted")
                            .setMessage("Saved to:\n" + savedPath)
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        } else {
            appendLog("Encryption failed for: " + displayName);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FinderActivity.this, "Encryption failed for: " + displayName, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getDisplayNameForUri(Uri uri) {
        String result = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    result = cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private void initBlast() {
        disableButton();
        File file = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
        Log.i("FindX", file.getAbsolutePath());
        String dbPath = null;
        File[] fileList = file.listFiles();
        if(fileList==null){
            appendLog("Can not find Vault. This may cause due to file permission issues. Please make sure the app has write permission.");
            return;
        }
        for (File f : fileList) {
            if (f.isDirectory() || f.getAbsolutePath().endsWith("journal")) {
                continue;
            }
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                byte[] bytes = new byte[10];
                fileInputStream.read(bytes);
                fileInputStream.close();
                if (new String(bytes).startsWith("SQLite")) {
                    dbPath = f.getAbsolutePath();
                    break;
                }
            } catch (Exception ex) {
            }
        }
        if (dbPath == null) {
            appendLog("Can not find Vault database.");
            return;
        }
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);
        if(cursor.getCount()==0){
            appendLog("No image or video found in the Database");
            return;
        }
        appendLog("Found "+cursor.getCount()+" items(s) in the vault.");

        Decoder decoder = new Decoder();

        // root decrypted folder
        File rootDec = new File(Environment.getExternalStorageDirectory(), "waidecryptor/decrypted");
        if (!rootDec.exists()) rootDec.mkdirs();

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            final String originalName = cursor.getString(1);
            final String currentPath = cursor.getString(2);
            final String passwordId = cursor.getString(0);
            final String fileType = cursor.getString(3);
            appendLog("Decoding "+(i+1)+" of "+cursor.getCount()+" ...");

            // Decide category for save folder based on fileType
            String category = categorizeFileType(fileType);
            File categoryDir = new File(rootDec, category);
            if (!categoryDir.exists()) categoryDir.mkdirs();

            String savedPath = decoder.decodeAndSave(originalName,currentPath,passwordId,fileType, categoryDir.getAbsolutePath());
            if(savedPath==null){
                appendLog("Decoding Failed for: "+originalName);
            } else {
                appendLog("Decoded and saved: " + savedPath);
                // show toast + dialog on UI thread
                final String finalSavedPath = savedPath;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FinderActivity.this, "Saved: " + finalSavedPath, Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(FinderActivity.this)
                                .setTitle("File saved")
                                .setMessage("Saved to:\n" + finalSavedPath)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        }
        appendLog("\nDecoding finished. All files saved under:\n"+rootDec.getAbsolutePath());
    }

    private String categorizeFileType(String fileType){
        if (fileType == null) return "other";
        String ft = fileType.toLowerCase();
        if (ft.contains("jpg") || ft.contains("jpeg") || ft.contains("png") || ft.contains("gif") || ft.contains("bmp") || ft.contains("image")) return "image";
        if (ft.contains("mp4") || ft.contains("mkv") || ft.contains("mov") || ft.contains("avi") || ft.contains("webm") || ft.contains("video")) return "video";
        return "other";
    }

    private boolean hasPermission() {
        boolean write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return write && read;
    }

    private void getPermission() {
        ArrayList<String> req = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            req.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!req.isEmpty()) {
            ActivityCompat.requestPermissions(this, req.toArray(new String[0]), REQ_PERM);
        }
    }

    private void sendBrodcast(int code,String text){
        Intent intent = new Intent("com.wai.vaultapp.log_u");
        intent.putExtra("log_content", new String[]{String.valueOf(code), text});
        sendBroadcast(intent);
    }

    private void disableButton(){
        sendBrodcast(1,"");
    }
    private void enableButton(){
        sendBrodcast(2,"");
    }

    private void appendLog(String text){
        sendBrodcast(0,text);
    }

    public static String getDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(new Date());
    }
}
