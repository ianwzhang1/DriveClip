package io.ianwzhang1.driveclip;

import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.model.File;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class CAController {

    @FXML
    private PasswordField keyField;

    private final DriveUtils drive;
    private final PropertiesConfiguration config;
    private String key;

    public CAController() throws IOException, ConfigurationException, GeneralSecurityException {
        this.drive = new DriveUtils();
        this.drive.getDrive(); // Request user credentials

        java.io.File cfgFile = new java.io.File(Paths.get("config").toAbsolutePath().toString());

        if (cfgFile.createNewFile()) {
            System.out.println("Created new cfg file");
        }

        this.config = new PropertiesConfiguration(cfgFile);
    }

    public void initialize() {
        // Utils.initTray(); // Tray temporarily disabled
        this.key = config.getString("key");
        if (key == null) {
            this.keyField.setStyle("-fx-background-color: #d75454");
            this.keyField.setPromptText("Key Required");
        } else {
            this.keyField.setStyle("-fx-background-color: #e5b55f");
            this.keyField.setPromptText("<Previously Saved Key>");
        }
    }

    public void upload() throws Exception {
        Clip clip = Utils.getClipboard();

        if (clip == null) {
            Utils.toast("Failed", "No clip found in clipboard");
            return;
        }

        byte[] encrypted = clip.encrypt(key);

        switch (clip.getFlavor()) {
            case STRING:
                drive.uploadFile(clip, encrypted, null);
                break;
            case FILELIST:
                String filename = ((List<java.io.File>) clip.getData()).get(0).getName();
                int extensionIndex = filename.lastIndexOf(".");
                drive.uploadFile(clip, encrypted, extensionIndex == -1 ? "" : filename.substring(extensionIndex + 1));
                break;
            case IMAGE:
                drive.uploadFile(clip, encrypted, null);
                break;
        }
    }


    public void download() throws Exception {
        String key = config.getString("key");

        File file = drive.getPreviousUpload();
        if (file == null) {
            return;
        }

        System.out.println(file.getDescription());
        JsonObject clipData = (JsonObject) JsonParser.parseString(file.getDescription());
        String extension = clipData.get("extension").getAsString();
        Clip.Type flavor = Clip.Type.valueOf(clipData.get("flavor").getAsString());

        String fileId = file.getId();
        HttpResponse resp = drive.getMedia(fileId);
        byte[] crypted = ByteStreams.toByteArray(resp.getContent());
        byte[] data;
        try {
            data = Utils.decrypt(crypted, key);
        } catch (Exception e) {
            Utils.toast("ERROR", "Bad Key");
            return;
        }

        switch (flavor) {
            case FILELIST:
                java.io.File downloadedFile = new java.io.File("output/output." + extension);
                Files.write(downloadedFile.toPath(), data); // Copy file data over
                FileTransferable fileTransfer = new FileTransferable(downloadedFile);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(fileTransfer, fileTransfer);
                Utils.toast("Saved file to output." + extension, null);
                break;
            case STRING:
                StringSelection selection = new StringSelection(new String(data));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                Utils.toast("Set clipboard contents", null);
                break;
            case IMAGE:
                ImageTransferable imgTransfer = new ImageTransferable(Utils.bytesToImage(data));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgTransfer, imgTransfer);
                Utils.toast("Downloaded image", null);
                break;
        }
    }

    public void login() {
        try {
            drive.getDrive();
        } catch (Exception e) {
            Utils.toast("Failed to login", e.getMessage());
        }
    }

    public void logout() {
        drive.deleteCredentials();
    }

    public void setKey() throws ConfigurationException {
        String key = keyField.getText();
        config.setProperty("key", key);
        config.save();
        keyField.clear();
        keyField.setPromptText("<Saved>");
        keyField.setStyle("-fx-background-color: #a7cba7");
    }

    public void passwordChanged() {
        keyField.setStyle("-fx-background-color: #d75454");
    }

    public void test() {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = c.getContents(null);
        System.out.println(t);
        System.out.println(t.getClass());
        System.out.println(Arrays.toString(t.getTransferDataFlavors()));
    }

}
