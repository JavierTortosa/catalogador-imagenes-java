package controlador.managers;

import java.util.Objects;

import servicios.ConfigurationManager;
import vista.VisorView;

public class ViewManager {

    private VisorView view;
    private ConfigurationManager configuration;

    public ViewManager(VisorView view, ConfigurationManager configuration) {
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en ViewManager");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ViewManager");
    }

    /**
     * Cambia la visibilidad de un componente principal de la UI y actualiza la configuración.
     *
     * @param identificadorComponente String que identifica el componente en la VisorView.
     * @param nuevoEstadoVisible El nuevo estado de visibilidad.
     * @param configKeyParaEstado La clave en ConfigurationManager para guardar este estado.
     */
    public void setComponentePrincipalVisible(String identificadorComponente, boolean nuevoEstadoVisible, String configKeyParaEstado) {
        System.out.println("[ViewManager] setComponentePrincipalVisible: " + identificadorComponente + " -> " + nuevoEstadoVisible);

        if (view == null || configuration == null) {
            System.err.println("ERROR [ViewManager]: Vista o Configuración nulas.");
            return;
        }

        boolean cambioRealizadoEnVista = false;

        switch (identificadorComponente) {
            case "Barra_de_Menu":
                if (view.getJMenuBar() != null && view.getJMenuBar().isVisible() != nuevoEstadoVisible) {
                    view.setJMenuBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "Barra_de_Botones":
                if (view.getPanelDeBotones() != null && view.getPanelDeBotones().isVisible() != nuevoEstadoVisible) {
                    view.setToolBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "mostrar_ocultar_la_lista_de_archivos":
                if (view.getPanelIzquierdo() != null && view.getPanelIzquierdo().isVisible() != nuevoEstadoVisible) {
                    view.setFileListVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "imagenes_en_miniatura":
                if (view.getScrollListaMiniaturas() != null && view.getScrollListaMiniaturas().isVisible() != nuevoEstadoVisible) {
                    view.setThumbnailsVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            case "linea_de_ubicacion_del_archivo":
                if (view.getTextoRuta() != null && view.getTextoRuta().isVisible() != nuevoEstadoVisible) {
                    view.setLocationBarVisible(nuevoEstadoVisible); // Llama al método existente en VisorView
                    cambioRealizadoEnVista = true;
                }
                break;
            // Los casos "fondo_a_cuadros" y "mantener_ventana_siempre_encima"
            // son manejados por sus Actions específicas que llaman a métodos directos en VisorView.
            // No necesitan pasar por este método genérico si su lógica es diferente.
            default:
                System.err.println("WARN [ViewManager]: Identificador de componente no manejado: '" + identificadorComponente + "'");
                return;
        }

        // Actualizar configuración en memoria
        if (configKeyParaEstado != null && !configKeyParaEstado.isBlank()) {
            configuration.setString(configKeyParaEstado, String.valueOf(nuevoEstadoVisible));
            System.out.println("  -> [ViewManager] Configuración '" + configKeyParaEstado + "' actualizada a: " + nuevoEstadoVisible);
        }

        // VisorView (en sus métodos setJMenuBarVisible, setToolBarVisible, etc.)
        // ya debería encargarse de revalidate/repaint. Si no, tendrías que hacerlo aquí
        // o asegurar que el cambio de visibilidad del componente lo dispare.
        // Si el cambio se hizo y el método en VisorView no hizo revalidate/repaint:
        // if (cambioRealizadoEnVista) {
        //     SwingUtilities.invokeLater(() -> {
        //         view.getFrame().revalidate();
        //         view.getFrame().repaint();
        //     });
        // }
    }

    // Aquí podrías añadir métodos para cambiar tema, etc. en el futuro.
    // public void aplicarTema(String nombreTema) { ... }
} // FIN de la clase ViewManager