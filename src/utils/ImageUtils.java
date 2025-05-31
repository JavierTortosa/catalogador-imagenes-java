package utils;

import java.nio.file.Path;

public class ImageUtils {

	public static String getImageFormat(Path path) {
        if (path == null) return "N/A";
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1); // No es necesario toUpperCase aquí si lo haces en InfoBarManager
        }
        return "Desconocido";
    }
}