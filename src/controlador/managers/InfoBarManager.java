package controlador.managers;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import modelo.VisorModel;
import vista.VisorView;
import vista.config.ViewUIConfig; // Necesitará acceso a los colores del tema

public class InfoBarManager {

    private final VisorModel model;
    private final VisorView view;
    private final ViewUIConfig uiConfig; // Para los colores de los indicadores

    public InfoBarManager(VisorModel model, VisorView view, ViewUIConfig uiConfig) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en InfoBarManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en InfoBarManager");
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null en InfoBarManager");
        System.out.println("[InfoBarManager] Instancia creada.");
    }

    public void actualizarBarrasDeInfo() {
        if (view == null || model == null || uiConfig == null) {
            System.err.println("ERROR [InfoBarManager]: Dependencias nulas al actualizar barras.");
            return;
        }

        // Asegurar que la actualización se haga en el EDT
        SwingUtilities.invokeLater(() -> {
            // --- Actualizar Barra de Información Superior ---
            actualizarBarraInfoSuperiorInterno();
            
            // --- Actualizar Barra de Estado Inferior ---
            actualizarBarraEstadoInferiorInterno();
        });
    }

    private void actualizarBarraInfoSuperiorInterno() {
        // 1. Nombre del Archivo
        String nombreArchivo = "N/A";
        if (model.getSelectedImageKey() != null) {
            Path rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaCompletaActual != null) {
                nombreArchivo = rutaCompletaActual.getFileName().toString();
            } else {
                nombreArchivo = model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        }
        view.getNombreArchivoInfoLabel().setText("Archivo: " + nombreArchivo);

        // 2. Índice/Total
        int indiceActual = model.getSelectedImageKey() != null ? model.getModeloLista().indexOf(model.getSelectedImageKey()) : -1;
        int totalImagenes = model.getModeloLista() != null ? model.getModeloLista().getSize() : 0;
        if (totalImagenes > 0 && indiceActual != -1) {
            view.getIndiceTotalInfoLabel().setText((indiceActual + 1) + "/" + totalImagenes);
        } else if (totalImagenes > 0 && indiceActual == -1){
             view.getIndiceTotalInfoLabel().setText("-/" + totalImagenes);
        } 
        else {
            view.getIndiceTotalInfoLabel().setText("0/0");
        }

        // 3. Dimensiones Originales
        String dims = "Dim: N/A";
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal != null) {
            dims = imgOriginal.getWidth() + "x" + imgOriginal.getHeight();
        }
        view.getDimensionesOriginalesInfoLabel().setText(dims);

        // 4. Modo de Zoom Nombre
        String modoZoomStr = "N/A";
        if (model.getCurrentZoomMode() != null) {
            modoZoomStr = model.getCurrentZoomMode().toString().replace('_', ' ').toLowerCase();
            modoZoomStr = Character.toUpperCase(modoZoomStr.charAt(0)) + modoZoomStr.substring(1);
        }
        view.getModoZoomNombreInfoLabel().setText("Modo: " + modoZoomStr);
        
        // 5. Porcentaje de Zoom Visual Real
        String zoomPctStr = "N/A";
        if (model.getCurrentImage() != null) {
             zoomPctStr = String.format("%.0f%%", model.getZoomFactor() * 100);
        }
        view.getPorcentajeZoomVisualRealInfoLabel().setText(zoomPctStr);

        // 6. Indicadores de Estado (con colores)
        Color colorTextoIndicador = uiConfig.colorTextoPrimario; // O un color específico
        Color colorFondoActivo = uiConfig.colorBotonActivado;     // Verde o color de "activo"
        Color colorFondoInactivo = uiConfig.colorBotonFondo;   // Rojo o color de "inactivo"/normal

        JLabel zmLabel = view.getIndicadorZoomManualInfoLabel();
        zmLabel.setText("ZM"); // Más corto
        zmLabel.setToolTipText(model.isZoomHabilitado() ? "Zoom Manual: Activado" : "Zoom Manual: Desactivado");
        zmLabel.setOpaque(true);
        zmLabel.setBackground(model.isZoomHabilitado() ? colorFondoActivo : colorFondoInactivo);
        zmLabel.setForeground(model.isZoomHabilitado() ? uiConfig.colorBotonTexto : colorTextoIndicador); // Texto blanco sobre fondo activo

        JLabel propLabel = view.getIndicadorMantenerPropInfoLabel();
        propLabel.setText("Prop");
        propLabel.setToolTipText(model.isMantenerProporcion() ? "Mantener Proporciones: Activado" : "Mantener Proporciones: Desactivado");
        propLabel.setOpaque(true);
        propLabel.setBackground(model.isMantenerProporcion() ? colorFondoActivo : colorFondoInactivo);
        propLabel.setForeground(model.isMantenerProporcion() ? uiConfig.colorBotonTexto : colorTextoIndicador);

        JLabel subcLabel = view.getIndicadorSubcarpetasInfoLabel();
        subcLabel.setText("SubC");
        subcLabel.setToolTipText(!model.isMostrarSoloCarpetaActual() ? "Incluir Subcarpetas: Activado" : "Incluir Subcarpetas: Desactivado");
        subcLabel.setOpaque(true);
        subcLabel.setBackground(!model.isMostrarSoloCarpetaActual() ? colorFondoActivo : colorFondoInactivo);
        subcLabel.setForeground(!model.isMostrarSoloCarpetaActual() ? uiConfig.colorBotonTexto : colorTextoIndicador);

        // System.out.println("  [InfoBarManager] Barra Info Superior actualizada.");
    }

    private void actualizarBarraEstadoInferiorInterno() {
        // Ruta Completa
        String rutaTexto = "";
        if (model.getSelectedImageKey() != null) {
            Path rutaCompletaActual = model.getRutaCompleta(model.getSelectedImageKey());
            if (rutaCompletaActual != null) {
                rutaTexto = rutaCompletaActual.toString();
            } else {
                rutaTexto = model.getSelectedImageKey() + " (Ruta no encontrada)";
            }
        } else if (model.getCarpetaRaizActual() != null) {
            rutaTexto = model.getCarpetaRaizActual().toString();
        }
        view.getTextoRuta().setText(rutaTexto); // Asumiendo que getTextoRuta() devuelve el JTextField/JLabel

        // Aquí iría la lógica para los mensajes temporales y los controles de la barra inferior
        // System.out.println("  [InfoBarManager] Barra Estado Inferior actualizada.");
    }

    // Método para mostrar mensajes temporales en la barra inferior
    // public void mostrarMensajeTemporal(String mensaje, int duracionMs) { ... }
}