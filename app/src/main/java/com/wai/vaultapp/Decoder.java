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

    public Decoder(){
        kvps = new ArrayList<>();
        lastPassword = 0;
        lastPasswordId = "";
    }

    public boolean decodeAndSave(String fileName, String currentPath, String passwordId, String fileExt, String whereSaveBase){
        int key  = -1;
        if(!currentPath.endsWith(".bin")){
            currentPath = currentPath.substring(0,currentPath.lastIndexOf("."))+".bin";
        }

        if(passwordId.equalsIgnoreCase(lastPasswordId)){
            key = lastPassword;
        } else {
            for(KVP kvp : kvps){
                if(kvp.key.equalsIgnoreCase(passwordId)){
                    key = kvp.value;
                    lastPassword = key;
                    lastPasswordId = passwordId;
                    break;
                }
            }
        }

        if(key < 0){
            try {
                byte[] bytes = new byte[12];
                FileInputStream fis = new FileInputStream(new File(currentPath));
                fis.read(bytes);
                fis.close();
                String[] extKey = KeyFinder.find(bytes,fileExt);
                if(extKey[0].endsWith(fileExt)){
                    key = Integer.parseInt(extKey[1]);
                    lastPassword = key;
                    lastPasswordId = passwordId;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(key < 0) return false;

        try {
            return decodeAndSaveImpl(currentPath, key, fileName, whereSaveBase, fileExt);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean decodeAndSaveImpl(String currentPath, int key, String fileName, String baseFolder, String fileExt) throws IOException {
        // Determine folder based on type
        String typeFolder = "others";
        if(fileExt.equalsIgnoreCase(".jpg") || fileExt.equalsIgnoreCase(".png") || fileExt.equalsIgnoreCase(".gif") || fileExt.equalsIgnoreCase(".svg") || fileExt.equalsIgnoreCase(".webp")) {
            typeFolder = "images";
        } else if(fileExt.equalsIgnoreCase(".mp4") || fileExt.equalsIgnoreCase(".mkv") || fileExt.equalsIgnoreCase(".mov") || fileExt.equalsIgnoreCase(".3gp") || fileExt.equalsIgnoreCase(".wmv") || fileExt.equalsIgnoreCase(".avi") || fileExt.equalsIgnoreCase(".flv") || fileExt.equalsIgnoreCase(".m4v") || fileExt.equalsIgnoreCase(".vob")) {
            typeFolder = "videos";
        }

        File saveDir = new File(baseFolder, typeFolder);
        if(!saveDir.exists()) saveDir.mkdirs();

        File saveFile = new File(saveDir, fileName);
        for(int i=0; saveFile.exists(); i++){
            saveFile = new File(saveDir, "(" + i + ") " + fileName);
        }

        FileInputStream fis = new FileInputStream(new File(currentPath));
        FileOutputStream fos = new FileOutputStream(saveFile);

        // First 128 bytes XOR
        byte[] bytes = new byte[128];
        int readLen = fis.read(bytes);
        for(int i=0; i<readLen; i++){
            bytes[i] = (byte)(bytes[i]^key);
        }
        fos.write(bytes,0,readLen);

        // Write rest in chunks
        byte[] buffer = new byte[262144];
        int len;
        while((len = fis.read(buffer)) > 0){
            fos.write(buffer,0,len);
        }

        fis.close();
        fos.close();
        return true;
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