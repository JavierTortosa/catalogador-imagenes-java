package vista.builders;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

// Clase auxiliar interna para la configuración del layout de botones
class ButtonGroupConfig { // Puedes mantenerla aquí o hacerla pública si es necesario
    final String category;
    final int alignment; // FlowLayout.LEFT, CENTER, RIGHT
    final List<String> iconNames;

    ButtonGroupConfig(String category, int alignment, List<String> iconNames) {
        this.category = category;
        this.alignment = alignment;
        this.iconNames = iconNames;
    }
}

public class ToolbarBuilder {

    // --- Constantes o Configuraciones ---
    // Podrías mover colores aquí si son específicos de la toolbar
    //private final Color colorOriginalFondoBoton = new Color(238, 238, 238); // Ejemplo

    // --- Resultados del Builder ---
    private JPanel toolbarPanel; // El panel principal de la barra de herramientas
    private Map<String, JButton> botonesPorNombre; // Mapa clave Larga -> Botón

    // --- Componentes Internos ---
    private JPanel panelBotonesIzquierda;
    private JPanel panelBotonesCentro;
    private JPanel panelBotonesDerecha;
    
    private Color colorOriginalFondoBoton;
    private int iconoAncho;
    private int iconoAlto;
    
    // Ya no necesitamos el array botonesCentro si no lo usas fuera de aquí

    private Map<String, Action> actionMap; // NUEVO: Mapa de acciones
    
    // --- Constructor acepta el mapa de Actions ---
    public ToolbarBuilder(Map<String, Action> actionMap, Color colorFondo, int iconoAncho, int iconoAlto) {
        // Guarda los parámetros recibidos
        this.actionMap = actionMap != null ? actionMap : new HashMap<>();
        this.colorOriginalFondoBoton = colorFondo != null ? colorFondo : new Color(238, 238, 238);
        this.iconoAncho = iconoAncho > 0 ? iconoAncho : 32;
        this.iconoAlto = iconoAlto; // Guarda -1 si se pasó para mantener proporción

        // Inicializa otras variables miembro
        this.botonesPorNombre = new HashMap<>();
        panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelBotonesCentro = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelBotonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(panelBotonesIzquierda, BorderLayout.WEST);
        toolbarPanel.add(panelBotonesCentro, BorderLayout.CENTER);
        toolbarPanel.add(panelBotonesDerecha, BorderLayout.EAST);

        // Opcional: Aplicar color de fondo a los paneles internos
        // panelBotonesIzquierda.setBackground(this.colorOriginalFondoBoton);
        // panelBotonesCentro.setBackground(this.colorOriginalFondoBoton);
        // panelBotonesDerecha.setBackground(this.colorOriginalFondoBoton);
        // toolbarPanel.setBackground(this.colorOriginalFondoBoton);
    }

