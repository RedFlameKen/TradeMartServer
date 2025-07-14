package com.trademart.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.trademart.async.SharedResource;
import com.trademart.db.DatabaseController;
import com.trademart.db.IDGenerator;
import com.trademart.encryption.Hasher;
import com.trademart.util.Encoder;
import com.trademart.util.FileUtil;
import com.trademart.util.Logger;
import com.trademart.util.OSDetect;
import com.trademart.util.Logger.LogLevel;
import com.trademart.util.OSDetect.OS;
import com.trademart.util.TimeUtil;

@Service
public class MediaController {

    public static final String MEDIA_CONTROLLER_CONFIG_FILE = ".media_conf.json";
    public static final String IMAGES_DIR = "/images";
    public static final String VIDEOS_DIR = "/videos";
    public static final String THUMBNAILS_DIR = "/images/thumbnails";
    public static final String HLS_PARTS_DIR = "/videos/hls";
    public static final String IMAGES_DIR_WIN = "\\images";
    public static final String VIDEOS_DIR_WIN = "\\videos";
    public static final String THUMBNAILS_DIR_WIN = "\\images\\thumbnails";
    public static final String HLS_PARTS_DIR_WIN = "\\videos\\hls";
    public static final String[] IMAGE_TYPES = {
            "jpg",
            "jpeg",
            "png",
    };
    public static final String[] VIDEO_TYPES = {
            "mp4",
            "m3u8",
            "ts"
    };
    public static final String[] HLS_TYPES = {
            "m3u8",
            "ts"
    };

    private String mediaStoragePath;
    private SharedResource sharedResource;

    public MediaController(SharedResource sharedResource) {
        this.sharedResource = sharedResource;
        int status = initController();
        assert status != -1;
    }

