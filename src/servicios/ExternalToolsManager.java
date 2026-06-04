package servicios;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalToolsManager {
    private static final Logger logger = LoggerFactory.getLogger(ExternalToolsManager.class);

    /**
     * Devuelve la ruta absoluta al ejecutable de 7z.
     * Si el ejecutable no existe en disco, lo extrae automáticamente desde el JAR.
     */
    public static String get7zPath() {
        String userDir = System.getProperty("user.dir");
        File exeFile = new File(userDir, "lib/bin/7z/7z.exe");
        File dllFile = new File(userDir, "lib/bin/7z/7z.dll");

        if (exeFile.exists()) {
            return exeFile.getAbsolutePath();
        }

        // Intentar extraer desde el JAR
        try {
            exeFile.getParentFile().mkdirs();
            extractFromJar("/bin/7z/7z.exe", exeFile);
            extractFromJar("/bin/7z/7z.dll", dllFile);
            if (exeFile.exists()) {
                logger.info("7z extraído desde JAR a: {}", exeFile.getAbsolutePath());
                return exeFile.getAbsolutePath();
            }
        } catch (Exception e) {
            logger.warn("No se pudo extraer 7z desde el JAR", e);
        }

        // Fallback: buscar en el PATH del sistema
        logger.warn("7z no encontrado en {} ni en JAR; se usará '7z' del PATH", exeFile.getAbsolutePath());
        return "7z";
    }

    private static void extractFromJar(String resourcePath, File target) throws Exception {
        try (InputStream in = ExternalToolsManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Recurso no encontrado en el JAR: " + resourcePath);
            }
            byte[] buffer = new byte[8192];
            int read;
            try (FileOutputStream out = new FileOutputStream(target)) {
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }
}
