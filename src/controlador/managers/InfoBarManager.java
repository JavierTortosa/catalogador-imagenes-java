package controlador.managers;

import java.awt.Color;
// import java.awt.Image; // No se usa directamente aquí ahora
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.text.SimpleDateFormat; // Para formatear fecha (futuro)
import java.util.Date; // Para fecha (futuro)
import java.io.File; // Para tamaño de archivo (futuro)
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import modelo.VisorModel;
import vista.VisorView;
import vista.config.ViewUIConfig;

public class InfoBarManager {

    private final VisorModel model;
    private final VisorView view;
    private final ViewUIConfig uiConfig;
    // private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm"); // Para formatear fecha (futuro)

    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
        System.out.println("[InfoBarManager] Instancia creada.");
    }

    /**
     * Método principal para actualizar ambas barras de información.
     * Se asegura de que la actualización se realice en el Event Dispatch Thread.
     */
    public void actualizarBarrasDeInfo() {
        if (view == null || model == null || uiConfig == null) {
            System.err.println("ERROR [InfoBarManager.actualizarBarrasDeInfo]: Dependencias nulas. No se pueden actualizar barras.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            actualizarBarraInfoSuperiorInterno();
            actualizarBarraEstadoInferiorInterno();
        });
    }

    /**
     * Actualiza los componentes de la barra de información superior.
     */
    private void actualizarBarraInfoSuperiorInterno() {
        // 1. Nombre del Archivo
        String nombreArchivoDisplay = "Archivo: N/A";
        Path rutaCompletaActual = null; // Guardar para usar después (tamaño, fecha)
        if (model.getSelectedImageKey() != null) {
            rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaCompletaActual != null) {
                nombreArchivoDisplay = "Archivo: " + rutaCompletaActual.getFileName().toString();
            } else {
                nombreArchivoDisplay = "Archivo: " + model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        }
        view.getNombreArchivoInfoLabel().setText(nombreArchivoDisplay);

        // 2. Índice/Total
        int indiceActual = -1;
        int totalImagenes = 0;
        if (model.getModeloLista() != null) {
            totalImagenes = model.getModeloLista().getSize();
            if (model.getSelectedImageKey() != null && totalImagenes > 0) {
                indiceActual = model.getModeloLista().indexOf(model.getSelectedImageKey());
            }
        }
        if (totalImagenes > 0 && indiceActual != -1) {
            view.getIndiceTotalInfoLabel().setText("Idx: " + (indiceActual + 1) + "/" + totalImagenes);
        } else if (totalImagenes > 0) {
            view.getIndiceTotalInfoLabel().setText("Idx: -/" + totalImagenes);
        } else {
            view.getIndiceTotalInfoLabel().setText("Idx: 0/0");
        }

        // 3. Dimensiones Originales
        String dimsDisplay = "Dim: N/A";
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal != null) {
            dimsDisplay = "Dim: " + imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
        }
        view.getDimensionesOriginalesInfoLabel().setText(dimsDisplay);

        // 4. Tamaño del Archivo (Placeholder - Implementación Futura)
        String tamanoDisplay = "Tam: N/A";
        // --- INICIO LÓGICA FUTURA TAMAÑO ARCHIVO ---
        // if (rutaCompletaActual != null) {
        //     try {
        //         File archivo = rutaCompletaActual.toFile();
        //         if (archivo.exists() && archivo.isFile()) {
        //             long bytes = archivo.length();
        //             tamanoDisplay = "Tam: " + formatFileSize(bytes);
        //         }
        //     } catch (Exception e) {
        //         System.err.println("Error al obtener tamaño de archivo: " + e.getMessage());
        //         tamanoDisplay = "Tam: Error";
        //     }
        // }
        // --- FIN LÓGICA FUTURA TAMAÑO ARCHIVO ---
        view.getTamanoArchivoInfoLabel().setText(tamanoDisplay);


        // 5. Fecha de Modificación del Archivo (Placeholder - Implementación Futura)
        String fechaDisplay = "Fch: N/A";
        // --- INICIO LÓGICA FUTURA FECHA ARCHIVO ---
        // if (rutaCompletaActual != null) {
        //     try {
        //         File archivo = rutaCompletaActual.toFile();
        //         if (archivo.exists()) {
        //             long lastModified = archivo.lastModified();
        //             fechaDisplay = "Fch: " + dateFormat.format(new Date(lastModified));
        //         }
        //     } catch (Exception e) {
        //         System.err.println("Error al obtener fecha de archivo: " + e.getMessage());
        //         fechaDisplay = "Fch: Error";
        //     }
        // }
        // --- FIN LÓGICA FUTURA FECHA ARCHIVO ---
        view.getFechaArchivoInfoLabel().setText(fechaDisplay);


        // 6. Modo de Zoom Nombre
        String modoZoomDisplay = "Modo: N/A";
        if (model.getCurrentZoomMode() != null) {

            modoZoomDisplay = "Modo: " + model.getCurrentZoomMode().getNombreLegible(); // Asumiendo getNombreLegible() en ZoomModeEnum
            
        }
        view.getModoZoomNombreInfoLabel().setText(modoZoomDisplay);
        
        // 7. Porcentaje de Zoom Visual Real
        String zoomPctDisplay = "%Z: N/A";
        if (model.getCurrentImage() != null) { // Solo mostrar si hay imagen
             zoomPctDisplay = String.format("%%Z: %.0f%%", model.getZoomFactor() * 100);
        }
        view.getPorcentajeZoomVisualRealInfoLabel().setText(zoomPctDisplay);

        // Los indicadores ZM, Prop, SubC han sido movidos a actualizarBarraEstadoInferiorInterno
        // System.out.println("  [InfoBarManager] Barra Info Superior actualizada.");
    }
    

    /**
     * Actualiza los componentes de la barra de estado/control inferior.
     * Esto incluye la ruta del archivo y los indicadores de estado para ZM, Prop y SubC.
     */
    private void actualizarBarraEstadoInferiorInterno() {
        // --- 1. Actualizar Ruta Completa del Archivo ---
        String rutaTextoDisplay = "Ruta: (ninguna imagen seleccionada)"; // Valor por defecto
        Path rutaActual = null; // Para referencia futura si se necesita (ej. config)

        //    1.1. Intentar obtener la ruta de la imagen seleccionada.
        if (model.getSelectedImageKey() != null) {
            rutaActual = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaActual != null) {
                rutaTextoDisplay = rutaActual.toString();
            } else {
                rutaTextoDisplay = model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        //    1.2. Si no hay imagen seleccionada, intentar mostrar la carpeta raíz actual.
        } else if (model.getCarpetaRaizActual() != null) {
            rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
        }
        //    1.3. Actualizar el JLabel en la vista.
        JLabel labelRuta = view.getRutaCompletaArchivoLabel();
        if (labelRuta != null) {
            labelRuta.setText(rutaTextoDisplay);
            // Opcional: Si sigue habiendo problemas de repintado SOLO para este label:
            // labelRuta.repaint(); 
            // if (labelRuta.getParent() != null) labelRuta.getParent().repaint();
        } else {
            System.err.println("WARN [InfoBarManager]: rutaCompletaArchivoLabel es null en VisorView.");
        }

        // --- 2. Actualizar Indicadores de Estado (ZM, Prop, SubC) ---
        //    2.1. Definir colores basados en la configuración UI.
        Color colorTextoIndicadorNormal = uiConfig.colorTextoPrimario; // Para texto si el icono no es claro o para tooltips
        Color colorIconoActivo = uiConfig.colorBotonTexto;       // Color del icono si se tiñera (no usado actualmente)
        Color colorFondoActivo = uiConfig.colorBotonActivado;     // Fondo cuando el toggle está ON
        Color colorFondoInactivo = uiConfig.colorFondoSecundario; // Fondo cuando el toggle está OFF (mismo que la barra)

        //    2.2. Indicador Zoom Manual (ZM)
        JLabel zmIconLabel = view.getIconoZoomManualLabel();
        if (zmIconLabel != null) {
            boolean zoomManualEstaActivo = model.isZoomHabilitado();
            zmIconLabel.setToolTipText(zoomManualEstaActivo ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
            zmIconLabel.setBackground(zoomManualEstaActivo ? colorFondoActivo : colorFondoInactivo);
            // El icono se establece en VisorView. Aquí solo se actualiza el fondo y tooltip.
            // Si tuvieras iconos ON/OFF diferentes, los cambiarías aquí:
            // zmIconLabel.setIcon(zoomManualEstaActivo ? iconoZM_ON : iconoZM_OFF);
            // zmIconLabel.setForeground(zoomManualEstaActivo ? colorIconoActivo : colorTextoIndicadorNormal); // Si el icono tuviera texto
        } else { System.err.println("WARN [InfoBarManager]: iconoZoomManualLabel es null."); }

        //    2.3. Indicador Mantener Proporciones (Prop)
        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
        if (propIconLabel != null) {
            boolean proporcionesEstanActivas = model.isMantenerProporcion();
            propIconLabel.setToolTipText(proporcionesEstanActivas ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
            propIconLabel.setBackground(proporcionesEstanActivas ? colorFondoActivo : colorFondoInactivo);
            // (Lógica similar para setIcon si los iconos cambian con el estado)
        } else { System.err.println("WARN [InfoBarManager]: iconoMantenerProporcionesLabel es null."); }

        //    2.4. Indicador Modo Subcarpetas (SubC)
        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
        if (subcIconLabel != null) {
            boolean incluyeSubcarpetas = !model.isMostrarSoloCarpetaActual(); // El estado activo es cuando SÍ se incluyen subcarpetas
            subcIconLabel.setToolTipText(incluyeSubcarpetas ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
            subcIconLabel.setBackground(incluyeSubcarpetas ? colorFondoActivo : colorFondoInactivo);
            // (Lógica similar para setIcon si los iconos cambian con el estado)
        } else { System.err.println("WARN [InfoBarManager]: iconoModoSubcarpetasLabel es null."); }
        
        // --- 3. Actualizar Mensajes de la Aplicación (Placeholder) ---
        //    (La lógica para mostrar mensajes temporales se implementará más adelante)
        //    JLabel mensajesLabel = view.getMensajesAppLabel();
        //    if (mensajesLabel != null) {
        //        // mensajesLabel.setText("Último mensaje...");
        //    }

        // System.out.println("  [InfoBarManager] Barra Estado Inferior actualizada."); // Log opcional
        
    } // FIN del metodo actualizarBarraEstadoInferiorInterno
    

//    /**
//     * Actualiza los componentes de la barra de estado/control inferior.
//     */
//    private void actualizarBarraEstadoInferiorInterno() {
//        // 1. Ruta Completa del Archivo
//        String rutaTextoDisplay = "Ruta: (ninguna)";
//        Path rutaActual = null;
//        if (model.getSelectedImageKey() != null) {
//            rutaActual = model.getRutaCompleta(model.getSelectedImageKey());
//            if (rutaActual != null) {
//                rutaTextoDisplay = rutaActual.toString();
//            } else {
//                rutaTextoDisplay = model.getSelectedImageKey() + " (Ruta no encontrada)";
//            }
//        } else if (model.getCarpetaRaizActual() != null) {
//            rutaTextoDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
//        }
//        // Actualizar el nuevo JLabel en la barra inferior
//        if (view.getRutaCompletaArchivoLabel() != null) {
//            view.getRutaCompletaArchivoLabel().setText(rutaTextoDisplay);
//        }
//
//        // 2. Indicadores de Estado (ZM, Prop, SubC) - Ahora en la barra inferior
//        Color colorTextoIndicadorNormal = uiConfig.colorTextoPrimario; 
//        Color colorTextoIndicadorActivo = uiConfig.colorBotonTexto; // Texto que contrasta con fondo activo
//        Color colorFondoActivo = uiConfig.colorBotonActivado;     
//        Color colorFondoInactivo = uiConfig.colorFondoSecundario; // Fondo de la barra de estado
//
//        // 2.1 Indicador Zoom Manual (ZM)
//        JLabel zmIconLabel = view.getIconoZoomManualLabel();
//        if (zmIconLabel != null) {
//            boolean activo = model.isZoomHabilitado();
//            zmIconLabel.setToolTipText(activo ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
//            zmIconLabel.setBackground(activo ? colorFondoActivo : colorFondoInactivo);
//            zmIconLabel.setForeground(activo ? colorTextoIndicadorActivo : colorTextoIndicadorNormal);
//            // zmIconLabel.setText("ZM"); // El texto se mantiene, o se pondrá icono
//        }
//
//        // 2.2 Indicador Mantener Proporciones (Prop)
//        JLabel propIconLabel = view.getIconoMantenerProporcionesLabel();
//        if (propIconLabel != null) {
//            boolean activo = model.isMantenerProporcion();
//            propIconLabel.setToolTipText(activo ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
//            propIconLabel.setBackground(activo ? colorFondoActivo : colorFondoInactivo);
//            propIconLabel.setForeground(activo ? colorTextoIndicadorActivo : colorTextoIndicadorNormal);
//            // propIconLabel.setText("Prop");
//        }
//
//        // 2.3 Indicador Modo Subcarpetas (SubC)
//        JLabel subcIconLabel = view.getIconoModoSubcarpetasLabel();
//        if (subcIconLabel != null) {
//            boolean activo = !model.isMostrarSoloCarpetaActual(); // Invertido: true si se INCLUYEN subcarpetas
//            subcIconLabel.setToolTipText(activo ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
//            subcIconLabel.setBackground(activo ? colorFondoActivo : colorFondoInactivo);
//            subcIconLabel.setForeground(activo ? colorTextoIndicadorActivo : colorTextoIndicadorNormal);
//            //subcIconLabel.setText("SubC");
//        }
//        
//        // 3. Mensajes de la Aplicación (Placeholder - lógica de mensajes temporales iría aquí)
//        // view.getMensajesAppLabel().setText(" "); // Limpiar o poner mensaje
//
//        // System.out.println("  [InfoBarManager] Barra Estado Inferior actualizada.");
//    }
    
    

    /**
     * Formatea el tamaño de un archivo de bytes a un formato legible (KB, MB, GB).
     * (Implementación futura)
     * @param sizeInBytes El tamaño en bytes.
     * @return Una cadena formateada del tamaño.
     */
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) return sizeInBytes + " B";
        int exp = (int) (Math.log(sizeInBytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", sizeInBytes / Math.pow(1024, exp), pre);
    }

    // (Método para mostrar mensajes temporales se podría añadir aquí más adelante)
}

//package controlador.managers;
//
//import java.awt.Color;
//import java.awt.Image;
//import java.awt.image.BufferedImage;
//import java.nio.file.Path;
//import java.util.Objects;
//import javax.swing.JLabel;
//import javax.swing.SwingUtilities;
//
//import modelo.VisorModel;
//import vista.VisorView;
//import vista.config.ViewUIConfig; // Necesitará acceso a los colores del tema
//
//public class InfoBarManager {
//
//    private final VisorModel model;
//    private final VisorView view;
//    private final ViewUIConfig uiConfig; // Para los colores de los indicadores
//
//    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig) {
//        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
//        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
//        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
//        System.out.println("[InfoBarManager] Instancia creada.");
//    }
//
//    public void actualizarBarrasDeInfo() {
//        if (view == null || model == null || uiConfig == null) {
//            System.err.println("ERROR [InfoBarManager]: Dependencias nulas al actualizar barras.");
//            return;
//        }
//
//        // Asegurar que la actualización se haga en el EDT
//        SwingUtilities.invokeLater(() -> {
//            // --- Actualizar Barra de Información Superior ---
//            actualizarBarraInfoSuperiorInterno();
//            
//            // --- Actualizar Barra de Estado Inferior ---
//            actualizarBarraEstadoInferiorInterno();
//        });
//    }
//
//    private void actualizarBarraInfoSuperiorInterno() {
//        // 1. Nombre del Archivo
//        String nombreArchivo = "N/A";
//        if (model.getSelectedImageKey() != null) {
//            Path rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
//            if (rutaCompletaActual != null) {
//                nombreArchivo = rutaCompletaActual.getFileName().toString();
//            } else {
//                nombreArchivo = model.getSelectedImageKey() + " (Ruta no encontrada)";
//            }
//        }
//        view.getNombreArchivoInfoLabel().setText("Archivo: " + nombreArchivo);
//
//        // 2. Índice/Total
//        int indiceActual = model.getSelectedImageKey() != null ? model.getModeloLista().indexOf(model.getSelectedImageKey()) : -1;
//        int totalImagenes = model.getModeloLista() != null ? model.getModeloLista().getSize() : 0;
//        if (totalImagenes > 0 && indiceActual != -1) {
//            view.getIndiceTotalInfoLabel().setText((indiceActual + 1) + "/" + totalImagenes);
//        } else if (totalImagenes > 0 && indiceActual == -1){
//             view.getIndiceTotalInfoLabel().setText("-/" + totalImagenes);
//        } 
//        else {
//            view.getIndiceTotalInfoLabel().setText("0/0");
//        }
//
//        // 3. Dimensiones Originales
//        String dims = "Dim: N/A";
//        BufferedImage imgOriginal = model.getCurrentImage();
//        if (imgOriginal != null) {
//            dims = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
//        }
//        view.getDimensionesOriginalesInfoLabel().setText(dims);
//
//        // 4. Modo de Zoom Nombre
//        String modoZoomStr = "N/A";
//        if (model.getCurrentZoomMode() != null) {
//            modoZoomStr = model.getCurrentZoomMode().toString().replace('_', ' ').toLowerCase();
//            modoZoomStr = Character.toUpperCase(modoZoomStr.charAt(0)) + modoZoomStr.substring(1);
//        }
//        view.getModoZoomNombreInfoLabel().setText("Modo: " + modoZoomStr);
//        
//        // 5. Porcentaje de Zoom Visual Real
//        String zoomPctStr = "N/A";
//        if (model.getCurrentImage() != null) {
//             zoomPctStr = String.format("%.0f%%", model.getZoomFactor() * 100);
//        }
//        view.getPorcentajeZoomVisualRealInfoLabel().setText(zoomPctStr);
//
//        // 6. Indicadores de Estado (con colores)
//        Color colorTextoIndicador = uiConfig.colorTextoPrimario; // O un color específico
//        Color colorFondoActivo = uiConfig.colorBotonActivado;     // Verde o color de "activo"
//        Color colorFondoInactivo = uiConfig.colorBotonFondo;   // Rojo o color de "inactivo"/normal
//
//        JLabel zmLabel = view.getIndicadorZoomManualInfoLabel();
//        zmLabel.setText("ZM"); // Más corto
//        zmLabel.setToolTipText(model.isZoomHabilitado() ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
//        zmLabel.setOpaque(true);
//        zmLabel.setBackground(model.isZoomHabilitado() ? colorFondoActivo : colorFondoInactivo);
//        zmLabel.setForeground(model.isZoomHabilitado() ? uiConfig.colorBotonTexto : colorTextoIndicador); // Texto blanco sobre fondo activo
//
//        JLabel propLabel = view.getIndicadorMantenerPropInfoLabel();
//        propLabel.setText("Prop");
//        propLabel.setToolTipText(model.isMantenerProporcion() ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
//        propLabel.setOpaque(true);
//        propLabel.setBackground(model.isMantenerProporcion() ? colorFondoActivo : colorFondoInactivo);
//        propLabel.setForeground(model.isMantenerProporcion() ? uiConfig.colorBotonTexto : colorTextoIndicador);
//
//        JLabel subcLabel = view.getIndicadorSubcarpetasInfoLabel();
//        subcLabel.setText("SubC");
//        subcLabel.setToolTipText(!model.isMostrarSoloCarpetaActual() ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
//        subcLabel.setOpaque(true);
//        subcLabel.setBackground(!model.isMostrarSoloCarpetaActual() ? colorFondoActivo : colorFondoInactivo);
//        subcLabel.setForeground(!model.isMostrarSoloCarpetaActual() ? uiConfig.colorBotonTexto : colorTextoIndicador);
//
//        // System.out.println("  [InfoBarManager] Barra Info Superior actualizada.");
//    }
//
//    private void actualizarBarraEstadoInferiorInterno() {
//        // Ruta Completa
//        String rutaTexto = "";
//        if (model.getSelectedImageKey() != null) {
//            Path rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
//            if (rutaCompletaActual != null) {
//                rutaTexto = rutaCompletaActual.toString();
//            } else {
//                rutaTexto = model.getSelectedImageKey() + " (Ruta no encontrada)";
//            }
//        } else if (model.getCarpetaRaizActual() != null) {
//            rutaTexto = model.getCarpetaRaizActual().toString();
//        }
////        view.getTextoRuta().setText(rutaTexto); // Asumiendo que getTextoRuta() devuelve el JTextField/JLabel
//        
//        if (view.getRutaCompletaArchivoLabel() != null) { // Asegúrate que el getter exista y el label no sea null
//            view.getRutaCompletaArchivoLabel().setText(rutaTexto);
//        } else {
//            System.err.println("ERROR [InfoBarManager]: view.getRutaCompletaArchivoLabel() es null. No se puede actualizar la ruta.");
//        }
//
//        // Aquí iría la lógica para los mensajes temporales y los controles de la barra inferior
//        // System.out.println("  [InfoBarManager] Barra Estado Inferior actualizada.");
//    }
//
//    // Método para mostrar mensajes temporales en la barra inferior
//    // public void mostrarMensajeTemporal(String mensaje, int duracionMs) { ... }
//}