    /**
     * Construye la barra de herramientas completa basada en la configuración
     * interna de iconos y layout.
     *
     * @return El JPanel que contiene la barra de herramientas construida.
     */
    public JPanel buildToolbar() {
        // --- Definición de Iconos y Layout (Movido desde VisorView) ---
        List<String> iconosMovimiento = List.of("01-Anterior_48x48.png", "02-Siguiente_48x48.png");
        List<String> iconosEdicion = List.of("03-Rotar_Izquierda_48x48.png", "04-Rotar_Derecha_48x48.png", "05-Espejo_Horizontal_48x48.png", "06-Espejo_Vertical_48x48.png", "07-Recortar_48x48.png");
        List<String> iconosZoom = List.of("08-Zoom_48x48.png", "09-Zoom_Auto_48x48.png", "10-Ajustar_al_Ancho_48x48.png", "11-Ajustar_al_Alto_48x48.png", "12-Escalar_Para_Ajustar_48x48.png", "13-Zoom_Fijo_48x48.png", "14-Reset_48x48.png");
        List<String> iconosVista = List.of("15-Panel-Galeria_48x48.png", "16-Grid_48x48.png", "17-Pantalla_Completa_48x48.png", "18-Lista_48x48.png", "19-Carrousel_48x48.png");
        List<String> iconosControl = List.of("20-Refrescar_48x48.png", "21-Subcarpetas_48x48.png", "22-lista_de_favoritos_48x48.png", "23-Borrar_48x48.png");
        List<String> iconosEspeciales = List.of("24-Selector_de_Carpetas_48x48.png", "25-Menu_48x48.png", "26-Botones_Ocultos_48x48.png");

        List<ButtonGroupConfig> buttonLayoutConfig = List.of(
            new ButtonGroupConfig("movimiento", FlowLayout.LEFT, iconosMovimiento),
            new ButtonGroupConfig("edicion", FlowLayout.CENTER, iconosEdicion),
            new ButtonGroupConfig("zoom", FlowLayout.CENTER, iconosZoom),
            new ButtonGroupConfig("vista", FlowLayout.CENTER, iconosVista),
            new ButtonGroupConfig("control", FlowLayout.CENTER, iconosControl),
            new ButtonGroupConfig("especiales", FlowLayout.RIGHT, iconosEspeciales)
        );
        // -------------------------------------------------------------

        boolean firstGroup = true;
        for (ButtonGroupConfig config : buttonLayoutConfig) {
            if (!firstGroup) {
                addSeparator(config.alignment); // Usa método interno
            } else {
                firstGroup = false;
            }

            for (String nombreIcono : config.iconNames) {
                // Llama al método interno para crear y configurar el botón
                procesarBoton(nombreIcono, config.category, config.alignment);
            }
        }

        return toolbarPanel; // Devuelve el panel construido
    }

