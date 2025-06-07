package com.trademart.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import com.trademart.encryption.Hasher;
import com.trademart.util.Encoder;
import com.trademart.util.FileUtil;
import com.trademart.util.Logger;
import com.trademart.util.Logger.LogLevel;

@Service
public class MediaController {

    public static final String MEDIA_CONTROLLER_CONFIG_FILE = ".media_conf.json";
    public static final String IMAGES_DIR = "/images";
    public static final String VIDEOS_DIR = "/videos";
    public static final String[] IMAGE_TYPES = {
        "jpg",
        "jpeg",
        "png",
    };
    public static final String[] VIDEO_TYPES = {
        "mp4"
    };

    private String mediaStoragePath;

    private static MediaController controller;

    public MediaController(){
        int status = initController();
        assert status != -1;
    }

    public static MediaController getController(){
        if(controller == null){
            controller = new MediaController();
        }
        return controller;
    }

    public int initController(){
        try {
            readConfig();
        } catch (FileNotFoundException e) {
            Logger.log("Could not find media config file!", LogLevel.CRITICAL);
            return -1;
        } catch (JSONException e) {
            Logger.log("The media config file was badly formatted", LogLevel.CRITICAL);
            return -1;
        }

        initDirectories();
        return 0;
    }
    
    // TODO: Structure media directory and Write methods for retreiving media

    public void writeFile(String filename, byte[] data){
        String hashedFilename = getHashedFilename(filename);
        String path = new StringBuilder()
            .append(mediaStoragePath)
            .append(getAppropriateMediaDir(filename))
            .append('/')
            .append(hashedFilename)
            .toString();
        File file = new File(path);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                Logger.log("Unable to create the file " + path, LogLevel.WARNING);
            }
        }
        try (FileOutputStream writer = new FileOutputStream(file)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Unable to write data to the file " + path, LogLevel.WARNING);
            file.delete();
        }
    }

    public String imagesDir(){
        StringBuilder path = new StringBuilder()
            .append(mediaStoragePath)
            .append(IMAGES_DIR);
        return path.toString();
    }

    public String videosDir(){
        StringBuilder path = new StringBuilder()
            .append(mediaStoragePath)
            .append(VIDEOS_DIR);
        return path.toString();
    }

    private void initDirectories(){
        initDirectory(mediaStoragePath);
        initDirectory(imagesDir());
        initDirectory(videosDir());
    }

    private void initDirectory(String path){
        File directory = new File(path.toString());
        if(!directory.exists()){
            directory.mkdirs();
        }
    }

    public String getHashedFilename(String filename){
        Hasher hasher = new Hasher();
        String hashed = hasher.hash(filename);
        String encoded = Encoder.encodeURLBase64(hashed);
        StringBuilder builder = new StringBuilder()
            .append(encoded)
            .append('.')
            .append(FileUtil.getExtension(filename));
        return builder.toString();
    }

    private void readConfig() throws FileNotFoundException, JSONException {
        FileReader reader = new FileReader(MEDIA_CONTROLLER_CONFIG_FILE);
        JSONObject configJson = new JSONObject(new JSONTokener(reader));
        mediaStoragePath = configJson.getString("storage_path");
    }
        
    private String getAppropriateMediaDir(String filename){
        String extension = FileUtil.getExtension(filename);
        if(isImageType(extension)){
            return IMAGES_DIR;
        }
        if(isVideoType(extension)){
            return VIDEOS_DIR;
        }
        return null;
    }

    public boolean isImageType(String extension){
        for (String ft : IMAGE_TYPES) {
            if(extension.equalsIgnoreCase(ft))
                return true;
        }
        return false;
    }

    public boolean isVideoType(String extension){
        for (String ft : VIDEO_TYPES) {
            if(extension.equalsIgnoreCase(ft))
                return true;
        }
        return false;
    }

}
