package servicios;

import java.io.File;
import java.nio.file.Paths;

public class ExternalToolsManager {

    /**
     * Devuelve la ruta absoluta al ejecutable de 7z.
     * Esto permite que funcione tanto en desarrollo (Eclipse) 
     * como cuando exportes el programa.
     */
    public static String get7zPath() {
        // Obtenemos la ruta donde se está ejecutando la aplicación
        String userDir = System.getProperty("user.dir");
        
        // Construimos la ruta hacia lib/bin/7z/7z.exe
        File tool = new File(userDir, "lib/bin/7z/7z.exe");
        
        if (!tool.exists()) {
            // Caso alternativo por si ejecutas desde una estructura distinta
            // o si quieres buscarlo en el sistema como último recurso
            return "7z"; 
        }
        
        return tool.getAbsolutePath();
    }
}