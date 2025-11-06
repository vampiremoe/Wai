package com.wai.vaultapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    
    private TextView terminalDisplay;
    private TextView logDisplay;
    private ScrollView logScroll;
    private Button btnScan, btnDecrypt, btnEncrypt;
    private Handler handler;
    private Random random;
    
    // Terminal falling letters variables
    private boolean isTerminalAnimating = false;
    private final String[] terminalMessages = {
        "SYSTEM INITIALIZED", "VAULT_SCANNER_READY", "DECRYPTION_MODULE_ACTIVE",
        "ENCRYPTION_PROTOCOL_LOADED", "AWAITING_USER_INPUT"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_modern);
        
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        
        setupToolbar();
        setupViews();
        setupButtonListeners();
        
        // Start initial terminal animation
        startTerminalAnimation();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Setup hamburger menu (empty for now)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }
    }
    
    private void setupViews() {
        terminalDisplay = findViewById(R.id.terminal_display);
        logDisplay = findViewById(R.id.log_display);
        logScroll = findViewById(R.id.log_scroll);
        
        btnScan = findViewById(R.id.btn_scan);
        btnDecrypt = findViewById(R.id.btn_decrypt);
        btnEncrypt = findViewById(R.id.btn_encrypt);
        
        // Setup button colors with automatic text contrast
        setupButtonWithContrast(btnScan, R.color.button_scan);
        setupButtonWithContrast(btnDecrypt, R.color.button_decrypt);
        setupButtonWithContrast(btnEncrypt, R.color.button_encrypt);
    }
    
    private void setupButtonWithContrast(Button button, int colorRes) {
        int color = getColor(colorRes);
        button.setBackgroundColor(color);
        
        // Calculate luminance for contrast
        double luminance = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color);
        if (luminance > 186) {
            button.setTextColor(Color.BLACK); // Dark text for bright backgrounds
        } else {
            button.setTextColor(Color.WHITE); // Light text for dark backgrounds
        }
    }
    
    private void setupButtonListeners() {
        btnScan.setOnClickListener(v -> onScanVaultClicked());
        btnDecrypt.setOnClickListener(v -> onDecryptClicked());
        btnEncrypt.setOnClickListener(v -> onEncryptClicked());
    }
    
    private void onScanVaultClicked() {
        appendLog("=== SCAN VAULT INITIATED ===");
        startTerminalAnimation();
        
        new Thread(() -> {
            // Simulate scanning process
            try {
                Thread.sleep(1000);
                
                handler.post(() -> {
                    boolean found = scanForVaultFiles();
                    if (found) {
                        showFancyDisplay("FOUND");
                        appendLog("Vault database found. Starting decryption...");
                        decryptAllFoundFiles();
                    } else {
                        showFancyDisplay("COULDN'T FIND IT");
                        appendLog("No vault database found in expected location.");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void onDecryptClicked() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Manual Decryption")
            .setMessage("Do you want to decrypt files yourself?")
            .setPositiveButton("Yes", (dialog, which) -> {
                startFilePickerForDecryption();
            })
            .setNegativeButton("No", (dialog, which) -> {
                appendLog("Decryption cancelled by user.");
            })
            .show();
    }
    
    private void onEncryptClicked() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("File Encryption")
            .setMessage("Do you want to encrypt files?")
            .setPositiveButton("Yes", (dialog, which) -> {
                startFilePickerForEncryption();
            })
            .setNegativeButton("No", (dialog, which) -> {
                appendLog("Encryption cancelled by user.");
            })
            .show();
    }
    
    private boolean scanForVaultFiles() {
        // Implementation from your original FinderActivity
        File vaultDir = new File(getExternalFilesDir(null), "SystemAndroid/Data");
        return vaultDir.exists() && vaultDir.listFiles() != null;
    }
    
    private void decryptAllFoundFiles() {
        // Use your existing FinderActivity logic here
        appendLog("Starting bulk decryption process...");
        // This would call your existing decryption logic
    }
    
    private void startFilePickerForDecryption() {
        // Implement file picker intent
        appendLog("Please select files to decrypt...");
        // File picker implementation would go here
    }
    
    private void startFilePickerForEncryption() {
        // Implement file picker intent  
        appendLog("Please select files to encrypt...");
        // File picker implementation would go here
    }
    
    private void startTerminalAnimation() {
        if (isTerminalAnimating) return;
        
        isTerminalAnimating = true;
        terminalDisplay.setText("");
        
        new Thread(() -> {
            String message = terminalMessages[random.nextInt(terminalMessages.length)];
            animateTerminalText(message);
        }).start();
    }
    
    private void animateTerminalText(String text) {
        StringBuilder currentText = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            try {
                Thread.sleep(50 + random.nextInt(100));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            currentText.append(c);
            String finalText = currentText.toString();
            
            handler.post(() -> {
                terminalDisplay.setText(finalText);
            });
        }
        
        // Add some random falling characters after the message
        addFallingCharacters();
    }
    
    private void addFallingCharacters() {
        // Implementation for falling matrix-style characters
        handler.postDelayed(() -> {
            isTerminalAnimating = false;
        }, 2000);
    }
    
    private void showFancyDisplay(String message) {
        handler.post(() -> {
            terminalDisplay.setText(message);
            terminalDisplay.setTextSize(24);
            terminalDisplay.setTextColor(getColor(R.color.lime_primary));
            
            // Reset after delay
            handler.postDelayed(() -> {
                terminalDisplay.setTextSize(14);
                startTerminalAnimation();
            }, 3000);
        });
    }
    
    private void appendLog(String message) {
        handler.post(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logDisplay.append("[" + timestamp + "] " + message + "\n");
            
            // Auto-scroll to bottom
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isTerminalAnimating = false;
        handler.removeCallbacksAndMessages(null);
    }
}