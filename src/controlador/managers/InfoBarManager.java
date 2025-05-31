package controlador.managers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
// import java.awt.Dimension; // No se usa directamente aquí
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.zoom.ZoomModeEnum;
import utils.ImageUtils;
import vista.VisorView;
import vista.config.ViewUIConfig;

public class InfoBarManager {

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS Y ESTADO) ---
    private final VisorModel model;
    private final VisorView view;
    private final ViewUIConfig uiConfig;
    private final ProjectManager projectService;

    private final DecimalFormat dfPorcentajeZoomLabel = new DecimalFormat("0'%'");
    private final SimpleDateFormat sdfFechaArchivo = new SimpleDateFormat("dd/MM/yy HH:mm");

    private ImageIcon zoomFitWidthIcon;
    private ImageIcon zoomFitHeightIcon;
    private ImageIcon zoomFitPageIcon;
    private ImageIcon zoomActualSizeIcon;
    private ImageIcon zoomUserSpecifiedIcon;
    private ImageIcon zoomFixedIcon;
    private final Map<String, String> comandoToClaveIconoMap;


    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * 2.0. Constructor de InfoBarManager.
     */
    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig,
                          Map<String, String> comandoToClaveIconoMap, ProjectManager projectService) {
        // 2.1. Asignar dependencias principales
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
        this.projectService = Objects.requireNonNull(projectService, "ProjectManager no puede ser null en InfoBarManager");

        // 2.2. Log de inicialización
        System.out.println("[InfoBarManager] Instancia creada. Configurando componentes...");

        // 2.3. Cargar iconos necesarios
        cargarIconosModoZoom();

        // 2.4. Configurar listeners para controles interactivos de las barras
        configurarListenersControlesZoomInferior();

        System.out.println("[InfoBarManager] Configuración de componentes finalizada.");
    }


    // --- SECCIÓN 3: MÉTODOS DE ACTUALIZACIÓN DE BARRAS ---
    /**
     * 3.1. Método principal para solicitar la actualización de todas las barras de información.
     *      Se asegura de que la actualización se realice en el Event Dispatch Thread (EDT).
     */
    public void actualizarBarrasDeInfo() {
        if (view == null || model == null || uiConfig == null || uiConfig.configurationManager == null) {
            System.err.println("ERROR [InfoBarManager.actualizarBarrasDeInfo]: Dependencias nulas. No se pueden actualizar barras.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            actualizarBarraInfoSuperiorInterno();
            actualizarBarraEstadoInferiorInterno();
        });
    }

    /**
     * 3.2. Método interno para actualizar los componentes de la barra de información superior.
     *      Lee la configuración de visibilidad para CADA ELEMENTO.
     */
    private void actualizarBarraInfoSuperiorInterno() {
        ConfigurationManager cfg = uiConfig.configurationManager;

        // 3.2.1. Visibilidad del PANEL COMPLETO de la Barra Superior
        boolean panelBarraSuperiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_VISIBLE, true);
        if (view.getPanelBarraSuperior() != null) {
            view.getPanelBarraSuperior().setVisible(panelBarraSuperiorVisible);
        } else {
            System.err.println("WARN [InfoBarManager]: view.getPanelBarraSuperior() es null. No se puede gestionar visibilidad del panel.");
            if (!panelBarraSuperiorVisible) return;
        }
        if (!panelBarraSuperiorVisible) return; // Si el panel está oculto, no procesar sus componentes internos.

        // 3.2.2. Nombre del Archivo / Ruta (Superior)
        JLabel nombreArchivoLabelSup = view.getNombreArchivoInfoLabel();
        if (nombreArchivoLabelSup != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_VISIBLE, true);
            nombreArchivoLabelSup.setVisible(esVisible);
            if (esVisible) {
                String nombreArchivoDisplay = "N/A";
                Path rutaCompletaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
                if (rutaCompletaActual != null) {
                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_FORMATO, "solo_nombre");
                    nombreArchivoDisplay = "ruta_completa".equalsIgnoreCase(formato) ? rutaCompletaActual.toString() : rutaCompletaActual.getFileName().toString();
                } else if (model.getSelectedImageKey() != null) {
                    nombreArchivoDisplay = model.getSelectedImageKey() + " (Ruta no enc.)";
                } else if (model.getCarpetaRaizActual() != null){
                     nombreArchivoDisplay = "Carpeta: " + model.getCarpetaRaizActual().getFileName().toString();
                }
                nombreArchivoLabelSup.setText(nombreArchivoDisplay);
                nombreArchivoLabelSup.setToolTipText(nombreArchivoDisplay);
            } else {
                nombreArchivoLabelSup.setText("");
            }
        }

        // 3.2.3. Índice Actual / Total de Imágenes
        JLabel indiceTotalLabel = view.getIndiceTotalInfoLabel();
        if (indiceTotalLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_INDICE_TOTAL_VISIBLE, true);
            indiceTotalLabel.setVisible(esVisible);
            if (esVisible) {
                int indiceActual = -1, totalImagenes = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
                if (model.getSelectedImageKey() != null && totalImagenes > 0) indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
                String display = (totalImagenes > 0 && indiceActual != -1) ? (indiceActual + 1) + "/" + totalImagenes : (totalImagenes > 0 ? "-/" + totalImagenes : "0/0");
                indiceTotalLabel.setText("Idx: " + display);
            } else {
                indiceTotalLabel.setText("");
            }
        }

        // 3.2.4. Dimensiones Originales de la Imagen
        JLabel dimensionesLabel = view.getDimensionesOriginalesInfoLabel();
        if (dimensionesLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_DIMENSIONES_VISIBLE, true);
            dimensionesLabel.setVisible(esVisible);
            if (esVisible) {
                String dimsDisplay = "N/A";
                BufferedImage imgOriginal = model.getCurrentImage();
                if (imgOriginal != null) dimsDisplay = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
                dimensionesLabel.setText("Dim: " + dimsDisplay);
            } else {
                dimensionesLabel.setText("");
            }
        }

        // 3.2.5. Tamaño del Archivo
        JLabel tamanoArchivoLabel = view.getTamanoArchivoInfoLabel();
        if (tamanoArchivoLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_TAMANO_ARCHIVO_VISIBLE, true);
            tamanoArchivoLabel.setVisible(esVisible);
            if (esVisible) {
                String tamanoDisplay = "N/A";
                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
                if (rutaActual != null && Files.exists(rutaActual)) {
                    try { tamanoDisplay = formatFileSize(Files.size(rutaActual)); }
                    catch (IOException ex) { System.err.println("Error al leer tamaño para " + rutaActual + ": " + ex.getMessage());}
                }
                tamanoArchivoLabel.setText("Tam: " + tamanoDisplay);
            } else {
                tamanoArchivoLabel.setText("");
            }
        }

        // 3.2.6. Fecha de Modificación del Archivo
        JLabel fechaArchivoLabel = view.getFechaArchivoInfoLabel();
        if (fechaArchivoLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FECHA_ARCHIVO_VISIBLE, true);
            fechaArchivoLabel.setVisible(esVisible);
            if (esVisible) {
                String fechaDisplay = "N/A";
                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
                if (rutaActual != null && Files.exists(rutaActual)) {
                    try { fechaDisplay = sdfFechaArchivo.format(new Date(Files.getLastModifiedTime(rutaActual).toMillis())); }
                    catch (IOException ex) { System.err.println("Error al leer fecha para " + rutaActual + ": " + ex.getMessage());}
                }
                fechaArchivoLabel.setText("Fch: " + fechaDisplay);
            } else {
                fechaArchivoLabel.setText("");
            }
        }

        // 3.2.7. Formato de Imagen
        JLabel formatoImagenLabel = view.getFormatoImagenInfoLabel();
        if (formatoImagenLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FORMATO_IMAGEN_VISIBLE, false);
            formatoImagenLabel.setVisible(esVisible);
            if (esVisible) {
                String formatoDisplay = "N/A";
                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
                if (rutaActual != null) formatoDisplay = ImageUtils.getImageFormat(rutaActual);
                formatoImagenLabel.setText("Fmt: " + (formatoDisplay != null ? formatoDisplay.toUpperCase() : "N/A"));
            } else {
                formatoImagenLabel.setText("");
            }
        }

        // 3.2.8. Nombre del Modo de Zoom Activo
        JLabel modoZoomNombreLabel = view.getModoZoomNombreInfoLabel();
        if (modoZoomNombreLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_MODO_ZOOM_VISIBLE, true);
            modoZoomNombreLabel.setVisible(esVisible);
            if (esVisible) {
                String modoZoomDisplay = (model.getCurrentZoomMode() != null) ? model.getCurrentZoomMode().getNombreLegible() : "N/A";
                modoZoomNombreLabel.setText("Modo: " + modoZoomDisplay);
            } else {
                modoZoomNombreLabel.setText("");
            }
        }

        // 3.2.9. Porcentaje de Zoom Visual Resultante
        JLabel zoomRealPctLabel = view.getPorcentajeZoomVisualRealInfoLabel();
        if (zoomRealPctLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_ZOOM_REAL_PCT_VISIBLE, true);
            zoomRealPctLabel.setVisible(esVisible);
            if (esVisible) {
                String zoomPctDisplay = (model.getCurrentImage() != null) ? String.format("%.0f%%", model.getZoomFactor() * 100) : "N/A";
                zoomRealPctLabel.setText("%Z: " + zoomPctDisplay);
            } else {
                zoomRealPctLabel.setText("");
            }
        }

        // 3.2.10. Revalidar y repintar el panel de la barra superior
        if (view.getPanelBarraSuperior() != null && view.getPanelBarraSuperior().isVisible()) {
            view.getPanelBarraSuperior().revalidate();
            view.getPanelBarraSuperior().repaint();
        }
    }

    /**
     * 3.3. Método interno para actualizar los componentes de la barra de estado/control inferior.
     */
    private void actualizarBarraEstadoInferiorInterno() {
        ConfigurationManager cfg = uiConfig.configurationManager;

        // 3.3.1. Visibilidad del PANEL COMPLETO de la Barra Inferior
        boolean panelBarraInferiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_VISIBLE, true);
        if (view.getPanelBarraEstado() != null) {
            view.getPanelBarraEstado().setVisible(panelBarraInferiorVisible);
        } else {
            System.err.println("WARN [InfoBarManager]: view.getPanelBarraEstado() es null.");
            if (!panelBarraInferiorVisible) return;
        }
        if (!panelBarraInferiorVisible) return;

        Color colorFondoActivo = (uiConfig.colorBotonActivado != null) ? uiConfig.colorBotonActivado : Color.CYAN;
        Color colorFondoInactivo = (uiConfig.colorFondoSecundario != null) ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY;

        // 3.3.2. Ruta Completa del Archivo (Inferior)
        JLabel rutaCompletaLabel = view.getRutaCompletaArchivoLabel();
        if (rutaCompletaLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_VISIBLE, true);
            rutaCompletaLabel.setVisible(esVisible);
            if (esVisible) {
                String rutaTextoDisplay = "(Ninguna imagen sel.)";
                Path rutaActualParaBarra = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
                if (rutaActualParaBarra != null) {
                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_FORMATO, "ruta_completa");
                    rutaTextoDisplay = "solo_nombre".equalsIgnoreCase(formato) ? rutaActualParaBarra.getFileName().toString() : rutaActualParaBarra.toString();
                    if (projectService != null && projectService.estaMarcada(rutaActualParaBarra)) rutaTextoDisplay += " [MARCADA]";
                } else if (model.getCarpetaRaizActual() != null) {
                    rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
                }
                rutaCompletaLabel.setText(rutaTextoDisplay);
                rutaCompletaLabel.setToolTipText(rutaTextoDisplay);
            } else {
                rutaCompletaLabel.setText("");
            }
        }

        // 3.3.3. Icono Zoom Manual (ZM)
        JLabel zmIconLabel = view.getIconoZoomManualLabel();
        if (zmIconLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_ZM_VISIBLE, true);
            zmIconLabel.setVisible(esVisible);
            if (esVisible) {
                boolean zoomManualActivo = model.isZoomHabilitado();
                zmIconLabel.setToolTipText(zoomManualActivo ? "Zoom Manual: Activado (Click para cambiar)" : "Zoom Manual: Desactivado (Click para cambiar)");
                zmIconLabel.setBackground(zoomManualActivo ? colorFondoActivo : colorFondoInactivo);
                zmIconLabel.setOpaque(true);
            }
        }

        // 3.3.4. Icono Mantener Proporciones (Prop)
        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
        if (propIconLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_PROP_VISIBLE, true);
            propIconLabel.setVisible(esVisible);
            if (esVisible) {
                boolean proporcionesActivas = model.isMantenerProporcion();
                propIconLabel.setToolTipText(proporcionesActivas ? "Mantener Proporciones: Activado (Click para cambiar)" : "Mantener Proporciones: Desactivado (Click para cambiar)");
                propIconLabel.setBackground(proporcionesActivas ? colorFondoActivo : colorFondoInactivo);
                propIconLabel.setOpaque(true);
            }
        }

        // 3.3.5. Icono Modo Subcarpetas (SubC)
        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
        if (subcIconLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_SUBC_VISIBLE, true);
            subcIconLabel.setVisible(esVisible);
            if (esVisible) {
                boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
                subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado (Click para cambiar)" : "Incluir Subcarpetas: Desactivado (Click para cambiar)");
                subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
                subcIconLabel.setOpaque(true);
            }
        }

        // 3.3.6. Control % Zoom (JLabel clickeable)
        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
        if (porcentajeLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_ZOOM_PCT_VISIBLE, true);
            porcentajeLabel.setVisible(esVisible);
            if (esVisible) {
                double customZoomPercent = cfg.getZoomPersonalizadoPorcentaje();
                String textoPct = dfPorcentajeZoomLabel.format(customZoomPercent);
                porcentajeLabel.setText(textoPct);
                porcentajeLabel.setToolTipText("Zoom Personalizado: " + textoPct + "% (Click para cambiar)");
            } else {
                porcentajeLabel.setText("");
            }
        }

        // 3.3.7. Control Modo Zoom (JButton con icono)
        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
        if (modoZoomBoton != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_MODO_ZOOM_VISIBLE, true);
            modoZoomBoton.setVisible(esVisible);
            if (esVisible) {
                if (model.getCurrentZoomMode() != null) {
                    // ... (lógica para setIcon y setToolTipText como antes) ...
                    ZoomModeEnum currentMode = model.getCurrentZoomMode();
                    ImageIcon iconToShow = null; String tooltipText = "Modo Zoom: ";
                    switch (currentMode) {
                        case FIT_TO_WIDTH: iconToShow = zoomFitWidthIcon; tooltipText += "Ajustar Ancho"; break;
                        case FIT_TO_HEIGHT: iconToShow = zoomFitHeightIcon; tooltipText += "Ajustar Alto"; break;
                        case FIT_TO_SCREEN: iconToShow = zoomFitPageIcon; tooltipText += "Ajustar Página"; break;
                        case DISPLAY_ORIGINAL: iconToShow = zoomActualSizeIcon; tooltipText += "Tamaño Real"; break;
                        case USER_SPECIFIED_PERCENTAGE: iconToShow = zoomUserSpecifiedIcon; tooltipText += "Personalizado %"; break;
                        case MAINTAIN_CURRENT_ZOOM: iconToShow = zoomFixedIcon; tooltipText += "Mantener Actual"; break;
                        default: tooltipText += "Desconocido"; break;
                    }
                    modoZoomBoton.setIcon(iconToShow);
                    modoZoomBoton.setToolTipText(tooltipText + " (Click para cambiar)");
                } else {
                    modoZoomBoton.setIcon(null); modoZoomBoton.setToolTipText("Modo no definido");
                }
            }
        }

        // 3.3.8. Área de Mensajes de la Aplicación
        JLabel mensajesLabel = view.getMensajesAppLabel();
        if (mensajesLabel != null) {
            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_MENSAJES_APP_VISIBLE, true);
            mensajesLabel.setVisible(esVisible);
            if (esVisible) {
                // mensajesLabel.setText(model.getAppStatusMessage() != null ? model.getAppStatusMessage() : "Listo.");
            } else {
                mensajesLabel.setText("");
            }
        }

        // 3.3.9. Revalidar y repintar el panel de la barra inferior
        if (view.getPanelBarraEstado() != null && view.getPanelBarraEstado().isVisible()) {
            view.getPanelBarraEstado().revalidate();
            view.getPanelBarraEstado().repaint();
        }
    }


    // --- SECCIÓN 4: MÉTODOS PRIVADOS AUXILIARES (Popups, carga de iconos, formato de tamaño) ---

    /**
     * 4.1. Configura los listeners para los controles de zoom interactivos en la barra inferior.
     */
    private void configurarListenersControlesZoomInferior() {
        // ... (tu código existente para este método) ...
        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
        if (porcentajeLabel != null) {
            porcentajeLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { showCustomZoomPercentagePopup(porcentajeLabel); }
                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
            });
        }
        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
        if (modoZoomBoton != null) {
            modoZoomBoton.addActionListener(e -> showZoomModeSelectionPopup(modoZoomBoton));
        }
    }

    /**
     * 4.2. Carga los iconos para los diferentes modos de zoom usados en la barra inferior.
     */
    private void cargarIconosModoZoom() {
        // ... (tu código existente para este método) ...
        if (uiConfig.iconUtils != null && this.comandoToClaveIconoMap != null) {
            int iconSize = (uiConfig.iconoAncho > 0) ? uiConfig.iconoAncho : 16;
            java.util.function.Function<String, ImageIcon> getIcon = (comando) -> {
                String claveIcono = this.comandoToClaveIconoMap.get(comando);
                if (claveIcono != null) return uiConfig.iconUtils.getScaledIcon(claveIcono, iconSize, iconSize);
                System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: No se encontró clave de icono para comando: " + comando);
                return null;
            };
            zoomFitPageIcon       = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR);
            zoomActualSizeIcon    = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AUTO);
            zoomFitWidthIcon      = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ANCHO);
            zoomFitHeightIcon     = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ALTO);
            zoomUserSpecifiedIcon = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
            zoomFixedIcon         = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_FIJO);
        } else { System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: IconUtils o comandoToClaveIconoMap nulos."); }
    }

    /**
     * 4.3. Muestra un JPopupMenu para que el usuario seleccione o introduzca un porcentaje de zoom personalizado.
     */
    private void showCustomZoomPercentagePopup(Component anchorComponent) {
        // ... (tu código existente para este método, asegurándote de que usa this.view y this.uiConfig) ...
        if (uiConfig == null || uiConfig.configurationManager == null || uiConfig.actionMap == null || view == null) {
            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar el zoom.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JPopupMenu percentageMenu = new JPopupMenu("Seleccionar Porcentaje de Zoom");
        int[] predefinedPercentages = {25, 50, 75, 100, 125, 150, 200, 300, 400, 500};
        for (int percent : predefinedPercentages) {
            JMenuItem item = new JMenuItem(percent + "%");
            item.addActionListener(e -> {
                uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percent);
                Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
                if (aplicarUserSpecifiedAction != null) {
                    aplicarUserSpecifiedAction.actionPerformed(new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
                }
            });
            percentageMenu.add(item);
        }
        percentageMenu.addSeparator();
        JMenuItem otherItem = new JMenuItem("Otro...");
        otherItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(view.getFrame(), "Introduce el porcentaje de zoom (ej: 150):", "Zoom Personalizado", JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                try {
                    input = input.replace("%", "").trim();
                    double percentValue = Double.parseDouble(input);
                    if (percentValue >= 1 && percentValue <= 5000) {
                        uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percentValue);
                        Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
                        if (aplicarUserSpecifiedAction != null) {
                            aplicarUserSpecifiedAction.actionPerformed(new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
                        }
                    } else { JOptionPane.showMessageDialog(view.getFrame(), "Porcentaje inválido.", "Error", JOptionPane.ERROR_MESSAGE); }
                } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(view.getFrame(), "Entrada inválida.", "Error", JOptionPane.ERROR_MESSAGE); }
            }
        });
        percentageMenu.add(otherItem);
        percentageMenu.show(anchorComponent, 0, anchorComponent.getHeight());
    }

    /**
     * 4.4. Muestra un JPopupMenu para que el usuario seleccione un nuevo modo de zoom.
     */
    private void showZoomModeSelectionPopup(Component anchorComponent) {
        // ... (tu código existente para este método, asegurándote de que usa this.uiConfig) ...
        if (model == null || view == null || uiConfig.actionMap == null) {
            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar modos de zoom.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JPopupMenu zoomMenu = new JPopupMenu("Seleccionar Modo de Zoom");
        Map<String, Action> actionMapRef = uiConfig.actionMap;
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR), zoomFitPageIcon);
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AUTO), zoomActualSizeIcon);
        // ... (resto de addZoomModeMenuItem)
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ANCHO), zoomFitWidthIcon);
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ALTO), zoomFitHeightIcon);
        zoomMenu.addSeparator();
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO), zoomUserSpecifiedIcon);
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_FIJO), zoomFixedIcon);
        zoomMenu.show(anchorComponent, 0, anchorComponent.getHeight());
    }

    /**
     * 4.5. Método helper para añadir un JMenuItem a un JPopupMenu.
     */
    private void addZoomModeMenuItem(JPopupMenu menu, Action action, ImageIcon icon) {
        // ... (tu código existente para este método) ...
        if (action != null) {
            JMenuItem menuItem = new JMenuItem(action);
            if (icon != null) {
                menuItem.setIcon(icon);
            }
            menu.add(menuItem);
        }
    }

    /**
     * 4.6. Formatea un tamaño de archivo en bytes a una cadena legible (KB, MB, GB, etc.).
     */
    private String formatFileSize(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = (exp > 0 && exp <= "KMGTPE".length()) ? "KMGTPE".charAt(exp - 1) + "" : "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
    }
}

