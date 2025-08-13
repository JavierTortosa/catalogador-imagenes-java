package vista.util; 

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vista.components.icons.ColorOverlayIcon;
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class IconUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(IconUtils.class);

    private final ThemeManager themeManager;

    public IconUtils(ThemeManager themeManager) {
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en IconUtils");
    } // --- Fin del constructor IconUtils ---

    
    /**
     * Carga un icono para la aplicación desde la carpeta de iconos comunes, sin reescalarlo.
     * @param iconName El nombre del archivo del icono (ej. "app-icon.png").
     * @return un ImageIcon, o null si no se encuentra.
     */
    public ImageIcon getAppIcon(String iconName) {
        String path = "/iconos/comunes/" + iconName;
        try {
            java.net.URL iconURL = getClass().getResource(path);
            if (iconURL != null) {
                return new ImageIcon(iconURL);
            } else {
                logger.error("No se pudo encontrar el recurso del icono de la app: " + path);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al cargar el icono de la app: " + path, e);
            return null;
        }
    } // --- Fin del metodo getAppIcon ---
    
    
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
            logger.error("ERROR [IconUtils loadImageIcon]: classpathResourcePath es nulo o vacío para el identificador: " + iconIdentifier);
            return null;
        }

        // logger.debug("  [IconUtils loadImageIcon] Intentando cargar: " + classpathResourcePath + " (para " + iconIdentifier + ")");
        
        URL imgURL = IconUtils.class.getResource(classpathResourcePath);

        if (imgURL != null) {
            // logger.debug("    -> Icono encontrado en classpath: " + classpathResourcePath);
            return new ImageIcon(imgURL);
        } else {
            logger.warn("WARN [IconUtils loadImageIcon]: Recurso NO encontrado en classpath: " + classpathResourcePath + " (para " + iconIdentifier + ")");
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
            logger.error("ERROR [IconUtils.getIcon]: nombreIconoTematizado no puede ser nulo o vacío.");
            return null;
        }

        Tema temaActual = themeManager.getTemaActual();
        if (temaActual == null) {
            logger.error("ERROR [IconUtils.getIcon]: No se pudo obtener el tema actual desde ThemeManager para el icono: " + nombreIconoTematizado);
            return null; // No se puede proceder sin un tema
        }

        String nombreCarpetaIconosTema = temaActual.carpetaIconos();
        if (nombreCarpetaIconosTema == null || nombreCarpetaIconosTema.isBlank()) {
            logger.error("ERROR [IconUtils.getIcon]: La carpeta de iconos definida en el tema '" +
                               temaActual.nombreInterno() + "' es inválida. Usando 'black' como fallback para determinar la carpeta del tema.");
            nombreCarpetaIconosTema = "black"; // Fallback para la carpeta del tema
        }

        // 1. Intento en la carpeta del tema actual
        String pathTema = "/iconos/" + nombreCarpetaIconosTema + "/" + nombreIconoTematizado;
        // logger.debug("  [IconUtils.getIcon] Intento 1 (Tema '" + nombreCarpetaIconosTema + "'): " + pathTema);
        ImageIcon icon = cargarIconoDesdePath(pathTema, nombreIconoTematizado + " (tema: " + nombreCarpetaIconosTema + ")");
        if (icon != null) {
            return icon;
        }
        // Si llegó aquí, no se encontró en la carpeta del tema actual. El log de error ya salió de cargarIconoDesdePath.

        // 2. Fallback a la carpeta "comunes" (si la carpeta del tema actual no era ya "comunes")
        if (!"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
            String pathComunes = "/iconos/comunes/" + nombreIconoTematizado;
            logger.debug("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en tema '" + nombreCarpetaIconosTema + "'. Intentando fallback a comunes: " + pathComunes);
            icon = cargarIconoDesdePath(pathComunes, nombreIconoTematizado + " (fallback comunes)");
            if (icon != null) {
                return icon;
            }
            // Log de error ya salió de cargarIconoDesdePath si falló.
        }

        // 3. Fallback final a la carpeta "black" (si la carpeta del tema actual no era "black" Y no era "comunes" donde ya falló)
        if (!"black".equalsIgnoreCase(nombreCarpetaIconosTema) && !"comunes".equalsIgnoreCase(nombreCarpetaIconosTema)) {
            String pathBlack = "/iconos/black/" + nombreIconoTematizado;
            logger.debug("  [IconUtils.getIcon] Icono '" + nombreIconoTematizado + "' no encontrado en comunes. Intentando fallback final a black: " + pathBlack);
            icon = cargarIconoDesdePath(pathBlack, nombreIconoTematizado + " (fallback black)");
            if (icon != null) {
                return icon;
            }
            // Log de error ya salió de cargarIconoDesdePath si falló.
        }
        
        logger.error("ERROR MUY GRAVE [IconUtils.getIcon]: Icono '" + nombreIconoTematizado + "' NO encontrado después de todos los intentos y fallbacks.");
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
            logger.error("ERROR [IconUtils.getCommonIcon]: nombreIconoComun no puede ser nulo o vacío.");
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
            logger.error("ERROR [IconUtils.getColoredOverlayIcon]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        
        // Obtener el color del tema.
        // Asumo que ThemeManager tiene un método para obtener colores por clave/nombre de tema.
        Color color = themeManager.getFondoSecundarioParaTema(themeColorKey); // o el método adecuado
        if (color == null) {
            logger.warn("WARN [IconUtils.getColoredOverlayIcon]: Color para clave '" + themeColorKey + "' no encontrado en el tema actual. Usando gris.");
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
            logger.error("ERROR [IconUtils.getColoredOverlayIcon(Color)]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        if (specificColor == null) {
            logger.warn("WARN [IconUtils.getColoredOverlayIcon(Color)]: El color específico es nulo. Usando gris.");
            specificColor = Color.GRAY;
        }
        return new ColorOverlayIcon(base, specificColor, width, height);
    } // --- Fin del método getColoredOverlayIcon (con Color) ---
    



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
                logger.error("ERROR [IconUtils.scaleImageIcon]: getScaledInstance devolvió null.");
                return null; // Retornar null si el escalado falla
            }
            return new ImageIcon(scaledImg);
        } catch (Exception e) {
            logger.error("ERROR [IconUtils.scaleImageIcon]: Excepción al escalar icono a " +
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
             logger.warn("WARN [IconUtils.getScaledIcon]: No se pudo obtener el icono original tematizado para: " + nombreIconoTematizado);
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
             logger.warn("WARN [IconUtils.getScaledCommonIcon]: No se pudo obtener el icono original común para: " + nombreIconoComun);
        }
        return scaleImageIcon(originalIcon, width, height);
    } // --- Fin del método getScaledCommonIcon ---


    public ImageIcon getTintedIcon(String iconMaskPath, Color tintColor, int width, int height) {
        Image maskImage = getRawCommonImage(iconMaskPath);
        if (maskImage == null) return null;
        if (tintColor == null) tintColor = Color.GRAY;
        

        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImage.createGraphics();

        try {
            g2d.drawImage(maskImage, 0, 0, width, height, null);
            g2d.setComposite(AlphaComposite.SrcIn);
            g2d.setColor(tintColor); // Pinta con el color exacto que recibe
            g2d.fillRect(0, 0, width, height);
        } finally {
            g2d.dispose();
        }
        
        return new ImageIcon(finalImage);

        
    } // --- FIN del metodo getTintedIcon ---
    
    
    /**
     * Crea un icono tintado a partir de un Icono base ya existente.
     * Convierte el Icon a Image y luego aplica el tinte.
     *
     * @param baseIcon  El Icono que sirve como máscara.
     * @param tintColor El color para tintar.
     * @param width     El ancho del icono final.
     * @param height    El alto del icono final.
     * @return Un nuevo ImageIcon tintado.
     */
    public ImageIcon getTintedIcon(Icon baseIcon, Color tintColor, int width, int height) {
        if (baseIcon == null) return null;
        if (tintColor == null) tintColor = Color.GRAY;

        // Convertir el Icon a una BufferedImage para poder manipularla
        BufferedImage maskImage = new BufferedImage(baseIcon.getIconWidth(), baseIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = maskImage.createGraphics();
        baseIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        // El resto de la lógica de pintado es la misma
        BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = finalImage.createGraphics();
        try {
            g2d.drawImage(maskImage, 0, 0, width, height, null);
            g2d.setComposite(AlphaComposite.SrcIn);
            g2d.setColor(tintColor);
            g2d.fillRect(0, 0, width, height);
        } finally {
            g2d.dispose();
        }
        return new ImageIcon(finalImage);
    } // --- FIN del metodo getTintedIcon ---

    
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
            logger.error("ERROR [IconUtils.getCheckeredOverlayIcon]: Imagen base '" + baseIconName + "' no encontrada.");
            return null;
        }
        return new ColorOverlayIcon(base, width, height);
    } // --- Fin del método getCheckeredOverlayIcon ---
    
    
    /**
     * Crea un icono con patrón de cuadros a partir de un Icono base ya existente.
     *
     * @param baseIcon El Icono que sirve como máscara.
     * @param width    El ancho del icono final.
     * @param height   El alto del icono final.
     * @return Un nuevo Icon con el patrón de cuadros.
     */
    public Icon getCheckeredOverlayIcon(Icon baseIcon, int width, int height) {
        if (baseIcon == null) return null;

        BufferedImage maskImage = new BufferedImage(baseIcon.getIconWidth(), baseIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = maskImage.createGraphics();
        baseIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        return new ColorOverlayIcon(maskImage, width, height);
    } // --- FIN del metodo getCheckeredOverlayIcon ---
    
    
    
    
    public Color aclararColor(Color colorOriginal, int cantidadAclarar) {
        if (colorOriginal == null) return Color.LIGHT_GRAY;
        int r = Math.min(255, colorOriginal.getRed() + cantidadAclarar);
        int g = Math.min(255, colorOriginal.getGreen() + cantidadAclarar);
        int b = Math.min(255, colorOriginal.getBlue() + cantidadAclarar);
        return new Color(r, g, b);
    } // --- Fin del método aclararColor ---

    public Color oscurecerColor(Color colorOriginal, int cantidadOscurecer) {
        if (colorOriginal == null) return Color.DARK_GRAY;
        int r = Math.max(0, colorOriginal.getRed() - cantidadOscurecer);
        int g = Math.max(0, colorOriginal.getGreen() - cantidadOscurecer);
        int b = Math.max(0, colorOriginal.getBlue() - cantidadOscurecer);
        return new Color(r, g, b);
    } // --- Fin del método oscurecerColor ---
    
    
    /**
     * Aclara un color preservando su tono (Hue) y saturación (Saturation) originales.
     * Trabaja en el espacio de color HSB para un resultado visualmente más natural.
     *
     * @param colorOriginal El color a aclarar.
     * @param factor El factor de aclarado (ej. 0.4f para aumentar el brillo en un 40%).
     * @return Un nuevo objeto Color más claro.
     */
    public Color aclararColorHSB(Color colorOriginal, float factor) {
        if (colorOriginal == null) return Color.LIGHT_GRAY;

        // 1. Convertir de RGB a HSB
        float[] hsbVals = Color.RGBtoHSB(colorOriginal.getRed(), colorOriginal.getGreen(), colorOriginal.getBlue(), null);

        // 2. Aumentar el brillo (el tercer componente, hsbVals[2])
        //    Nos aseguramos de que no supere 1.0f (blanco total)
        float nuevoBrillo = Math.min(1.0f, hsbVals[2] * (1.0f + factor));

        // Opcional pero recomendado: Aumentar un poco la saturación para que no se vea "lavado"
        float nuevaSaturacion = Math.min(1.0f, hsbVals[1] + 0.1f);

        // 3. Convertir de vuelta a RGB
        int rgb = Color.HSBtoRGB(hsbVals[0], nuevaSaturacion, nuevoBrillo);

        return new Color(rgb);
    } // FIN del metodo aclararColorHSB

    /**
     * Oscurece un color preservando su tono (Hue) y saturación (Saturation) originales.
     * Trabaja en el espacio de color HSB para un resultado visualmente más natural.
     *
     * @param colorOriginal El color a oscurecer.
     * @param factor El factor de oscurecido (ej. 0.4f para reducir el brillo en un 40%).
     * @return Un nuevo objeto Color más oscuro.
     */
    public Color oscurecerColorHSB(Color colorOriginal, float factor) {
        if (colorOriginal == null) return Color.DARK_GRAY;

        // 1. Convertir de RGB a HSB
        float[] hsbVals = Color.RGBtoHSB(colorOriginal.getRed(), colorOriginal.getGreen(), colorOriginal.getBlue(), null);

        // 2. Reducir el brillo (el tercer componente, hsbVals[2])
        //    Nos aseguramos de que no sea menor que 0.0f (negro total)
        float nuevoBrillo = Math.max(0.0f, hsbVals[2] * (1.0f - factor));

        // 3. Convertir de vuelta a RGB
        int rgb = Color.HSBtoRGB(hsbVals[0], hsbVals[1], nuevoBrillo);

        return new Color(rgb);
    } // FIN del metodo oscurecerColorHSB

    
    /**
     * Carga la imagen de bienvenida desde la carpeta de iconos comunes, sin reescalarla.
     * @param imageName El nombre del archivo de la imagen (ej. "modeltag-welcome.png").
     * @return un ImageIcon, o null si no se encuentra.
     */
    public ImageIcon getWelcomeImage(String imageName) {
        // Asumimos que la imagen está en "resources/iconos/comunes/application/"
        String path = "/iconos/comunes/application/" + imageName;
        
        try {
            java.net.URL imageURL = getClass().getResource(path);
            if (imageURL != null) {
                return new ImageIcon(imageURL);
            } else {
                logger.error("No se pudo encontrar el recurso de la imagen de bienvenida: " + path);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al cargar la imagen de bienvenida: " + path, e);
            return null;
        }
    }
    
    
} // --- FIN de la clase IconUtils

