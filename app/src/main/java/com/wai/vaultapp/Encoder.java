package com.wai.vaultapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Encoder {

    public boolean encodeFile(File inputFile, String baseOutputFolder, int key){
        try {
            String ext = getExtension(inputFile.getName());
            File saveFolder = new File(baseOutputFolder, getFolderByType(ext));
            if(!saveFolder.exists()) saveFolder.mkdirs();

            File outputFile = new File(saveFolder,inputFile.getName());
            for(int i=0; outputFile.exists(); i++){
                outputFile = new File(saveFolder,"("+i+") "+inputFile.getName());
            }

            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int read;
            while((read=fis.read(buffer))!=-1){
                for(int i=0;i<read;i++){
                    buffer[i] = (byte)(buffer[i]^key);
                }
                fos.write(buffer,0,read);
            }
            fis.close();
            fos.close();
            return true;
        } catch(Exception e){ e.printStackTrace(); return false; }
    }

    private String getFolderByType(String ext){
        ext = ext.toLowerCase();
        if(ext.contains("jpg")||ext.contains("png")||ext.contains("gif")||ext.contains("svg")||ext.contains("webp")) return "images";
        if(ext.contains("mp4")||ext.contains("3gp")||ext.contains("mkv")||ext.contains("mov")||ext.contains("avi")||ext.contains("wmv")||ext.contains("flv")||ext.contains("vob")||ext.contains("webm")||ext.contains("m4v")) return "videos";
        return "others";
    }

    private String getExtension(String name){
        int idx = name.lastIndexOf('.');
        if(idx==-1) return "";
        return name.substring(idx+1);
    }
}