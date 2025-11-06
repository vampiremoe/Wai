package com.wai.vaultapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DELAY = 2000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    File file = new File(getFilesDir(), getResources().getString(R.string.eula_file));
                    if (file.exists()) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, EulaActivity.class));
                    }
                } else {
                    Intent intent = new Intent(SplashActivity.this, QActivity.class);
                    intent.putExtra("QMSG", "The app currently supports SDK version 28 or lower (Android 9 or lower).\nYou have SDK version " + android.os.Build.VERSION.SDK_INT);
                    startActivity(intent);
                }
                finish();
            }
        }, SPLASH_DELAY);
    }
}