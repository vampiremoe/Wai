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
     * Returns absolute saved path on success, null on failure
     */
    public String decodeAndSave(String fileName, String currentPath, String passwordId, String fileExt, String whereSave) {
        int key = -1;

        // Ensure bin extension
        if (!currentPath.endsWith(".bin")) {
            if (currentPath.contains(".")) currentPath = currentPath.substring(0, currentPath.lastIndexOf(".")) + ".bin";
            else currentPath = currentPath + ".bin";
        }

        if (passwordId.equalsIgnoreCase(lastPasswordId)) {
            key = lastPassword;
        }

        if (key < 0) {
            // Dummy key logic (replace with real KeyFinder logic)
            key = 0xAA;
            lastPassword = key;
            lastPasswordId = passwordId;
        }

        try {
            File saveFile = new File(whereSave, fileName);
            int i = 0;
            while (saveFile.exists()) {
                saveFile = new File(whereSave, "(" + i + ") " + fileName);
                i++;
            }

            FileInputStream fis = new FileInputStream(new File(currentPath));
            FileOutputStream fos = new FileOutputStream(saveFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                for (int j = 0; j < read; j++) buffer[j] = (byte) (buffer[j] ^ key);
                fos.write(buffer, 0, read);
            }
            fis.close();
            fos.close();
            return saveFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    static class KVP {
        String key;
        int value;

        KVP(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
