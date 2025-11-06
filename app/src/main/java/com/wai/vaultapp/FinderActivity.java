package com.wai.vaultapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FinderActivity extends AppCompatActivity {

    TextView logs;
    ScrollView scrollView;
    Button decryptBtn, encryptBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finder);

        logs = findViewById(R.id.logs);
        scrollView = findViewById(R.id.scroll_log);
        decryptBtn = findViewById(R.id.decrypt_btn);
        encryptBtn = findViewById(R.id.encrypt_btn);

        decryptBtn.setBackgroundColor(Color.parseColor("#C3B091")); // Kaki
        decryptBtn.setTextColor(Color.BLACK);
        encryptBtn.setTextColor(Color.BLACK);

        decryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasPermission()){
                    getPermission();
                    return;
                }
                new Thread(() -> {
                    initDecrypt();
                    runOnUiThread(() -> Snackbar.make(decryptBtn, "Decryption complete", Snackbar.LENGTH_SHORT).show());
                }).start();
            }
        });

        encryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasPermission()){
                    getPermission();
                    return;
                }
                new Thread(() -> {
                    initEncrypt();
                    runOnUiThread(() -> Snackbar.make(encryptBtn, "Encryption complete", Snackbar.LENGTH_SHORT).show());
                }).start();
            }
        });
    }

    private void initDecrypt(){
        File vaultFolder = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
        File saveFolder = new File(Environment.getExternalStorageDirectory(), "waivault/decrypt/" + getDate());
        if(!saveFolder.exists()) saveFolder.mkdirs();

        String dbPath = null;
        File[] fileList = vaultFolder.listFiles();
        if(fileList==null){
            appendLog("Cannot access vault folder.");
            return;
        }
        for(File f:fileList){
            if(f.isDirectory() || f.getAbsolutePath().endsWith("journal")) continue;
            try(FileInputStream fis = new FileInputStream(f)){
                byte[] bytes = new byte[10];
                fis.read(bytes);
                if(new String(bytes).startsWith("SQLite")){
                    dbPath = f.getAbsolutePath();
                    break;
                }
            }catch(Exception ex){}
        }
        if(dbPath==null){
            appendLog("Vault DB not found.");
            return;
        }

        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath,null,SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);
        if(cursor.getCount()==0){
            appendLog("No items in vault.");
            return;
        }
        appendLog("Decrypting "+cursor.getCount()+" items...");

        Decoder decoder = new Decoder();
        for(int i=0;i<cursor.getCount();i++){
            cursor.moveToPosition(i);
            appendLog("Decoding "+(i+1)+" of "+cursor.getCount());
            if(!decoder.decodeAndSave(cursor.getString(1),cursor.getString(2),cursor.getString(0),cursor.getString(3),saveFolder.getAbsolutePath())){
                appendLog("Failed: "+cursor.getString(1));
            }
        }
        appendLog("Decryption finished at "+saveFolder.getAbsolutePath());
    }

    private void initEncrypt(){
        File base = new File(Environment.getExternalStorageDirectory(),"waivault/decrypt");
        if(!base.exists()) base.mkdirs();

        Encoder encoder = new Encoder();
        File[] files = base.listFiles();
        if(files==null || files.length==0){
            appendLog("No files to encrypt.");
            return;
        }

        File saveFolder = new File(Environment.getExternalStorageDirectory(),"waivault/encrypt/"+getDate());
        if(!saveFolder.exists()) saveFolder.mkdirs();

        int key = 123; // Example XOR key
        for(File f:files){
            appendLog("Encrypting: "+f.getName());
            if(!encoder.encodeFile(f, saveFolder.getAbsolutePath(), key)){
                appendLog("Failed: "+f.getName());
            }
        }
        appendLog("Encryption finished at "+saveFolder.getAbsolutePath());
    }

    private boolean hasPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission(){
        if(!hasPermission()){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId()==R.id.menu_theme){
            Toast.makeText(this,"Theme selection dialog placeholder",Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}