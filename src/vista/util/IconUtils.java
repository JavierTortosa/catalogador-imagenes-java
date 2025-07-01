// Contenido de la clase vista.util.IconUtils.java

package vista.util; 

import java.net.URL;
import java.util.Objects;
import java.awt.Image;
import java.awt.Color; // Importar Color

import javax.swing.ImageIcon;
import javax.swing.Icon; // Importar Icon

import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.components.icons.ColorOverlayIcon; // <-- NUEVO: Importar ColorOverlayIcon

public class IconUtils {

    private final ThemeManager themeManager;

    public IconUtils(ThemeManager themeManager) {
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en IconUtils");
    } // --- Fin del constructor IconUtils ---

    /**
     * Método PRIVADO y COMÚN para cargar un ImageIcon desde una ruta de classpath dada.
     * Esta es la lógica de carga base que será reutilizada.
     *
     * @param classpathResourcePath La ruta completa y absoluta al recurso dentro del classpath
     *                              (ej. "/iconos/black/guardar.png" o "/iconos/comunes/error.png").
     * @param iconIdentifier        Un identificador del icono (nombre o ruta original) para logging.
     * @return El ImageIcon cargado, o null si no se encuentra o hay un error.
     */
    private ImageIcon cargarIconoDesdePath(String classpathResourcePath, String iconIdentifier) {
        if (classpathResourcePath == null || classpathResourcePath.trim().isEmpty()) {
            System.err.println("ERROR [IconUtils loadImageIcon]: classpathResourcePath es nulo o vacío para el identificador: " + iconIdentifier);
            return null;
        }

        // System.out.println("  [IconUtils loadImageIcon] Intentando cargar: " + classpathResourcePath + " (para " + iconIdentifier + ")");
        URL imgURL = getClass().getResource(classpathResourcePath);

        if (imgURL != null) {
            // System.out.println("    -> Icono encontrado en classpath: " + classpathResourcePath);
            return new ImageIcon(imgURL);
        } else {
            System.err.println("WARN [IconUtils loadImageIcon]: Recurso NO encontrado en classpath: " + classpathResourcePath + " (para " + iconIdentifier + ")");
            return null;
        }
    } // --- Fin del método cargarIconoDesdePath ---