//package controlador.managers;
//
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Cursor;
//// import java.awt.Dimension; // No se usa directamente aquí, pero podría ser útil en popups
//import java.awt.event.ActionEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.attribute.FileTime;
//import java.text.DecimalFormat;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Map;
//import java.util.Objects;
//
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JMenuItem;
//import javax.swing.JOptionPane;
//import javax.swing.JPopupMenu;
//import javax.swing.SwingUtilities; // Para invokeLater
//
//import controlador.commands.AppActionCommands;
//import modelo.VisorModel;
//import servicios.ConfigurationManager;
//import servicios.ProjectManager;
//import servicios.zoom.ZoomModeEnum;
//import utils.ImageUtils; // Necesitarás esta clase de utilidad
//import vista.VisorView;
//import vista.config.ViewUIConfig;
//
//public class InfoBarManager {
//
//    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS Y ESTADO) ---
//    private final VisorModel model;
//    private final VisorView view;
//    private final ViewUIConfig uiConfig;
//    private final ProjectManager projectService; // Para la marca de proyecto
//
//    private final DecimalFormat dfPorcentajeZoomLabel = new DecimalFormat("0'%'");
//    private final SimpleDateFormat sdfFechaArchivo = new SimpleDateFormat("dd/MM/yy HH:mm");
//
//    // Iconos para el botón de modo de zoom
//    private ImageIcon zoomFitWidthIcon;
//    private ImageIcon zoomFitHeightIcon;
//    private ImageIcon zoomFitPageIcon;
//    private ImageIcon zoomActualSizeIcon;
//    private ImageIcon zoomUserSpecifiedIcon;
//    private ImageIcon zoomFixedIcon;
//    private final Map<String, String> comandoToClaveIconoMap;
//
//
//    // --- SECCIÓN 2: CONSTRUCTOR ---
//    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig,
//                          Map<String, String> comandoToClaveIconoMap, ProjectManager projectService) {
//        // 2.1. Asignar dependencias principales
//        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
//        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
//        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
//        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
//        this.projectService = Objects.requireNonNull(projectService, "ProjectManager no puede ser null en InfoBarManager");
//
//        // 2.2. Log de inicialización
//        System.out.println("[InfoBarManager] Instancia creada. Configurando componentes...");
//
//        // 2.3. Cargar iconos necesarios
//        cargarIconosModoZoom();
//
//        // 2.4. Configurar listeners para controles interactivos de las barras
//        configurarListenersControlesZoomInferior();
//
//        System.out.println("[InfoBarManager] Configuración de componentes finalizada.");
//    }
//
//
//    // --- SECCIÓN 3: MÉTODOS DE ACTUALIZACIÓN DE BARRAS ---
//    /**
//     * 3.1. Método principal para solicitar la actualización de todas las barras de información.
//     *      Se asegura de que la actualización se realice en el Event Dispatch Thread (EDT).
//     */
//    public void actualizarBarrasDeInfo() {
//        if (view == null || model == null || uiConfig == null || uiConfig.configurationManager == null) {
//            System.err.println("ERROR [InfoBarManager.actualizarBarrasDeInfo]: Dependencias nulas. No se pueden actualizar barras.");
//            return;
//        }
//        SwingUtilities.invokeLater(() -> {
//            // System.out.println("  [InfoBarManager EDT] Ejecutando actualización de barras..."); // Log opcional
//            actualizarBarraInfoSuperiorInterno();
//            actualizarBarraEstadoInferiorInterno();
//            // System.out.println("  [InfoBarManager EDT] Actualización de barras completada."); // Log opcional
//        });
//    }
//
//    /**
//     * 3.2. Método interno para actualizar los componentes de la barra de información superior.
//     *      Se asume que se llama desde el EDT.
//     *      Lee la configuración de visibilidad para CADA ELEMENTO.
//     */
//    private void actualizarBarraInfoSuperiorInterno() {
//        ConfigurationManager cfg = uiConfig.configurationManager;
//
//        // 3.2.1. Visibilidad del PANEL COMPLETO de la Barra Superior
//        boolean panelBarraSuperiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_VISIBLE, true);
//        if (view.getPanelBarraSuperior() != null) {
//            view.getPanelBarraSuperior().setVisible(panelBarraSuperiorVisible);
//        } else {
//            System.err.println("WARN [InfoBarManager]: view.getPanelBarraSuperior() es null. No se puede gestionar visibilidad del panel.");
//            // Si el panel no existe, no podemos continuar.
//            if (!panelBarraSuperiorVisible) return; // Si además está configurado como no visible, salimos.
//        }
//
//        // Si el panel completo está oculto, no procesar sus componentes internos.
//        if (!panelBarraSuperiorVisible) {
//            // System.out.println("  [InfoBarManager] Barra Superior Completa está configurada como NO VISIBLE.");
//            return;
//        }
//
//        // 3.2.2. Nombre del Archivo / Ruta (Superior)
//        JLabel nombreArchivoLabelSup = view.getNombreArchivoInfoLabel();
//        if (nombreArchivoLabelSup != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_VISIBLE, true);
//            nombreArchivoLabelSup.setVisible(esVisible);
//            if (esVisible) {
//                String nombreArchivoDisplay = "N/A"; // Default text
//                Path rutaCompletaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//
//                if (rutaCompletaActual != null) {
//                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_FORMATO, "solo_nombre");
//                    if ("ruta_completa".equalsIgnoreCase(formato)) {
//                        nombreArchivoDisplay = rutaCompletaActual.toString();
//                    } else { // solo_nombre o default
//                        nombreArchivoDisplay = rutaCompletaActual.getFileName().toString();
//                    }
//                } else if (model.getSelectedImageKey() != null) {
//                    nombreArchivoDisplay = model.getSelectedImageKey() + " (Ruta no enc.)";
//                } else if (model.getCarpetaRaizActual() != null){
//                     nombreArchivoDisplay = "Carpeta: " + model.getCarpetaRaizActual().getFileName().toString(); // Mostrar solo nombre de carpeta
//                }
//                nombreArchivoLabelSup.setText(nombreArchivoDisplay);
//                nombreArchivoLabelSup.setToolTipText(nombreArchivoDisplay); // Añadir tooltip
//            } else {
//                nombreArchivoLabelSup.setText("");
//            }
//        }
//
//        // 3.2.3. Índice Actual / Total de Imágenes
//        JLabel indiceTotalLabel = view.getIndiceTotalInfoLabel();
//        if (indiceTotalLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_INDICE_TOTAL_VISIBLE, true);
//            indiceTotalLabel.setVisible(esVisible);
//            if (esVisible) {
//                int indiceActual = -1;
//                int totalImagenes = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
//                if (model.getSelectedImageKey() != null && totalImagenes > 0) {
//                    indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
//                }
//                String display;
//                if (totalImagenes > 0 && indiceActual != -1) display = (indiceActual + 1) + "/" + totalImagenes;
//                else if (totalImagenes > 0) display = "-/" + totalImagenes;
//                else display = "0/0";
//                indiceTotalLabel.setText("Idx: " + display);
//            } else {
//                indiceTotalLabel.setText("");
//            }
//        }
//
//        // 3.2.4. Dimensiones Originales de la Imagen
//        JLabel dimensionesLabel = view.getDimensionesOriginalesInfoLabel();
//        if (dimensionesLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_DIMENSIONES_VISIBLE, true);
//            dimensionesLabel.setVisible(esVisible);
//            if (esVisible) {
//                String dimsDisplay = "N/A";
//                BufferedImage imgOriginal = model.getCurrentImage();
//                if (imgOriginal != null) {
//                    dimsDisplay = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
//                }
//                dimensionesLabel.setText("Dim: " + dimsDisplay);
//            } else {
//                dimensionesLabel.setText("");
//            }
//        }
//
//        // 3.2.5. Tamaño del Archivo
//        JLabel tamanoArchivoLabel = view.getTamanoArchivoInfoLabel();
//        if (tamanoArchivoLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_TAMANO_ARCHIVO_VISIBLE, true);
//            tamanoArchivoLabel.setVisible(esVisible);
//            if (esVisible) {
//                String tamanoDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null && Files.exists(rutaActual)) {
//                    try {
//                        long bytes = Files.size(rutaActual);
//                        tamanoDisplay = formatFileSize(bytes);
//                    } catch (IOException ex) { System.err.println("Error al leer tamaño para " + rutaActual + ": " + ex.getMessage());}
//                }
//                tamanoArchivoLabel.setText("Tam: " + tamanoDisplay);
//            } else {
//                tamanoArchivoLabel.setText("");
//            }
//        }
//
//        // 3.2.6. Fecha de Modificación del Archivo
//        JLabel fechaArchivoLabel = view.getFechaArchivoInfoLabel();
//        if (fechaArchivoLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FECHA_ARCHIVO_VISIBLE, true);
//            fechaArchivoLabel.setVisible(esVisible);
//            if (esVisible) {
//                String fechaDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null && Files.exists(rutaActual)) {
//                    try {
//                        FileTime fileTime = Files.getLastModifiedTime(rutaActual);
//                        fechaDisplay = sdfFechaArchivo.format(new Date(fileTime.toMillis()));
//                    } catch (IOException ex) { System.err.println("Error al leer fecha para " + rutaActual + ": " + ex.getMessage());}
//                }
//                fechaArchivoLabel.setText("Fch: " + fechaDisplay);
//            } else {
//                fechaArchivoLabel.setText("");
//            }
//        }
//
//        // 3.2.7. Formato de Imagen
//        JLabel formatoImagenLabel = view.getFormatoImagenInfoLabel();
//        if (formatoImagenLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FORMATO_IMAGEN_VISIBLE, false);
//            formatoImagenLabel.setVisible(esVisible);
//            if (esVisible) {
//                String formatoDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null) {
//                    formatoDisplay = ImageUtils.getImageFormat(rutaActual);
//                }
//                formatoImagenLabel.setText("Fmt: " + (formatoDisplay != null ? formatoDisplay.toUpperCase() : "N/A"));
//            } else {
//                formatoImagenLabel.setText("");
//            }
//        }
//
//        // 3.2.8. Nombre del Modo de Zoom Activo
//        JLabel modoZoomNombreLabel = view.getModoZoomNombreInfoLabel();
//        if (modoZoomNombreLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_MODO_ZOOM_VISIBLE, true);
//            modoZoomNombreLabel.setVisible(esVisible);
//            if (esVisible) {
//                String modoZoomDisplay = "N/A";
//                if (model.getCurrentZoomMode() != null) {
//                    modoZoomDisplay = model.getCurrentZoomMode().getNombreLegible();
//                }
//                modoZoomNombreLabel.setText("Modo: " + modoZoomDisplay);
//            } else {
//                modoZoomNombreLabel.setText("");
//            }
//        }
//
//        // 3.2.9. Porcentaje de Zoom Visual Resultante
//        JLabel zoomRealPctLabel = view.getPorcentajeZoomVisualRealInfoLabel();
//        if (zoomRealPctLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_ZOOM_REAL_PCT_VISIBLE, true);
//            zoomRealPctLabel.setVisible(esVisible);
//            if (esVisible) {
//                String zoomPctDisplay = "N/A";
//                if (model.getCurrentImage() != null) {
//                    zoomPctDisplay = String.format("%.0f%%", model.getZoomFactor() * 100);
//                }
//                zoomRealPctLabel.setText("%Z: " + zoomPctDisplay);
//            } else {
//                zoomRealPctLabel.setText("");
//            }
//        }
//
//        // 3.2.10. Revalidar y repintar el panel de la barra superior
//        if (view.getPanelBarraSuperior() != null && view.getPanelBarraSuperior().isVisible()) {
//            view.getPanelBarraSuperior().revalidate();
//            view.getPanelBarraSuperior().repaint();
//        }
//    }
//
//    /**
//     * 3.3. Método interno para actualizar los componentes de la barra de estado/control inferior.
//     */
//    private void actualizarBarraEstadoInferiorInterno() {
//        ConfigurationManager cfg = uiConfig.configurationManager;
//
//        // 3.3.1. Visibilidad del PANEL COMPLETO de la Barra Inferior
//        boolean panelBarraInferiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_VISIBLE, true);
//        if (view.getPanelBarraEstado() != null) {
//            view.getPanelBarraEstado().setVisible(panelBarraInferiorVisible);
//        } else {
//            System.err.println("WARN [InfoBarManager]: view.getPanelBarraEstado() es null.");
//            if (!panelBarraInferiorVisible) return;
//        }
//
//        if (!panelBarraInferiorVisible) {
//            return;
//        }
//
//        Color colorFondoActivo = (uiConfig.colorBotonActivado != null) ? uiConfig.colorBotonActivado : Color.CYAN;
//        Color colorFondoInactivo = (uiConfig.colorFondoSecundario != null) ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY;
//
//        // 3.3.2. Ruta Completa del Archivo (Inferior)
//        JLabel rutaCompletaLabel = view.getRutaCompletaArchivoLabel();
//        if (rutaCompletaLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_VISIBLE, true);
//            rutaCompletaLabel.setVisible(esVisible);
//            if (esVisible) {
//                String rutaTextoDisplay = "(Ninguna imagen sel.)";
//                Path rutaActualParaBarra = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//
//                if (rutaActualParaBarra != null) {
//                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_FORMATO, "ruta_completa");
//                    if ("solo_nombre".equalsIgnoreCase(formato)) {
//                        rutaTextoDisplay = rutaActualParaBarra.getFileName().toString();
//                    } else {
//                        rutaTextoDisplay = rutaActualParaBarra.toString();
//                    }
//                    if (projectService != null && projectService.estaMarcada(rutaActualParaBarra)) {
//                        rutaTextoDisplay += " [MARCADA]";
//                    }
//                } else if (model.getCarpetaRaizActual() != null) {
//                    rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
//                }
//                rutaCompletaLabel.setText(rutaTextoDisplay);
//                rutaCompletaLabel.setToolTipText(rutaTextoDisplay);
//            } else {
//                rutaCompletaLabel.setText("");
//            }
//        }
//
//        // 3.3.3. Icono Zoom Manual (ZM)
//        JLabel zmIconLabel = view.getIconoZoomManualLabel();
//        if (zmIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_ZM_VISIBLE, true);
//            zmIconLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean zoomManualEstaActivo = model.isZoomHabilitado();
//                zmIconLabel.setToolTipText(zoomManualEstaActivo ? "Zoom Manual: Activado (Click para cambiar)" : "Zoom Manual: Desactivado (Click para cambiar)");
//                zmIconLabel.setBackground(zoomManualEstaActivo ? colorFondoActivo : colorFondoInactivo);
//                zmIconLabel.setOpaque(true); // Necesario para que setBackground tenga efecto en JLabels
//            }
//        }
//
//        // 3.3.4. Icono Mantener Proporciones (Prop)
//        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
//        if (propIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_PROP_VISIBLE, true);
//            propIconLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean proporcionesEstanActivas = model.isMantenerProporcion();
//                propIconLabel.setToolTipText(proporcionesEstanActivas ? "Mantener Proporciones: Activado (Click para cambiar)" : "Mantener Proporciones: Desactivado (Click para cambiar)");
//                propIconLabel.setBackground(proporcionesEstanActivas ? colorFondoActivo : colorFondoInactivo);
//                propIconLabel.setOpaque(true);
//            }
//        }
//
//        // 3.3.5. Icono Modo Subcarpetas (SubC)
//        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
//        if (subcIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_SUBC_VISIBLE, true);
//            subcIconLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
//                subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado (Click para cambiar)" : "Incluir Subcarpetas: Desactivado (Click para cambiar)");
//                subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
//                subcIconLabel.setOpaque(true);
//            }
//        }
//
//        // 3.3.6. Control % Zoom (JLabel clickeable)
//        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
//        if (porcentajeLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_ZOOM_PCT_VISIBLE, true);
//            porcentajeLabel.setVisible(esVisible);
//            if (esVisible) {
//                double customZoomPercent = cfg.getZoomPersonalizadoPorcentaje();
//                String textoPct = dfPorcentajeZoomLabel.format(customZoomPercent);
//                porcentajeLabel.setText(textoPct);
//                porcentajeLabel.setToolTipText("Zoom Personalizado: " + textoPct + " (Click para cambiar)");
//            } else {
//                porcentajeLabel.setText("");
//            }
//        }
//
//        // 3.3.7. Control Modo Zoom (JButton con icono)
//        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//        if (modoZoomBoton != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_MODO_ZOOM_VISIBLE, true);
//            modoZoomBoton.setVisible(esVisible);
//            if (esVisible) {
//                if (model.getCurrentZoomMode() != null) {
//                    ZoomModeEnum currentMode = model.getCurrentZoomMode();
//                    ImageIcon iconToShow = null;
//                    String tooltipText = "Modo Zoom: ";
//                    switch (currentMode) {
//                        case FIT_TO_WIDTH: iconToShow = zoomFitWidthIcon; tooltipText += "Ajustar Ancho"; break;
//                        case FIT_TO_HEIGHT: iconToShow = zoomFitHeightIcon; tooltipText += "Ajustar Alto"; break;
//                        case FIT_TO_SCREEN: iconToShow = zoomFitPageIcon; tooltipText += "Ajustar Página"; break;
//                        case DISPLAY_ORIGINAL: iconToShow = zoomActualSizeIcon; tooltipText += "Tamaño Real"; break;
//                        case USER_SPECIFIED_PERCENTAGE: iconToShow = zoomUserSpecifiedIcon; tooltipText += "Personalizado %"; break;
//                        case MAINTAIN_CURRENT_ZOOM: iconToShow = zoomFixedIcon; tooltipText += "Mantener Actual"; break;
//                        default: tooltipText += "Desconocido"; break;
//                    }
//                    modoZoomBoton.setIcon(iconToShow);
//                    modoZoomBoton.setToolTipText(tooltipText + " (Click para cambiar)");
//                } else { // Si no hay modo de zoom actual (raro)
//                    modoZoomBoton.setIcon(null);
//                    modoZoomBoton.setToolTipText("Modo de Zoom no definido");
//                }
//            }
//        }
//
//        // 3.3.8. Área de Mensajes de la Aplicación
//        JLabel mensajesLabel = view.getMensajesAppLabel();
//        if (mensajesLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_MENSAJES_APP_VISIBLE, true);
//            mensajesLabel.setVisible(esVisible);
//            if (esVisible) {
//                // mensajesLabel.setText(model.getAppStatusMessage() != null ? model.getAppStatusMessage() : "Listo.");
//            } else {
//                mensajesLabel.setText("");
//            }
//        }
//
//        // 3.3.9. Revalidar y repintar el panel de la barra inferior
//        if (view.getPanelBarraEstado() != null && view.getPanelBarraEstado().isVisible()) {
//            view.getPanelBarraEstado().revalidate();
//            view.getPanelBarraEstado().repaint();
//        }
//    }
//
//
//    // --- SECCIÓN 4: MÉTODOS PRIVADOS AUXILIARES (Popups, carga de iconos, formato de tamaño) ---
//
//    /**
//     * 4.1. Carga los iconos para los diferentes modos de zoom usados en la barra inferior.
//     */
//    private void cargarIconosModoZoom() {
//        if (uiConfig.iconUtils != null && this.comandoToClaveIconoMap != null) {
//            int iconSize = (uiConfig.iconoAncho > 0) ? uiConfig.iconoAncho : 16;
//            java.util.function.Function<String, ImageIcon> getIcon = (comando) -> {
//                String claveIcono = this.comandoToClaveIconoMap.get(comando);
//                if (claveIcono != null) return uiConfig.iconUtils.getScaledIcon(claveIcono, iconSize, iconSize);
//                System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: No se encontró clave de icono para comando: " + comando);
//                return null;
//            };
//            zoomFitPageIcon       = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR);
//            zoomActualSizeIcon    = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AUTO);
//            zoomFitWidthIcon      = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ANCHO);
//            zoomFitHeightIcon     = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ALTO);
//            zoomUserSpecifiedIcon = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
//            zoomFixedIcon         = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_FIJO);
//        } else {
//            System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: IconUtils o comandoToClaveIconoMap nulos.");
//        }
//    }
//
//    /**
//     * 4.2. Muestra un JPopupMenu para que el usuario seleccione o introduzca un porcentaje de zoom personalizado.
//     */
//    private void showCustomZoomPercentagePopup(Component anchorComponent) {
//        // ... (código del popup de porcentaje, sin cambios respecto a tu versión)
//        if (uiConfig == null || uiConfig.configurationManager == null || uiConfig.actionMap == null) {
//            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar el zoom.", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//        JPopupMenu percentageMenu = new JPopupMenu("Seleccionar Porcentaje de Zoom");
//        int[] predefinedPercentages = {25, 50, 75, 100, 125, 150, 200, 300, 400, 500};
//        for (int percent : predefinedPercentages) {
//            JMenuItem item = new JMenuItem(percent + "%");
//            item.addActionListener(e -> {
//                uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percent);
//                Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
//                if (aplicarUserSpecifiedAction != null) {
//                    aplicarUserSpecifiedAction.actionPerformed(
//                        new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
//                    );
//                }
//            });
//            percentageMenu.add(item);
//        }
//        percentageMenu.addSeparator();
//        JMenuItem otherItem = new JMenuItem("Otro...");
//        otherItem.addActionListener(e -> {
//            String input = JOptionPane.showInputDialog(view.getFrame(), "Introduce el porcentaje de zoom (ej: 150):", "Zoom Personalizado", JOptionPane.PLAIN_MESSAGE);
//            if (input != null && !input.trim().isEmpty()) {
//                try {
//                    input = input.replace("%", "").trim();
//                    double percentValue = Double.parseDouble(input);
//                    if (percentValue >= 1 && percentValue <= 5000) {
//                        uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percentValue);
//                        Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
//                        if (aplicarUserSpecifiedAction != null) {
//                            aplicarUserSpecifiedAction.actionPerformed(
//                                new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
//                            );
//                        }
//                    } else { JOptionPane.showMessageDialog(view.getFrame(), "Porcentaje inválido. Debe estar entre 1 y 5000.", "Error de Entrada", JOptionPane.ERROR_MESSAGE); }
//                } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(view.getFrame(), "Entrada inválida. Por favor, introduce un número.", "Error de Formato", JOptionPane.ERROR_MESSAGE); }
//            }
//        });
//        percentageMenu.add(otherItem);
//        percentageMenu.show(anchorComponent, 0, anchorComponent.getHeight());
//    }
//
//    /**
//     * 4.3. Muestra un JPopupMenu para que el usuario seleccione un nuevo modo de zoom.
//     */
//    private void showZoomModeSelectionPopup(Component anchorComponent) {
//        // ... (código del popup de modo de zoom, sin cambios respecto a tu versión)
//        if (model == null || view == null || uiConfig.actionMap == null) {
//            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar modos de zoom.", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//        JPopupMenu zoomMenu = new JPopupMenu("Seleccionar Modo de Zoom");
//        Map<String, Action> actionMapRef = uiConfig.actionMap;
//
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR), zoomFitPageIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AUTO), zoomActualSizeIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ANCHO), zoomFitWidthIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ALTO), zoomFitHeightIcon);
//        zoomMenu.addSeparator();
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO), zoomUserSpecifiedIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_FIJO), zoomFixedIcon);
//
//        zoomMenu.show(anchorComponent, 0, anchorComponent.getHeight());
//    }
//
//    /**
//     * 4.4. Método helper para añadir un JMenuItem a un JPopupMenu.
//     */
//    private void addZoomModeMenuItem(JPopupMenu menu, Action action, ImageIcon icon) {
//        // ... (código del helper, sin cambios)
//        if (action != null) {
//            JMenuItem menuItem = new JMenuItem(action);
//            if (icon != null) {
//                menuItem.setIcon(icon);
//            }
//            menu.add(menuItem);
//        }
//    }
//
//    /**
//     * 4.5. Formatea un tamaño de archivo en bytes a una cadena legible (KB, MB, GB, etc.).
//     */
//    private String formatFileSize(long bytes) {
//        if (bytes < 0) return "N/A";
//        if (bytes < 1024) return bytes + " B";
//        int exp = (int) (Math.log(bytes) / Math.log(1024));
//        String pre = (exp > 0 && exp <= "KMGTPE".length()) ? "KMGTPE".charAt(exp - 1) + "" : "";
//        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
//    }
//    
//    
// // En controlador.managers.InfoBarManager.java
// // ... (después del constructor o junto a los otros métodos privados de la SECCIÓN 4) ...
//
//     /**
//      * 4.X. (Nuevo método privado) Configura los listeners para los controles de zoom interactivos
//      *      en la barra inferior (JLabel de porcentaje y JButton de modo de zoom).
//      *      Se llama desde el constructor.
//      */
//     private void configurarListenersControlesZoomInferior() {
//         // Listener para el JLabel/Botón de Porcentaje de Zoom Personalizado.
//         JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
//         if (porcentajeLabel != null) {
//             porcentajeLabel.addMouseListener(new MouseAdapter() {
//                 @Override
//                 public void mouseClicked(MouseEvent e) {
//                     System.out.println("  [InfoBarManager Listener] Clic en PorcentajeZoomPersonalizadoLabel.");
//                     showCustomZoomPercentagePopup(porcentajeLabel);
//                 }
//                 @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//                 @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
//             });
//             // System.out.println("    -> MouseListener añadido a PorcentajeZoomPersonalizadoLabel."); // Log ya está en el constructor general
//         } else {
//             System.err.println("WARN [InfoBarManager.configurarListenersControlesZoomInferior]: view.getPorcentajeZoomPersonalizadoLabel() es null.");
//         }
//
//         // Listener para el JButton del Icono del Modo de Zoom Actual.
//         JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//         if (modoZoomBoton != null) {
//             modoZoomBoton.addActionListener(e -> {
//                 System.out.println("  [InfoBarManager Listener] Clic en ModoZoomActualIconoBoton.");
//                 showZoomModeSelectionPopup(modoZoomBoton);
//             });
//             // System.out.println("    -> ActionListener añadido a ModoZoomActualIconoBoton."); // Log ya está en el constructor general
//         } else {
//             System.err.println("WARN [InfoBarManager.configurarListenersControlesZoomInferior]: view.getModoZoomActualIconoBoton() es null.");
//         }
//     }
//    
//    
//}
//
////package controlador.managers;
////
////import java.awt.Color;
////import java.awt.Component; // Para el ancla del JPopupMenu
////import java.awt.Cursor;   // Para el cursor de mano
////import java.awt.event.ActionEvent;
////import java.awt.event.MouseAdapter;
////import java.awt.event.MouseEvent;
////import java.awt.image.BufferedImage;
////import java.io.IOException;
////import java.nio.file.Files;
////import java.nio.file.Path;
////import java.nio.file.attribute.FileTime;
////import java.text.DecimalFormat;
////import java.util.Date;
////import java.util.Map;
////import java.util.Objects;
////
////import javax.swing.Action;
////import javax.swing.ImageIcon;
////import javax.swing.JButton;
////import javax.swing.JLabel;
////import javax.swing.JMenuItem;
////import javax.swing.JOptionPane;
////import javax.swing.JPopupMenu;
////import javax.swing.SwingUtilities;
////
////import controlador.commands.AppActionCommands; // Necesario para los comandos de Action
////import modelo.VisorModel;
////import servicios.ConfigurationManager; // Para las claves de configuración
////import servicios.ProjectManager;
////import servicios.zoom.ZoomModeEnum;
////import utils.ImageUtils;
////import vista.VisorView;
////import vista.config.ViewUIConfig; // Asumo que uiConfig se pasa y tiene todo
////
////public class InfoBarManager {
////
////    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS Y ESTADO) ---
////    // 1.1. Referencias a componentes principales del sistema
////    private final VisorModel model;
////    private final VisorView view;
////    private final ViewUIConfig uiConfig; // Contiene ConfigurationManager, ActionMap, IconUtils, colores, etc.
////
////    // 1.2. Formateadores y otros helpers
////    private final DecimalFormat dfPorcentajeZoomLabel = new DecimalFormat("0'%'"); // Ej: "100%"
////
////    // 1.3. Iconos para el botón de modo de zoom (cargados en el constructor)
////    private ImageIcon zoomFitWidthIcon;
////    private ImageIcon zoomFitHeightIcon;
////    private ImageIcon zoomFitPageIcon;
////    private ImageIcon zoomActualSizeIcon;
////    private ImageIcon zoomUserSpecifiedIcon;
////    private ImageIcon zoomFixedIcon;
////    private final Map<String, String> comandoToClaveIconoMap;
////
////    
////    private final ProjectManager projectService;
////    
////    // --- SECCIÓN 2: CONSTRUCTOR ---
////    /**
////     * Constructor de InfoBarManager.
////     * Inicializa las referencias y configura los listeners para los controles interactivos
////     * de las barras de información.
////     *
////     * @param model La instancia del VisorModel.
////     * @param view La instancia de la VisorView.
////     * @param uiConfig La configuración de la UI que contiene ConfigurationManager, ActionMap, etc.
////     */
////    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig,
////                          Map<String, String> comandoToClaveIconoMap,
////                          ProjectManager projectService /* <<<< AÑADIR PARÁMETRO */ ) {
////        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
////        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
////        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
////        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
////        this.projectService = Objects.requireNonNull(projectService, "ProjectManager no puede ser null en InfoBarManager"); // <<<< ASIGNAR
////
////        System.out.println("[InfoBarManager] Instancia creada. Configurando listeners...");
////        cargarIconosModoZoom();
////        configurarListenersControlesZoomInferior(); // Método helper para agrupar listeners
////        System.out.println("[InfoBarManager] Configuración de listeners finalizada.");
////    }
////
////    // Nuevo método helper que estaba en tu constructor original
////    private void configurarListenersControlesZoomInferior() {
////        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
////        if (porcentajeLabel != null) {
////            porcentajeLabel.addMouseListener(new MouseAdapter() {
////                @Override
////                public void mouseClicked(MouseEvent e) {
////                    System.out.println("  [InfoBarManager Listener] Clic en PorcentajeZoomPersonalizadoLabel.");
////                    showCustomZoomPercentagePopup(porcentajeLabel);
////                }
////                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
////                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
////            });
////            // System.out.println("    -> MouseListener añadido a PorcentajeZoomPersonalizadoLabel."); // Log ya está en constructor
////        } else {
////            // System.err.println("WARN [InfoBarManager Constructor]: view.getPorcentajeZoomPersonalizadoLabel() es null. Listener no añadido.");
////        }
////
////        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
////        if (modoZoomBoton != null) {
////            modoZoomBoton.addActionListener(e -> {
////                System.out.println("  [InfoBarManager Listener] Clic en ModoZoomActualIconoBoton.");
////                showZoomModeSelectionPopup(modoZoomBoton);
////            });
////            // System.out.println("    -> ActionListener añadido a ModoZoomActualIconoBoton.");
////        } else {
////            // System.err.println("WARN [InfoBarManager Constructor]: view.getModoZoomActualIconoBoton() es null. Listener no añadido.");
////        }
////    }
////    
//////    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig, Map<String, String> comandoToClaveIconoMap, ProjectManager projectService) {
//////        // 2.1. Asignar dependencias principales, validando que no sean nulas.
//////        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
//////        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
//////        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
//////        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
//////        this.projectService = Objects.requireNonNull(projectService, "ProjectManager no puede ser null en InfoBarManager");
//////        
//////        // 2.2. Log de inicialización.
//////        System.out.println("[InfoBarManager] Instancia creada. Configurando listeners...");
//////
//////        // 2.3. Cargar los iconos necesarios para los modos de zoom.
//////        cargarIconosModoZoom();
//////        
//////        // 2.4. Configurar listeners para los controles de zoom en la barra inferior.
//////
//////        // 2.4.1. Listener para el JLabel/Botón de Porcentaje de Zoom Personalizado.
//////        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
//////        if (porcentajeLabel != null) {
//////            porcentajeLabel.addMouseListener(new MouseAdapter() {
//////                @Override
//////                public void mouseClicked(MouseEvent e) {
//////                    // Al hacer clic, mostrar el JPopupMenu para seleccionar/ingresar porcentaje.
//////                    System.out.println("  [InfoBarManager Listener] Clic en PorcentajeZoomPersonalizadoLabel.");
//////                    showCustomZoomPercentagePopup(porcentajeLabel); // 'porcentajeLabel' actúa como ancla.
//////                }
//////                // Cambiar cursor a mano al pasar por encima para indicar que es clickeable.
//////                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//////                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
//////            });
//////            System.out.println("    -> MouseListener añadido a PorcentajeZoomPersonalizadoLabel.");
//////        } else {
//////            System.err.println("WARN [InfoBarManager Constructor]: view.getPorcentajeZoomPersonalizadoLabel() es null. Listener no añadido.");
//////        }
//////
//////        // 2.4.2. Listener para el JButton del Icono del Modo de Zoom Actual.
//////        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//////        if (modoZoomBoton != null) {
//////            modoZoomBoton.addActionListener(e -> {
//////                // Al hacer clic, mostrar el JPopupMenu para seleccionar un nuevo modo de zoom.
//////                System.out.println("  [InfoBarManager Listener] Clic en ModoZoomActualIconoBoton.");
//////                showZoomModeSelectionPopup(modoZoomBoton); // 'modoZoomBoton' actúa como ancla.
//////            });
//////            System.out.println("    -> ActionListener añadido a ModoZoomActualIconoBoton.");
//////        } else {
//////            System.err.println("WARN [InfoBarManager Constructor]: view.getModoZoomActualIconoBoton() es null. Listener no añadido.");
//////        }
//////        System.out.println("[InfoBarManager] Configuración de listeners finalizada.");
//////    } // --- FIN DEL CONSTRUCTOR ---
////
////    
////    
////    // --- SECCIÓN 3: MÉTODOS DE ACTUALIZACIÓN DE BARRAS ---
////
////    /**
////     * Método principal para solicitar la actualización de todas las barras de información.
////     * Se asegura de que la actualización se realice en el Event Dispatch Thread (EDT).
////     */
////    public void actualizarBarrasDeInfo() {
////        // 3.1. Validar dependencias antes de programar en el EDT.
////        if (view == null || model == null || uiConfig == null) {
////            System.err.println("ERROR [InfoBarManager.actualizarBarrasDeInfo]: Dependencias nulas (Vista, Modelo o uiConfig). No se pueden actualizar barras.");
////            return;
////        }
////
////        // 3.2. Programar la actualización de las barras internas en el EDT.
////        SwingUtilities.invokeLater(() -> {
////            // System.out.println("  [InfoBarManager EDT] Ejecutando actualización de barras..."); // Log opcional
////            actualizarBarraInfoSuperiorInterno();
////            actualizarBarraEstadoInferiorInterno();
////            // System.out.println("  [InfoBarManager EDT] Actualización de barras completada."); // Log opcional
////        });
////    }
////    
////    
////    /**
////     * Método interno para actualizar los componentes de la barra de información superior.
////     * Se asume que se llama desde el EDT.
////     * AHORA LEE LA CONFIGURACIÓN DE VISIBILIDAD PARA CADA ELEMENTO.
////     */
////    private void actualizarBarraInfoSuperiorInterno() {
////        // 3.2.0. Validar dependencias tempranamente
////        if (uiConfig == null || uiConfig.configurationManager == null || model == null || view == null) {
////            System.err.println("ERROR [InfoBarManager.actualizarBarraInfoSuperiorInterno]: Dependencias esenciales nulas.");
////            return;
////        }
////        ConfigurationManager cfg = uiConfig.configurationManager;
////
////        // 3.2.1. Visibilidad del PANEL COMPLETO de la Barra Superior
////        boolean panelBarraSuperiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_VISIBLE, true);
////        if (view.getPanelBarraSuperior() != null) { // Asumiendo que tienes view.getPanelBarraSuperior() en VisorView
////            view.getPanelBarraSuperior().setVisible(panelBarraSuperiorVisible);
////        } else {
////            System.err.println("WARN [InfoBarManager]: view.getPanelBarraSuperior() es null. No se puede gestionar visibilidad del panel.");
////            // Si el panel no existe, no podemos continuar asumiendo que sus hijos sí.
////            // Podríamos retornar, o continuar y que los getters de los JLabels fallen individualmente.
////            // Por seguridad, si el panel principal de la barra no existe, es mejor no seguir.
////            if (!panelBarraSuperiorVisible) return; // Si está configurado para no ser visible, igualmente retornamos.
////        }
////
////        if (!panelBarraSuperiorVisible) {
////            System.out.println("  [InfoBarManager] Barra Superior Completa está configurada como NO VISIBLE. No se actualizan sus componentes internos.");
////            return;
////        }
////
////        // 3.2.2. Nombre del Archivo / Ruta (Superior)
////        JLabel nombreArchivoLabelSup = view.getNombreArchivoInfoLabel(); // Necesitas este getter en VisorView
////        if (nombreArchivoLabelSup != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_VISIBLE, true);
////            nombreArchivoLabelSup.setVisible(esVisible);
////            if (esVisible) {
////                String nombreArchivoDisplay = "Archivo: N/A";
////                Path rutaCompletaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
////
////                if (rutaCompletaActual != null) {
////                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_NOMBRE_RUTA_FORMATO, "solo_nombre");
////                    if ("ruta_completa".equalsIgnoreCase(formato)) {
////                        nombreArchivoDisplay = rutaCompletaActual.toString();
////                    } else { // solo_nombre o default
////                        nombreArchivoDisplay = rutaCompletaActual.getFileName().toString(); // Solo el nombre del archivo
////                    }
////                } else if (model.getSelectedImageKey() != null) { // Si hay clave pero no ruta (error)
////                    nombreArchivoDisplay = model.getSelectedImageKey() + " (Ruta no encontrada)";
////                } else if (model.getCarpetaRaizActual() != null){ // Si no hay imagen, mostrar carpeta raíz
////                     nombreArchivoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
////                } // else: queda "Archivo: N/A" si no hay ni imagen ni carpeta
////                nombreArchivoLabelSup.setText(nombreArchivoDisplay);
////            } else {
////                nombreArchivoLabelSup.setText(""); // Limpiar si no es visible
////            }
////        }
////
////        // 3.2.3. Índice Actual / Total de Imágenes
////        JLabel indiceTotalLabel = view.getIndiceTotalInfoLabel();
////        if (indiceTotalLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_INDICE_TOTAL_VISIBLE, true);
////            indiceTotalLabel.setVisible(esVisible);
////            if (esVisible) {
////                int indiceActual = -1;
////                int totalImagenes = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
////                if (model.getSelectedImageKey() != null && totalImagenes > 0) {
////                    indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
////                }
////                String display;
////                if (totalImagenes > 0 && indiceActual != -1) display = (indiceActual + 1) + "/" + totalImagenes;
////                else if (totalImagenes > 0) display = "-/" + totalImagenes;
////                else display = "0/0";
////                indiceTotalLabel.setText("Idx: " + display);
////            } else {
////                indiceTotalLabel.setText("");
////            }
////        }
////
////        // 3.2.4. Dimensiones Originales de la Imagen
////        JLabel dimensionesLabel = view.getDimensionesOriginalesInfoLabel();
////        if (dimensionesLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_DIMENSIONES_VISIBLE, true);
////            dimensionesLabel.setVisible(esVisible);
////            if (esVisible) {
////                String dimsDisplay = "N/A";
////                BufferedImage imgOriginal = model.getCurrentImage();
////                if (imgOriginal != null) {
////                    dimsDisplay = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
////                }
////                dimensionesLabel.setText("Dim: " + dimsDisplay);
////            } else {
////                dimensionesLabel.setText("");
////            }
////        }
////
////        // 3.2.5. Tamaño del Archivo
////        JLabel tamanoArchivoLabel = view.getTamanoArchivoInfoLabel();
////        if (tamanoArchivoLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_TAMANO_ARCHIVO_VISIBLE, true);
////            tamanoArchivoLabel.setVisible(esVisible);
////            if (esVisible) {
////                String tamanoDisplay = "N/A";
////                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
////                if (rutaActual != null && Files.exists(rutaActual)) {
////                    try {
////                        long bytes = Files.size(rutaActual);
////                        tamanoDisplay = formatFileSize(bytes);
////                    } catch (IOException ex) {
////                        System.err.println("Error al leer tamaño de archivo para '" + rutaActual + "': " + ex.getMessage());
////                    }
////                }
////                tamanoArchivoLabel.setText("Tam: " + tamanoDisplay);
////            } else {
////                tamanoArchivoLabel.setText("");
////            }
////        }
////
////        // 3.2.6. Fecha de Modificación del Archivo
////        JLabel fechaArchivoLabel = view.getFechaArchivoInfoLabel();
////        if (fechaArchivoLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FECHA_ARCHIVO_VISIBLE, true);
////            fechaArchivoLabel.setVisible(esVisible);
////            if (esVisible) {
////                String fechaDisplay = "N/A";
////                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
////                if (rutaActual != null && Files.exists(rutaActual)) {
////                    try {
////                        FileTime fileTime = Files.getLastModifiedTime(rutaActual);
////                        fechaDisplay = sdfFechaArchivo.format(new Date(fileTime.toMillis()));
////                    } catch (IOException ex) {
////                        System.err.println("Error al leer fecha de archivo para '" + rutaActual + "': " + ex.getMessage());
////                    }
////                }
////                fechaArchivoLabel.setText("Fch: " + fechaDisplay);
////            } else {
////                fechaArchivoLabel.setText("");
////            }
////        }
////
////        // 3.2.7. Formato de Imagen
////        JLabel formatoImagenLabel = view.getFormatoImagenInfoLabel();
////        if (formatoImagenLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_FORMATO_IMAGEN_VISIBLE, false); // Default false
////            formatoImagenLabel.setVisible(esVisible);
////            if (esVisible) {
////                String formatoDisplay = "N/A";
////                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
////                if (rutaActual != null) {
////                    formatoDisplay = ImageUtils.getImageFormat(rutaActual); // Usar tu utilidad
////                }
////                formatoImagenLabel.setText("Fmt: " + formatoDisplay.toUpperCase());
////            } else {
////                formatoImagenLabel.setText("");
////            }
////        }
////
////        // 3.2.8. Nombre del Modo de Zoom Activo
////        JLabel modoZoomNombreLabel = view.getModoZoomNombreInfoLabel();
////        if (modoZoomNombreLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_MODO_ZOOM_VISIBLE, true);
////            modoZoomNombreLabel.setVisible(esVisible);
////            if (esVisible) {
////                String modoZoomDisplay = "N/A";
////                if (model.getCurrentZoomMode() != null) {
////                    modoZoomDisplay = model.getCurrentZoomMode().getNombreLegible();
////                }
////                modoZoomNombreLabel.setText("Modo: " + modoZoomDisplay);
////            } else {
////                modoZoomNombreLabel.setText("");
////            }
////        }
////
////        // 3.2.9. Porcentaje de Zoom Visual Resultante
////        JLabel zoomRealPctLabel = view.getPorcentajeZoomVisualRealInfoLabel();
////        if (zoomRealPctLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_SUPERIOR_ZOOM_REAL_PCT_VISIBLE, true);
////            zoomRealPctLabel.setVisible(esVisible);
////            if (esVisible) {
////                String zoomPctDisplay = "N/A";
////                if (model.getCurrentImage() != null) { // Solo mostrar si hay imagen
////                    zoomPctDisplay = String.format("%.0f%%", model.getZoomFactor() * 100);
////                }
////                zoomRealPctLabel.setText("%Z: " + zoomPctDisplay);
////            } else {
////                zoomRealPctLabel.setText("");
////            }
////        }
////
////        // 3.2.10. Revalidar y repintar el panel de la barra superior
////        if (view.getPanelBarraSuperior() != null && view.getPanelBarraSuperior().isVisible()) {
////            view.getPanelBarraSuperior().revalidate();
////            view.getPanelBarraSuperior().repaint();
////        }
////    } // Fin actualizarBarraInfoSuperiorInterno
////    
////    
////    /**
////     * 3.3. Método interno para actualizar los componentes de la barra de estado/control inferior.
////     */
////    private void actualizarBarraEstadoInferiorInterno() {
////        // 3.3.0. Validar dependencias tempranamente
////        if (uiConfig == null || uiConfig.configurationManager == null || model == null || view == null) {
////            System.err.println("ERROR [InfoBarManager.actualizarBarraEstadoInferiorInterno]: Dependencias esenciales nulas.");
////            return;
////        }
////        ConfigurationManager cfg = uiConfig.configurationManager;
////
////        // 3.3.1. Visibilidad del PANEL COMPLETO de la Barra Inferior
////        boolean panelBarraInferiorVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_VISIBLE, true);
////        if (view.getPanelBarraEstado() != null) { // Asumiendo que getPanelBarraEstado() es el JPanel de la barra inferior
////            view.getPanelBarraEstado().setVisible(panelBarraInferiorVisible);
////        } else {
////            System.err.println("WARN [InfoBarManager]: view.getPanelBarraEstado() es null. No se puede gestionar visibilidad del panel.");
////            if (!panelBarraInferiorVisible) return;
////        }
////
////        if (!panelBarraInferiorVisible) {
////            System.out.println("  [InfoBarManager] Barra Inferior Completa está configurada como NO VISIBLE. No se actualizan sus componentes internos.");
////            return;
////        }
////
////        Color colorFondoActivo = (uiConfig.colorBotonActivado != null) ? uiConfig.colorBotonActivado : Color.CYAN;
////        Color colorFondoInactivo = (uiConfig.colorFondoSecundario != null) ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY;
////
////        // 3.3.2. Ruta Completa del Archivo (Inferior)
////        JLabel rutaCompletaLabel = view.getRutaCompletaArchivoLabel();
////        if (rutaCompletaLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_VISIBLE, true);
////            rutaCompletaLabel.setVisible(esVisible);
////            if (esVisible) {
////                String rutaTextoDisplay = "(Ninguna imagen seleccionada)";
////                Path rutaActualParaBarra = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
////
////                if (rutaActualParaBarra != null) {
////                    String formato = cfg.getString(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_NOMBRE_RUTA_FORMATO, "ruta_completa");
////                    if ("solo_nombre".equalsIgnoreCase(formato)) {
////                        rutaTextoDisplay = rutaActualParaBarra.getFileName().toString();
////                    } else { // ruta_completa o default
////                        rutaTextoDisplay = rutaActualParaBarra.toString();
////                    }
////                    // Añadir marca de proyecto
////                    if (projectService != null && projectService.estaMarcada(rutaActualParaBarra)) {
////                        rutaTextoDisplay += " [MARCADA]";
////                    }
////                } else if (model.getCarpetaRaizActual() != null) { // Si no hay imagen, mostrar carpeta raíz
////                    rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
////                }
////                rutaCompletaLabel.setText(rutaTextoDisplay);
////            } else {
////                rutaCompletaLabel.setText("");
////            }
////        }
////
////        // 3.3.3. Icono Zoom Manual (ZM)
////        JLabel zmIconLabel = view.getIconoZoomManualLabel();
////        if (zmIconLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_ZM_VISIBLE, true);
////            zmIconLabel.setVisible(esVisible);
////            if (esVisible) {
////                boolean zoomManualEstaActivo = model.isZoomHabilitado();
////                zmIconLabel.setToolTipText(zoomManualEstaActivo ? "Zoom Manual: Activado (Click para desactivar)" : "Zoom Manual: Desactivado (Click para activar)");
////                zmIconLabel.setBackground(zoomManualEstaActivo ? colorFondoActivo : colorFondoInactivo);
////                // TODO: Cambiar el ImageIcon del JLabel si tienes iconos específicos para on/off
////                // zmIconLabel.setIcon(zoomManualEstaActivo ? iconoZmOn : iconoZmOff);
////            }
////        }
////
////        // 3.3.4. Icono Mantener Proporciones (Prop)
////        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
////        if (propIconLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_PROP_VISIBLE, true);
////            propIconLabel.setVisible(esVisible);
////            if (esVisible) {
////                boolean proporcionesEstanActivas = model.isMantenerProporcion();
////                propIconLabel.setToolTipText(proporcionesEstanActivas ? "Mantener Proporciones: Activado (Click para desactivar)" : "Mantener Proporciones: Desactivado (Click para activar)");
////                propIconLabel.setBackground(proporcionesEstanActivas ? colorFondoActivo : colorFondoInactivo);
////            }
////        }
////
////        // 3.3.5. Icono Modo Subcarpetas (SubC)
////        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
////        if (subcIconLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_ICONO_SUBC_VISIBLE, true);
////            subcIconLabel.setVisible(esVisible);
////            if (esVisible) {
////                boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
////                subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado (Click para desactivar)" : "Incluir Subcarpetas: Desactivado (Click para activar)");
////                subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
////            }
////        }
////
////        // 3.3.6. Control % Zoom (JLabel clickeable)
////        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
////        if (porcentajeLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_ZOOM_PCT_VISIBLE, true);
////            porcentajeLabel.setVisible(esVisible);
////            if (esVisible) {
////                double customZoomPercent = cfg.getZoomPersonalizadoPorcentaje(); // Método en ConfigurationManager
////                porcentajeLabel.setText(dfPorcentajeZoomLabel.format(customZoomPercent));
////                porcentajeLabel.setToolTipText("Zoom Personalizado: " + dfPorcentajeZoomLabel.format(customZoomPercent) + "% (Click para cambiar)");
////            } else {
////                porcentajeLabel.setText("");
////            }
////        }
////
////        // 3.3.7. Control Modo Zoom (JButton con icono)
////        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
////        if (modoZoomBoton != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_CTRL_MODO_ZOOM_VISIBLE, true);
////            modoZoomBoton.setVisible(esVisible);
////            if (esVisible && model.getCurrentZoomMode() != null) {
////                ZoomModeEnum currentMode = model.getCurrentZoomMode();
////                ImageIcon iconToShow = null;
////                String tooltipText = "Modo: ";
////                switch (currentMode) {
////                    case FIT_TO_WIDTH: iconToShow = zoomFitWidthIcon; tooltipText += "Ajustar Ancho"; break;
////                    case FIT_TO_HEIGHT: iconToShow = zoomFitHeightIcon; tooltipText += "Ajustar Alto"; break;
////                    case FIT_TO_SCREEN: iconToShow = zoomFitPageIcon; tooltipText += "Ajustar Página"; break;
////                    case DISPLAY_ORIGINAL: iconToShow = zoomActualSizeIcon; tooltipText += "Tamaño Real (100%)"; break;
////                    case USER_SPECIFIED_PERCENTAGE: iconToShow = zoomUserSpecifiedIcon; tooltipText += "Zoom Personalizado %"; break;
////                    case MAINTAIN_CURRENT_ZOOM: iconToShow = zoomFixedIcon; tooltipText += "Mantener Zoom Actual"; break;
////                    default: tooltipText += "Desconocido"; break;
////                }
////                modoZoomBoton.setIcon(iconToShow);
////                modoZoomBoton.setToolTipText(tooltipText + ". Click para cambiar.");
////            } else if (esVisible) { // Si es visible pero no hay modo de zoom o icono
////                modoZoomBoton.setIcon(null); // O un icono por defecto
////                modoZoomBoton.setToolTipText("Modo de Zoom no definido");
////            }
////        }
////
////        // 3.3.8. Área de Mensajes de la Aplicación
////        JLabel mensajesLabel = view.getMensajesAppLabel(); // Asume getter en VisorView
////        if (mensajesLabel != null) {
////            boolean esVisible = cfg.getBoolean(ConfigurationManager.KEY_INTERFAZ_INFOBAR_INFERIOR_MENSAJES_APP_VISIBLE, true);
////            mensajesLabel.setVisible(esVisible);
////            if (esVisible) {
////                // Aquí actualizarías el texto de los mensajes si tienes un sistema para ello.
////                // Ejemplo: mensajesLabel.setText(model.getAppStatusMessage() != null ? model.getAppStatusMessage() : "Listo.");
////            } else {
////                mensajesLabel.setText("");
////            }
////        }
////
////        // 3.3.9. Revalidar y repintar el panel de la barra inferior
////        if (view.getPanelBarraEstado() != null && view.getPanelBarraEstado().isVisible()) {
////            view.getPanelBarraEstado().revalidate();
////            view.getPanelBarraEstado().repaint();
////        }
////    } //---FIN del metodo actualizarBarraEstadoInferiorInterno
////
////
//////    /**
//////     * Método interno para actualizar los componentes de la barra de información superior.
//////     * Se asume que se llama desde el EDT.
//////     */
//////    private void actualizarBarraInfoSuperiorInterno() {//NO weno
//////        // --- 3.1. Actualizar Nombre del Archivo ---
//////        String nombreArchivoDisplay = "Archivo: N/A";
//////        Path rutaCompletaActual = null;
//////        if (model.getSelectedImageKey() != null) {
//////            rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
//////            if (rutaCompletaActual != null) {
//////                nombreArchivoDisplay = "Archivo: " + rutaCompletaActual.getFileName().toString();
//////            } else {
//////                nombreArchivoDisplay = "Archivo: " + model.getSelectedImageKey() + " (Ruta no encontrada)";
//////            }
//////        }
//////        if (view.getNombreArchivoInfoLabel() != null) {
//////            view.getNombreArchivoInfoLabel().setText(nombreArchivoDisplay);
//////        }
//////
//////        // --- 3.2. Actualizar Índice Actual / Total de Imágenes ---
//////        int indiceActual = -1;
//////        int totalImagenes = 0;
//////        if (model.getModeloLista() != null) {
//////            totalImagenes = model.getModeloLista().getSize();
//////            if (model.getSelectedImageKey() != null && totalImagenes > 0) {
//////                // El índice que se muestra es 1-based para el usuario.
//////                indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
//////            }
//////        }
//////        String indiceTotalDisplay;
//////        if (totalImagenes > 0 && indiceActual != -1) {
//////            indiceTotalDisplay = "Idx: " + (indiceActual + 1) + "/" + totalImagenes;
//////        } else if (totalImagenes > 0) {
//////            indiceTotalDisplay = "Idx: -/" + totalImagenes;
//////        } else {
//////            indiceTotalDisplay = "Idx: 0/0";
//////        }
//////        if (view.getIndiceTotalInfoLabel() != null) {
//////            view.getIndiceTotalInfoLabel().setText(indiceTotalDisplay);
//////        }
//////
//////        // --- 3.3. Actualizar Dimensiones Originales de la Imagen ---
//////        String dimsDisplay = "Dim: N/A";
//////        BufferedImage imgOriginal = model.getCurrentImage();
//////        if (imgOriginal != null) {
//////            dimsDisplay = "Dim: " + imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
//////        }
//////        if (view.getDimensionesOriginalesInfoLabel() != null) {
//////            view.getDimensionesOriginalesInfoLabel().setText(dimsDisplay);
//////        }
//////
//////        // --- 3.4. Actualizar Tamaño del Archivo (Placeholder - Implementación Futura) ---
//////        String tamanoDisplay = "Tam: N/A"; // TODO: Implementar obtención de tamaño de archivo
//////        if (view.getTamanoArchivoInfoLabel() != null) {
//////            view.getTamanoArchivoInfoLabel().setText(tamanoDisplay);
//////        }
//////
//////        // --- 3.5. Actualizar Fecha de Modificación del Archivo (Placeholder - Implementación Futura) ---
//////        String fechaDisplay = "Fch: N/A"; // TODO: Implementar obtención de fecha de archivo
//////        if (view.getFechaArchivoInfoLabel() != null) {
//////            view.getFechaArchivoInfoLabel().setText(fechaDisplay);
//////        }
//////
//////        // --- 3.6. Actualizar Nombre del Modo de Zoom Activo ---
//////        String modoZoomDisplay = "Modo: N/A";
//////        if (model.getCurrentZoomMode() != null) {
//////            modoZoomDisplay = "Modo: " + model.getCurrentZoomMode().getNombreLegible();
//////        }
//////        if (view.getModoZoomNombreInfoLabel() != null) {
//////            view.getModoZoomNombreInfoLabel().setText(modoZoomDisplay);
//////        }
//////
//////        // --- 3.7. Actualizar Porcentaje de Zoom Visual Resultante ---
//////        String zoomPctDisplay = "%Z: N/A";
//////        if (model.getCurrentImage() != null) { // Solo mostrar si hay imagen
//////            // El factor de zoom se multiplica por 100 para mostrar como porcentaje.
//////            zoomPctDisplay = String.format("%%Z: %.0f%%", model.getZoomFactor() * 100);
//////        }
//////        if (view.getPorcentajeZoomVisualRealInfoLabel() != null) {
//////            view.getPorcentajeZoomVisualRealInfoLabel().setText(zoomPctDisplay);
//////        }
//////    }
////
//////    /**
//////     * Método interno para actualizar los componentes de la barra de estado/control inferior.
//////     * Se asume que se llama desde el EDT.
//////     */
//////    private void actualizarBarraEstadoInferiorInterno() {
//////        // --- 3.8. Actualizar Ruta Completa del Archivo ---
//////        String rutaTextoDisplay = "Ruta: (ninguna imagen seleccionada)";
//////        Path rutaActualParaBarra = null;
//////        if (model.getSelectedImageKey() != null) {
//////            rutaActualParaBarra = model.getRutaCompleta(model.getSelectedImageKey());
//////            if (rutaActualParaBarra != null) {
//////                rutaTextoDisplay = rutaActualParaBarra.toString();
//////            } else {
//////                rutaTextoDisplay = model.getSelectedImageKey() + " (Ruta no encontrada)";
//////            }
//////        } else if (model.getCarpetaRaizActual() != null) {
//////            rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
//////        }
//////        if (view.getRutaCompletaArchivoLabel() != null) {
//////            view.getRutaCompletaArchivoLabel().setText(rutaTextoDisplay);
//////        }
//////
//////        // --- 3.9. Actualizar Indicadores de Estado (Iconos ZM, Prop, SubC) ---
//////        //      Los colores vienen de uiConfig (o podrían ser defaults si uiConfig es null).
//////        Color colorFondoActivo = (uiConfig != null && uiConfig.colorBotonActivado != null)
//////                                 ? uiConfig.colorBotonActivado : Color.CYAN; // Fallback
//////        Color colorFondoInactivo = (uiConfig != null && uiConfig.colorFondoSecundario != null)
//////                                   ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY; // Fallback
//////
//////        // 3.9.1. Indicador Zoom Manual (ZM)
//////        JLabel zmIconLabel = view.getIconoZoomManualLabel();
//////        if (zmIconLabel != null) {
//////            boolean zoomManualEstaActivo = model.isZoomHabilitado();
//////            zmIconLabel.setToolTipText(zoomManualEstaActivo ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
//////            zmIconLabel.setBackground(zoomManualEstaActivo ? colorFondoActivo : colorFondoInactivo);
//////        }
//////
//////        // 3.9.2. Indicador Mantener Proporciones (Prop)
//////        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
//////        if (propIconLabel != null) {
//////            boolean proporcionesEstanActivas = model.isMantenerProporcion();
//////            propIconLabel.setToolTipText(proporcionesEstanActivas ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
//////            propIconLabel.setBackground(proporcionesEstanActivas ? colorFondoActivo : colorFondoInactivo);
//////        }
//////
//////        // 3.9.3. Indicador Modo Subcarpetas (SubC)
//////        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
//////        if (subcIconLabel != null) {
//////            // El estado activo es cuando SÍ se incluyen subcarpetas,
//////            // que es lo opuesto a model.isMostrarSoloCarpetaActual().
//////            boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
//////            subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
//////            subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
//////        }
//////
//////        // --- 3.10. Actualizar Controles de Zoom Personalizados ---
//////
//////        // 3.10.1. Actualizar Label de Porcentaje de Zoom Personalizado
//////        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
//////        if (porcentajeLabel != null && uiConfig.configurationManager != null) {
//////            // Usa el nuevo método getZoomPersonalizadoPorcentaje() de ConfigurationManager
//////            double customZoomPercent = uiConfig.configurationManager.getZoomPersonalizadoPorcentaje();
//////            porcentajeLabel.setText(dfPorcentajeZoomLabel.format(customZoomPercent));
//////        } else if (porcentajeLabel == null) {
//////            System.err.println("WARN [InfoBarManager actualizarInferior]: view.getPorcentajeZoomPersonalizadoLabel() es null.");
//////        } else { // configurationManager es null
//////            System.err.println("WARN [InfoBarManager actualizarInferior]: uiConfig.configurationManager es null. No se puede leer zoom personalizado.");
//////            if (porcentajeLabel != null) porcentajeLabel.setText("Z: N/A");
//////        }
//////
//////        // 3.10.2. Actualizar Icono y Tooltip del Botón de Modo de Zoom Actual
//////        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//////        if (modoZoomBoton != null && model.getCurrentZoomMode() != null) {
//////            ZoomModeEnum currentMode = model.getCurrentZoomMode();
//////            ImageIcon iconToShow = null;
//////            String tooltipText = "Modo: ";
//////
//////            switch (currentMode) {
//////                case FIT_TO_WIDTH: iconToShow = zoomFitWidthIcon; tooltipText += "Ajustar Ancho"; break;
//////                case FIT_TO_HEIGHT: iconToShow = zoomFitHeightIcon; tooltipText += "Ajustar Alto"; break;
//////                case FIT_TO_SCREEN: iconToShow = zoomFitPageIcon; tooltipText += "Ajustar Página"; break;
//////                case DISPLAY_ORIGINAL: iconToShow = zoomActualSizeIcon; tooltipText += "Tamaño Real (100%)"; break;
//////                case USER_SPECIFIED_PERCENTAGE: iconToShow = zoomUserSpecifiedIcon; tooltipText += "Zoom Personalizado"; break;
//////                case MAINTAIN_CURRENT_ZOOM: iconToShow = zoomFixedIcon; tooltipText += "Zoom Fijo"; break;
//////                default: tooltipText += "Desconocido"; break; // No debería ocurrir
//////            }
//////            modoZoomBoton.setIcon(iconToShow);
//////            modoZoomBoton.setToolTipText(tooltipText + ". Click para cambiar.");
//////            // Podrías querer cambiar el fondo del botón si ciertos modos están "activos"
//////            // de una manera especial, pero por ahora solo se actualiza el icono/tooltip.
//////        } else if (modoZoomBoton == null) {
//////            System.err.println("WARN [InfoBarManager actualizarInferior]: view.getModoZoomActualIconoBoton() es null.");
//////        }
//////
//////        // --- 3.11. Actualizar Mensajes de la Aplicación (si hubiera un sistema para ellos) ---
//////        // JLabel mensajesLabel = view.getMensajesAppLabel();
//////        // if (mensajesLabel != null) { /* ... actualizar mensajes ... */ }
//////    }
////
////
////    // --- SECCIÓN 4: MÉTODOS PRIVADOS AUXILIARES ---
////
////    /**
////     * Carga los iconos para los diferentes modos de zoom.
////     * Se llama desde el constructor.
////     */
////    private void cargarIconosModoZoom() {
////        if (uiConfig.iconUtils != null && this.comandoToClaveIconoMap != null) {
////            int iconSize = (uiConfig.iconoAncho > 0) ? uiConfig.iconoAncho : 16;
////
////            // Función auxiliar para obtener el icono o loguear si falta la clave
////            java.util.function.Function<String, ImageIcon> getIcon = (comando) -> {
////                String claveIcono = this.comandoToClaveIconoMap.get(comando);
////                
////                if (claveIcono != null) {
////                	ImageIcon icon = uiConfig.iconUtils.getScaledIcon(claveIcono, iconSize, iconSize);
////                	
////                    return icon;
////                	
////                } else {
////                    System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: No se encontró clave de icono para el comando: " + comando + " en comandoToClaveIconoMap.");
////                    return null;
////                }
////            };
////
////            zoomFitWidthIcon      = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ANCHO);
////            zoomFitHeightIcon     = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ALTO);
////            zoomFitPageIcon       = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR);    // Corresponde a FIT_TO_SCREEN
////            zoomActualSizeIcon    = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AUTO);       // Corresponde a DISPLAY_ORIGINAL ("Zoom Automático")
////            zoomUserSpecifiedIcon = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
////            zoomFixedIcon         = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_FIJO);         // Corresponde a MAINTAIN_CURRENT_ZOOM
////
////            System.out.println("    [InfoBarManager] Iconos de modo de zoom cargados dinámicamente desde comandoToClaveIconoMap.");
////
////        } else {
////            if (uiConfig.iconUtils == null) {
////                System.err.println("WARN [InfoBarManager cargarIconosModoZoom]: IconUtils es null en uiConfig.");
////            }
////            if (this.comandoToClaveIconoMap == null) {
////                System.err.println("WARN [InfoBarManager cargarIconosModoZoom]: comandoToClaveIconoMap es null.");
////            }
////            System.err.println("    -> No se cargarán iconos de modo de zoom.");
////        }
////    }
////    
////
////    /**
////     * Muestra un JPopupMenu para que el usuario seleccione o introduzca un porcentaje
////     * de zoom personalizado.
////     *
////     * @param anchorComponent El componente Swing (JLabel o JButton) sobre el cual
////     *                        se mostrará el JPopupMenu.
////     */
////    private void showCustomZoomPercentagePopup(Component anchorComponent) {
////        // 4.1. Validar que las dependencias necesarias estén disponibles.
////        //      uiConfig debe tener configurationManager y actionMap no nulos.
////        if (uiConfig == null || uiConfig.configurationManager == null || uiConfig.actionMap == null) {
////            System.err.println("ERROR [InfoBarManager.showCustomZoomPercentagePopup]: uiConfig, ConfigurationManager o ActionMap nulos.");
////            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar el zoom.", "Error", JOptionPane.ERROR_MESSAGE);
////            return;
////        }
////
////        // 4.2. Crear el JPopupMenu.
////        JPopupMenu percentageMenu = new JPopupMenu("Seleccionar Porcentaje de Zoom");
////
////        // 4.3. Definir los porcentajes predefinidos a mostrar en el menú.
////        int[] predefinedPercentages = {25, 50, 75, 100, 125, 150, 200, 300, 400, 500};
////
////        // 4.4. Crear un JMenuItem para cada porcentaje predefinido.
////        for (int percent : predefinedPercentages) {
////            JMenuItem item = new JMenuItem(percent + "%");
////            // ActionListener para cada ítem predefinido:
////            item.addActionListener(e -> {
////                System.out.println("  [InfoBarManager Popup] Porcentaje predefinido seleccionado: " + percent + "%");
////                // 4.4.1. Actualizar la configuración con el nuevo porcentaje.
////                //         Usa el método setZoomPersonalizadoPorcentaje que añadimos/verificamos.
////                uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percent);
////
////                // 4.4.2. Obtener y ejecutar la Action para aplicar el modo USER_SPECIFIED_PERCENTAGE.
////                Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
////                if (aplicarUserSpecifiedAction != null) {
////                    // La Action AplicarModoZoomAction no es un toggle, simplemente se ejecuta.
////                    // Al ejecutarse, leerá el nuevo porcentaje de la configuración.
////                    aplicarUserSpecifiedAction.actionPerformed(
////                        new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
////                    );
////                    System.out.println("    -> Action CMD_ZOOM_TIPO_ESPECIFICADO ejecutada.");
////                } else {
////                    System.err.println("ERROR [InfoBarManager Popup]: Action CMD_ZOOM_TIPO_ESPECIFICADO no encontrada en actionMap.");
////                }
////                // La barra de información (incluido el label de porcentaje) se actualizará
////                // como resultado de que ZoomManager llame a infoBarManager.actualizarBarrasDeInfo()
////                // después de aplicar el nuevo modo de zoom.
////            });
////            percentageMenu.add(item); // Añadir el ítem al popup.
////        }
////
////        // 4.5. Añadir un separador visual al popup.
////        percentageMenu.addSeparator();
////
////        // 4.6. Crear el JMenuItem "Otro..." para entrada manual.
////        JMenuItem otherItem = new JMenuItem("Otro...");
////        // ActionListener para el ítem "Otro...":
////        otherItem.addActionListener(e -> {
////            System.out.println("  [InfoBarManager Popup] Opción 'Otro...' seleccionada.");
////            // 4.6.1. Mostrar un JOptionPane para que el usuario ingrese el porcentaje.
////            String input = JOptionPane.showInputDialog(
////                    view.getFrame(), // Padre del diálogo
////                    "Introduce el porcentaje de zoom (ej: 150):", // Mensaje
////                    "Zoom Personalizado", // Título del diálogo
////                    JOptionPane.PLAIN_MESSAGE); // Tipo de mensaje
////
////            // 4.6.2. Procesar la entrada del usuario.
////            if (input != null && !input.trim().isEmpty()) { // Si el usuario no canceló y escribió algo
////                try {
////                    // 4.6.2.1. Limpiar la entrada (quitar '%' y espacios) y convertir a double.
////                    input = input.replace("%", "").trim();
////                    double percentValue = Double.parseDouble(input);
////
////                    // 4.6.2.2. Validar el rango del porcentaje (ajusta según tus necesidades).
////                    if (percentValue >= 1 && percentValue <= 5000) { // Ej. de 1% a 5000%
////                        // 4.6.2.2.1. Actualizar la configuración.
////                        uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percentValue);
////                        System.out.println("    -> Configuración actualizada con porcentaje manual: " + percentValue + "%");
////
////                        // 4.6.2.2.2. Obtener y ejecutar la Action.
////                        Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
////                        if (aplicarUserSpecifiedAction != null) {
////                            aplicarUserSpecifiedAction.actionPerformed(
////                                new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
////                            );
////                            System.out.println("    -> Action CMD_ZOOM_TIPO_ESPECIFICADO ejecutada.");
////                        } else {
////                            System.err.println("ERROR [InfoBarManager Popup 'Otro...']: Action CMD_ZOOM_TIPO_ESPECIFICADO no encontrada.");
////                        }
////                    } else {
////                        // 4.6.2.3. Mostrar error si el porcentaje está fuera de rango.
////                        JOptionPane.showMessageDialog(view.getFrame(),
////                                "Porcentaje inválido. Debe estar entre 1 y 5000 (o el rango que definas).",
////                                "Error de Entrada", JOptionPane.ERROR_MESSAGE);
////                    }
////                } catch (NumberFormatException ex) {
////                    // 4.6.2.4. Mostrar error si la entrada no es un número válido.
////                    JOptionPane.showMessageDialog(view.getFrame(),
////                            "Entrada inválida. Por favor, introduce un número.",
////                            "Error de Formato", JOptionPane.ERROR_MESSAGE);
////                }
////            } // Fin del if (input != null)
////        });
////        percentageMenu.add(otherItem); // Añadir el ítem "Otro..." al popup.
////
////        // 4.7. Mostrar el JPopupMenu.
////        //      Se muestra relativo al 'anchorComponent' (el JLabel/JButton que se clickeó),
////        //      posicionado justo debajo de él (x=0, y=altura del componente).
////        percentageMenu.show(anchorComponent, 0, anchorComponent.getHeight());
////        System.out.println("  [InfoBarManager] JPopupMenu de porcentaje mostrado.");
////    }
////
////    /**
////     * Muestra un JPopupMenu para que el usuario seleccione un nuevo modo de zoom.
////     *
////     * @param anchorComponent El componente Swing (generalmente un JButton) sobre el cual
////     *                        se mostrará el JPopupMenu.
////     */
////    private void showZoomModeSelectionPopup(Component anchorComponent) {
////        // 4.8. Validar dependencias (modelo, vista, actionMap).
////        if (model == null || view == null || uiConfig.actionMap == null) {
////            System.err.println("ERROR [InfoBarManager.showZoomModeSelectionPopup]: Modelo, Vista o ActionMap nulos.");
////            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar modos de zoom.", "Error", JOptionPane.ERROR_MESSAGE);
////            return;
////        }
////
////        // 4.9. Crear el JPopupMenu.
////        JPopupMenu zoomMenu = new JPopupMenu("Seleccionar Modo de Zoom");
////
////        // 4.10. Obtener el mapa de acciones desde uiConfig.
////        java.util.Map<String, Action> actionMapRef = uiConfig.actionMap;
////
////        // 4.11. Añadir JMenuItems al popup para cada modo de zoom relevante.
////        //       Cada JMenuItem se crea directamente a partir de la Action correspondiente.
////        //       La Action ya tiene nombre, icono (si se configuró), y su actionPerformed.
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR), zoomFitPageIcon);    // FIT_TO_SCREEN ("Ajustar")
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AUTO), zoomActualSizeIcon); // DISPLAY_ORIGINAL ("Tamaño Real")
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ANCHO), zoomFitWidthIcon);
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ALTO), zoomFitHeightIcon);
////        zoomMenu.addSeparator(); // Separador visual
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO), zoomUserSpecifiedIcon);
////        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_FIJO), zoomFixedIcon); // MAINTAIN_CURRENT_ZOOM ("Zoom Fijo")
////
////        // 4.12. Mostrar el JPopupMenu.
////        zoomMenu.show(anchorComponent, 0, anchorComponent.getHeight());
////        System.out.println("  [InfoBarManager] JPopupMenu de selección de modo de zoom mostrado.");
////    }
////
////    /**
////     * Método helper para añadir un JMenuItem (creado a partir de una Action) a un JPopupMenu.
////     * También asigna un icono al JMenuItem si se proporciona.
////     *
////     * @param menu El JPopupMenu al que se añadirá el ítem.
////     * @param action La Action a partir de la cual se creará el JMenuItem.
////     *               El JMenuItem tomará el nombre y el estado enabled de la Action.
////     *               El estado SELECTED_KEY de la Action (si es un JRadioButtonMenuItem)
////     *               determinará si aparece marcado.
////     * @param icon El ImageIcon a asignar al JMenuItem (puede ser null).
////     */
////    private void addZoomModeMenuItem(JPopupMenu menu, Action action, ImageIcon icon) {
////        if (action != null) {
////            // Crear el JMenuItem directamente desde la Action.
////            // Si la Action está asociada a un JRadioButtonMenuItem en el menú principal,
////            // y ese JRadioButtonMenuItem se crea con new JRadioButtonMenuItem(action),
////            // entonces el estado Action.SELECTED_KEY se reflejará aquí.
////            JMenuItem menuItem = new JMenuItem(action);
////
////            // Asignar el icono si se proporciona.
////            if (icon != null) {
////                menuItem.setIcon(icon);
////            }
////            // Opcional: Si quieres un texto en este popup diferente al Action.NAME,
////            // puedes descomentar y ajustar lo siguiente:
////            // menuItem.setText("Texto específico para este popup: " + action.getValue(Action.NAME));
////
////            menu.add(menuItem); // Añadir al JPopupMenu.
////        } else {
////            System.err.println("WARN [InfoBarManager addZoomModeMenuItem]: La acción proporcionada es null. No se añadió ítem al popup.");
////        }
////    }
////
////    
////    
////    
////    // --- SECCIÓN 5: OTROS MÉTODOS (formatFileSize) ---
////    /**
////     * 5.1. Formatea un tamaño de archivo en bytes a una cadena legible (KB, MB, GB, etc.).
////     */
////    private String formatFileSize(long bytes) {
////        if (bytes < 0) return "N/A"; // Manejar tamaño inválido
////        if (bytes < 1024) return bytes + " B";
////        int exp = (int) (Math.log(bytes) / Math.log(1024));
////        // Asegurar que el índice de 'pre' sea válido
////        String pre = (exp > 0 && exp <= "KMGTPE".length()) ? "KMGTPE".charAt(exp - 1) + "" : "";
////        // Si exp es 0 (porque bytes < 1024, pero ya manejado) o si es > length, pre será vacío.
////        // Si pre está vacío, es porque es B o un tamaño muy grande, en ese caso el cálculo es directo.
////        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
////    }
////    
////    
////    // --- SECCIÓN 5: OTROS MÉTODOS (formatFileSize si fuera necesario aquí) ---
////    // El método formatFileSize no se usa directamente en InfoBarManager por ahora,
////    // ya que la información de tamaño de archivo es un TODO.
////    // Si se implementara, podría estar aquí o en una clase de utilidades.
////
////} // --- FIN DE LA CLASE InfoBarManager ---