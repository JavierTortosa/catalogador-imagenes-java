package controlador.managers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import utils.ImageUtils;

/**
 * Gestiona la actualización de la barra de información superior, que muestra
 * detalles sobre la imagen actualmente seleccionada.
 * Esta clase es responsable de leer el estado del modelo y la configuración,
 * y de reflejarlo en los componentes de la UI correspondientes.
 */
public class InfobarImageManager {

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
        JLabel label = registry.get("label.info.nombreArchivo");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, true);
        if (label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            String display = "N/A";
            Path ruta = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
            if (ruta != null) {
                String formato = configuration.getString(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
                display = "ruta_completa".equalsIgnoreCase(formato) ? ruta.toString() : ruta.getFileName().toString();
            } else if (model.getCarpetaRaizActual() != null) {
                display = "Carpeta: " + model.getCarpetaRaizActual().getFileName().toString();
            }
            label.setText(display);
            label.setToolTipText(display);
        }
    } // --- Fin del método actualizarNombreArchivo ---

    private void actualizarIndiceTotal() {
        JLabel label = registry.get("label.info.indiceTotal");
        if (label == null) return;

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, true);
        if (label.isVisible() != esVisible) label.setVisible(esVisible);

        if (esVisible) {
            int indice = -1, total = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
            if (model.getSelectedImageKey() != null && total > 0) indice = model.getModeloLista().indexOf(model.getSelectedImageKey());
            String display = (total > 0 && indice != -1) ? (indice + 1) + "/" + total : "0/0";
            label.setText("Idx: " + display);
        }
    } // --- Fin del método actualizarIndiceTotal ---

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
            Path ruta = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
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
            Path ruta = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
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
            Path ruta = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
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

//    private void actualizarPorcentajeZoom() {
//        JLabel label = registry.get("label.info.porcentajeZoom");
//        if (label == null) return;
//
//        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, true);
//        if(label.isVisible() != esVisible) label.setVisible(esVisible);
//
//        if (esVisible) {
//            String display = "N/A";
//            BufferedImage img = model.getCurrentImage();
//            
//            if (img != null) {
//                // --- INICIO LÓGICA DE CÁLCULO DEL ZOOM VISUAL REAL ---
//                double zoomVisualReal = model.getZoomFactor(); // Partimos del zoom de intención
//
//                // Si el BMP está activo, debemos comprobar si hubo un reajuste
//                if (model.isMantenerProporcion()) {
//                    JPanel displayPanel = registry.get("panel.display.imagen");
//                    if (displayPanel != null && displayPanel.getWidth() > 0 && displayPanel.getHeight() > 0) {
//                        int panelW = displayPanel.getWidth();
//                        int panelH = displayPanel.getHeight();
//                        int imgW = img.getWidth();
//                        int imgH = img.getHeight();
//
//                        // Calculamos las dimensiones que la imagen TENDRÍA con el zoom de intención
//                        int tempW = (int) (imgW * zoomVisualReal);
//                        int tempH = (int) (imgH * zoomVisualReal);
//
//                        // Si se sale de la pantalla, hubo un reajuste
//                        if (tempW > panelW || tempH > panelH) {
//                            // Calculamos el ratio de ajuste real que se aplicó
//                            double ratioAjuste = Math.min((double) panelW / tempW, (double) panelH / tempH);
//                            zoomVisualReal *= ratioAjuste; // Aplicamos el ajuste para obtener el zoom visual real
//                        }
//                    }
//                }
//                display = String.format("%.0f%%", zoomVisualReal * 100);
//                // --- FIN LÓGICA DE CÁLCULO ---
//            }
//            label.setText("%Z: " + display);
//        }
//    } // --- Fin del método actualizarPorcentajeZoom ---
    
    
    private void actualizarPorcentajeZoom() {
    	
    	// LOG ### DEBUG: InfobarImageManager.actualizarPorcentajeZoom() - INICIO"
    	System.out.println("### DEBUG: InfobarImageManager.actualizarPorcentajeZoom() - INICIO");
    	
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

} // --- Fin de la clase ImageInfoManager ---