    /**
     * Obtiene un ImageIcon TEMATIZADO.
     * Busca primero en la carpeta del tema actual. Si no lo encuentra, intenta un fallback
     * a la carpeta "comunes", y si aún falla, intenta un fallback a la carpeta "black".
     *
     * @param nombreIconoTematizado El nombre del archivo de icono (ej. "guardar.png", "abrir_archivo.png")
     *                              que se espera encontrar dentro de una carpeta de tema.
     * @return El ImageIcon correspondiente, o null si no se encuentra después de todos los fallbacks.
     */
    public ImageIcon getIcon(String nombreIconoTematizado) {
        if (nombreIconoTematizado == null || nombreIconoTematizado.trim().isEmpty()) {
            System.err.println("ERROR [IconUtils.getIcon]: nombreIconoTematizado no puede ser nulo o vacío.");
            return null;
        }

        Tema temaActual = themeManager.getTemaActual();
        if (temaActual == null) {
            System.err.println("ERROR [IconUtils.getIcon]: No se pudo obtener el tema actual desde ThemeManager para el icono: " + nombreIconoTematizado);
            return null; // No se puede proceder sin un tema
        }

        String nombreCarpetaIconosTema = temaActual.carpetaIconos();
        if (nombreCarpetaIconosTema == null || nombreCarpetaIconosTema.isBlank()) {
            System.err.println("ERROR [IconUtils.getIcon]: La carpeta de iconos definida en el tema '" +
                               temaActual.nombreInterno() + "' es inválida. Usando 'black' como fallback para determinar la carpeta del tema.");
            nombreCarpetaIconosTema = "black"; // Fallback para la carpeta del tema
        }

        // 1. Intento en la carpeta del tema actual
        String pathTema = "/iconos/" + nombreCarpetaIconosTema + "/" + nombreIconoTematizado;
        // System.out.println("  [IconUtils.getIcon] Intento 1 (Tema '" + nombreCarpetaIconosTema + "'): " + pathTema);
        ImageIcon icon = cargarIconoDesdePath(pathTema, nombreIconoTematizado + " (tema: " + nombreCarpetaIconosTema + ")");
        if (icon != null) {
            return icon;
        }
        // Si llegó aquí, no se encontró en la carpeta del tema actual. El log de error ya salió de cargarIconoDesdePath.

        // 2. Fallback a la carpeta "comunes" (si la carpeta del tema actual no era ya "comunes")
        if (!"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
            String pathComunes = "/iconos/comunes/" + nombreIconoTematizado;
            System.out.println("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en tema '" + nombreCarpetaIconosTema + "'. Intentando fallback a comunes: " + pathComunes);
            icon = cargarIconoDesdePath(pathComunes, nombreIconoTematizado + " (fallback comunes)");
            if (icon != null) {
                return icon;
            }
            // Log de error ya salió de cargarIconoDesdePath si falló.
        }

        // 3. Fallback final a la carpeta "black" (si la carpeta del tema actual no era "black" Y no era "comunes" donde ya falló)
        if (!"black".equalsIgnoreCase(nombreCarpetaIconosTema) && !"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
            String pathBlack = "/iconos/black/" + nombreIconoTematizado;
            System.out.println("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en comunes. Intentando fallback final a black: " + pathBlack);
            icon = cargarIconoDesdePath(pathBlack, nombreIconoTematizado + " (fallback black)");
            if (icon != null) {
                return icon;
            }
            // Log de error ya salió de cargarIconoDesdePath si falló.
        }
        
        System.err.println("ERROR MUY GRAVE [IconUtils.getIcon]: Icono '" + nombreIconoTematizado + "' NO encontrado después de todos los intentos y fallbacks.");
        return null;
    } // --- Fin del método getIcon ---

    /**
     * Obtiene un ImageIcon de la subcarpeta "comunes" dentro de la ruta base de iconos (/iconos/comunes/).
     * Estos iconos no dependen del tema actual y siempre se buscan en la misma ubicación.
     *
     * @param nombreIconoComun El nombre del archivo de icono que se espera encontrar
     *                         directamente dentro de "resources/iconos/comunes/"
     *                         (ej. "imagen-rota.png", "alerta.png").
     * @return El ImageIcon correspondiente, o null si no se encuentra.
     */
    public ImageIcon getCommonIcon(String nombreIconoComun) {
        if (nombreIconoComun == null || nombreIconoComun.trim().isEmpty()) {
            System.err.println("ERROR [IconUtils.getCommonIcon]: nombreIconoComun no puede ser nulo o vacío.");
            return null;
        }
        // Construir la ruta directamente a la carpeta "comunes"
        String pathCompleto = "/iconos/comunes/" + nombreIconoComun;
        return cargarIconoDesdePath(pathCompleto, nombreIconoComun + " (común)");
    } // --- Fin del método getCommonIcon ---

    // --- MÉTODOS DE OBTENCIÓN DE IMAGENES RAW (sin envolver en ImageIcon) ---

    /**
     * Obtiene la `java.awt.Image` subyacente de un icono tematizado (con fallbacks),
     * sin envolverlo en `ImageIcon`. Útil para componentes personalizados que necesitan `Image` directamente.
     *
     * @param nombreIconoTematizado El nombre del archivo de icono tematizado.
     * @return La `Image` correspondiente, o `null` si no se encuentra.
     */
    public Image getRawIcon(String nombreIconoTematizado) {
        ImageIcon icon = getIcon(nombreIconoTematizado);
        return (icon != null) ? icon.getImage() : null;
    } // --- Fin del método getRawIcon ---

