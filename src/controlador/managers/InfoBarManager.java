package controlador.managers;

import java.awt.Color;
import java.awt.Component; // Para el ancla del JPopupMenu
import java.awt.Cursor;   // Para el cursor de mano
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.text.DecimalFormat;
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

import controlador.commands.AppActionCommands; // Necesario para los comandos de Action
import modelo.VisorModel;
import servicios.ConfigurationManager; // Para las claves de configuración
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.config.ViewUIConfig; // Asumo que uiConfig se pasa y tiene todo

public class InfoBarManager {

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA (DEPENDENCIAS Y ESTADO) ---
    // 1.1. Referencias a componentes principales del sistema
    private final VisorModel model;
    private final VisorView view;
    private final ViewUIConfig uiConfig; // Contiene ConfigurationManager, ActionMap, IconUtils, colores, etc.

    // 1.2. Formateadores y otros helpers
    private final DecimalFormat dfPorcentajeZoomLabel = new DecimalFormat("0'%'"); // Ej: "100%"

    // 1.3. Iconos para el botón de modo de zoom (cargados en el constructor)
    private ImageIcon zoomFitWidthIcon;
    private ImageIcon zoomFitHeightIcon;
    private ImageIcon zoomFitPageIcon;
    private ImageIcon zoomActualSizeIcon;
    private ImageIcon zoomUserSpecifiedIcon;
    private ImageIcon zoomFixedIcon;
    private final Map<String, String> comandoToClaveIconoMap;

    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * Constructor de InfoBarManager.
     * Inicializa las referencias y configura los listeners para los controles interactivos
     * de las barras de información.
     *
     * @param model La instancia del VisorModel.
     * @param view La instancia de la VisorView.
     * @param uiConfig La configuración de la UI que contiene ConfigurationManager, ActionMap, etc.
     */
    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig, Map<String, String> comandoToClaveIconoMap) {
        // 2.1. Asignar dependencias principales, validando que no sean nulas.
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
        this.comandoToClaveIconoMap = Objects.requireNonNull(comandoToClaveIconoMap, "comandoToClaveIconoMap no puede ser null");
        
        // 2.2. Log de inicialización.
        System.out.println("[InfoBarManager] Instancia creada. Configurando listeners...");

        // 2.3. Cargar los iconos necesarios para los modos de zoom.
        cargarIconosModoZoom();

        // 2.4. Configurar listeners para los controles de zoom en la barra inferior.

        // 2.4.1. Listener para el JLabel/Botón de Porcentaje de Zoom Personalizado.
        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
        if (porcentajeLabel != null) {
            porcentajeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Al hacer clic, mostrar el JPopupMenu para seleccionar/ingresar porcentaje.
                    System.out.println("  [InfoBarManager Listener] Clic en PorcentajeZoomPersonalizadoLabel.");
                    showCustomZoomPercentagePopup(porcentajeLabel); // 'porcentajeLabel' actúa como ancla.
                }
                // Cambiar cursor a mano al pasar por encima para indicar que es clickeable.
                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(Cursor.getDefaultCursor()); }
            });
            System.out.println("    -> MouseListener añadido a PorcentajeZoomPersonalizadoLabel.");
        } else {
            System.err.println("WARN [InfoBarManager Constructor]: view.getPorcentajeZoomPersonalizadoLabel() es null. Listener no añadido.");
        }

        // 2.4.2. Listener para el JButton del Icono del Modo de Zoom Actual.
        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
        if (modoZoomBoton != null) {
            modoZoomBoton.addActionListener(e -> {
                // Al hacer clic, mostrar el JPopupMenu para seleccionar un nuevo modo de zoom.
                System.out.println("  [InfoBarManager Listener] Clic en ModoZoomActualIconoBoton.");
                showZoomModeSelectionPopup(modoZoomBoton); // 'modoZoomBoton' actúa como ancla.
            });
            System.out.println("    -> ActionListener añadido a ModoZoomActualIconoBoton.");
        } else {
            System.err.println("WARN [InfoBarManager Constructor]: view.getModoZoomActualIconoBoton() es null. Listener no añadido.");
        }
        System.out.println("[InfoBarManager] Configuración de listeners finalizada.");
    } // --- FIN DEL CONSTRUCTOR ---

    // --- SECCIÓN 3: MÉTODOS DE ACTUALIZACIÓN DE BARRAS ---

    /**
     * Método principal para solicitar la actualización de todas las barras de información.
     * Se asegura de que la actualización se realice en el Event Dispatch Thread (EDT).
     */
    public void actualizarBarrasDeInfo() {
        // 3.1. Validar dependencias antes de programar en el EDT.
        if (view == null || model == null || uiConfig == null) {
            System.err.println("ERROR [InfoBarManager.actualizarBarrasDeInfo]: Dependencias nulas (Vista, Modelo o uiConfig). No se pueden actualizar barras.");
            return;
        }

        // 3.2. Programar la actualización de las barras internas en el EDT.
        SwingUtilities.invokeLater(() -> {
            // System.out.println("  [InfoBarManager EDT] Ejecutando actualización de barras..."); // Log opcional
            actualizarBarraInfoSuperiorInterno();
            actualizarBarraEstadoInferiorInterno();
            // System.out.println("  [InfoBarManager EDT] Actualización de barras completada."); // Log opcional
        });
    }

    /**
     * Método interno para actualizar los componentes de la barra de información superior.
     * Se asume que se llama desde el EDT.
     */
    private void actualizarBarraInfoSuperiorInterno() {
        // --- 3.1. Actualizar Nombre del Archivo ---
        String nombreArchivoDisplay = "Archivo: N/A";
        Path rutaCompletaActual = null;
        if (model.getSelectedImageKey() != null) {
            rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaCompletaActual != null) {
                nombreArchivoDisplay = "Archivo: " + rutaCompletaActual.getFileName().toString();
            } else {
                nombreArchivoDisplay = "Archivo: " + model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        }
        if (view.getNombreArchivoInfoLabel() != null) {
            view.getNombreArchivoInfoLabel().setText(nombreArchivoDisplay);
        }

        // --- 3.2. Actualizar Índice Actual / Total de Imágenes ---
        int indiceActual = -1;
        int totalImagenes = 0;
        if (model.getModeloLista() != null) {
            totalImagenes = model.getModeloLista().getSize();
            if (model.getSelectedImageKey() != null && totalImagenes > 0) {
                // El índice que se muestra es 1-based para el usuario.
                indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
            }
        }
        String indiceTotalDisplay;
        if (totalImagenes > 0 && indiceActual != -1) {
            indiceTotalDisplay = "Idx: " + (indiceActual + 1) + "/" + totalImagenes;
        } else if (totalImagenes > 0) {
            indiceTotalDisplay = "Idx: -/" + totalImagenes;
        } else {
            indiceTotalDisplay = "Idx: 0/0";
        }
        if (view.getIndiceTotalInfoLabel() != null) {
            view.getIndiceTotalInfoLabel().setText(indiceTotalDisplay);
        }

        // --- 3.3. Actualizar Dimensiones Originales de la Imagen ---
        String dimsDisplay = "Dim: N/A";
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal != null) {
            dimsDisplay = "Dim: " + imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
        }
        if (view.getDimensionesOriginalesInfoLabel() != null) {
            view.getDimensionesOriginalesInfoLabel().setText(dimsDisplay);
        }

        // --- 3.4. Actualizar Tamaño del Archivo (Placeholder - Implementación Futura) ---
        String tamanoDisplay = "Tam: N/A"; // TODO: Implementar obtención de tamaño de archivo
        if (view.getTamanoArchivoInfoLabel() != null) {
            view.getTamanoArchivoInfoLabel().setText(tamanoDisplay);
        }

        // --- 3.5. Actualizar Fecha de Modificación del Archivo (Placeholder - Implementación Futura) ---
        String fechaDisplay = "Fch: N/A"; // TODO: Implementar obtención de fecha de archivo
        if (view.getFechaArchivoInfoLabel() != null) {
            view.getFechaArchivoInfoLabel().setText(fechaDisplay);
        }

        // --- 3.6. Actualizar Nombre del Modo de Zoom Activo ---
        String modoZoomDisplay = "Modo: N/A";
        if (model.getCurrentZoomMode() != null) {
            modoZoomDisplay = "Modo: " + model.getCurrentZoomMode().getNombreLegible();
        }
        if (view.getModoZoomNombreInfoLabel() != null) {
            view.getModoZoomNombreInfoLabel().setText(modoZoomDisplay);
        }

        // --- 3.7. Actualizar Porcentaje de Zoom Visual Resultante ---
        String zoomPctDisplay = "%Z: N/A";
        if (model.getCurrentImage() != null) { // Solo mostrar si hay imagen
            // El factor de zoom se multiplica por 100 para mostrar como porcentaje.
            zoomPctDisplay = String.format("%%Z: %.0f%%", model.getZoomFactor() * 100);
        }
        if (view.getPorcentajeZoomVisualRealInfoLabel() != null) {
            view.getPorcentajeZoomVisualRealInfoLabel().setText(zoomPctDisplay);
        }
    }

    /**
     * Método interno para actualizar los componentes de la barra de estado/control inferior.
     * Se asume que se llama desde el EDT.
     */
    private void actualizarBarraEstadoInferiorInterno() {
        // --- 3.8. Actualizar Ruta Completa del Archivo ---
        String rutaTextoDisplay = "Ruta: (ninguna imagen seleccionada)";
        Path rutaActualParaBarra = null;
        if (model.getSelectedImageKey() != null) {
            rutaActualParaBarra = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaActualParaBarra != null) {
                rutaTextoDisplay = rutaActualParaBarra.toString();
            } else {
                rutaTextoDisplay = model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        } else if (model.getCarpetaRaizActual() != null) {
            rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
        }
        if (view.getRutaCompletaArchivoLabel() != null) {
            view.getRutaCompletaArchivoLabel().setText(rutaTextoDisplay);
        }

        // --- 3.9. Actualizar Indicadores de Estado (Iconos ZM, Prop, SubC) ---
        //      Los colores vienen de uiConfig (o podrían ser defaults si uiConfig es null).
        Color colorFondoActivo = (uiConfig != null && uiConfig.colorBotonActivado != null)
                                 ? uiConfig.colorBotonActivado : Color.CYAN; // Fallback
        Color colorFondoInactivo = (uiConfig != null && uiConfig.colorFondoSecundario != null)
                                   ? uiConfig.colorFondoSecundario : Color.LIGHT_GRAY; // Fallback

        // 3.9.1. Indicador Zoom Manual (ZM)
        JLabel zmIconLabel = view.getIconoZoomManualLabel();
        if (zmIconLabel != null) {
            boolean zoomManualEstaActivo = model.isZoomHabilitado();
            zmIconLabel.setToolTipText(zoomManualEstaActivo ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
            zmIconLabel.setBackground(zoomManualEstaActivo ? colorFondoActivo : colorFondoInactivo);
        }

        // 3.9.2. Indicador Mantener Proporciones (Prop)
        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
        if (propIconLabel != null) {
            boolean proporcionesEstanActivas = model.isMantenerProporcion();
            propIconLabel.setToolTipText(proporcionesEstanActivas ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
            propIconLabel.setBackground(proporcionesEstanActivas ? colorFondoActivo : colorFondoInactivo);
        }

        // 3.9.3. Indicador Modo Subcarpetas (SubC)
        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
        if (subcIconLabel != null) {
            // El estado activo es cuando SÍ se incluyen subcarpetas,
            // que es lo opuesto a model.isMostrarSoloCarpetaActual().
            boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
            subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
            subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
        }

        // --- 3.10. Actualizar Controles de Zoom Personalizados ---

        // 3.10.1. Actualizar Label de Porcentaje de Zoom Personalizado
        JLabel porcentajeLabel = view.getPorcentajeZoomPersonalizadoLabel();
        if (porcentajeLabel != null && uiConfig.configurationManager != null) {
            // Usa el nuevo método getZoomPersonalizadoPorcentaje() de ConfigurationManager
            double customZoomPercent = uiConfig.configurationManager.getZoomPersonalizadoPorcentaje();
            porcentajeLabel.setText(dfPorcentajeZoomLabel.format(customZoomPercent));
        } else if (porcentajeLabel == null) {
            System.err.println("WARN [InfoBarManager actualizarInferior]: view.getPorcentajeZoomPersonalizadoLabel() es null.");
        } else { // configurationManager es null
            System.err.println("WARN [InfoBarManager actualizarInferior]: uiConfig.configurationManager es null. No se puede leer zoom personalizado.");
            if (porcentajeLabel != null) porcentajeLabel.setText("Z: N/A");
        }

        // 3.10.2. Actualizar Icono y Tooltip del Botón de Modo de Zoom Actual
        JButton modoZoomBoton = view.getModoZoomActualIconoBoton();
        if (modoZoomBoton != null && model.getCurrentZoomMode() != null) {
            ZoomModeEnum currentMode = model.getCurrentZoomMode();
            ImageIcon iconToShow = null;
            String tooltipText = "Modo: ";

            switch (currentMode) {
                case FIT_TO_WIDTH: iconToShow = zoomFitWidthIcon; tooltipText += "Ajustar Ancho"; break;
                case FIT_TO_HEIGHT: iconToShow = zoomFitHeightIcon; tooltipText += "Ajustar Alto"; break;
                case FIT_TO_SCREEN: iconToShow = zoomFitPageIcon; tooltipText += "Ajustar Página"; break;
                case DISPLAY_ORIGINAL: iconToShow = zoomActualSizeIcon; tooltipText += "Tamaño Real (100%)"; break;
                case USER_SPECIFIED_PERCENTAGE: iconToShow = zoomUserSpecifiedIcon; tooltipText += "Zoom Personalizado"; break;
                case MAINTAIN_CURRENT_ZOOM: iconToShow = zoomFixedIcon; tooltipText += "Zoom Fijo"; break;
                default: tooltipText += "Desconocido"; break; // No debería ocurrir
            }
            modoZoomBoton.setIcon(iconToShow);
            modoZoomBoton.setToolTipText(tooltipText + ". Click para cambiar.");
            // Podrías querer cambiar el fondo del botón si ciertos modos están "activos"
            // de una manera especial, pero por ahora solo se actualiza el icono/tooltip.
        } else if (modoZoomBoton == null) {
            System.err.println("WARN [InfoBarManager actualizarInferior]: view.getModoZoomActualIconoBoton() es null.");
        }

        // --- 3.11. Actualizar Mensajes de la Aplicación (si hubiera un sistema para ellos) ---
        // JLabel mensajesLabel = view.getMensajesAppLabel();
        // if (mensajesLabel != null) { /* ... actualizar mensajes ... */ }
    }


    // --- SECCIÓN 4: MÉTODOS PRIVADOS AUXILIARES ---

    /**
     * Carga los iconos para los diferentes modos de zoom.
     * Se llama desde el constructor.
     */
    private void cargarIconosModoZoom() {
        if (uiConfig.iconUtils != null && this.comandoToClaveIconoMap != null) {
            int iconSize = (uiConfig.iconoAncho > 0) ? uiConfig.iconoAncho : 16;

            // Función auxiliar para obtener el icono o loguear si falta la clave
            java.util.function.Function<String, ImageIcon> getIcon = (comando) -> {
                String claveIcono = this.comandoToClaveIconoMap.get(comando);
                
                if (claveIcono != null) {
                	ImageIcon icon = uiConfig.iconUtils.getScaledIcon(claveIcono, iconSize, iconSize);
                	
                    return icon;
                	
                } else {
                    System.err.println("WARN [InfoBarManager.cargarIconosModoZoom]: No se encontró clave de icono para el comando: " + comando + " en comandoToClaveIconoMap.");
                    return null;
                }
            };

            zoomFitWidthIcon      = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ANCHO);
            zoomFitHeightIcon     = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ALTO);
            zoomFitPageIcon       = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR);    // Corresponde a FIT_TO_SCREEN
            zoomActualSizeIcon    = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_AUTO);       // Corresponde a DISPLAY_ORIGINAL ("Zoom Automático")
            zoomUserSpecifiedIcon = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
            zoomFixedIcon         = getIcon.apply(AppActionCommands.CMD_ZOOM_TIPO_FIJO);         // Corresponde a MAINTAIN_CURRENT_ZOOM

            System.out.println("    [InfoBarManager] Iconos de modo de zoom cargados dinámicamente desde comandoToClaveIconoMap.");

        } else {
            if (uiConfig.iconUtils == null) {
                System.err.println("WARN [InfoBarManager cargarIconosModoZoom]: IconUtils es null en uiConfig.");
            }
            if (this.comandoToClaveIconoMap == null) {
                System.err.println("WARN [InfoBarManager cargarIconosModoZoom]: comandoToClaveIconoMap es null.");
            }
            System.err.println("    -> No se cargarán iconos de modo de zoom.");
        }
    }
    

    /**
     * Muestra un JPopupMenu para que el usuario seleccione o introduzca un porcentaje
     * de zoom personalizado.
     *
     * @param anchorComponent El componente Swing (JLabel o JButton) sobre el cual
     *                        se mostrará el JPopupMenu.
     */
    private void showCustomZoomPercentagePopup(Component anchorComponent) {
        // 4.1. Validar que las dependencias necesarias estén disponibles.
        //      uiConfig debe tener configurationManager y actionMap no nulos.
        if (uiConfig == null || uiConfig.configurationManager == null || uiConfig.actionMap == null) {
            System.err.println("ERROR [InfoBarManager.showCustomZoomPercentagePopup]: uiConfig, ConfigurationManager o ActionMap nulos.");
            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar el zoom.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4.2. Crear el JPopupMenu.
        JPopupMenu percentageMenu = new JPopupMenu("Seleccionar Porcentaje de Zoom");

        // 4.3. Definir los porcentajes predefinidos a mostrar en el menú.
        int[] predefinedPercentages = {25, 50, 75, 100, 125, 150, 200, 300, 400, 500};

        // 4.4. Crear un JMenuItem para cada porcentaje predefinido.
        for (int percent : predefinedPercentages) {
            JMenuItem item = new JMenuItem(percent + "%");
            // ActionListener para cada ítem predefinido:
            item.addActionListener(e -> {
                System.out.println("  [InfoBarManager Popup] Porcentaje predefinido seleccionado: " + percent + "%");
                // 4.4.1. Actualizar la configuración con el nuevo porcentaje.
                //         Usa el método setZoomPersonalizadoPorcentaje que añadimos/verificamos.
                uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percent);

                // 4.4.2. Obtener y ejecutar la Action para aplicar el modo USER_SPECIFIED_PERCENTAGE.
                Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
                if (aplicarUserSpecifiedAction != null) {
                    // La Action AplicarModoZoomAction no es un toggle, simplemente se ejecuta.
                    // Al ejecutarse, leerá el nuevo porcentaje de la configuración.
                    aplicarUserSpecifiedAction.actionPerformed(
                        new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
                    );
                    System.out.println("    -> Action CMD_ZOOM_TIPO_ESPECIFICADO ejecutada.");
                } else {
                    System.err.println("ERROR [InfoBarManager Popup]: Action CMD_ZOOM_TIPO_ESPECIFICADO no encontrada en actionMap.");
                }
                // La barra de información (incluido el label de porcentaje) se actualizará
                // como resultado de que ZoomManager llame a infoBarManager.actualizarBarrasDeInfo()
                // después de aplicar el nuevo modo de zoom.
            });
            percentageMenu.add(item); // Añadir el ítem al popup.
        }

        // 4.5. Añadir un separador visual al popup.
        percentageMenu.addSeparator();

        // 4.6. Crear el JMenuItem "Otro..." para entrada manual.
        JMenuItem otherItem = new JMenuItem("Otro...");
        // ActionListener para el ítem "Otro...":
        otherItem.addActionListener(e -> {
            System.out.println("  [InfoBarManager Popup] Opción 'Otro...' seleccionada.");
            // 4.6.1. Mostrar un JOptionPane para que el usuario ingrese el porcentaje.
            String input = JOptionPane.showInputDialog(
                    view.getFrame(), // Padre del diálogo
                    "Introduce el porcentaje de zoom (ej: 150):", // Mensaje
                    "Zoom Personalizado", // Título del diálogo
                    JOptionPane.PLAIN_MESSAGE); // Tipo de mensaje

            // 4.6.2. Procesar la entrada del usuario.
            if (input != null && !input.trim().isEmpty()) { // Si el usuario no canceló y escribió algo
                try {
                    // 4.6.2.1. Limpiar la entrada (quitar '%' y espacios) y convertir a double.
                    input = input.replace("%", "").trim();
                    double percentValue = Double.parseDouble(input);

                    // 4.6.2.2. Validar el rango del porcentaje (ajusta según tus necesidades).
                    if (percentValue >= 1 && percentValue <= 5000) { // Ej. de 1% a 5000%
                        // 4.6.2.2.1. Actualizar la configuración.
                        uiConfig.configurationManager.setZoomPersonalizadoPorcentaje(percentValue);
                        System.out.println("    -> Configuración actualizada con porcentaje manual: " + percentValue + "%");

                        // 4.6.2.2.2. Obtener y ejecutar la Action.
                        Action aplicarUserSpecifiedAction = uiConfig.actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
                        if (aplicarUserSpecifiedAction != null) {
                            aplicarUserSpecifiedAction.actionPerformed(
                                new ActionEvent(anchorComponent, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO)
                            );
                            System.out.println("    -> Action CMD_ZOOM_TIPO_ESPECIFICADO ejecutada.");
                        } else {
                            System.err.println("ERROR [InfoBarManager Popup 'Otro...']: Action CMD_ZOOM_TIPO_ESPECIFICADO no encontrada.");
                        }
                    } else {
                        // 4.6.2.3. Mostrar error si el porcentaje está fuera de rango.
                        JOptionPane.showMessageDialog(view.getFrame(),
                                "Porcentaje inválido. Debe estar entre 1 y 5000 (o el rango que definas).",
                                "Error de Entrada", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    // 4.6.2.4. Mostrar error si la entrada no es un número válido.
                    JOptionPane.showMessageDialog(view.getFrame(),
                            "Entrada inválida. Por favor, introduce un número.",
                            "Error de Formato", JOptionPane.ERROR_MESSAGE);
                }
            } // Fin del if (input != null)
        });
        percentageMenu.add(otherItem); // Añadir el ítem "Otro..." al popup.

        // 4.7. Mostrar el JPopupMenu.
        //      Se muestra relativo al 'anchorComponent' (el JLabel/JButton que se clickeó),
        //      posicionado justo debajo de él (x=0, y=altura del componente).
        percentageMenu.show(anchorComponent, 0, anchorComponent.getHeight());
        System.out.println("  [InfoBarManager] JPopupMenu de porcentaje mostrado.");
    }

    /**
     * Muestra un JPopupMenu para que el usuario seleccione un nuevo modo de zoom.
     *
     * @param anchorComponent El componente Swing (generalmente un JButton) sobre el cual
     *                        se mostrará el JPopupMenu.
     */
    private void showZoomModeSelectionPopup(Component anchorComponent) {
        // 4.8. Validar dependencias (modelo, vista, actionMap).
        if (model == null || view == null || uiConfig.actionMap == null) {
            System.err.println("ERROR [InfoBarManager.showZoomModeSelectionPopup]: Modelo, Vista o ActionMap nulos.");
            JOptionPane.showMessageDialog(anchorComponent, "Error interno al configurar modos de zoom.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4.9. Crear el JPopupMenu.
        JPopupMenu zoomMenu = new JPopupMenu("Seleccionar Modo de Zoom");

        // 4.10. Obtener el mapa de acciones desde uiConfig.
        java.util.Map<String, Action> actionMapRef = uiConfig.actionMap;

        // 4.11. Añadir JMenuItems al popup para cada modo de zoom relevante.
        //       Cada JMenuItem se crea directamente a partir de la Action correspondiente.
        //       La Action ya tiene nombre, icono (si se configuró), y su actionPerformed.
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR), zoomFitPageIcon);    // FIT_TO_SCREEN ("Ajustar")
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_AUTO), zoomActualSizeIcon); // DISPLAY_ORIGINAL ("Tamaño Real")
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ANCHO), zoomFitWidthIcon);
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ALTO), zoomFitHeightIcon);
        zoomMenu.addSeparator(); // Separador visual
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO), zoomUserSpecifiedIcon);
        addZoomModeMenuItem(zoomMenu, actionMapRef.get(AppActionCommands.CMD_ZOOM_TIPO_FIJO), zoomFixedIcon); // MAINTAIN_CURRENT_ZOOM ("Zoom Fijo")

        // 4.12. Mostrar el JPopupMenu.
        zoomMenu.show(anchorComponent, 0, anchorComponent.getHeight());
        System.out.println("  [InfoBarManager] JPopupMenu de selección de modo de zoom mostrado.");
    }

    /**
     * Método helper para añadir un JMenuItem (creado a partir de una Action) a un JPopupMenu.
     * También asigna un icono al JMenuItem si se proporciona.
     *
     * @param menu El JPopupMenu al que se añadirá el ítem.
     * @param action La Action a partir de la cual se creará el JMenuItem.
     *               El JMenuItem tomará el nombre y el estado enabled de la Action.
     *               El estado SELECTED_KEY de la Action (si es un JRadioButtonMenuItem)
     *               determinará si aparece marcado.
     * @param icon El ImageIcon a asignar al JMenuItem (puede ser null).
     */
    private void addZoomModeMenuItem(JPopupMenu menu, Action action, ImageIcon icon) {
        if (action != null) {
            // Crear el JMenuItem directamente desde la Action.
            // Si la Action está asociada a un JRadioButtonMenuItem en el menú principal,
            // y ese JRadioButtonMenuItem se crea con new JRadioButtonMenuItem(action),
            // entonces el estado Action.SELECTED_KEY se reflejará aquí.
            JMenuItem menuItem = new JMenuItem(action);

            // Asignar el icono si se proporciona.
            if (icon != null) {
                menuItem.setIcon(icon);
            }
            // Opcional: Si quieres un texto en este popup diferente al Action.NAME,
            // puedes descomentar y ajustar lo siguiente:
            // menuItem.setText("Texto específico para este popup: " + action.getValue(Action.NAME));

            menu.add(menuItem); // Añadir al JPopupMenu.
        } else {
            System.err.println("WARN [InfoBarManager addZoomModeMenuItem]: La acción proporcionada es null. No se añadió ítem al popup.");
        }
    }

    // --- SECCIÓN 5: OTROS MÉTODOS (formatFileSize si fuera necesario aquí) ---
    // El método formatFileSize no se usa directamente en InfoBarManager por ahora,
    // ya que la información de tamaño de archivo es un TODO.
    // Si se implementara, podría estar aquí o en una clase de utilidades.

} // --- FIN DE LA CLASE InfoBarManager ---