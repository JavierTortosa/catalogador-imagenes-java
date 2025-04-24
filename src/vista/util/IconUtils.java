// --- NUEVA CLASE ---
package vista.util; // O el paquete que prefieras para utilidades de vista

import java.net.URL;
import java.util.Objects;

import javax.swing.ImageIcon;

import vista.theme.Tema;
import vista.theme.ThemeManager;


/**
 * Clase de utilidad para cargar y gestionar iconos de la aplicación.
 * Utiliza ConfigurationManager para determinar la carpeta de iconos
 * correcta según el tema seleccionado.
 */
public class IconUtils {
	
	private final ThemeManager themeManager;


    /**
     * Constructor que requiere una instancia de ConfigurationManager.
     * @param configManager La instancia del gestor de configuración.
     */
	 public IconUtils(ThemeManager themeManager)
	 {
		 this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en IconUtils");
		 
        
    }

	 /**
     * Obtiene un ImageIcon a partir del nombre del archivo de icono.
     * La carpeta de iconos se determina dinámicamente según el tema actual.
     *
     * @param nombreIcono El nombre del archivo de icono (ej. "add.png").
     * @return El ImageIcon correspondiente, o null si no se encuentra o hay error.
     */
    public ImageIcon getIcon(String nombreIcono) 
    {
        if (nombreIcono == null || nombreIcono.trim().isEmpty()) {
            System.err.println("ERROR [IconUtils]: nombreIcono no puede ser nulo o vacío.");
            return null;
        }

        // 1. Obtener el TEMA actual del ThemeManager
        Tema temaActual = themeManager.getTemaActual();
        if (temaActual == null) {
             System.err.println("ERROR [IconUtils]: No se pudo obtener el tema actual desde ThemeManager.");
             // Puedes intentar obtener el tema por defecto aquí como fallback si ThemeManager lo permite
             // temaActual = themeManager.obtenerTemaPorDefecto();
             // if (temaActual == null) return null; // Salir si ni el default funciona
             return null; // Salir por ahora
        }

        // 2. Obtener la CARPETA de iconos desde el objeto Tema
        String nombreCarpetaIconos = temaActual.carpetaIconos(); // Usa el getter del Record/Clase Tema
        if (nombreCarpetaIconos == null || nombreCarpetaIconos.isBlank()){
            System.err.println("ERROR [IconUtils]: La carpeta de iconos definida en el tema '" + temaActual.nombreInterno() + "' es inválida.");
            nombreCarpetaIconos = "black"; // Fallback a 'black' si la carpeta del tema es inválida
        }

        // 3. Construir la ruta completa al recurso del icono
        String pathCompleto = "/iconos/" + nombreCarpetaIconos + "/" + nombreIcono;

        // System.out.println("[IconUtils] Intentando cargar icono desde: " + pathCompleto);

        // 4. Cargar el recurso ImageIcon
        URL imgURL = getClass().getResource(pathCompleto);

        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("WARN [IconUtils]: No se pudo encontrar el icono en la ruta: " + pathCompleto);
            // Fallback a la carpeta "black" (asumiendo que siempre existe)
            String fallbackPath = "/iconos/black/" + nombreIcono;
            URL fallbackURL = getClass().getResource(fallbackPath);
            if (fallbackURL != null) {
                 System.out.println("  -> Usando icono de fallback desde: " + fallbackPath);
                 return new ImageIcon(fallbackURL);
            } else {
                 System.err.println("  -> Fallback tampoco encontrado: " + fallbackPath);
                 return null;
            }
        }
    }

    /**
     * Obtiene un ImageIcon redimensionado.
     *
     * @param nombreIcono El nombre del archivo de icono.
     * @param width       El ancho deseado. Si es <= 0, se calcula desde el alto.
     * @param height      El alto deseado. Si es <= 0, se calcula desde el ancho o se usa el original.
     *                    Si ambos son <=0, devuelve el icono original. Si uno es <=0, mantiene proporción.
     * @return El ImageIcon redimensionado, o null si el icono original no se encuentra o hay error.
     */
    public ImageIcon getScaledIcon(String nombreIcono, int width, int height) {
        ImageIcon originalIcon = getIcon(nombreIcono);
        if (originalIcon == null) {
            // El error ya se registró en getIcon()
            return null;
        }

        // Si no se necesita escalar (ambos <= 0 o coinciden exactamente)
        if ((width <= 0 && height <= 0) || (originalIcon.getIconWidth() == width && originalIcon.getIconHeight() == height)) {
            return originalIcon;
        }

        // Determinar dimensiones finales manteniendo proporción si una es <= 0
        int targetWidth = width;
        int targetHeight = height;

        if (targetWidth <= 0 && targetHeight <= 0) { // Ambos no válidos, no escalar (ya cubierto arriba, pero por si acaso)
             return originalIcon;
        } else if (targetWidth <= 0) { // Calcular ancho desde alto
            double ratio = (double) targetHeight / originalIcon.getIconHeight();
            targetWidth = (int) (originalIcon.getIconWidth() * ratio);
        } else if (targetHeight <= 0) { // Calcular alto desde ancho
            double ratio = (double) targetWidth / originalIcon.getIconWidth();
            targetHeight = (int) (originalIcon.getIconHeight() * ratio);
        }
         // Si ambos son positivos, se usan tal cual (sin mantener proporción explícita aquí)

         // Asegurar que las dimensiones no sean 0 o negativas después del cálculo
         targetWidth = Math.max(1, targetWidth);
         targetHeight = Math.max(1, targetHeight);


        try {
            java.awt.Image img = originalIcon.getImage();
            java.awt.Image scaledImg = img.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);

            if (scaledImg == null) {
                 System.err.println("ERROR [IconUtils]: getScaledInstance devolvió null para: " + nombreIcono);
                 return null;
            }
            // ImageIcon puede ser pesado, pero es la forma estándar
             ImageIcon scaledIcon = new ImageIcon(scaledImg);
             // Esperar a que la imagen esté lista puede ser necesario en algunos casos,
             // aunque getScaledInstance suele ser síncrono. Si hay problemas, investigar MediaTracker.
             // System.out.println("  -> Icono escalado ("+targetWidth+"x"+targetHeight+"): " + nombreIcono);
            return scaledIcon;

        } catch (Exception e) {
             System.err.println("ERROR [IconUtils]: Excepción al escalar icono " + nombreIcono + " a " + targetWidth + "x" + targetHeight + ": " + e.getMessage());
             e.printStackTrace(); // Para depuración
             return null; // Devuelve null si falla el escalado
        }
    }

}
// --- FIN NUEVA CLASE ---