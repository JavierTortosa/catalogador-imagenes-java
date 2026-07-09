package controlador.managers;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import utils.ImageUtils;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;

/**
 * Gestiona la actualización de la barra de información superior, que muestra
 * detalles sobre la imagen actualmente seleccionada.
 * Esta clase es responsable de leer el estado del modelo y la configuración,
 * y de reflejarlo en los componentes de la UI correspondientes.
 */
public class InfobarImageManager implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(InfobarImageManager.class);
	
    // --- Dependencias Clave ---
    private final VisorModel model;
    private final ComponentRegistry registry;
    private final ConfigurationManager configuration;

    // --- Formateadores ---
    private final SimpleDateFormat sdfFechaArchivo = new SimpleDateFormat("dd/MM/yy HH:mm");

    /**
     * Constructor del gestor de la barra de información de la imagen.
     * @param model El modelo de datos principal de la aplicación.
     * @param registry El registro central de componentes de la UI.
     * @param configuration El gestor de configuración para leer las preferencias de visibilidad.
     */
    public InfobarImageManager(VisorModel model, ComponentRegistry registry, ConfigurationManager configuration) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser nulo.");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser nulo.");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser nulo.");
    } // --- Fin del constructor ImageInfoManager ---

    /**
     * Método público principal para solicitar la actualización de la barra de información.
     * Se asegura de que la actualización se realice en el Event Dispatch Thread (EDT).
     */
    public void actualizar() {
    	
        logger.debug("actualizar() Activado");
        
    	if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::actualizarBarraInfoSuperior);
        } else {
            actualizarBarraInfoSuperior();
        }
    	
    } // --- Fin del método actualizar ---

    /**
     * Orquesta la actualización de la barra de información superior.
     * Comprueba la visibilidad del panel y llama a métodos auxiliares para cada componente.
     */
    private void actualizarBarraInfoSuperior() {
        JPanel panel = registry.get("panel.info.superior");
        if (panel == null) return;

        boolean panelVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_VISIBLE, true);
        if (panel.isVisible() != panelVisible) {
            panel.setVisible(panelVisible);
        }
        if (!panelVisible) return;

        actualizarNombreArchivo();
        actualizarIndiceTotal();
        actualizarDimensiones();
        actualizarTamanoArchivo();
        actualizarFechaArchivo();
        actualizarFormatoImagen();
        actualizarModoZoom();
        actualizarPorcentajeZoom();

        panel.revalidate();
        panel.repaint();
    } // --- Fin del método actualizarBarraInfoSuperior ---


    private void actualizarNombreArchivo() {
        javax.swing.JTextField pathField = registry.get("textfield.info.rutaImagen");
        JLabel fileLabel = registry.get("label.info.nombreArchivo");
        if (pathField == null || fileLabel == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, true);
        if (pathField.isVisible() != esVisible) pathField.setVisible(esVisible);
        if (fileLabel.isVisible() != esVisible) fileLabel.setVisible(esVisible);

        if (esVisible) {
            String pathDisplay = "Ruta: N/A";
            String fileDisplay = "Archivo: N/A";

            String selectedKey = model.getSelectedImageKey();
            String fullPathString = "Ruta: N/A";
            if (selectedKey != null) {
                Path fullPath = model.getRutaCompleta(selectedKey);
                if (fullPath == null) {
                    Path raiz = model.getCarpetaRaizActual();
                    fullPath = (raiz != null) ? raiz.resolve(selectedKey) : Path.of(selectedKey);
                }
                if (fullPath != null) {
                    Path folderPath = fullPath.getParent();
                    Path fileName = fullPath.getFileName();
                    
                    if (folderPath != null) {
                        fullPathString = "Ruta: " + folderPath.toString() + "\\";
                        pathDisplay = formatDynamicPath(folderPath.toString(), pathField, "Ruta: ", "\\");
                    }
                    fileDisplay = "Archivo: " + (fileName != null ? fileName.toString() : "");
                }
            } else if (model.getCarpetaRaizActual() != null) {
                fullPathString = "Carpeta: " + model.getCarpetaRaizActual().toString();
                pathDisplay = formatDynamicPath(model.getCarpetaRaizActual().toString(), pathField, "Carpeta: ", "");
            }

            pathField.setText(pathDisplay);
            pathField.setToolTipText(fullPathString);
            fileLabel.setText(fileDisplay);
            fileLabel.setToolTipText(fileDisplay);
        }
    } // --- Fin del método actualizarNombreArchivo ---

    private String formatDynamicPath(String pathStr, javax.swing.JTextField field, String prefix, String suffix) {
        if (pathStr == null) return prefix + suffix;
        try {
            java.awt.FontMetrics fm = field.getFontMetrics(field.getFont());
            // Ancho disponible (asumimos el preferredSize menos un margen de seguridad)
            int availableWidth = field.getPreferredSize().width - 15;
            if (availableWidth <= 0) availableWidth = 385; // fallback
            
            String fullText = prefix + pathStr + suffix;
            if (fm.stringWidth(fullText) <= availableWidth) {
                return fullText;
            }
            
            java.nio.file.Path p = java.nio.file.Paths.get(pathStr);
            int nameCount = p.getNameCount();
            
            // Vamos quitando carpetas desde la raíz hasta que quepa
            for (int i = 0; i < nameCount; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("...\\");
                for (int j = i + 1; j < nameCount; j++) {
                    sb.append(p.getName(j).toString());
                    if (j < nameCount - 1) {
                        sb.append("\\");
                    }
                }
                String testStr = prefix + sb.toString() + suffix;
                if (fm.stringWidth(testStr) <= availableWidth) {
                    return testStr;
                }
            }
            
            // Si incluso la última carpeta no cabe, devolvemos lo mínimo
            if (nameCount > 0) {
                return prefix + "...\\" + p.getName(nameCount - 1).toString() + suffix;
            }
            
            return prefix + "...\\" + suffix;
        } catch (Exception e) {
            return prefix + pathStr + suffix;
        }
    }

    private void actualizarIndiceTotal() {
        JLabel label = registry.get("label.info.indiceTotal");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, true);
        if (label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "0/0";
            if (model.getCurrentWorkMode() == modelo.VisorModel.WorkMode.CLIENTE) {
                display = obtenerIndiceModoCliente();
            } else {
                int indice = -1, total = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
                if (model.getSelectedImageKey() != null && total > 0) indice = model.getModeloLista().indexOf(model.getSelectedImageKey());
                if (total > 0 && indice != -1) display = (indice + 1) + "/" + total;
            }
            label.setText("Idx: " + display);
        }
    } // --- Fin del método actualizarIndiceTotal ---

    private String obtenerIndiceModoCliente() {
        java.awt.Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner instanceof JTable) {
            JTable t = (JTable) focusOwner;
            int total = t.getRowCount();
            int selRow = t.getSelectedRow();
            if (total > 0 && selRow >= 0) return (selRow + 1) + "/" + total;
        }
        // Fallback: buscar la primera tabla con una fila seleccionada
        String[] tableKeys = {"table.cliente.proyecto.seleccion", "table.cliente.proyecto.descartes",
                              "table.cliente.cliente.seleccion", "table.cliente.cliente.descartes"};
        for (String key : tableKeys) {
            JTable t = registry.get(key);
            if (t != null && t.getRowCount() > 0 && t.getSelectedRow() >= 0) {
                return (t.getSelectedRow() + 1) + "/" + t.getRowCount();
            }
        }
        return "0/0";
    } // --- FIN de metodo obtenerIndiceModoCliente ---

    private void actualizarDimensiones() {
        JLabel label = registry.get("label.info.dimensiones");
        if (label == null) return;
        
        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            BufferedImage img = model.getCurrentImage();
            if (img != null) display = img.getWidth() + "x" + img.getHeight();
            label.setText("Dim: " + display);
        }
    } // --- Fin del método actualizarDimensiones ---

    private void actualizarTamanoArchivo() {
        JLabel label = registry.get("label.info.tamano");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            Path ruta = resolvePath(model.getSelectedImageKey());
            if (ruta != null && Files.exists(ruta)) {
                try {
                    display = formatFileSize(Files.size(ruta));
                } catch (IOException ex) { display = "Error"; }
            }
            label.setText("Tam: " + display);
        }
    } // --- Fin del método actualizarTamanoArchivo ---

    private void actualizarFechaArchivo() {
        JLabel label = registry.get("label.info.fecha");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            Path ruta = resolvePath(model.getSelectedImageKey());
            if (ruta != null && Files.exists(ruta)) {
                try {
                    display = sdfFechaArchivo.format(new Date(Files.getLastModifiedTime(ruta).toMillis()));
                } catch (IOException ex) { display = "Error"; }
            }
            label.setText("Fch: " + display);
        }
    } // --- Fin del método actualizarFechaArchivo ---

    private void actualizarFormatoImagen() {
        JLabel label = registry.get("label.info.formatoImagen");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, false);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            Path ruta = resolvePath(model.getSelectedImageKey());
            if (ruta != null) display = ImageUtils.getImageFormat(ruta);
            label.setText("Fmt: " + (display != null ? display.toUpperCase() : "N/A"));
        }
    } // --- Fin del método actualizarFormatoImagen ---

    private void actualizarModoZoom() {
        JLabel label = registry.get("label.info.modoZoom");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = (model.getCurrentZoomMode() != null) ? model.getCurrentZoomMode().getNombreLegible() : "N/A";
            label.setText("Modo: " + display);
        }
    } // --- Fin del método actualizarModoZoom ---

    
    private void actualizarPorcentajeZoom() {
    	
    	logger.debug("### DEBUG: InfobarImageManager.actualizarPorcentajeZoom() - INICIO");
    	
        JLabel label = registry.get("label.info.porcentajeZoom");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            
            // La única condición es que haya una imagen para tener un zoom.
            if (model.getCurrentImage() != null) {
                // Se lee el valor final y real directamente del modelo.
                // Este es el valor que el ZoomManager ha calculado y que el 
                // ImageDisplayPanel usa para pintar.
                double zoomActualEnModelo = model.getZoomFactor();
                display = String.format("%.0f%%", zoomActualEnModelo * 100);
            }
            
            label.setText("%Z: " + display);
        }
    }
    // --- FIN del método actualizarPorcentajeZoom ---
    

    /**
     * Formatea un tamaño de archivo en bytes a una cadena legible (KB, MB, GB, etc.).
     * @param bytes El tamaño del archivo en bytes.
     * @return Una cadena de texto formateada.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
    } // --- Fin del método formatFileSize ---
    
    
    /**
     * Se ejecuta cuando el tema de la aplicación cambia.
     * Vuelve a aplicar el estilo personalizado a la barra de estado superior.
     * @param newTheme El nuevo tema que se ha aplicado.
     */
    @Override
    public void onThemeChanged(Tema newTheme) {
        logger.debug("Cambio de tema detectado en InfobarImageManager. Re-aplicando estilo.");
        applyCustomStatusBarStyle();
    } // ---FIN de metodo [onThemeChanged]---

    
    /**
     * Recorre un componente contenedor y todos sus hijos recursivamente
     * para aplicarles un color de texto (foreground) específico.
     * @param container El componente raíz desde el que empezar a aplicar colores.
     * @param color El color de texto a aplicar.
     */
    private void actualizarColoresDeTextoRecursivamente(java.awt.Container container, Color color) {
        for (java.awt.Component component : container.getComponents()) {
            if (component.isEnabled()) { // Solo cambiamos el color de componentes habilitados
                component.setForeground(color);
            }
            if (component instanceof java.awt.Container) {
                actualizarColoresDeTextoRecursivamente((java.awt.Container) component, color);
            }
        }
    } // --- fin del método actualizarColoresDeTextoRecursivamente ---
    
    
    /**
     * Aplica el estilo de fondo y borde a la barra de estado superior.
     * Lee el color personalizado del UIManager y lo aplica. Si no existe,
     * restaura el estilo por defecto del tema actual.
     */
    private void applyCustomStatusBarStyle() {
        JPanel panel = registry.get("panel.info.superior");
        if (panel == null) return;

        // Buscamos los colores personalizados definidos en los .properties
        Color customBackgroundColor = UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND);
        Color customForegroundColor = UIManager.getColor(ThemeManager.KEY_STATUSBAR_FOREGROUND); // <-- AÑADIDO

        if (customBackgroundColor != null) {
            // Si el tema actual define este color (ej: "Obsidian Orange"), lo usamos.
            panel.setBackground(customBackgroundColor);
            panel.setBorder(new LineBorder(customBackgroundColor, 2));
            
            // --- INICIO DE LA CORRECCIÓN ---
            if (customForegroundColor != null) {
                actualizarColoresDeTextoRecursivamente(panel, customForegroundColor);
            }
            // --- FIN DE LA CORRECCIÓN ---
            
        } else {
            // Si el tema NO define el color (ej: "Cyan Light"), restauramos el estilo por defecto.
            panel.setBackground(UIManager.getColor("Panel.background"));
            panel.setBorder(UIManager.getBorder("Panel.border"));
            
            // --- INICIO DE LA CORRECCIÓN ---
            // También restauramos el color del texto al por defecto
            Color defaultForegroundColor = UIManager.getColor("Label.foreground");
            if (defaultForegroundColor != null) {
                actualizarColoresDeTextoRecursivamente(panel, defaultForegroundColor);
            }
            // --- FIN DE LA CORRECCIÓN ---
        }
        
        panel.revalidate();
        panel.repaint();
    } // ---FIN de metodo [applyCustomStatusBarStyle]---

    private Path resolvePath(String selectedKey) {
        if (selectedKey == null) return null;
        Path ruta = model.getRutaCompleta(selectedKey);
        if (ruta == null) {
            try {
                ruta = Path.of(selectedKey);
            } catch (Exception e) {
                return null;
            }
        }
        return ruta;
    } // --- FIN de metodo resolvePath ---
    
} // --- Fin de la clase InfobarImageManager ---