package io.ianwzhang1.driveclip;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class Clip {

    private final Type flavor;
    private final Object data;

    public Clip(DataFlavor flavor, Object data) {
        this.flavor = Type.getEnumFlavor(flavor); // Get enum representation of flavor
        this.data = data;
    }

    @Nonnull
    public Type getFlavor() {
        return flavor;
    }

    public Object getData() {
        return data;
    }

    public byte[] encrypt(String key) {
        return this.flavor.encrypt(this.data, key);
    }

    public String createJsonDescription(@Nullable String extension) {
        JsonObject json = new JsonObject();
        json.addProperty("flavor", flavor.name());
        json.addProperty("extension", extension);

        return json.toString();
    }

    public enum Type {
        // List of supported flavors
        STRING(DataFlavor.stringFlavor, (data, key) -> Utils.encrypt((String) data, key)),
        IMAGE(DataFlavor.imageFlavor, (data, key) -> {
            BufferedImage bufferedImage = Utils.iconToBuf(new ImageIcon((Image) data));
            String extension;
            try {extension = Utils.getImageExtension();} catch (Exception e) {e.printStackTrace(); return null;} // Extract img extension from html
            byte[] bytes;
            System.out.printf("Converting image with extension %s%n", extension);
            try {bytes = Utils.imageToBytes(bufferedImage, extension);} catch (Exception e) {e.printStackTrace(); return null;} // Convert img to bytes
            return Utils.encrypt(bytes, key);
        }),
        FILELIST(DataFlavor.javaFileListFlavor, (data, key) -> {
            java.util.List<File> fileList = (List<File>) data;
            java.io.File file = fileList.get(0); // Only process first file
            byte[] bytes;
            try {bytes = Files.readAllBytes(file.toPath());} catch (Exception e) {e.printStackTrace(); return null;} // File to bytes
            return Utils.encrypt(bytes, key);
        });

        private final DataFlavor flavor;
        private final BiFunction<Object, String, byte[]> encryptor;
        private static final Map<DataFlavor, Type> enumLookup = new HashMap<>();

        Type(DataFlavor flavor, BiFunction<Object, String, byte[]> encryptor) {
            this.flavor = flavor;
            this.encryptor = encryptor;
        }

        public DataFlavor getFlavor() {
            return this.flavor;
        }

        public byte[] encrypt(Object data, String key) {
            return encryptor.apply(data, key);
        }

        public static Type getEnumFlavor(DataFlavor flavor) {
            return enumLookup.get(flavor);
        }

        public static Set<DataFlavor> getSupportedFlavors() {
            return enumLookup.keySet();
        }

        static {
            // Populate map for reverse lookup
            for (Type flavorEnum : Type.values()) {
                enumLookup.put(flavorEnum.getFlavor(), flavorEnum);
            }
        }
    }

}
