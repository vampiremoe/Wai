package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Encoder {
    
    public boolean encodeAndSave(String inputPath, String outputFileName, int key) {
        try {
            return encodeAndSaveImpl(inputPath, outputFileName, key);
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean encodeAndSaveImpl(String inputPath, String outputFileName, int key) throws IOException {
        File inputFile = new File(inputPath);
        File outputDir = new File(inputFile.getParent(), "encrypted");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        File outputFile = new File(outputDir, outputFileName + ".enc");
        
        for (int i = 0; outputFile.exists(); i++) {
            outputFile = new File(outputDir, "(" + i + ") " + outputFileName + ".enc");
        }
        
        FileInputStream inputStream = new FileInputStream(inputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                buffer[i] = (byte) (buffer[i] ^ key);
            }
            outputStream.write(buffer, 0, bytesRead);
        }
        
        inputStream.close();
        outputStream.close();
        
        return true;
    }
    
    public boolean encodeAndSave(String inputPath, int key, String outputFolder) {
        try {
            File inputFile = new File(inputPath);
            String outputFileName = inputFile.getName() + ".enc";
            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, outputFileName);
            
            for (int i = 0; outputFile.exists(); i++) {
                outputFile = new File(outputDir, "(" + i + ") " + outputFileName);
            }
            
            return encodeAndSaveImpl(inputPath, outputFile.getAbsolutePath(), key);
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean encodeFileWithKey(String inputFilePath, int key, String outputDirectory) {
        try {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                return false;
            }
            
            String originalName = inputFile.getName();
            String encryptedName = originalName + ".encrypted";
            
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, encryptedName);
            
            int counter = 1;
            while (outputFile.exists()) {
                encryptedName = "(" + counter + ") " + originalName + ".encrypted";
                outputFile = new File(outputDir, encryptedName);
                counter++;
            }
            
            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    buffer[i] = (byte) (buffer[i] ^ key);
                }
                fos.write(buffer, 0, bytesRead);
            }
            
            fis.close();
            fos.close();
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}