    public int initController() {
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

    // TODO: Figure out how generation of HLS files work
    public File writeFile(String filename, byte[] data) throws IOException {
        String hashedFilename = getHashedFilename(filename);

        String path = getMediaPath(hashedFilename);
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                if(isVideoType(FileUtil.getExtension(filename)))
                    generateHLSFiles(file, FileUtil.removeExtension(hashedFilename));
            } catch (IOException e) {
                Logger.log("Unable to create the file " + path, LogLevel.WARNING);
                throw e;
            }
        }
        try (FileOutputStream writer = new FileOutputStream(file)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Unable to write data to the file " + path, LogLevel.WARNING);
            file.delete();
            throw e;
        }
        return file;
    }

    public File writeFileNoEncode(String filename, byte[] data) throws IOException {
        File file = new File(filename);
        try (FileOutputStream writer = new FileOutputStream(file)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private String generateThumbnailPath(String filename){
        String outputFileName = new StringBuilder()
            .append(thumbnailsDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .append(".jpg")
            .toString();
        return outputFileName;
    }

    private String generateHLSPath(String filename){
        String outputFileName = new StringBuilder()
            .append(hlsDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .append(".m3u8")
            .toString();
        return outputFileName;
    }

    private void generateHLSFiles(File file, String basename){
        String inputFilePath = file.getAbsolutePath();
        String hlsOutput = generateHLSPath(basename);
        String thumbnailOutput = generateThumbnailPath(basename);
        String message = new StringBuilder()
            .append("basename: ").append(basename)
            .append("inputFilePath: ").append(inputFilePath)
            .append("hlsOutput: ").append(hlsOutput)
            .toString();
        Logger.log(message, LogLevel.INFO);
        FFmpegUtil.generateHLS(inputFilePath, hlsOutput);
        FFmpegUtil.generateThumbnail(inputFilePath, thumbnailOutput);
    }

    public byte[] readFileBytes(File file){
        byte[] bytes = null;

        try (FileInputStream outstream = new FileInputStream(file)) {
            bytes = outstream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public File getThumbnailFile(String filename){
        filename = FileUtil.removeExtension(filename).concat(".jpg");
        String path = new StringBuilder()
            .append(thumbnailsDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .toString();
        File file = new File(path);
        if(!file.exists()){
            return null;
        }
        return file;
    }

    public File getImageFile(String filename){
        String path = new StringBuilder()
            .append(imagesDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .toString();
        return new File(path);
    }

    public File getVideoHLSFile(String filename){
        String path = new StringBuilder()
            .append(hlsDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .toString();
        return new File(path);
    }

    public File getVideoFile(String filename){
        String path = new StringBuilder()
            .append(videosDir())
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .toString();
        return new File(path);
    }

    public File getFile(String filename){
        return new File(getMediaPath(filename));
    }

    public String getMediaPath(String filename){
        String path = new StringBuilder()
            .append(mediaStoragePath)
            .append(getAppropriateMediaDir(filename))
            .append(OSDetect.getOS() == OS.WINDOWS ? '\\' : '/')
            .append(filename)
            .toString();
        return path;
    }

    public String thumbnailsDir() {
        StringBuilder path = new StringBuilder()
                .append(mediaStoragePath)
                .append(THUMBNAILS_DIR);
        return path.toString();
    }

    public String imagesDir() {
        StringBuilder path = new StringBuilder()
                .append(mediaStoragePath)
                .append(IMAGES_DIR);
        return path.toString();
    }

    public String videosDir() {
        StringBuilder path = new StringBuilder()
                .append(mediaStoragePath)
                .append(VIDEOS_DIR);
        return path.toString();
    }

    public String hlsDir() {
        StringBuilder path = new StringBuilder()
                .append(mediaStoragePath)
                .append(HLS_PARTS_DIR);
        return path.toString();
    }

    private void initDirectories() {
        initDirectory(mediaStoragePath);
        initDirectory(thumbnailsDir());
        initDirectory(imagesDir());
        initDirectory(videosDir());
        initDirectory(hlsDir());
    }

    private void initDirectory(String path) {
        File directory = new File(path.toString());
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public String getHashedFilename(String filename) {
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

    private String getAppropriateMediaDir(String filename) {
        String extension = FileUtil.getExtension(filename);
        if (isImageType(extension)) {
            return IMAGES_DIR;
        }
        if (isVideoType(extension)) {
            return VIDEOS_DIR;
        }
        if (isHLSType(extension)) {
            return HLS_PARTS_DIR;
        }
        return null;
    }

    public boolean isImageType(String extension) {
        for (String ft : IMAGE_TYPES) {
            if (extension.equalsIgnoreCase(ft))
                return true;
        }
        return false;
    }

    public boolean isVideoType(String extension) {
        for (String ft : VIDEO_TYPES) {
            if (extension.equalsIgnoreCase(ft))
                return true;
        }
        return false;
    }

    public boolean isHLSType(String extension) {
        for (String ft : HLS_TYPES) {
            if (extension.equalsIgnoreCase(ft))
                return true;
        }
        return false;
    }

    public int insertPostMediaToDB(String filepath, int userId, int postId) throws SQLException {
        int mediaId = generateMediaID();
        insertMediaToDB(filepath, mediaId, userId);
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "insert into post_media(media_id, post_id) values (?, ?)";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, mediaId);
        prep.setInt(2, postId);
        prep.execute();
        prep.close();

        sharedResource.unlock();
        return mediaId;
    }

    public int insertServiceMediaToDB(String filepath, int userId, int serviceId) throws SQLException {
        int mediaId = generateMediaID();
        insertMediaToDB(filepath, mediaId, userId);
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "insert into service_media(media_id, service_id) values (?, ?)";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, mediaId);
        prep.setInt(2, serviceId);
        prep.execute();
        prep.close();

        sharedResource.unlock();
        return mediaId;
    }

    public void insertMediaToDB(String filepath, int mediaId, int userId) throws SQLException {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String ext = FileUtil.getExtension(filepath);

        DatabaseController db = sharedResource.getDatabaseController();
        String command = "insert into media(media_id, media_type, media_url, date_uploaded, user_id) values (?, ?, ?, ?, ?)";
        PreparedStatement prep = db.prepareStatement(command);
        prep.setInt(1, mediaId);
        prep.setString(2, getMediaType(ext));
        if(ext.equals("m3u8")){
            filepath = filepath.replaceFirst(videosDir(), hlsDir());
        }
        prep.setString(3, filepath);
        prep.setTimestamp(4, Timestamp.valueOf(TimeUtil.curDateTime()));
        prep.setInt(5, userId);
        prep.execute();
        prep.close();

        sharedResource.unlock();
    }

    public int generateMediaID() {
        try {
            sharedResource.lock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int id = IDGenerator.generateDBID(sharedResource.getDatabaseController(), "media", "media_id");
        sharedResource.unlock();
        return id;
    }


    public static String getMediaType(String extension){
        for (String string : IMAGE_TYPES) {
            if(string.equalsIgnoreCase(extension)){
                return "image";
            }
        }
        for (String string : VIDEO_TYPES) {
            if(string.equalsIgnoreCase(extension)){
                return "video";
            }
        }
        for (String string : HLS_TYPES) {
            if(string.equalsIgnoreCase(extension)){
                return "video";
            }
        }
        return "undefined";
    }

    public MediaType getMediaTypeEnum(String filename){
        switch (FileUtil.getExtension(filename)) {
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
            case "jepg":
                return MediaType.IMAGE_JPEG;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

}
