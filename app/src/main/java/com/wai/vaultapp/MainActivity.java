package com.wai.vaultapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    
    // ========== SCAN VAULT FUNCTION ==========
    private void onScanVaultClicked() {
        appendLog("=== SCAN VAULT INITIATED ===");
        startTerminalAnimation();
        
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                handler.post(() -> {
                    boolean found = scanAndDecryptVault();
                    if (found) {
                        showFancyDisplay("FOUND");
                    } else {
                        showFancyDisplay("COULDN'T FIND IT");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private boolean scanAndDecryptVault() {
        appendLog("Searching for vault database...");
        
        File vaultDir = new File(Environment.getExternalStorageDirectory(), "SystemAndroid/Data");
        if (!vaultDir.exists() || vaultDir.listFiles() == null) {
            appendLog("Vault directory not found: " + vaultDir.getAbsolutePath());
            return false;
        }
        
        appendLog("Found vault directory: " + vaultDir.getAbsolutePath());
        
        String dbPath = null;
        File[] fileList = vaultDir.listFiles();
        
        // Find SQLite database file
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
                    appendLog("Found database: " + f.getName());
                    break;
                }
            } catch (Exception ex) {
                // Continue searching
            }
        }
        
        if (dbPath == null) {
            appendLog("No vault database found.");
            return false;
        }
        
        // Create waivault/decrypted folder with organized subfolders
        File whereSave = new File(Environment.getExternalStorageDirectory(), "waivault/decrypted");
        if (!whereSave.exists()) {
            whereSave.mkdirs();
        }
        
        appendLog("Output folder: " + whereSave.getAbsolutePath());
        
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT password_id,file_name_from,file_path_new,file_type FROM hideimagevideo", null);
            
            if(cursor.getCount() == 0){
                appendLog("No files found in vault database.");
                cursor.close();
                db.close();
                return true; // Found vault but empty
            }
            
            appendLog("Found " + cursor.getCount() + " items in vault. Decoding...");
            
            Decoder decoder = new Decoder();
            int successCount = 0;

            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String fileName = cursor.getString(1);
                String filePath = cursor.getString(2);
                String passwordId = cursor.getString(0);
                String fileType = cursor.getString(3);
                
                appendLog("Decoding " + (i+1) + "/" + cursor.getCount() + ": " + fileName);
                
                if(decoder.decodeAndSave(fileName, filePath, passwordId, fileType, whereSave.getAbsolutePath())){
                    successCount++;
                } else {
                    appendLog("Failed: " + fileName);
                }
            }
            
            cursor.close();
            db.close();
            
            appendLog("Vault decryption completed!");
            appendLog("Successfully decoded: " + successCount + "/" + cursor.getCount() + " files");
            appendLog("Files saved to: " + whereSave.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            appendLog("Error accessing database: " + e.getMessage());
            return false;
        }
    }
    
    // ========== DECRYPT FUNCTION ==========
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
                        handler.post(() -> appendLog("Saved to: " + outputDir));
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
                        handler.post(() -> appendLog("Saved to: " + outputDir));
                    } else {
                        handler.post(() -> appendLog("Failed to decrypt: " + fileName));
                    }
                    
                } catch (Exception e) {
                    handler.post(() -> appendLog("Error decrypting: " + uri.getLastPathSegment()));
                }
            }
        }).start();
    }
    
    // ========== ENCRYPT FUNCTION ==========
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
    
    private void askForEncryptionKey(List<Uri> uris) {
        showKeyInputDialog(uris, false);
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
                        handler.post(() -> appendLog("Saved to: " + outputDir));
                    } else {
                        handler.post(() -> appendLog("Failed to encrypt: " + fileName));
                    }
                    
                } catch (Exception e) {
                    handler.post(() -> appendLog("Error encrypting: " + uri.getLastPathSegment()));
                }
            }
        }).start();
    }
    
    // ========== HELPER METHODS ==========
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
    
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null && path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return "unknown_file";
    }
    
    private String getOutputDirectory(String type) {
        File outputDir = new File(Environment.getExternalStorageDirectory(), "waivault/" + type);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir.getAbsolutePath();
    }
    
    // ========== MENU FUNCTIONALITY ==========
    @Override
    public boolean onSupportNavigateUp() {
        showMainMenu();
        return true;
    }
    
    private void showMainMenu() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Wai Vault Menu")
            .setItems(new String[]{
                "Open Output Folder", 
                "Change Theme", 
                "Settings", 
                "Help", 
                "Clear Logs", 
                "About", 
                "Exit"
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        openOutputFolder();
                        break;
                    case 1:
                        changeTheme();
                        break;
                    case 2:
                        showSettings();
                        break;
                    case 3:
                        showHelp();
                        break;
                    case 4:
                        clearLogs();
                        break;
                    case 5:
                        showAbout();
                        break;
                    case 6:
                        finish();
                        break;
                }
            })
            .show();
    }
    
    private void openOutputFolder() {
        File outputDir = new File(Environment.getExternalStorageDirectory(), "waivault");
        appendLog("Output folder: " + outputDir.getAbsolutePath());
        appendLog("Use file manager to navigate to this path");
    }
    
    private void changeTheme() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Change Theme")
            .setItems(new String[]{
                "Dark Theme (Current)", 
                "Light Theme", 
                "Blue Theme", 
                "Purple Theme"
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        applyDarkTheme();
                        break;
                    case 1:
                        applyLightTheme();
                        break;
                    case 2:
                        applyBlueTheme();
                        break;
                    case 3:
                        applyPurpleTheme();
                        break;
                }
            })
            .show();
    }
    
    private void applyDarkTheme() {
        appendLog("Theme: Dark (Current)");
    }
    
    private void applyLightTheme() {
        appendLog("Theme: Light - Coming in next update!");
    }
    
    private void applyBlueTheme() {
        appendLog("Theme: Blue - Coming in next update!");
    }
    
    private void applyPurpleTheme() {
        appendLog("Theme: Purple - Coming in next update!");
    }
    
    private void showSettings() {
        View settingsView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setView(settingsView)
            .setPositiveButton("Save", (dialog, which) -> {
                appendLog("Settings saved");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showHelp() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Help Guide")
            .setMessage(
                "📱 WAI VAULT HELP\n\n" +
                "🔍 SCAN VAULT:\n" +
                "• Automatically finds and decrypts vault files\n" +
                "• Looks in: /SystemAndroid/Data/\n" +
                "• Saves to: /waivault/decrypted/\n\n" +
                
                "🔓 DECRYPT:\n" +
                "• Manually decrypt any file\n" +
                "• Auto-detect key or enter manually (0-255)\n" +
                "• Saves to organized folders\n\n" +
                
                "🔐 ENCRYPT:\n" +
                "• Encrypt files with XOR encryption\n" +
                "• Set your own key (0-255)\n" +
                "• Compatible with decrypt function\n\n" +
                
                "💾 OUTPUT:\n" +
                "• All files saved to /storage/emulated/0/waivault/\n" +
                "• Organized by type: images/, videos/, audio/, etc.\n\n" +
                
                "⚠️  Note: For educational purposes only"
            )
            .setPositiveButton("Got it!", null)
            .show();
    }
    
    private void clearLogs() {
        logDisplay.setText("");
        appendLog("Logs cleared");
    }
    
    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("About Wai Vault")
            .setMessage("Wai Vault Decryptor v1.0\n\nFile recovery and encryption tool\nDeveloped for educational purposes")
            .setPositiveButton("OK", null)
            .show();
    }
    
    // ========== UI ANIMATIONS ==========
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
            terminalDisplay.setTextColor(ContextCompat.getColor(this, R.color.lime_primary));
            
            handler.postDelayed(() -> {
                terminalDisplay.setTextSize(14);
                startTerminalAnimation();
            }, 3000);
        });
    }
    
    private void appendLog(String message) {
        handler.post(() -> {
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
    
    public static String getDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return sdf.format(new Date());
    }
}