    /**
     * Obtiene la `java.awt.Image` subyacente de un icono común (de la carpeta "comunes"),
     * sin envolverlo en `ImageIcon`.
     * Esto es útil para componentes personalizados que necesitan `Image` directamente.
     *
     * @param nombreIconoComun El nombre del archivo de icono común (ej. "dpad_base.pad.png").
     * @return La `Image` correspondiente, o `null` si no se encuentra.
     */
    public Image getRawCommonImage(String nombreIconoComun) {
        ImageIcon icon = getCommonIcon(nombreIconoComun);
        // Si el ImageIcon es nulo, o su Image interna es nula, devolvemos null.
        return (icon != null) ? icon.getImage() : null;
    } // --- Fin del método getRawCommonImage ---
    
    // --- NUEVOS MÉTODOS PARA OBTENER ICONOS CON SUPERPOSICIÓN DE COLOR/PATRÓN ---

    /**
     * Obtiene un Icono con una imagen base y una superposición de color.
     * Utiliza la imagen base proporcionada por `baseIconName` y la rellena
     * con el color del tema actual correspondiente a `themeColorKey`.
     *
     * @param baseIconName La clave/nombre del archivo de la imagen base (ej. "circular_base.png").
     * @param themeColorKey Una clave que ThemeManager puede usar para obtener un Color (ej. "clear", "dark").
     * @param width El ancho deseado para el icono.
     * @param height El alto deseado para el icono.
     * @return Un Icon que dibuja la imagen base con el color superpuesto, o null si hay errores.
     */
    public Icon getColoredOverlayIcon(String baseIconName, String themeColorKey, int width, int height) {
        Image base = getRawCommonImage(baseIconName); // Usar getRawCommonImage para iconos de base comunes
        if (base == null) {
            System.err.println("ERROR [IconUtils.getColoredOverlayIcon]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        
        // Obtener el color del tema.
        // Asumo que ThemeManager tiene un método para obtener colores por clave/nombre de tema.
        Color color = themeManager.getFondoSecundarioParaTema(themeColorKey); // o el método adecuado
        if (color == null) {
            System.err.println("WARN [IconUtils.getColoredOverlayIcon]: Color para clave '" + themeColorKey + "' no encontrado en el tema actual. Usando gris.");
            color = Color.GRAY; // Fallback
        }
        
        return new ColorOverlayIcon(base, color, width, height);
    } // --- Fin del método getColoredOverlayIcon ---
    
    
    /**
     * Obtiene un Icono con una imagen base y una superposición de un color específico.
     * Esta es una sobrecarga que permite pasar un objeto Color directamente, en lugar
     * de una clave de tema. Es útil cuando el color ha sido modificado o elegido por el usuario.
     *
     * @param baseIconName La clave/nombre del archivo de la imagen base (ej. "circular_base.png").
     * @param specificColor El objeto Color específico a usar para la superposición.
     * @param width El ancho deseado para el icono.
     * @param height El alto deseado para el icono.
     * @return Un Icon que dibuja la imagen base con el color superpuesto, o null si hay errores.
     */
    public Icon getColoredOverlayIcon(String baseIconName, Color specificColor, int width, int height) {
        Image base = getRawCommonImage(baseIconName);
        if (base == null) {
            System.err.println("ERROR [IconUtils.getColoredOverlayIcon(Color)]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        if (specificColor == null) {
            System.err.println("WARN [IconUtils.getColoredOverlayIcon(Color)]: El color específico es nulo. Usando gris.");
            specificColor = Color.GRAY;
        }
        return new ColorOverlayIcon(base, specificColor, width, height);
    } // --- Fin del método getColoredOverlayIcon (con Color) ---
    

    /**
     * Obtiene un Icono con una imagen base y un patrón de cuadros.
     *
     * @param baseIconName La clave/nombre del archivo de la imagen base (ej. "circular_base.png").
     * @param width El ancho deseado para el icono.
     * @param height El alto deseado para el icono.
     * @return Un Icon que dibuja la imagen base con el patrón de cuadros, o null si hay errores.
     */
    public Icon getCheckeredOverlayIcon(String baseIconName, int width, int height) {
        Image base = getRawCommonImage(baseIconName); // Usar getRawCommonImage para iconos de base comunes
        if (base == null) {
            System.err.println("ERROR [IconUtils.getCheckeredOverlayIcon]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        return new ColorOverlayIcon(base, width, height);
    } // --- Fin del método getCheckeredOverlayIcon ---

    // --- MÉTODOS DE ESCALADO ---

    /**
     * Método helper privado para escalar un ImageIcon dado.
     *
     * @param originalIcon El ImageIcon a escalar.
     * @param width El ancho deseado.
     * @param height El alto deseado.
     * @return El ImageIcon escalado, o el original si no se necesita escalar, o null si hay error.
     */
    public ImageIcon scaleImageIcon(ImageIcon originalIcon, int width, int height) {
        if (originalIcon == null) {
            return null;
        }

        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();

        // Si no se necesita escalar (ambos <= 0 o coinciden exactamente con el original)
        // O si las dimensiones originales son inválidas para escalar.
        if ((width <= 0 && height <= 0) || 
            (originalWidth == width && originalHeight == height) ||
             originalWidth <= 0 || originalHeight <= 0) {
            return originalIcon;
        }

        int targetWidth = width;
        int targetHeight = height;

        // Mantener proporción si una dimensión no es válida (<=0) o no se especifica
        if (targetWidth <= 0) { // Calcular ancho desde alto
            double ratio = (double) targetHeight / originalHeight;
            targetWidth = (int) (originalWidth * ratio);
        } else if (targetHeight <= 0) { // Calcular alto desde ancho
            double ratio = (double) targetWidth / originalWidth;
            targetHeight = (int) (originalHeight * ratio);
        }
        // Si ambas, width y height, son > 0, se usan tal cual (puede no mantener proporción).

        targetWidth = Math.max(1, targetWidth);   // Asegurar al menos 1px
        targetHeight = Math.max(1, targetHeight); // Asegurar al menos 1px

        try {
            java.awt.Image img = originalIcon.getImage();
            // Usar SCALE_SMOOTH para mejor calidad, aunque puede ser más lento
            java.awt.Image scaledImg = img.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
            if (scaledImg == null) {
                System.err.println("ERROR [IconUtils.scaleImageIcon]: getScaledInstance devolvió null.");
                return null; // Retornar null si el escalado falla
            }
            return new ImageIcon(scaledImg);
        } catch (Exception e) {
            System.err.println("ERROR [IconUtils.scaleImageIcon]: Excepción al escalar icono a " +
                               targetWidth + "x" + targetHeight + ". Icono original: " + (originalIcon.getDescription() != null ? originalIcon.getDescription() : "descripción nula") + ". Mensaje: " + e.getMessage());
            // e.printStackTrace(); // Descomentar para depuración profunda
            return null; // Retornar null si hay una excepción
        }
    } // --- Fin del método scaleImageIcon ---
    
    /**
     * Obtiene un ImageIcon tematizado y redimensionado.
     *
     * @param nombreIconoTematizado El nombre del archivo de icono tematizado.
     * @param width El ancho deseado.
     * @param height El alto deseado.
     * @return El ImageIcon redimensionado, o null si el original no se encuentra o hay error.
     */
    public ImageIcon getScaledIcon(String nombreIconoTematizado, int width, int height) {
        ImageIcon originalIcon = getIcon(nombreIconoTematizado); // Obtiene el icono tematizado (con fallbacks)
        if (originalIcon == null) {
             System.err.println("WARN [IconUtils.getScaledIcon]: No se pudo obtener el icono original tematizado para: " + nombreIconoTematizado);
        }
        return scaleImageIcon(originalIcon, width, height);    // Llama al helper de escalado
    } // --- Fin del método getScaledIcon ---

    /**
     * Obtiene un ImageIcon común y redimensionado.
     *
     * @param nombreIconoComun El nombre del archivo de icono en la carpeta "comunes".
     * @param width El ancho deseado.
     * @param height El alto deseado.
     * @return El ImageIcon redimensionado, o null si el original no se encuentra o hay error.
     */
    public ImageIcon getScaledCommonIcon(String nombreIconoComun, int width, int height) {
        ImageIcon originalIcon = getCommonIcon(nombreIconoComun);
         if (originalIcon == null) {
             System.err.println("WARN [IconUtils.getScaledCommonIcon]: No se pudo obtener el icono original común para: " + nombreIconoComun);
        }
        return scaleImageIcon(originalIcon, width, height);
    } // --- Fin del método getScaledCommonIcon ---

    
    
    
    
} // --- FIN de la clase IconUtils

//package vista.util; 
//
//import java.net.URL;
//import java.util.Objects;
//
//import javax.swing.ImageIcon;
//
//import vista.theme.Tema;
//import vista.theme.ThemeManager;
//
//public class IconUtils {
//
//    private final ThemeManager themeManager;
//    // private final Object HINT_INTERPOLACION = RenderingHints.VALUE_INTERPOLATION_BILINEAR; // Para escalado
//
//    public IconUtils(ThemeManager themeManager) {
//        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en IconUtils");
//    }
//
//    /**
//     * Método PRIVADO y COMÚN para cargar un ImageIcon desde una ruta de classpath dada.
//     * Esta es la lógica de carga base que será reutilizada.
//     *
//     * @param classpathResourcePath La ruta completa y absoluta al recurso dentro del classpath
//     *                              (ej. "/iconos/black/guardar.png" o "/iconos/comunes/error.png").
//     * @param iconIdentifier        Un identificador del icono (nombre o ruta original) para logging.
//     * @return El ImageIcon cargado, o null si no se encuentra o hay un error.
//     */
//    private ImageIcon cargarIconoDesdePath(String classpathResourcePath, String iconIdentifier) {
//        if (classpathResourcePath == null || classpathResourcePath.trim().isEmpty()) {
//            System.err.println("ERROR [IconUtils loadImageIcon]: classpathResourcePath es nulo o vacío para el identificador: " + iconIdentifier);
//            return null;
//        }
//
//        // System.out.println("  [IconUtils loadImageIcon] Intentando cargar: " + classpathResourcePath + " (para " + iconIdentifier + ")");
//        URL imgURL = getClass().getResource(classpathResourcePath);
//
//        if (imgURL != null) {
//            // System.out.println("    -> Icono encontrado en classpath: " + classpathResourcePath);
//            return new ImageIcon(imgURL);
//        } else {
//            System.err.println("WARN [IconUtils loadImageIcon]: Recurso NO encontrado en classpath: " + classpathResourcePath + " (para " + iconIdentifier + ")");
//            return null;
//        }
//    }
//
//    /**
//     * Obtiene un ImageIcon TEMATIZADO.
//     * Busca primero en la carpeta del tema actual. Si no lo encuentra, intenta un fallback
//     * a la carpeta "comunes", y si aún falla, intenta un fallback a la carpeta "black".
//     *
//     * @param nombreIconoTematizado El nombre del archivo de icono (ej. "guardar.png", "abrir_archivo.png")
//     *                              que se espera encontrar dentro de una carpeta de tema.
//     * @return El ImageIcon correspondiente, o null si no se encuentra después de todos los fallbacks.
//     */
//    public ImageIcon getIcon(String nombreIconoTematizado) {
//        if (nombreIconoTematizado == null || nombreIconoTematizado.trim().isEmpty()) {
//            System.err.println("ERROR [IconUtils.getIcon]: nombreIconoTematizado no puede ser nulo o vacío.");
//            return null;
//        }
//
//        Tema temaActual = themeManager.getTemaActual();
//        if (temaActual == null) {
//            System.err.println("ERROR [IconUtils.getIcon]: No se pudo obtener el tema actual desde ThemeManager para el icono: " + nombreIconoTematizado);
//            return null; // No se puede proceder sin un tema
//        }
//
//        String nombreCarpetaIconosTema = temaActual.carpetaIconos();
//        if (nombreCarpetaIconosTema == null || nombreCarpetaIconosTema.isBlank()) {
//            System.err.println("ERROR [IconUtils.getIcon]: La carpeta de iconos definida en el tema '" +
//                               temaActual.nombreInterno() + "' es inválida. Usando 'black' como fallback para determinar la carpeta del tema.");
//            nombreCarpetaIconosTema = "black"; // Fallback para la carpeta del tema
//        }
//
//        // 1. Intento en la carpeta del tema actual
//        String pathTema = "/iconos/" + nombreCarpetaIconosTema + "/" + nombreIconoTematizado;
//        // System.out.println("  [IconUtils.getIcon] Intento 1 (Tema '" + nombreCarpetaIconosTema + "'): " + pathTema);
//        ImageIcon icon = cargarIconoDesdePath(pathTema, nombreIconoTematizado + " (tema: " + nombreCarpetaIconosTema + ")");
//        if (icon != null) {
//            return icon;
//        }
//        // Si llegó aquí, no se encontró en la carpeta del tema actual. El log de error ya salió de cargarIconoDesdePath.
//
//        // 2. Fallback a la carpeta "comunes" (si la carpeta del tema actual no era ya "comunes")
//        if (!"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
//            String pathComunes = "/iconos/comunes/" + nombreIconoTematizado;
//            System.out.println("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en tema '" + nombreCarpetaIconosTema + "'. Intentando fallback a comunes: " + pathComunes);
//            icon = cargarIconoDesdePath(pathComunes, nombreIconoTematizado + " (fallback comunes)");
//            if (icon != null) {
//                return icon;
//            }
//            // Log de error ya salió de cargarIconoDesdePath si falló.
//        }
//
//        // 3. Fallback final a la carpeta "black" (si la carpeta del tema actual no era "black" Y no era "comunes" donde ya falló)
//        if (!"black".equalsIgnoreCase(nombreCarpetaIconosTema) && !"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
//            String pathBlack = "/iconos/black/" + nombreIconoTematizado;
//            System.out.println("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en comunes. Intentando fallback final a black: " + pathBlack);
//            icon = cargarIconoDesdePath(pathBlack, nombreIconoTematizado + " (fallback black)");
//            if (icon != null) {
//                return icon;
//            }
//            // Log de error ya salió de cargarIconoDesdePath si falló.
//        }
//        
//        System.err.println("ERROR MUY GRAVE [IconUtils.getIcon]: Icono '" + nombreIconoTematizado + "' NO encontrado después de todos los intentos y fallbacks.");
//        return null;
//    }
//
//    /**
//     * Obtiene un ImageIcon de la subcarpeta "comunes" dentro de la ruta base de iconos (/iconos/comunes/).
//     * Estos iconos no dependen del tema actual y siempre se buscan en la misma ubicación.
//     *
//     * @param nombreIconoComun El nombre del archivo de icono que se espera encontrar
//     *                         directamente dentro de "resources/iconos/comunes/"
//     *                         (ej. "imagen-rota.png", "alerta.png").
//     * @return El ImageIcon correspondiente, o null si no se encuentra.
//     */
//    public ImageIcon getCommonIcon(String nombreIconoComun) {
//        if (nombreIconoComun == null || nombreIconoComun.trim().isEmpty()) {
//            System.err.println("ERROR [IconUtils.getCommonIcon]: nombreIconoComun no puede ser nulo o vacío.");
//            return null;
//        }
//        // Construir la ruta directamente a la carpeta "comunes"
//        String pathCompleto = "/iconos/comunes/" + nombreIconoComun;
//        return cargarIconoDesdePath(pathCompleto, nombreIconoComun + " (común)");
//    }
//
//    
//    // --- MÉTODOS DE ESCALADO ---
//
//    
//    /**
//     * Método helper privado para escalar un ImageIcon dado.
//     *
//     * @param originalIcon El ImageIcon a escalar.
//     * @param width El ancho deseado.
//     * @param height El alto deseado.
//     * @return El ImageIcon escalado, o el original si no se necesita escalar, o null si hay error.
//     */
//    public ImageIcon scaleImageIcon(ImageIcon originalIcon, int width, int height) {
//        if (originalIcon == null) {
//            return null;
//        }
//
//        int originalWidth = originalIcon.getIconWidth();
//        int originalHeight = originalIcon.getIconHeight();
//
//        // Si no se necesita escalar (ambos <= 0 o coinciden exactamente con el original)
//        // O si las dimensiones originales son inválidas para escalar.
//        if ((width <= 0 && height <= 0) || 
//            (originalWidth == width && originalHeight == height) ||
//             originalWidth <= 0 || originalHeight <= 0) {
//            return originalIcon;
//        }
//
//        int targetWidth = width;
//        int targetHeight = height;
//
//        // Mantener proporción si una dimensión no es válida (<=0) o no se especifica
//        if (targetWidth <= 0) { // Calcular ancho desde alto
//            double ratio = (double) targetHeight / originalHeight;
//            targetWidth = (int) (originalWidth * ratio);
//        } else if (targetHeight <= 0) { // Calcular alto desde ancho
//            double ratio = (double) targetWidth / originalWidth;
//            targetHeight = (int) (originalHeight * ratio);
//        }
//        // Si ambas, width y height, son > 0, se usan tal cual (puede no mantener proporción).
//
//        targetWidth = Math.max(1, targetWidth);   // Asegurar al menos 1px
//        targetHeight = Math.max(1, targetHeight); // Asegurar al menos 1px
//
//        try {
//            java.awt.Image img = originalIcon.getImage();
//            // Usar SCALE_SMOOTH para mejor calidad, aunque puede ser más lento
//            java.awt.Image scaledImg = img.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
//            if (scaledImg == null) {
//                System.err.println("ERROR [IconUtils.scaleImageIcon]: getScaledInstance devolvió null.");
//                return null; // Retornar null si el escalado falla
//            }
//            return new ImageIcon(scaledImg);
//        } catch (Exception e) {
//            System.err.println("ERROR [IconUtils.scaleImageIcon]: Excepción al escalar icono a " +
//                               targetWidth + "x" + targetHeight + ". Icono original: " + (originalIcon.getDescription() != null ? originalIcon.getDescription() : "descripción nula") + ". Mensaje: " + e.getMessage());
//            // e.printStackTrace(); // Descomentar para depuración profunda
//            return null; // Retornar null si hay una excepción
//        }
//    }
//    
//    /**
//     * Obtiene un ImageIcon tematizado y redimensionado.
//     *
//     * @param nombreIconoTematizado El nombre del archivo de icono tematizado.
//     * @param width El ancho deseado.
//     * @param height El alto deseado.
//     * @return El ImageIcon redimensionado, o null si el original no se encuentra o hay error.
//     */
//    public ImageIcon getScaledIcon(String nombreIconoTematizado, int width, int height) {
//        ImageIcon originalIcon = getIcon(nombreIconoTematizado); // Obtiene el icono tematizado (con fallbacks)
//        if (originalIcon == null) {
//             System.err.println("WARN [IconUtils.getScaledIcon]: No se pudo obtener el icono original tematizado para: " + nombreIconoTematizado);
//        }
//        return scaleImageIcon(originalIcon, width, height);    // Llama al helper de escalado
//    }
//
//    /**
//     * Obtiene un ImageIcon común y redimensionado.
//     *
//     * @param nombreIconoComun El nombre del archivo de icono en la carpeta "comunes".
//     * @param width El ancho deseado.
//     * @param height El alto deseado.
//     * @return El ImageIcon redimensionado, o null si el original no se encuentra o hay error.
//     */
//    public ImageIcon getScaledCommonIcon(String nombreIconoComun, int width, int height) {
//        ImageIcon originalIcon = getCommonIcon(nombreIconoComun);
//         if (originalIcon == null) {
//             System.err.println("WARN [IconUtils.getScaledCommonIcon]: No se pudo obtener el icono original común para: " + nombreIconoComun);
//        }
//        return scaleImageIcon(originalIcon, width, height);
//    }
//
//} // --- FIN de la clase IconUtils
