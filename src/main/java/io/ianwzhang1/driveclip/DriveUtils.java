package io.ianwzhang1.driveclip;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class DriveUtils {

    private static final String APPLICATION_NAME = "DriveClip Windows Service";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final String FOLDER_MIMETYPE = "application/vnd.google-apps.folder";
    public static final String TEXT_MIMETYPE = "text/plain";
    private static final String FOLDER_NAME = "DriveClip";

    private Drive drive;

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = DriveClipApplication.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        // Returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public void deleteCredentials() {
        new java.io.File(TOKENS_DIRECTORY_PATH).delete();
    }

    public void getDrive() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void uploadFile(Clip clip, byte[] encrypted, String extension) throws Exception {
        if (encrypted == null) {
            Utils.windowsNotify("Encryption error", null);
            return;
        }

        // Write encrypted byte array to tmp file
        Path tempFile = Files.createTempFile(null, null);
        Files.write(tempFile, encrypted);

        File prevUpload = getPreviousUpload(); // Attempt to locate old upload file, creating new folder/file if not found
        if (prevUpload == null) {
            File fileMeta = new File();
            fileMeta.setName(DriveClipApplication.CLIP_NAME);
            fileMeta.setParents(Collections.singletonList(getDCFolder()));
            fileMeta.setDescription(clip.createJsonDescription(extension));
            drive.files().create(fileMeta, new FileContent("text/plain", tempFile.toFile())).execute();
        } else {
            File updateMeta = new File();
            updateMeta.setDescription(clip.createJsonDescription(extension));
            drive.files().update(prevUpload.getId(), updateMeta, new FileContent("text/plain", tempFile.toFile())).execute();
        }

        System.out.println("Uploaded");
        Utils.windowsNotify("Uploaded file successfully", String.format("Extension: %s", extension));
    }

    /**
     * Downloads a clip from drive
     */
    public HttpResponse getMedia(String fileId) throws IOException {
        return drive.files().get(fileId).executeMedia();
    }

    /**
     * Returns the fileid of a previous upload
     */
    @Nullable
    File getPreviousUpload() throws IOException {
        List<File> fileList = this.drive.files().list()
                .setQ("mimeType='" + TEXT_MIMETYPE + "'" +
                        " and name='" + DriveClipApplication.CLIP_NAME + "'" +
                        " and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, description)")
                .execute().getFiles();

        return fileList.isEmpty() ? null : fileList.get(0);
    }

    /**
     * Creates a new folder to store paste data
     */
    private String createDataFolder() throws IOException {
        File folderMeta = new File();
        folderMeta.setName(FOLDER_NAME);
        folderMeta.setMimeType(FOLDER_MIMETYPE);
        File folder = this.drive.files().create(folderMeta).setFields("files(id)").execute();
        System.out.println("Created " + FOLDER_NAME);
        return folder.getId();
    }

    private String getDCFolder() throws IOException {
        List<File> fileList = this.drive.files().list()
                .setQ("mimeType='" + FOLDER_MIMETYPE + "'" +
                        " and name='" + FOLDER_NAME + "'" +
                        " and trashed=false")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute().getFiles();

        return fileList.isEmpty() ? createDataFolder() : fileList.get(0).getId();
    }
}
