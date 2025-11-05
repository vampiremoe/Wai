package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Decoder {
    ArrayList<KVP> kvps;
    private String lastPasswordId;
    private int lassPassword;

    public Decoder() {
        kvps = new ArrayList<>();
        lassPassword = 0;
        lastPasswordId = "";
    }

    /**
     * Attempts to decode the encrypted file identified by currentPath and save it into parentFolder with fileName.
     * Returns the absolute saved path on success, or null on failure.
     */
    public String decodeAndSave(String fileName, String currentPath, String passwordId, String fileExt, String whereSave) {
        int key = -1;

        if (!currentPath.endsWith(".bin")) {
            if (currentPath.contains(".")) {
                currentPath = currentPath.substring(0, currentPath.lastIndexOf(".")) + ".bin";
            } else {
                currentPath = currentPath + ".bin";
            }
        }

        if (passwordId.equalsIgnoreCase(lastPasswordId)) {
            key = lassPassword;
        } else {
            for (KVP kvp : kvps) {
                if (kvp.key.equalsIgnoreCase(passwordId)) {
                    key = kvp.value;
                    lassPassword = key;
                    lastPasswordId = passwordId;
                    break;
                }
            }
        }

        if (key < 0) {
            byte[] bytes = new byte[12];
            try (FileInputStream fileInputStream = new FileInputStream(new File(currentPath))) {
                fileInputStream.read(bytes);
                String[] extKey = KeyFinder.find(bytes, fileExt);
                if (extKey[0].endsWith(fileExt)) {
                    key = Integer.parseInt(extKey[1]);
                    lassPassword = key;
                    lastPasswordId = passwordId;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        if (key < 0) return null;

        try {
            return decodeAndSaveImpl(currentPath, key, fileName, whereSave);
        } catch (IOException e) {
            return null;
        }
    }

    private String decodeAndSaveImpl(String currentPath, int key, String fileName, String parentFolder) throws IOException {
        File saveFile = new File(parentFolder, fileName);
        for (int i = 0; saveFile.exists(); i++) {
            saveFile = new File(parentFolder, "(" + i + ") " + fileName);
        }

        try (FileInputStream fileInputStream = new FileInputStream(new File(currentPath));
             FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {

            byte[] bytes = new byte[128];
            fileInputStream.read(bytes);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ key);
            }
            fileOutputStream.write(bytes);

            while (fileInputStream.available() > 262144) {
                byte[] b256kB = new byte[262144];
                fileInputStream.read(b256kB);
                fileOutputStream.write(b256kB);
            }

            byte[] bXkB = new byte[fileInputStream.available()];
            fileInputStream.read(bXkB);
            fileOutputStream.write(bXkB);
        }

        return saveFile.getAbsolutePath();
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