    /**
     * Procesa la creación y configuración de un botón individual,
     * usando los colores y tamaños de icono configurados.
     */
    private void procesarBoton(String nombreIcono, String categoria, int flowLayoutAlignment) {
        JButton button = new JButton(); // Crea el botón

        // --- Claves y Action Command ---
        String nombreBotonBase = nombreIcono.replaceAll("\\d+-", "").replace(".png", "");
        String fullConfigKey = "interfaz.boton." + categoria + "." + nombreBotonBase; // Clave Larga
        String shortActionCommand = nombreBotonBase; // Comando Corto

        // --- Configurar propiedades básicas del botón ---
        // Determinar tamaño final del botón basado en config
        int anchoBoton = this.iconoAncho;
        int altoBoton = (this.iconoAlto <= 0) ? this.iconoAncho : this.iconoAlto; // Si alto es -1, usar ancho
        button.setPreferredSize(new Dimension(anchoBoton, altoBoton));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(this.colorOriginalFondoBoton); // Usar color de fondo base
        button.setOpaque(true); // Asegurarse de que el fondo sea visible si no es transparente

        //LOG [ToolbarBuilder] Procesando botón:
        System.out.println("  [ToolbarBuilder] Procesando botón: " + shortActionCommand + " (Icono Archivo: " + nombreIcono + ")");

        // --- Intentar asignar Action ---
        Action action = this.actionMap.get(shortActionCommand); // Busca Action por comando CORTO

        ImageIcon iconParaBoton = null; // Variable para guardar el icono final
        String toolTipParaBoton = null; // Variable para el tooltip final

        if (action != null) {
            // --- Caso 1: Se encontró una Action ---
            System.out.println("    -> Action encontrada: " + action.getValue(Action.NAME));
            button.setAction(action); // ASIGNAR ACTION!

            // Intentar obtener icono y tooltip DESDE la Action
            Object iconValue = action.getValue(Action.SMALL_ICON);
            if (iconValue instanceof Icon) {
                iconParaBoton = (ImageIcon) iconValue; // Guardar icono de la action
                System.out.println("    -> Icono VÁLIDO encontrado en Action.");
                button.setHideActionText(true); // Ocultar texto si la Action tiene icono VÁLIDO
                button.setText(""); // Borrar texto por si acaso
            } else {
                System.out.println("    -> Icono NO válido o NULL encontrado en Action (Valor: " + iconValue + ").");
                button.setIcon(null);
                button.setHideActionText(false); // Mostrar texto de Action si no hay icono
                // El texto se pondrá automáticamente por setAction
            }

            Object tooltipValue = action.getValue(Action.SHORT_DESCRIPTION);
             if (tooltipValue instanceof String) {
                 toolTipParaBoton = (String) tooltipValue; // Guardar tooltip de la action
             }

        } else {
            // --- Caso 2: No se encontró Action (Fallback) ---
            System.out.println("    -> Sin Action asociada. Configurando manualmente.");
            button.setActionCommand(shortActionCommand); // Asignar comando corto

            // Cargar icono manualmente
            try {
                //System.out.println("      -> Intentando cargar icono fallback: /iconos/" + nombreIcono);
                java.net.URL iconUrl = getClass().getResource("/iconos/" + nombreIcono);
                if (iconUrl != null) {
                    ImageIcon icon = new ImageIcon(ImageIO.read(iconUrl));
                     if (icon.getImageLoadStatus() == java.awt.MediaTracker.COMPLETE) {
                        // Escalar al tamaño configurado
                        int altoFinalFallback = (this.iconoAlto <= 0) ? -1 : this.iconoAlto;
                        Image scaledImg = icon.getImage().getScaledInstance(this.iconoAncho, altoFinalFallback, Image.SCALE_SMOOTH);
                        iconParaBoton = new ImageIcon(scaledImg); // Guardar icono fallback
                        //System.out.println("      -> Icono fallback cargado.");
                     } else {
                         System.err.println("      -> ERROR: Icono fallback NO cargado correctamente (Estado: " + icon.getImageLoadStatus() + ")");
                     }
                } else {
                     System.err.println("      -> ERROR: Icono fallback NO encontrado en ruta: /iconos/" + nombreIcono);
                }
            } catch (Exception e) {
                System.err.println("      -> ERROR EXCEPCIÓN cargando icono fallback: " + e.getMessage());
            }

            // Si no se pudo cargar icono en fallback, poner placeholder
            if (iconParaBoton == null) {
                 button.setText("?");
            } else {
                 button.setText(""); // Borrar texto si hay icono fallback
            }
        }

        // --- Asignar Icono y Tooltip finales ---
        if (iconParaBoton != null) {
            button.setIcon(iconParaBoton);
            // Opcional: Poner icono deshabilitado si tienes uno específico
            // button.setDisabledIcon(...);
        }

        if (toolTipParaBoton != null) {
             button.setToolTipText(toolTipParaBoton);
        } else {
            // Generar tooltip si no vino de la Action
            button.setToolTipText(
                    nombreIcono.replace(".png", "").replace("_", " ").replace("48x48", "").replaceAll("\\d+-", "").trim());
        }


        // --- Añadir al Mapa del Builder ---
        this.botonesPorNombre.put(fullConfigKey, button); // Usa clave LARGA para el mapa

        // --- Añadir al Panel Correcto ---
        if (flowLayoutAlignment == FlowLayout.LEFT) panelBotonesIzquierda.add(button);
        else if (flowLayoutAlignment == FlowLayout.CENTER) panelBotonesCentro.add(button);
        else if (flowLayoutAlignment == FlowLayout.RIGHT) panelBotonesDerecha.add(button);
        else System.err.println("Advertencia: Alineación desconocida para botón '" + nombreIcono + "'");

    } // Fin de procesarBoton

   

    /**
     * Añade un separador visual al panel correspondiente.
     * (Lógica movida desde VisorView.separadorVisualParaBotones)
     */
    private void addSeparator(int alignment) {
        if (alignment == FlowLayout.LEFT) panelBotonesIzquierda.add(Box.createHorizontalStrut(10));
        else if (alignment == FlowLayout.CENTER) panelBotonesCentro.add(Box.createHorizontalStrut(10));
        else if (alignment == FlowLayout.RIGHT) panelBotonesDerecha.add(Box.createHorizontalStrut(10));
    }

    
    /**
     * Devuelve el mapa que asocia las claves de configuración LARGAS
     * con las instancias de JButton creadas.
     *
     * @return El mapa de botones por nombre (clave larga).
     */
    public Map<String, JButton> getBotonesPorNombreMap() {
        return this.botonesPorNombre;
    }
}
