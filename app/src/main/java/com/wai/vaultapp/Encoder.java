package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Encoder {

    /**
     * Encrypts the input file using a simple XOR key and saves it in the target folder.
     * Returns the absolute path of the saved encrypted file, or null if failed.
     */
    public String encodeAndSave(String fileName, String inputPath, String targetFolder) {
        int key = 0xA5; // simple XOR key, can be replaced with dynamic logic

        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) return null;

            // Make sure output directory exists
            File outDir = new File(targetFolder);
            if (!outDir.exists()) outDir.mkdirs();

            // Handle duplicate filenames
            File outFile = new File(outDir, fileName + ".bin");
            for (int i = 0; outFile.exists(); i++) {
                outFile = new File(outDir, "(" + i + ") " + fileName + ".bin");
            }

            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outFile);

            byte[] buffer = new byte[128];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                for (int i = 0; i < len; i++) buffer[i] ^= key;
                fos.write(buffer, 0, len);
            }

            fis.close();
            fos.close();

            return outFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
