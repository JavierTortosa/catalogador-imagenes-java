package controlador.utils;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;

public class DesktopUtils {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    /**
     * Abre el explorador de archivos del sistema y selecciona un archivo específico.
     * Actualmente, la selección solo está implementada para Windows.
     * En otros sistemas operativos, solo abrirá la carpeta contenedora.
     * @param path El Path del archivo a seleccionar.
     */
    public static void openAndSelectFile(Path path) throws IOException {
        if (path == null || !java.nio.file.Files.exists(path)) {
            throw new IOException("La ruta del archivo es nula o no existe.");
        }

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Comando específico de Windows para abrir el explorador y seleccionar un archivo
            Runtime.getRuntime().exec("explorer.exe /select," + path.toAbsolutePath().toString());
        } else {
            // Fallback para otros sistemas operativos (Mac, Linux): solo abre la carpeta
            java.awt.Desktop.getDesktop().open(path.getParent().toFile());
        }
    } // --- Fin del método openAndSelectFile ---

} // --- FIN de la clase DesktopUtils ---