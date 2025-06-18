//FIXME BORRAR ESTA CLASE SI TODO FUNCIONA

//package controlador.managers;
//
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Cursor;
//// import java.awt.Dimension; // No se usa directamente aquí
//import java.awt.event.ActionEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
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
//import javax.swing.SwingUtilities;
//
//import controlador.commands.AppActionCommands;
//import controlador.utils.ComponentRegistry;
//import modelo.VisorModel;
//import servicios.ConfigKeys;
//import servicios.ConfigurationManager;
//import servicios.ProjectManager;
//import servicios.zoom.ZoomModeEnum;
//import utils.ImageUtils;
//import vista.VisorView;
//import vista.config.ViewUIConfig;
//import vista.theme.ThemeManager;
//
//public class InfoBarManager {
//
//    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS Y ESTADO) ---
//    private final VisorModel model;
//    private final ComponentRegistry registry;
//    private final ThemeManager themeManager;
//    private final ConfigurationManager configuration;
//    private final ProjectManager projectService;
//    
////    private final VisorView view;
////    private final ViewUIConfig uiConfig;
//
//    private final DecimalFormat dfPorcentajeZoomLabel = new DecimalFormat("0'%'");
//    private final SimpleDateFormat sdfFechaArchivo = new SimpleDateFormat("dd/MM/yy HH:mm");
//
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
//    /**
//     * 2.0. Constructor de InfoBarManager.
//     */
//    public InfoBarManager(
//    		VisorModel model, 
//    		ComponentRegistry registry, 
//            ThemeManager themeManager,   
//            ConfigurationManager configuration,
//    		Map<String, String> comandoToClaveIconoMap, 
//    		ProjectManager projectService) 
//    {
//        // 2.1. Asignar dependencias principales
//        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
////        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
////        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
//        this.registry = Objects.requireNonNull(registry);
//        this.themeManager = Objects.requireNonNull(themeManager);
//        this.configuration = Objects.requireNonNull(configuration);
////        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
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
//            actualizarBarraInfoSuperiorInterno();
//            actualizarBarraEstadoInferiorInterno();
//        });
//    }
//
//    /**
//     * 3.2. Método interno para actualizar los componentes de la barra de información superior.
//     *      Lee la configuración de visibilidad para CADA ELEMENTO.
//     */
//    private void actualizarBarraInfoSuperiorInterno() {
//        ConfigurationManager cfg = uiConfig.configurationManager;
//
//        // 3.2.1. Visibilidad del PANEL COMPLETO de la Barra Superior
//        boolean panelBarraSuperiorVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_VISIBLE, true);
//        if (view.getPanelBarraSuperior() != null) {
//            view.getPanelBarraSuperior().setVisible(panelBarraSuperiorVisible);
//        } else {
//            System.err.println("WARN [InfoBarManager]: view.getPanelBarraSuperior() es null. No se puede gestionar visibilidad del panel.");
//            if (!panelBarraSuperiorVisible) return;
//        }
//        if (!panelBarraSuperiorVisible) return; // Si el panel está oculto, no procesar sus componentes internos.
//
//        // 3.2.2. Nombre del Archivo / Ruta (Superior)
//        JLabel nombreArchivoLabelSup = view.getNombreArchivoInfoLabel();
//        if (nombreArchivoLabelSup != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, true);
//            nombreArchivoLabelSup.setVisible(esVisible);
//            if (esVisible) {
//                String nombreArchivoDisplay = "N/A";
//                Path rutaCompletaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaCompletaActual != null) {
//                    String formato = cfg.getString(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
//                    nombreArchivoDisplay = "ruta_completa".equalsIgnoreCase(formato) ? rutaCompletaActual.toString() : rutaCompletaActual.getFileName().toString();
//                } else if (model.getSelectedImageKey() != null) {
//                    nombreArchivoDisplay = model.getSelectedImageKey() + " (Ruta no enc.)";
//                } else if (model.getCarpetaRaizActual() != null){
//                     nombreArchivoDisplay = "Carpeta: " + model.getCarpetaRaizActual().getFileName().toString();
//                }
//                nombreArchivoLabelSup.setText(nombreArchivoDisplay);
//                nombreArchivoLabelSup.setToolTipText(nombreArchivoDisplay);
//            } else {
//                nombreArchivoLabelSup.setText("");
//            }
//        }
//
//        // 3.2.3. Índice Actual / Total de Imágenes
//        JLabel indiceTotalLabel = view.getIndiceTotalInfoLabel();
//        if (indiceTotalLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, true);
//            indiceTotalLabel.setVisible(esVisible);
//            if (esVisible) {
//                int indiceActual = -1, totalImagenes = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
//                if (model.getSelectedImageKey() != null && totalImagenes > 0) indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
//                String display = (totalImagenes > 0 && indiceActual != -1) ? (indiceActual + 1) + "/" + totalImagenes : (totalImagenes > 0 ? "-/" + totalImagenes : "0/0");
//                indiceTotalLabel.setText("Idx: " + display);
//            } else {
//                indiceTotalLabel.setText("");
//            }
//        }
//
//        // 3.2.4. Dimensiones Originales de la Imagen
//        JLabel dimensionesLabel = view.getDimensionesOriginalesInfoLabel();
//        if (dimensionesLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, true);
//            dimensionesLabel.setVisible(esVisible);
//            if (esVisible) {
//                String dimsDisplay = "N/A";
//                BufferedImage imgOriginal = model.getCurrentImage();
//                if (imgOriginal != null) dimsDisplay = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
//                dimensionesLabel.setText("Dim: " + dimsDisplay);
//            } else {
//                dimensionesLabel.setText("");
//            }
//        }
//
//        // 3.2.5. Tamaño del Archivo
//        JLabel tamanoArchivoLabel = view.getTamanoArchivoInfoLabel();
//        if (tamanoArchivoLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, true);
//            tamanoArchivoLabel.setVisible(esVisible);
//            if (esVisible) {
//                String tamanoDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null && Files.exists(rutaActual)) {
//                    try { tamanoDisplay = formatFileSize(Files.size(rutaActual)); }
//                    catch (IOException ex) { System.err.println("Error al leer tamaño para " + rutaActual + ": " + ex.getMessage());}
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
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, true);
//            fechaArchivoLabel.setVisible(esVisible);
//            if (esVisible) {
//                String fechaDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null && Files.exists(rutaActual)) {
//                    try { fechaDisplay = sdfFechaArchivo.format(new Date(Files.getLastModifiedTime(rutaActual).toMillis())); }
//                    catch (IOException ex) { System.err.println("Error al leer fecha para " + rutaActual + ": " + ex.getMessage());}
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
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, false);
//            formatoImagenLabel.setVisible(esVisible);
//            if (esVisible) {
//                String formatoDisplay = "N/A";
//                Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//                if (rutaActual != null) formatoDisplay = ImageUtils.getImageFormat(rutaActual);
//                formatoImagenLabel.setText("Fmt: " + (formatoDisplay != null ? formatoDisplay.toUpperCase() : "N/A"));
//            } else {
//                formatoImagenLabel.setText("");
//            }
//        }
//
//        // 3.2.8. Nombre del Modo de Zoom Activo
//        JLabel modoZoomNombreLabel = view.getModoZoomNombreInfoLabel();
//        if (modoZoomNombreLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, true);
//            modoZoomNombreLabel.setVisible(esVisible);
//            if (esVisible) {
//                String modoZoomDisplay = (model.getCurrentZoomMode() != null) ? model.getCurrentZoomMode().getNombreLegible() : "N/A";
//                modoZoomNombreLabel.setText("Modo: " + modoZoomDisplay);
//            } else {
//                modoZoomNombreLabel.setText("");
//            }
//        }
//
//        // 3.2.9. Porcentaje de Zoom Visual Resultante
//        JLabel zoomRealPctLabel = view.getPorcentajeZoomVisualRealInfoLabel();
//        if (zoomRealPctLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, true);
//            zoomRealPctLabel.setVisible(esVisible);
//            if (esVisible) {
//                String zoomPctDisplay = (model.getCurrentImage() != null) ? String.format("%.0f%%", model.getZoomFactor() * 100) : "N/A";
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
//        boolean panelBarraInferiorVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_VISIBLE, true);
//        if (view.getPanelBarraEstado() != null) {
//            view.getPanelBarraEstado().setVisible(panelBarraInferiorVisible);
//        } else {
//            System.err.println("WARN [InfoBarManager]: view.getPanelBarraEstado() es null.");
//            if (!panelBarraInferiorVisible) return;
//        }
//        if (!panelBarraInferiorVisible) return;
//
//        Color colorFondoActivo = (uiConfig.colorBotonActivado != null) ? uiConfig.colorBotonActivado : Color.CYAN;
//        Color colorFondoInactivo = (uiConfig.colorFondoSecundario != null) ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY;
//
//     // 1. Obtener la ruta de la imagen actual (una sola vez)
//        Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
//        
//     // 2. Actualizar el JLabel de la RUTA/NOMBRE
//        JLabel rutaCompletaLabel = view.getRutaCompletaArchivoLabel();
//        if (rutaCompletaLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, true);
//            rutaCompletaLabel.setVisible(esVisible);
//            if (esVisible) {
//                String textoRutaDisplay = "(Ninguna imagen seleccionada)";
//                if (rutaActual != null) {
//                    // La lógica ahora es simple: solo muestra la ruta/nombre, sin el [MARCADA]
//                    String formato = cfg.getString(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "ruta_completa");
//                    textoRutaDisplay = "solo_nombre".equalsIgnoreCase(formato) 
//                        ? rutaActual.getFileName().toString() 
//                        : rutaActual.toString();
//                } else if (model.getCarpetaRaizActual() != null) {
//                    textoRutaDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
//                }
//                rutaCompletaLabel.setText(textoRutaDisplay);
//                rutaCompletaLabel.setToolTipText(textoRutaDisplay);
//            }
//        }
//        
//     // 3. Actualizar el JLabel de MENSAJES con el estado de MARCADO
//        JLabel mensajesLabel = view.getMensajesAppLabel();
//        if (mensajesLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE, true);
//            mensajesLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean estaMarcada = (rutaActual != null && projectService != null && projectService.estaMarcada(rutaActual));
//                
//                if (estaMarcada) {
//                    mensajesLabel.setText("MARCADA");
//                    // Aplicar feedback visual
//                    mensajesLabel.setForeground(Color.BLACK); // O un color de texto de énfasis
//                    mensajesLabel.setBackground(new Color(255, 230, 120)); // Un amarillo pálido, por ejemplo
////                    mensajesLabel.setBackground(colorFondoActivo);
//                    mensajesLabel.setOpaque(true);
//                } else {
//                    // Restaurar a estado normal
//                    mensajesLabel.setText(" "); // Un espacio para mantener la altura
//                    mensajesLabel.setForeground(uiConfig.colorTextoSecundario); // Color normal
//                    mensajesLabel.setOpaque(false); // Hacerlo transparente de nuevo
//                }
//            }
//        }
//        
//        // 3.3.3. Icono Zoom Manual (ZM)
//        JLabel zmIconLabel = view.getIconoZoomManualLabel();
//        if (zmIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, true);
//            zmIconLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean zoomManualActivo = model.isZoomHabilitado();
//                zmIconLabel.setToolTipText(zoomManualActivo ? "Zoom Manual: Activado (Click para cambiar)" : "Zoom Manual: Desactivado (Click para cambiar)");
//                zmIconLabel.setBackground(zoomManualActivo ? colorFondoActivo : colorFondoInactivo);
//                zmIconLabel.setOpaque(true);
//            }
//        }
//
//        // 3.3.4. Icono Mantener Proporciones (Prop)
//        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
//        if (propIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, true);
//            propIconLabel.setVisible(esVisible);
//            if (esVisible) {
//                boolean proporcionesActivas = model.isMantenerProporcion();
//                propIconLabel.setToolTipText(proporcionesActivas ? "Mantener Proporciones: Activado (Click para cambiar)" : "Mantener Proporciones: Desactivado (Click para cambiar)");
//                propIconLabel.setBackground(proporcionesActivas ? colorFondoActivo : colorFondoInactivo);
//                propIconLabel.setOpaque(true);
//            }
//        }
//
//        // 3.3.5. Icono Modo Subcarpetas (SubC)
//        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
//        if (subcIconLabel != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, true);
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
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, true);
//            porcentajeLabel.setVisible(esVisible);
//            if (esVisible) {
//                double customZoomPercent = cfg.getZoomPersonalizadoPorcentaje();
//                String textoPct = dfPorcentajeZoomLabel.format(customZoomPercent);
//                porcentajeLabel.setText(textoPct);
//                porcentajeLabel.setToolTipText("Zoom Personalizado: " + textoPct + "% (Click para cambiar)");
//            } else {
//                porcentajeLabel.setText("");
//            }
//        }
//
//        // 3.3.7. Control Modo Zoom (JButton con icono)
//        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//        if (modoZoomBoton != null) {
//            boolean esVisible = cfg.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, true);
//            modoZoomBoton.setVisible(esVisible);
//            if (esVisible) {
//                if (model.getCurrentZoomMode() != null) {
//                    // ... (lógica para setIcon y setToolTipText como antes) ...
//                    ZoomModeEnum currentMode = model.getCurrentZoomMode();
//                    ImageIcon iconToShow = null; String tooltipText = "Modo Zoom: ";
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
//                } else {
//                    modoZoomBoton.setIcon(null); modoZoomBoton.setToolTipText("Modo no definido");
//                }
//            }
//        }
//
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
//     * 4.1. Configura los listeners para los controles de zoom interactivos en la barra inferior.
//     */
//    private void configurarListenersControlesZoomInferior() {
//        // ... (tu código existente para este método) ...
//        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
//        if (porcentajeLabel != null) {
//            porcentajeLabel.addMouseListener(new MouseAdapter() {
//                @Override public void mouseClicked(MouseEvent e) { showCustomZoomPercentagePopup(porcentajeLabel); }
//                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
//                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
//            });
//        }
//        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
//        if (modoZoomBoton != null) {
//            modoZoomBoton.addActionListener(e -> showZoomModeSelectionPopup(modoZoomBoton));
//        }
//    }
//
//    /**
//     * 4.2. Carga los iconos para los diferentes modos de zoom usados en la barra inferior.
//     */
//    private void cargarIconosModoZoom() {
//        // ... (tu código existente para este método) ...
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
//        } else { System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: IconUtils o comandoToClaveIconoMap nulos."); }
//    }
//
//    /**
//     * 4.3. Muestra un JPopupMenu para que el usuario seleccione o introduzca un porcentaje de zoom personalizado.
//     */
//    private void showCustomZoomPercentagePopup(Component anchorComponent) {
//        // ... (tu código existente para este método, asegurándote de que usa this.view y this.uiConfig) ...
//        if (uiConfig == null || uiConfig.configurationManager == null || uiConfig.actionMap == null || view == null) {
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
//                    aplicarUserSpecifiedAction.actionPerformed(new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
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
//                            aplicarUserSpecifiedAction.actionPerformed(new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
//                        }
//                    } else { JOptionPane.showMessageDialog(view.getFrame(), "Porcentaje inválido.", "Error", JOptionPane.ERROR_MESSAGE); }
//                } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(view.getFrame(), "Entrada inválida.", "Error", JOptionPane.ERROR_MESSAGE); }
//            }
//        });
//        percentageMenu.add(otherItem);
//        percentageMenu.show(anchorComponent, 0, anchorComponent.getHeight());
//    }
//
//    /**
//     * 4.4. Muestra un JPopupMenu para que el usuario seleccione un nuevo modo de zoom.
//     */
//    private void showZoomModeSelectionPopup(Component anchorComponent) {
//        // ... (tu código existente para este método, asegurándote de que usa this.uiConfig) ...
//        if (model == null || view == null || uiConfig.actionMap == null) {
//            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar modos de zoom.", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//        JPopupMenu zoomMenu = new JPopupMenu("Seleccionar Modo de Zoom");
//        Map<String, Action> actionMapRef = uiConfig.actionMap;
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR), zoomFitPageIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AUTO), zoomActualSizeIcon);
//        // ... (resto de addZoomModeMenuItem)
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ANCHO), zoomFitWidthIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ALTO), zoomFitHeightIcon);
//        zoomMenu.addSeparator();
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO), zoomUserSpecifiedIcon);
//        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_FIJO), zoomFixedIcon);
//        zoomMenu.show(anchorComponent, 0, anchorComponent.getHeight());
//    }
//
//    /**
//     * 4.5. Método helper para añadir un JMenuItem a un JPopupMenu.
//     */
//    private void addZoomModeMenuItem(JPopupMenu menu, Action action, ImageIcon icon) {
//        // ... (tu código existente para este método) ...
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
//     * 4.6. Formatea un tamaño de archivo en bytes a una cadena legible (KB, MB, GB, etc.).
//     */
//    private String formatFileSize(long bytes) {
//        if (bytes < 0) return "N/A";
//        if (bytes < 1024) return bytes + " B";
//        int exp = (int) (Math.log(bytes) / Math.log(1024));
//        String pre = (exp > 0 && exp <= "KMGTPE".length()) ? "KMGTPE".charAt(exp - 1) + "" : "";
//        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre).replace(',', '.');
//    }
//}
//
