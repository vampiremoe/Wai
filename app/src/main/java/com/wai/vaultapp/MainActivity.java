package com.wai.vaultapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    
    private TextView terminalDisplay;
    private TextView logDisplay;
    private ScrollView logScroll;
    private Button btnScan, btnDecrypt, btnEncrypt;
    private Handler handler;
    private Random random;
    
    private boolean isTerminalAnimating = false;
    private final String[] terminalMessages = {
        "SYSTEM INITIALIZED", "VAULT_SCANNER_READY", "DECRYPTION_MODULE_ACTIVE",
        "ENCRYPTION_PROTOCOL_LOADED", "AWAITING_USER_INPUT"
    };
    
    private final ActivityResultLauncher<String[]> decryptFilePicker = registerForActivityResult(
        new ActivityResultContracts.OpenMultipleDocuments(),
        uris -> {
            if (uris != null && !uris.isEmpty()) {
                askForDecryptionKey(uris);
            } else {
                appendLog("No files selected for decryption.");
            }
        }
    );
    
    private final ActivityResultLauncher<String[]> encryptFilePicker = registerForActivityResult(
        new ActivityResultContracts.OpenMultipleDocuments(),
        uris -> {
            if (uris != null && !uris.isEmpty()) {
                askForEncryptionKey(uris);
            } else {
                appendLog("No files selected for encryption.");
            }
        }
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_modern);
        
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        
        setupToolbar();
        setupViews();
        setupButtonListeners();
        startTerminalAnimation();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
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
        
        setupButtonWithContrast(btnScan, R.color.button_scan);
        setupButtonWithContrast(btnDecrypt, R.color.button_decrypt);
        setupButtonWithContrast(btnEncrypt, R.color.button_encrypt);
    }
    
    private void setupButtonWithContrast(Button button, int colorRes) {
        // FIXED: Use ContextCompat.getColor() instead of getColor() for API 21+ compatibility
        int color = ContextCompat.getColor(this, colorRes);
        button.setBackgroundColor(color);
        double luminance = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color);
        button.setTextColor(luminance > 186 ? Color.BLACK : Color.WHITE);
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
            try {
                Thread.sleep(1000);
                handler.post(() -> {
                    boolean found = scanForVaultFiles();
                    if (found) {
                        showFancyDisplay("FOUND");
                        appendLog("Vault database found. Starting decryption...");
                        startActivity(new Intent(MainActivity.this, FinderActivity.class));
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
                decryptFilePicker.launch(new String[]{"*/*"});
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
                encryptFilePicker.launch(new String[]{"*/*"});
            })
            .setNegativeButton("No", (dialog, which) -> {
                appendLog("Encryption cancelled by user.");
            })
            .show();
    }
    
    private void askForDecryptionKey(List<Uri> uris) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Decryption Key")
            .setMessage("Do you know the XOR key?")
            .setPositiveButton("Yes", (dialog, which) -> {
                showKeyInputDialog(uris, true);
            })
            .setNegativeButton("No", (dialog, which) -> {
                appendLog("Auto-detecting keys for selected files...");
                autoDecryptFiles(uris);
            })
            .show();
    }
    
    private void askForEncryptionKey(List<Uri> uris) {
        showKeyInputDialog(uris, false);
    }
    
    private void showKeyInputDialog(List<Uri> uris, boolean isDecrypt) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_key_input, null);
        EditText keyInput = dialogView.findViewById(R.id.key_input);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(isDecrypt ? "Enter XOR Key (0-255)" : "Set XOR Key (0-255)")
            .setView(dialogView)
            .setPositiveButton("Done", (dialog, which) -> {
                try {
                    String keyText = keyInput.getText().toString();
                    if (!keyText.isEmpty()) {
                        int key = Integer.parseInt(keyText);
                        if (key >= 0 && key <= 255) {
                            if (isDecrypt) {
                                decryptFilesWithKey(uris, key);
                            } else {
                                encryptFilesWithKey(uris, key);
                            }
                        } else {
                            appendLog("Invalid key. Must be between 0-255.");
                        }
                    } else {
                        appendLog("Please enter a key value.");
                    }
                } catch (NumberFormatException e) {
                    appendLog("Invalid key format. Please enter a number 0-255.");
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                appendLog("Operation cancelled.");
            })
            .show();
    }
    
    private void autoDecryptFiles(List<Uri> uris) {
        new Thread(() -> {
            Decoder decoder = new Decoder();
            
            for (Uri uri : uris) {
                try {
                    String fileName = getFileName(uri);
                    String outputDir = getOutputDirectory("decrypted");
                    
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    String tempPath = getCacheDir() + "/temp_" + System.currentTimeMillis() + "_" + fileName;
                    FileOutputStream tempStream = new FileOutputStream(tempPath);
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        tempStream.write(buffer, 0, bytesRead);
                    }
                    
                    inputStream.close();
                    tempStream.close();
                    
                    String[] result = decoder.decodeFileAutoDetect(tempPath, outputDir);
                    
                    new File(tempPath).delete();
                    
                    if (result[0].equals("SUCCESS")) {
                        handler.post(() -> appendLog("Auto-decrypted: " + fileName + " - " + result[1]));
                    } else {
                        handler.post(() -> appendLog("Failed to auto-decrypt: " + fileName + " - " + result[1]));
                    }
                    
                } catch (Exception e) {
                    handler.post(() -> appendLog("Error processing: " + uri.getLastPathSegment()));
                }
            }
        }).start();
    }
    
    private void decryptFilesWithKey(List<Uri> uris, int key) {
        new Thread(() -> {
            Decoder decoder = new Decoder();
            
            for (Uri uri : uris) {
                try {
                    String fileName = getFileName(uri);
                    String outputDir = getOutputDirectory("decrypted");
                    
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    String tempPath = getCacheDir() + "/temp_" + System.currentTimeMillis() + "_" + fileName;
                    FileOutputStream tempStream = new FileOutputStream(tempPath);
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        tempStream.write(buffer, 0, bytesRead);
                    }
                    
                    inputStream.close();
                    tempStream.close();
                    
                    boolean success = decoder.decodeFileWithKey(tempPath, key, outputDir);
                    
                    new File(tempPath).delete();
                    
                    if (success) {
                        handler.post(() -> appendLog("Decrypted: " + fileName + " with key: " + key));
                    } else {
                        handler.post(() -> appendLog("Failed to decrypt: " + fileName));
                    }
                    
                } catch (Exception e) {
                    handler.post(() -> appendLog("Error decrypting: " + uri.getLastPathSegment()));
                }
            }
        }).start();
    }
    
    private void encryptFilesWithKey(List<Uri> uris, int key) {
        new Thread(() -> {
            Encoder encoder = new Encoder();
            
            for (Uri uri : uris) {
                try {
                    String fileName = getFileName(uri);
                    String outputDir = getOutputDirectory("encrypted");
                    
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    String tempPath = getCacheDir() + "/temp_" + System.currentTimeMillis() + "_" + fileName;
                    FileOutputStream tempStream = new FileOutputStream(tempPath);
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        tempStream.write(buffer, 0, bytesRead);
                    }
                    
                    inputStream.close();
                    tempStream.close();
                    
                    boolean success = encoder.encodeFileWithKey(tempPath, key, outputDir);
                    
                    new File(tempPath).delete();
                    
                    if (success) {
                        handler.post(() -> appendLog("Encrypted: " + fileName + " with key: " + key));
                    } else {
                        handler.post(() -> appendLog("Failed to encrypt: " + fileName));
                    }
                    
                } catch (Exception e) {
                    handler.post(() -> appendLog("Error encrypting: " + uri.getLastPathSegment()));
                }
            }
        }).start();
    }
    
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null && path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return "unknown_file";
    }
    
    private String getOutputDirectory(String type) {
        File outputDir;
        
        if (type.equals("decrypted")) {
            outputDir = new File(getExternalFilesDir(null), "waivault/decrypted");
        } else if (type.equals("encrypted")) {
            outputDir = new File(getExternalFilesDir(null), "waivault/encrypted");
        } else {
            outputDir = new File(getExternalFilesDir(null), "waivault/" + type);
        }
        
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir.getAbsolutePath();
    }
    
    private boolean scanForVaultFiles() {
        File vaultDir = new File(getExternalFilesDir(null), "SystemAndroid/Data");
        return vaultDir.exists() && vaultDir.listFiles() != null && vaultDir.listFiles().length > 0;
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
            final String displayText = currentText.toString();
            handler.post(() -> terminalDisplay.setText(displayText));
        }
        
        handler.postDelayed(() -> {
            isTerminalAnimating = false;
        }, 2000);
    }
    
    private void showFancyDisplay(String message) {
        handler.post(() -> {
            terminalDisplay.setText(message);
            terminalDisplay.setTextSize(24);
            // FIXED: Use ContextCompat.getColor() instead of getColor()
            terminalDisplay.setTextColor(ContextCompat.getColor(this, R.color.lime_primary));
            
            handler.postDelayed(() -> {
                terminalDisplay.setTextSize(14);
                startTerminalAnimation();
            }, 3000);
        });
    }
    
    private void appendLog(String message) {
    handler.post(() -> {
        // FIXED: Use SimpleDateFormat instead of java.time for API 21+ compatibility
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        logDisplay.append(logEntry);
        
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isTerminalAnimating = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        appendLog("Main menu opened");
        return true;
    }
}
