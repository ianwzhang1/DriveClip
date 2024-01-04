package io.ianwzhang1.driveclip;

/**
 * JavaFX fails to launch when the main method is located in
 * the same class as the {@link javafx.application.Application}.
 * See <a href="https://stackoverflow.com/questions/56894627/how-to-fix-error-javafx-runtime-components-are-missing-and-are-required-to-ru">...</a>
 * Start the application from here instead.
 */
public class Launcher {

    public static void main(String[] args) {
        DriveClipApplication.run();
    }

}
