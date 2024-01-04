package io.ianwzhang1.driveclip;

import com.github.mervick.aes_everywhere.Aes256;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static TrayIcon trayIcon;

    public static byte[] encrypt(byte[] txt, String key) {
        try {
            return Aes256.encrypt(txt, key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] encrypt(String txt, String key) {
        try {
            return Aes256.encrypt(txt, key).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(String crypted, String key) throws Exception {
        return Aes256.decrypt(crypted, key).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] decrypt(byte[] crypted, String key) throws Exception {
        System.out.println("Decrypting " + crypted + " with " + key);
        return Aes256.decrypt(crypted, key.getBytes(StandardCharsets.UTF_8));
    }

    public static <T extends Serializable> String serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(obj);

        oos.flush();
        oos.close();

        return baos.toString();
    }

    public static Object deserialize(InputStream serialized) throws IOException, ClassNotFoundException {
        ObjectInputStream oos = new ObjectInputStream(serialized);

        Object out = oos.readObject();
        oos.close();

        return out;
    }

    public static BufferedImage iconToBuf(ImageIcon icon) {
        BufferedImage bi = new BufferedImage(
                icon.getIconWidth(),
                icon.getIconHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        return bi;
    }

    public static Clip getClipboard() throws IOException, UnsupportedFlavorException {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        // Try to clip each supported flavor until one works
        for (DataFlavor flavor : Clip.Type.getSupportedFlavors()) {
            if (c.isDataFlavorAvailable(flavor)) {
                System.out.println("\uD83C\uDF66 Clipboard flavor: " + flavor.getHumanPresentableName());
                System.out.println(c.getContents(DriveClipApplication.getInstance()).getTransferData(flavor));
                try {
                    return new Clip(flavor, c.getData(flavor));
                } catch (Exception e) {
                    System.out.println(e.getClass().getCanonicalName());
                    System.out.println(e.getMessage());
                }
            }
        }

        return null;
    }

    public static String getImageExtension() throws Exception {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        Object data = c.getData(DataFlavor.allHtmlFlavor);
        Pattern imgTypePattern = Pattern.compile("<img src=\".*\\.(.*?)[\"?]");
        Matcher matcher = imgTypePattern.matcher((String) data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new Exception(String.format("No match could be found for the image pattern: %s", data));
        }
    }

    public static void initTray() {
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(DriveClipApplication.class.getClassLoader().getResource("icon.png"));

        trayIcon = new TrayIcon(image, "DriveClip Tray");
        trayIcon.setImageAutoSize(true);

        trayIcon.addActionListener(e -> { // Doubleclicjk action
            System.out.println("Action performed");
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Failed to add tray icon");
        }
    }

    public static void windowsNotify(String caption, String text) {
        trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
    }

    public static byte[] imageToBytes(BufferedImage bufferedImage, String extension) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, extension, baos);
        baos.close();
        return baos.toByteArray();
    }

    public static BufferedImage bytesToImage(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(bais);
        bais.close();
        return image;
    }


}
