package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Encoder {

    /**
     * Encrypts a file using simple XOR mirroring like Decoder.
     * @param inputFile File to encrypt
     * @param outputBaseFolder Base folder for encrypted files
     * @param key XOR key
     * @return true if successful
     */
    public boolean encodeFile(File inputFile, String outputBaseFolder, int key) {
        if(!inputFile.exists()) return false;

        String ext = getFileExtension(inputFile.getName());
        String typeFolder = "others";
        if(ext.equalsIgnoreCase(".jpg") || ext.equalsIgnoreCase(".png") || ext.equalsIgnoreCase(".gif") || ext.equalsIgnoreCase(".svg") || ext.equalsIgnoreCase(".webp")) {
            typeFolder = "images";
        } else if(ext.equalsIgnoreCase(".mp4") || ext.equalsIgnoreCase(".mkv") || ext.equalsIgnoreCase(".mov") || ext.equalsIgnoreCase(".3gp") || ext.equalsIgnoreCase(".wmv") || ext.equalsIgnoreCase(".avi") || ext.equalsIgnoreCase(".flv") || ext.equalsIgnoreCase(".m4v") || ext.equalsIgnoreCase(".vob")) {
            typeFolder = "videos";
        }

        File saveDir = new File(outputBaseFolder + "/encrypt", typeFolder);
        if(!saveDir.exists()) saveDir.mkdirs();

        File saveFile = new File(saveDir, inputFile.getName() + ".bin");
        for(int i=0; saveFile.exists(); i++){
            saveFile = new File(saveDir, "(" + i + ") " + inputFile.getName() + ".bin");
        }

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(saveFile)) {

            byte[] buffer = new byte[128];
            int readLen = fis.read(buffer);
            for(int i=0; i<readLen; i++){
                buffer[i] = (byte)(buffer[i]^key);
            }
            fos.write(buffer,0,readLen);

            // Write remaining
            buffer = new byte[262144];
            int len;
            while((len = fis.read(buffer)) > 0){
                fos.write(buffer,0,len);
            }

        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getFileExtension(String name){
        int lastDot = name.lastIndexOf(".");
        if(lastDot>0) return name.substring(lastDot);
        return "";
    }
}