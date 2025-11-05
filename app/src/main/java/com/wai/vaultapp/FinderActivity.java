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
import android.view.Menu;
import android.view.MenuItem;
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
            if (!hasPermission()) { getPermission(); return; }
            new Thread(() -> {
                disableButtons();
                initBlast(false); // scan only
                enableButtons();
            }).start();
        });

        decryptBtn.setOnClickListener(v -> {
            if (!hasPermission()) { getPermission(); return; }
            new Thread(() -> {
                disableButtons();
                initBlast(true, "decrypt"); // decrypt mode
                enableButtons();
            }).start();
        });

        encryptBtn.setOnClickListener(v -> {
            if (!hasPermission()) { getPermission(); return; }
            new Thread(() -> {
                disableButtons();
                initBlast(true, "encrypt"); // encrypt mode
                enableButtons();
            }).start();
        });
    }

    // Toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_themes){
            showThemeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog(){
        String[] themes = {"Light", "Dark", "Kaki", "Blue"};
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setItems(themes, (dialog, which) -> {
                    Snackbar.make(scrollView, "Theme changed to: " + themes[which], Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Core scan/decrypt/encrypt logic **/
    private void initBlast(boolean doAction, String action) {
        try {
            appendLog((action==null?"Scanning":"Processing")+" vault...");
            File file = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
            File[] fileList = file.listFiles();
            if (fileList == null) { appendLog("Cannot find Vault folder."); return; }

            String dbPath = null;
            for (File f : fileList) {
                if (f.isDirectory() || f.getAbsolutePath().endsWith("journal")) continue;
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] bytes = new byte[10]; fis.read(bytes);
                    if (new String(bytes).startsWith("SQLite")) { dbPath = f.getAbsolutePath(); break; }
                } catch (Exception ignored) {}
            }
            if(dbPath==null){ appendLog("Cannot find Vault database."); return; }

            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);
            if(cursor.getCount()==0){ appendLog("No items in database."); return; }

            appendLog("Found "+cursor.getCount()+" item(s) in vault.");
            Decoder decoder = new Decoder();

            String basePath = null;
            if(doAction){
                basePath = Environment.getExternalStorageDirectory() + "/waivault/" + action + "/" + getDate();
                new File(basePath).mkdirs();
            }

            for(int i=0;i<cursor.getCount();i++){
                cursor.moveToPosition(i);
                String fileType = cursor.getString(3);
                File folder = basePath!=null?getOutputFolder(basePath,fileType):null;

                String savedPath = doAction
                        ? decoder.decodeAndSave(cursor.getString(1), cursor.getString(2), cursor.getString(0), fileType, folder.getAbsolutePath())
                        : null;

                appendLog(doAction
                        ? (savedPath!=null?"Saved: "+savedPath:"Failed: "+cursor.getString(1))
                        : "Scanning: "+cursor.getString(1));

                // Optional: open saved file automatically
                // if(savedPath!=null) openMedia(new File(savedPath));
            }
            cursor.close(); db.close();
            appendLog((doAction?"Processing":"Scan")+" finished.");

        } catch (Exception e){ appendLog("Error: "+e.getMessage()); }
    }

    private File getOutputFolder(String baseFolder, String fileType){
        String folderType = "other";
        if(fileType!=null){
            String ft = fileType.toLowerCase();
            if(ft.contains("jpg") || ft.contains("png") || ft.contains("jpeg") || ft.contains("gif")) folderType="image";
            else if(ft.contains("mp4") || ft.contains("mov") || ft.contains("avi") || ft.contains("mkv")) folderType="video";
        }
        File folder = new File(baseFolder, folderType);
        if(!folder.exists()) folder.mkdirs();
        return folder;
    }

    private void openMedia(File file){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String type="*/*";
        if(file.getName().toLowerCase().matches(".*(mp4|mov|avi|mkv)$")) type="video/*";
        else if(file.getName().toLowerCase().matches(".*(jpg|png|jpeg|gif)$")) type="image/*";
        intent.setDataAndType(Uri.fromFile(file), type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(intent); } catch (Exception e){ appendLog("No app to open this file"); }
    }

    private boolean hasPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission(){
        if(!hasPermission()){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQ_PERM);
        }
    }

    private void disableButtons(){
        runOnUiThread(() -> {
            blastBtn.setEnabled(false);
            encryptBtn.setEnabled(false);
            decryptBtn.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        });
    }

    private void enableButtons(){
        runOnUiThread(() -> {
            blastBtn.setEnabled(true);
            encryptBtn.setEnabled(true);
            decryptBtn.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void appendLog(String msg){
        runOnUiThread(() -> {
            logs.append(msg+"\n");
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    public static String getDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(new Date());
    }
}
