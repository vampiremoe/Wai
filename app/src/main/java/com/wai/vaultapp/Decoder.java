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
    public Decoder(){
        kvps = new ArrayList<>();
        lassPassword = 0;
        lastPasswordId = "";
    }

    public boolean decodeAndSave(String fileName, String currentPath, String passwordId,String fileExt, String whereSave){
        int key  = -1;
        if(!currentPath.endsWith(".bin")){
            currentPath = currentPath.substring(0,currentPath.lastIndexOf("."))+".bin";
        }
        if(passwordId.equalsIgnoreCase(lastPasswordId)){
            key = lassPassword;
        } else {
            for(int i=0;i<kvps.size();i++){
                KVP kvp = (KVP) kvps.get(i);
                if(kvp.key.equalsIgnoreCase(passwordId)){
                    key = kvp.value;
                    lassPassword = key;
                    lastPasswordId = passwordId;
                    break;
                }
            }
        }
        if(key<0){
            byte[] bytes = new byte[12];
            try {
                FileInputStream fileInputStream = new FileInputStream(new File(currentPath));
                fileInputStream.read(bytes);
                fileInputStream.close();
                String[] extKey = KeyFinder.find(bytes,fileExt);
                if(extKey[0].endsWith(fileExt)){
                    key = Integer.parseInt(extKey[1]);
                    lassPassword = key;
                    lastPasswordId = passwordId;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if(key<0)
            return false;
        try {
            // Create organized folder structure
            String subFolder = getFileTypeFolder(fileExt);
            File saveDir = new File(whereSave, subFolder);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            
            return decodeAndSaveImpl(currentPath, key, fileName, saveDir.getAbsolutePath());
        } catch (IOException e) {
            return false;
        }
    }

    private boolean decodeAndSaveImpl(String currentPath, int key, String fileName, String savePath) throws IOException {
        File saveFile = new File(savePath, fileName);
        for(int i=0;saveFile.exists();i++){
            saveFile = new File(savePath,"("+i+") "+fileName);
        }
        FileInputStream fileInputStream = new FileInputStream(new File(currentPath));
        byte[] bytes = new byte[128];
        fileInputStream.read(bytes);
        for(int i=0;i<bytes.length;i++){
            bytes[i] = (byte)(bytes[i]^key);
        }
        FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
        fileOutputStream.write(bytes);
        while (fileInputStream.available()>262144){
            byte[] b256kB = new byte[262144];
            fileInputStream.read(b256kB);
            fileOutputStream.write(b256kB);
        }
        byte[] bXkB = new byte[fileInputStream.available()];
        fileInputStream.read(bXkB);
        fileOutputStream.write(bXkB);
        fileInputStream.close();
        fileOutputStream.close();

        return true;
    }

    private String getFileTypeFolder(String fileExt) {
        if (fileExt == null) return "other";
        
        String lowerExt = fileExt.toLowerCase();
        if (lowerExt.equals("jpg") || lowerExt.equals("jpeg") || 
            lowerExt.equals("png") || lowerExt.equals("gif") || 
            lowerExt.equals("bmp") || lowerExt.equals("webp")) {
            return "images";
        } else if (lowerExt.equals("mp4") || lowerExt.equals("avi") || 
                   lowerExt.equals("mkv") || lowerExt.equals("mov") || 
                   lowerExt.equals("3gp") || lowerExt.equals("wmv") ||
                   lowerExt.equals("flv") || lowerExt.equals("m4v") ||
                   lowerExt.equals("mpg") || lowerExt.equals("vob")) {
            return "videos";
        } else if (lowerExt.equals("mp3") || lowerExt.equals("wav") || 
                   lowerExt.equals("aac") || lowerExt.equals("flac")) {
            return "audio";
        } else if (lowerExt.equals("pdf") || lowerExt.equals("doc") || 
                   lowerExt.equals("docx") || lowerExt.equals("txt")) {
            return "documents";
        } else {
            return "other";
        }
    }

    // New method for manual file decryption
    public boolean decodeFileWithKey(String inputFilePath, int key, String outputDirectory) {
        try {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                return false;
            }
            
            // Detect file type first
            byte[] header = new byte[12];
            FileInputStream fis = new FileInputStream(inputFile);
            fis.read(header);
            fis.close();
            
            String[] detectionResult = KeyFinder.find(header);
            String fileExtension = detectionResult[0];
            
            if (fileExtension.equals("NFND")) {
                // If cannot detect, use original extension or .bin
                String originalName = inputFile.getName();
                if (originalName.contains(".")) {
                    fileExtension = originalName.substring(originalName.lastIndexOf("."));
                } else {
                    fileExtension = ".decrypted";
                }
            }
            
            // Create organized output directory
            String subFolder = getFileTypeFolder(fileExtension.replace(".", ""));
            File outputDir = new File(outputDirectory, subFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            String originalName = inputFile.getName();
            String baseName = originalName;
            if (originalName.contains(".")) {
                baseName = originalName.substring(0, originalName.lastIndexOf("."));
            }
            
            String outputFileName = baseName + fileExtension;
            File outputFile = new File(outputDir, outputFileName);
            
            // Ensure unique filename
            int counter = 1;
            while (outputFile.exists()) {
                outputFileName = "(" + counter + ") " + baseName + fileExtension;
                outputFile = new File(outputDir, outputFileName);
                counter++;
            }
            
            return decodeFileWithKeyImpl(inputFilePath, key, outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean decodeFileWithKeyImpl(String inputPath, int key, String outputPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(inputPath));
        FileOutputStream outputStream = new FileOutputStream(new File(outputPath));
        
        // Decrypt and write file
        byte[] buffer = new byte[8192];
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
    
    // Method for auto-detecting key and decrypting
    public String[] decodeFileAutoDetect(String inputFilePath, String outputDirectory) {
        try {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                return new String[]{"ERROR", "File not found"};
            }
            
            // Read file header for detection
            byte[] header = new byte[12];
            FileInputStream fis = new FileInputStream(inputFile);
            fis.read(header);
            fis.close();
            
            // Auto-detect key and file type
            String[] detectionResult = KeyFinder.find(header);
            String fileExtension = detectionResult[0];
            int key = Integer.parseInt(detectionResult[1]);
            
            if (fileExtension.equals("NFND")) {
                return new String[]{"ERROR", "Cannot detect file type or key"};
            }
            
            // Decrypt with detected key
            boolean success = decodeFileWithKey(inputFilePath, key, outputDirectory);
            
            if (success) {
                return new String[]{"SUCCESS", "Key: " + key + ", Type: " + fileExtension};
            } else {
                return new String[]{"ERROR", "Decryption failed"};
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"ERROR", "Processing error: " + e.getMessage()};
        }
    }

    class KVP{
        String key;
        int value;
        public KVP(String key, int value){
            this.key = key;
            this.value = value;
        }
    }
}