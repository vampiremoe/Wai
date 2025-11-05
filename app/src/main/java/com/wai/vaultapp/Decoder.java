package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Decoder {

    ArrayList<KVP> kvps;
    private String lastPasswordId;
    private int lastPassword;

    public Decoder() {
        kvps = new ArrayList<>();
        lastPassword = 0;
        lastPasswordId = "";
    }

    // Decode a vault file
    public String decodeAndSave(String fileName, String currentPath, String passwordId, String fileExt, String whereSave) {
        int key = -1;

        if (passwordId.equalsIgnoreCase(lastPasswordId)) {
            key = lastPassword;
        } else {
            for (KVP kvp : kvps) {
                if (kvp.key.equalsIgnoreCase(passwordId)) {
                    key = kvp.value;
                    lastPassword = key;
                    lastPasswordId = passwordId;
                    break;
                }
            }
        }

        if (key < 0) {
            byte[] bytes = new byte[12];
            try (FileInputStream fis = new FileInputStream(new File(currentPath))) {
                fis.read(bytes);
                String[] extKey = KeyFinder.find(bytes, fileExt);
                if (extKey[0].endsWith(fileExt)) {
                    key = Integer.parseInt(extKey[1]);
                    lastPassword = key;
                    lastPasswordId = passwordId;
                }
            } catch (Exception ignored) {}
        }

        if (key < 0) return null;

        try {
            return decodeAndSaveImpl(currentPath, key, fileName, whereSave);
        } catch (IOException e) {
            return null;
        }
    }

    // Encode file (for encryption)
    public String encodeAndSave(String inputPath, String whereSave) {
        File inFile = new File(inputPath);
        int key = 123; // example XOR key
        File saveFile = new File(whereSave, inFile.getName());

        try (FileInputStream fis = new FileInputStream(inFile);
             FileOutputStream fos = new FileOutputStream(saveFile)) {

            byte[] buffer = new byte[128];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    buffer[i] = (byte) (buffer[i] ^ key);
                }
                fos.write(buffer, 0, read);
            }
            return saveFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decodeAndSaveImpl(String currentPath, int key, String fileName, String parentFolder) throws IOException {
        File saveFile = new File(parentFolder, fileName);
        for (int i = 0; saveFile.exists(); i++) {
            saveFile = new File(parentFolder, "(" + i + ") " + fileName);
        }

        try (FileInputStream fis = new FileInputStream(new File(currentPath));
             FileOutputStream fos = new FileOutputStream(saveFile)) {

            byte[] bytes = new byte[128];
            int read;
            while ((read = fis.read(bytes)) != -1) {
                for (int i = 0; i < read; i++) {
                    bytes[i] = (byte) (bytes[i] ^ key);
                }
                fos.write(bytes, 0, read);
            }
        }
        return saveFile.getAbsolutePath();
    }

    static class KVP {
        String key;
        int value;
        public KVP(String key, int value){
            this.key = key;
            this.value = value;
        }
    }
}
