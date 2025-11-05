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

    /**
     * Attempts to decode the encrypted file identified by currentPath and save it into parentFolder with fileName.
     * Returns true on success, false on failure.
     */
    public boolean decodeAndSave(String fileName, String currentPath, String passwordId, String fileExt, String whereSave) {
        int key = -1;

        if (!currentPath.endsWith(".bin")) {
            if (currentPath.contains(".")) {
                currentPath = currentPath.substring(0, currentPath.lastIndexOf(".")) + ".bin";
            } else {
                currentPath = currentPath + ".bin";
            }
        }

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (key < 0) return false;

        try {
            decodeAndSaveImpl(currentPath, key, fileName, whereSave);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void decodeAndSaveImpl(String currentPath, int key, String fileName, String parentFolder) throws IOException {
        File saveFile = new File(parentFolder, fileName);
        for (int i = 0; saveFile.exists(); i++) {
            saveFile = new File(parentFolder, "(" + i + ") " + fileName);
        }

        try (FileInputStream fis = new FileInputStream(new File(currentPath));
             FileOutputStream fos = new FileOutputStream(saveFile)) {

            byte[] bytes = new byte[128];
            fis.read(bytes);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ key);
            }
            fos.write(bytes);

            while (fis.available() > 262144) {
                byte[] b256kB = new byte[262144];
                fis.read(b256kB);
                fos.write(b256kB);
            }

            byte[] bRemaining = new byte[fis.available()];
            fis.read(bRemaining);
            fos.write(bRemaining);
        }
    }

    class KVP {
        String key;
        int value;

        public KVP(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
