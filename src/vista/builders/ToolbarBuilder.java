// En src/vista/builders/ToolbarBuilder.java
package vista.builders;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.Collections; // Para Collections.emptyMap() si es necesario
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import controlador.VisorController; // Para ActionListener fallback
import vista.config.ToolbarButtonDefinition;
import vista.util.IconUtils;

public class ToolbarBuilder {

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA Y CONSTANTES ---

    // 1.1. Resultados del Builder
    private JPanel mainToolbarPanel;         // El panel principal que contendrá todas las barras/secciones
    private Map<String, JButton> botonesPorNombre; // Mapa: Clave Larga de Config -> JButton

    // 1.2. Paneles Internos para Alineación
    private JPanel panelBotonesIzquierda;
    private JPanel panelBotonesCentro;
    private JPanel panelBotonesDerecha;
    
    // 1.3. Dependencias y Configuración
    private final Map<String, Action> actionMap;
    private final IconUtils iconUtils;
    private final VisorController controllerRef; // Para ActionListener fallback
    private final Color colorBotonFondoDefault;
    private final Color colorBotonTextoDefault; // No se usa actualmente si los botones son solo icono
    private final int iconoAncho;
    private final int iconoAlto;

    // --- SECCIÓN 2: CONSTRUCTOR ---
    public ToolbarBuilder(
            Map<String, Action> actionMap,
            Color colorBotonFondo,
            Color colorBotonTexto,
            Color _colorBotonActivadoIgnorado,  // Parámetro no usado directamente aquí, lo usa ViewUIConfig
            Color _colorBotonAnimacionIgnorado, // Parámetro no usado directamente aquí, lo usa ViewUIConfig
            int iconoAncho,
            int iconoAlto,
            IconUtils iconUtils,
            VisorController controller
    ) {
        System.out.println("[ToolbarBuilder Constructor] Iniciando...");

        // 2.1. Asignación de Dependencias
        this.actionMap = (actionMap != null) ? actionMap : Collections.emptyMap();
        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ToolbarBuilder");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en ToolbarBuilder");
        
        // 2.2. Configuración de Apariencia por Defecto para Botones
        this.colorBotonFondoDefault = (colorBotonFondo != null) ? colorBotonFondo : new Color(238, 238, 238);
        this.colorBotonTextoDefault = (colorBotonTexto != null) ? colorBotonTexto : Color.BLACK;
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto <= 0) ? ((this.iconoAncho > 0) ? -1 : 24) : iconoAlto; // Si alto <= 0, usa -1 (mantener proporción) o 24 si ancho también es inválido

        // 2.3. Inicialización del Mapa de Botones Creados
        this.botonesPorNombre = new HashMap<>();

        // 2.4. Inicialización de Paneles para Alineación
        // Estos paneles usarán FlowLayout para alinear los botones DENTRO de su sección.
        panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // (alineación, hgap, vgap)
        panelBotonesCentro = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        panelBotonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        // 2.5. Configuración de Apariencia de los Paneles de Alineación
        // Es importante que sean opacos si tienen un color de fondo diferente al panel principal,
        // o no opacos si quieres que tomen el color del panel principal.
        // Asumimos que la toolbar tiene un fondo uniforme.
        panelBotonesIzquierda.setBackground(this.colorBotonFondoDefault); // O el color de fondo de la toolbar principal
        panelBotonesCentro.setBackground(this.colorBotonFondoDefault);
        panelBotonesDerecha.setBackground(this.colorBotonFondoDefault);
        panelBotonesIzquierda.setOpaque(true);
        panelBotonesCentro.setOpaque(true);
        panelBotonesDerecha.setOpaque(true);

        // 2.6. Creación del Panel Principal de la Toolbar
        // Este panel usa BorderLayout para posicionar los paneles de alineación.
        mainToolbarPanel = new JPanel(new BorderLayout());
        mainToolbarPanel.setBackground(this.colorBotonFondoDefault); // O el color de fondo general de la UI
        mainToolbarPanel.setOpaque(true);
        
        // 2.7. Añadir Paneles de Alineación al Panel Principal
        mainToolbarPanel.add(panelBotonesIzquierda, BorderLayout.WEST);
        mainToolbarPanel.add(panelBotonesCentro, BorderLayout.CENTER);
        mainToolbarPanel.add(panelBotonesDerecha, BorderLayout.EAST);
        
        System.out.println("[ToolbarBuilder Constructor] Paneles de alineación configurados.");
        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    }

    // --- SECCIÓN 3: MÉTODO PRINCIPAL DE CONSTRUCCIÓN DE LA TOOLBAR ---
    /**
     * Construye la barra de herramientas completa basada en una lista de definiciones
     * y el mapa de acciones.
     *
     * @param toolbarStructure Lista de {@link ToolbarButtonDefinition} que define los botones.
     * @return El {@link JPanel} que contiene la barra de herramientas construida.
     */
    public JPanel buildToolbar(List<ToolbarButtonDefinition> toolbarStructure) {
        System.out.println("\n--- [ToolbarBuilder buildToolbar] Iniciando construcción de la toolbar ---");

        // 3.1. Reiniciar los paneles y el mapa de botones (por si se reutiliza el builder)
        panelBotonesIzquierda.removeAll();
        panelBotonesCentro.removeAll();
        panelBotonesDerecha.removeAll();
        this.botonesPorNombre.clear();
        System.out.println("  -> Paneles internos y mapa de botones reiniciados.");

        // 3.2. Validar la estructura de la toolbar proporcionada
        if (toolbarStructure == null || toolbarStructure.isEmpty()) {
            System.err.println("WARN [ToolbarBuilder buildToolbar]: La estructura de la toolbar (toolbarStructure) está vacía o es nula.");
            // Revalidar paneles vacíos y devolver el panel principal vacío
            mainToolbarPanel.revalidate();
            mainToolbarPanel.repaint();
            System.out.println("--- [ToolbarBuilder buildToolbar] Finalizado (toolbar vacía) ---");
            return this.mainToolbarPanel;
        }
        System.out.println("  -> Procesando " + toolbarStructure.size() + " definiciones de botones.");

        // 3.3. Iterar sobre las definiciones y procesar cada botón
        String ultimaCategoriaLayoutProcesada = null;
        for (ToolbarButtonDefinition definition : toolbarStructure) {
            // 3.3.1. (Opcional) Añadir separadores entre diferentes categorías de layout
            String categoriaActual = definition.categoriaLayout();
            if (ultimaCategoriaLayoutProcesada != null && !ultimaCategoriaLayoutProcesada.equals(categoriaActual)) {
                // Si la categoría cambió, decidimos a qué panel añadir el separador.
                // Podríamos añadirlo al panel del grupo ANTERIOR o al del NUEVO.
                // Por simplicidad, lo añadimos al panel que corresponde a la categoría ANTERIOR.
                addSeparatorToPanel(ultimaCategoriaLayoutProcesada);
            }
            
            // 3.3.2. Procesar la definición actual para crear y añadir el botón
            procesarToolbarButtonDefinition(definition);
            
            ultimaCategoriaLayoutProcesada = categoriaActual;
        }

        // 3.4. Revalidar y repintar los paneles al final para asegurar la correcta visualización
        panelBotonesIzquierda.revalidate();
        panelBotonesIzquierda.repaint();
        panelBotonesCentro.revalidate();
        panelBotonesCentro.repaint();
        panelBotonesDerecha.revalidate();
        panelBotonesDerecha.repaint();
        mainToolbarPanel.revalidate();
        mainToolbarPanel.repaint();

        System.out.println("--- [ToolbarBuilder buildToolbar] Toolbar construida. Total botones en mapa: " + this.botonesPorNombre.size() + " ---");
        return mainToolbarPanel;
    }

    // --- SECCIÓN 4: MÉTODOS HELPER INTERNOS ---

    // 4.1. Método para procesar una definición de botón individual
    /**
     * Crea un JButton a partir de una {@link ToolbarButtonDefinition}, lo configura
     * con su Action, icono, tooltip, y lo añade al panel de alineación correcto
     * y al mapa `botonesPorNombre`.
     *
     * @param definition La definición del botón a procesar.
     */
    private void procesarToolbarButtonDefinition(ToolbarButtonDefinition definition) {
        // 4.1.1. Validación de la definición
        if (definition == null) {
            System.err.println("WARN [TB procesarDef]: Se recibió una ToolbarButtonDefinition nula. Saltando botón.");
            return;
        }
        if (definition.comandoCanonico() == null || definition.claveIcono() == null) {
             System.err.println("WARN [TB procesarDef]: Definición de botón inválida (comando o claveIcono nulos). Tooltip: " + definition.textoTooltip());
             return;
        }
        System.out.println("  [TB procesarDef] Def: Comando='" + definition.comandoCanonico() + "', Icono='" + definition.claveIcono() + "', Cat='" + definition.categoriaLayout() + "'");

        // 4.1.2. Creación y configuración básica del JButton
        JButton button = new JButton();
        int anchoBotonCalculado = this.iconoAncho; // Asumimos que el ancho del botón es el del icono
        int altoBotonCalculado = (this.iconoAlto <= 0) ? anchoBotonCalculado : this.iconoAlto; // Si alto es -1, usar ancho (cuadrado) o el alto especificado

        button.setPreferredSize(new Dimension(anchoBotonCalculado + 6, altoBotonCalculado + 6)); // Pequeño padding visual
        button.setMargin(new Insets(1, 1, 1, 1)); // Margen mínimo
        button.setBorderPainted(false);          // Generalmente false para botones de toolbar con iconos
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);       // Necesario para que setBackground funcione
        button.setBackground(this.colorBotonFondoDefault);
        // button.setForeground(this.colorBotonTextoDefault); // No necesario si es solo icono
        button.setOpaque(true);                  // Asegurar que el fondo se pinte

        // 4.1.3. Asignar Tooltip
        if (definition.textoTooltip() != null && !definition.textoTooltip().isBlank()) {
            button.setToolTipText(definition.textoTooltip());
        } else {
            button.setToolTipText(definition.comandoCanonico()); // Fallback
        }

        // 4.1.4. Conectar Acción e Icono
        Action action = this.actionMap.get(definition.comandoCanonico());
        ImageIcon iconoParaBoton = null;

        if (action != null) {
            button.setAction(action); // Asigna la Action, esto puede configurar texto, icono, tooltip, enabled
            System.out.println("    -> Action '" + definition.comandoCanonico() + "' asignada al botón.");

            // Intentar obtener el icono desde la Action si ya fue configurado allí
            Object iconFromAction = action.getValue(Action.SMALL_ICON);
            if (iconFromAction instanceof ImageIcon) {
                iconoParaBoton = (ImageIcon) iconFromAction;
                 System.out.println("    -> Icono obtenido de la Action.");
            } else {
                // Si la Action no tiene icono, o no es ImageIcon, cargarlo usando claveIcono
                if (definition.claveIcono() != null) {
                    iconoParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                    if (iconoParaBoton != null) {
                        System.out.println("    -> Icono '" + definition.claveIcono() + "' cargado y escalado vía IconUtils (Action no lo tenía).");
                    } else {
                        System.err.println("ERROR [TB procesarDef]: Icono '" + definition.claveIcono() + "' NO ENCONTRADO para comando " + definition.comandoCanonico());
                    }
                }
            }
        } else {
            // Si no se encontró Action para el comando (podría ser un botón no funcional o de config)
            System.err.println("WARN [TB procesarDef]: No se encontró Action para comando: '" + definition.comandoCanonico() + "'. Configurando manualmente.");
            if (definition.comandoCanonico() != null) { // Establecer action command para identificación
                button.setActionCommand(definition.comandoCanonico());
            }
            // Cargar icono directamente, ya que no hay Action de donde tomarlo
            if (definition.claveIcono() != null) {
                iconoParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                 if (iconoParaBoton == null) {
                     System.err.println("ERROR [TB procesarDef]: Icono '" + definition.claveIcono() + "' NO ENCONTRADO (botón sin Action).");
                 }
            }
            // Añadir ActionListener fallback (VisorController)
            if (this.controllerRef != null) {
                button.addActionListener(this.controllerRef);
                 System.out.println("    -> ActionListener fallback (VisorController) añadido a botón sin Action.");
            }
        }

        // 4.1.5. Establecer Icono y Ocultar Texto si hay Icono
        if (iconoParaBoton != null) {
            button.setIcon(iconoParaBoton);
            button.setHideActionText(true); // Si hay icono, el texto de la Action no se muestra
            button.setText(null);           // Asegurar que no haya texto literal
        } else {
            // Si no hay icono, el botón mostrará el texto de la Action (si se asignó Action y tiene NAME)
            // o el texto literal si se puso button.setText("Algo") y no hay Action.
            button.setHideActionText(false);
            // Podrías poner un texto placeholder si es un error y no hay Action ni icono
            // if (action == null) button.setText("?");
        }

        // 4.1.6. Generar Clave Larga de Configuración y Añadir al Mapa del Builder
        //         (Esta clave se usa para que VisorView.getBotonesPorNombre() funcione
        //          y para que ConfigurationManager pueda guardar/leer estado de visibilidad/enabled)
        //         Usamos una combinación de categoría y el nombre base del icono.
        String nombreBaseDelIcono;
        if (definition.claveIcono() != null && !definition.claveIcono().isBlank()) {
            nombreBaseDelIcono = definition.claveIcono().replace(".png", ""); // Quitar extensión
            // Quitar prefijo numérico si existe (ej. "1001-")
            int dashIndex = nombreBaseDelIcono.indexOf('-');
            if (dashIndex != -1 && dashIndex < nombreBaseDelIcono.length() - 1) {
                nombreBaseDelIcono = nombreBaseDelIcono.substring(dashIndex + 1);
            }
        } else {
            // Fallback si no hay claveIcono (usar parte del comando)
            nombreBaseDelIcono = definition.comandoCanonico().replace("cmd.", "").replace(".", "_");
        }
        String fullConfigKey = "interfaz.boton." + definition.categoriaLayout() + "." + nombreBaseDelIcono;
        this.botonesPorNombre.put(fullConfigKey, button);
        System.out.println("    -> Botón añadido a mapa botonesPorNombre con clave: " + fullConfigKey);


        // 4.1.7. Añadir el Botón al Panel de Alineación Correspondiente
        int alignmentTarget = getAlignmentForCategory(definition.categoriaLayout());
        if (alignmentTarget == FlowLayout.LEFT) {
            panelBotonesIzquierda.add(button);
            System.out.println("    -> Botón añadido a panelBotonesIzquierda.");
        } else if (alignmentTarget == FlowLayout.CENTER) {
            panelBotonesCentro.add(button);
            System.out.println("    -> Botón añadido a panelBotonesCentro.");
        } else if (alignmentTarget == FlowLayout.RIGHT) {
            panelBotonesDerecha.add(button);
            System.out.println("    -> Botón añadido a panelBotonesDerecha.");
        } else { // Fallback
            System.err.println("WARN [TB procesarDef]: CategoríaLayout no mapeada a panel. Añadiendo a CENTRO por defecto: " + definition.categoriaLayout());
            panelBotonesCentro.add(button);
        }
    }

    // 4.2. Método para determinar la alineación basada en la categoría
    /**
     * Devuelve la constante de alineación de FlowLayout (LEFT, CENTER, RIGHT)
     * basada en la categoría de layout proporcionada.
     *
     * @param category La categoría del layout del botón (ej. "movimiento", "edicion").
     * @return La constante de alineación de FlowLayout.
     */
    private int getAlignmentForCategory(String category) {
        System.out.println("  [TB getAlignment] Para categoría: '" + category + "'");
        if (category == null) {
            System.err.println("WARN [TB getAlignment]: Categoría es null. Usando CENTER por defecto.");
            return FlowLayout.CENTER;
        }
        int alignment;
        switch (category.toLowerCase()) { // Usar toLowerCase para ser más robusto
            case "movimiento":
                alignment = FlowLayout.LEFT;
                break;
            case "edicion":
            case "zoom":
            case "vista":
            case "control":
            case "proyecto": 
            case "toggle":
                alignment = FlowLayout.CENTER;
                break;
            case "especiales":
                alignment = FlowLayout.RIGHT;
                break;
            default:
                System.err.println("WARN [TB getAlignment]: Categoría de layout desconocida '" + category + "'. Usando CENTER.");
                alignment = FlowLayout.CENTER;
        }
        System.out.println("    -> Alineación determinada: " + (alignment == FlowLayout.LEFT ? "LEFT" : alignment == FlowLayout.CENTER ? "CENTER" : "RIGHT"));
        return alignment;
    }

    // 4.3. Método para añadir separadores
    /**
     * Añade un separador visual (un espacio horizontal) al panel con la
     * alineación correspondiente a la categoría proporcionada.
     *
     * @param categoriaDelGrupoAnterior La categoría del grupo de botones ANTES del cual se quiere
     *                                  poner el separador.
     */
    private void addSeparatorToPanel(String categoriaDelGrupoAnterior) {
        if (categoriaDelGrupoAnterior == null) return;

        int alignmentTarget = getAlignmentForCategory(categoriaDelGrupoAnterior);
        Component separator = Box.createHorizontalStrut(10); // Espacio de 10px
        
        System.out.println("  [TB addSeparator] Añadiendo separador después de categoría: '" + categoriaDelGrupoAnterior + "'");

        if (alignmentTarget == FlowLayout.LEFT) {
            panelBotonesIzquierda.add(separator);
        } else if (alignmentTarget == FlowLayout.CENTER) {
            // Si el grupo anterior era central, el separador también va al central.
            // Si el siguiente grupo es central, el separador también va al central.
            // Generalmente, un separador entre un grupo central y otro (izq o der) se añade al panel central.
            panelBotonesCentro.add(separator);
        } else if (alignmentTarget == FlowLayout.RIGHT) {
            // Si el grupo anterior era derecho, el separador también va al derecho.
            panelBotonesDerecha.add(separator);
        }
        // Nota: La lógica de dónde poner el separador si hay un cambio de LEFT a CENTER,
        // o de CENTER a RIGHT puede necesitar ajustes finos.
        // Esta implementación simple añade el separador al panel que correspondía
        // a la *categoría anterior*.
    }

    // --- SECCIÓN 5: GETTER PARA EL MAPA DE BOTONES ---
    /**
     * Devuelve el mapa que asocia las claves de configuración LARGAS
     * con las instancias de JButton creadas.
     *
     * @return El mapa de botones por nombre (clave larga).
     */
    public Map<String, JButton> getBotonesPorNombre() {
        return Collections.unmodifiableMap(this.botonesPorNombre); // Devolver copia inmutable
    }
}

//package vista.builders;
//
//import java.awt.Color;
//import java.awt.FlowLayout; // Opcional, si el panel principal usa FlowLayout
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JPanel;
//import javax.swing.JToolBar; 
//// No es necesario importar VisorController directamente si solo pasamos una interfaz
//// o si las Actions ya manejan su propia lógica completamente.
//// Si se necesita para un listener fallback MUY específico, se podría pasar
//// una instancia de ActionListener o una interfaz funcional.
//// import controlador.VisorController; // Temporalmente, si es para el listener fallback
//
//import vista.config.ToolbarButtonDefinition;
//import vista.util.IconUtils;
//
//public class ToolbarBuilder {
//
//    // --- Campos de Instancia ---
//    private final Map<String, Action> actionMap;
//    private final IconUtils iconUtils;
//    private final Color colorBotonFondo;
//    private final Color colorBotonTexto; // No se usa si los botones solo tienen icono
//    private final Color colorBotonActivado;
//    private final Color colorBotonAnimacion;
//    private final int iconoAncho;
//    private final int iconoAlto;
//    // private final VisorController controllerRef; // Si necesitas el controller para algo específico (ej. listener fallback)
//
//    private final Map<String, JButton> botonesPorNombre; // Mapa para almacenar los botones creados por su clave larga
//
//    /**
//     * Constructor para ToolbarBuilder.
//     *
//     * @param actionMap           El mapa de acciones disponibles.
//     * @param colorBotonFondo     Color de fondo para los botones.
//     * @param colorBotonTexto     Color de texto para los botones (si muestran texto).
//     * @param colorBotonActivado  Color para botones en estado activado/toggle.
//     * @param colorBotonAnimacion Color para la animación de clic.
//     * @param iconoAncho          Ancho deseado para los iconos.
//     * @param iconoAlto           Alto deseado para los iconos.
//     * @param iconUtils           Utilidad para cargar iconos.
//     * @param controller          Referencia al VisorController (opcional, para listeners fallback o lógica compleja).
//     */
//    public ToolbarBuilder(
//            Map<String, Action> actionMap,
//            Color colorBotonFondo,
//            Color colorBotonTexto,
//            Color colorBotonActivado,
//            Color colorBotonAnimacion,
//            int iconoAncho,
//            int iconoAlto,
//            IconUtils iconUtils,
//            Object controllerCallbackProvider // Cambiado a Object para ser más genérico, o usa una interfaz funcional
//    ) {
//        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
//        this.colorBotonFondo = Objects.requireNonNull(colorBotonFondo, "colorBotonFondo no puede ser nulo");
//        this.colorBotonTexto = Objects.requireNonNull(colorBotonTexto, "colorBotonTexto no puede ser nulo");
//        this.colorBotonActivado = Objects.requireNonNull(colorBotonActivado, "colorBotonActivado no puede ser nulo");
//        this.colorBotonAnimacion = Objects.requireNonNull(colorBotonAnimacion, "colorBotonAnimacion no puede ser nulo");
//        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24; // Default si es inválido
//        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;   // Default si es inválido
//        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser nulo");
//        // this.controllerRef = controller; // Guardar si se usa
//
//        this.botonesPorNombre = new HashMap<>();
//        System.out.println("[ToolbarBuilder] Instancia creada.");
//    }
//
//    /**
//     * Construye el panel de la barra de herramientas (o múltiples JToolBars)
//     * basándose en la estructura de definiciones proporcionada.
//     *
//     * @param structure La lista de definiciones de botones de la toolbar.
//     * @return Un JPanel que contiene la(s) JToolBar(s) construida(s).
//     */
//    public JPanel buildToolbar(List<ToolbarButtonDefinition> structure) {
//        // Panel principal que contendrá todas las JToolBars (si hay varias)
//        // Usar FlowLayout para que las JToolBars se coloquen una al lado de la otra.
//        JPanel mainToolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
//        // mainToolbarPanel.setOpaque(false); // Opcional: si quieres que tome el fondo del contenedor
//
//        JToolBar currentToolBar = null;
//        String lastCategory = null;
//
//        System.out.println("  [ToolbarBuilder] Construyendo toolbar con " + structure.size() + " definiciones de botones...");
//
//        for (ToolbarButtonDefinition def : structure) {
//            String currentCategory = def.categoriaLayout();
//            if (currentCategory == null || currentCategory.isBlank()) {
//                currentCategory = "General"; // Categoría por defecto
//            }
//
//            // Si la categoría cambia, o es la primera toolbar, crear una nueva JToolBar
//            if (currentToolBar == null || !currentCategory.equals(lastCategory)) {
//                if (currentToolBar != null) {
//                    mainToolbarPanel.add(currentToolBar); // Añadir la JToolBar anterior al panel principal
//                }
//                currentToolBar = new JToolBar(currentCategory); // Nombre de la JToolBar (opcionalmente visible)
//                currentToolBar.setFloatable(false); // Evitar que la toolbar se pueda desacoplar
//                currentToolBar.setRollover(true);   // Efecto rollover en botones
//                // currentToolBar.setOpaque(false); // Si quieres que la JToolBar sea transparente
//                // currentToolBar.setBorder(BorderFactory.createEmptyBorder(2,2,2,2)); // Padding
//                lastCategory = currentCategory;
//                System.out.println("    -> Creando/Cambiando a JToolBar para categoría: " + currentCategory);
//            }
//
//            // Obtener la Action asociada al comando canónico
//            Action action = (def.comandoCanonico() != null) ? this.actionMap.get(def.comandoCanonico()) : null;
//            JButton button;
//
//            if (action != null) {
//                button = new JButton(action); // Crear botón a partir de la Action
//                
//                // Configuración adicional si la Action no establece todo (ej. icono específico de toolbar)
//                // El texto del botón se tomará de Action.NAME (si setHideActionText es false).
//                // El icono de Action.SMALL_ICON.
//                // El tooltip de Action.SHORT_DESCRIPTION.
//
//                // Si la Action no tiene un icono, pero la ToolbarButtonDefinition sí, podemos usarlo.
//                if (button.getIcon() == null && def.claveIcono() != null && this.iconUtils != null) {
//                    ImageIcon icon = this.iconUtils.getScaledIcon(def.claveIcono(), this.iconoAncho, this.iconoAlto);
//                    if (icon != null) {
//                        button.setIcon(icon);
//                    }
//                }
//            } else {
//                // Si no hay Action, crear un botón deshabilitado con el icono y tooltip
//                button = new JButton(); // Botón vacío
//                if (def.claveIcono() != null && this.iconUtils != null) {
//                    ImageIcon icon = this.iconUtils.getScaledIcon(def.claveIcono(), this.iconoAncho, this.iconoAlto);
//                    if (icon != null) {
//                        button.setIcon(icon);
//                    }
//                }
//                button.setEnabled(false); // Deshabilitar si no hay acción
//                System.err.println("WARN [ToolbarBuilder]: No se encontró Action para el comando: " + def.comandoCanonico() +
//                                   " (Tooltip: " + def.textoTooltip() + "). Botón creado deshabilitado.");
//            }
//
//            // Establecer tooltip siempre desde la definición, puede ser más específico que el SHORT_DESCRIPTION de la Action.
//            button.setToolTipText(def.textoTooltip());
//
//            // Ocultar texto si el botón tiene un icono (comportamiento típico de toolbar)
//            if (button.getIcon() != null) {
//                // button.setText(null); // Quitar texto explícitamente
//                button.setHideActionText(true); // Preferido, pero depende del LookAndFeel
//            }
//            
//            // Aplicar estilos comunes a los botones de la toolbar
//            button.setBackground(this.colorBotonFondo);
//            // button.setForeground(this.colorBotonTexto); // Solo si el texto es visible
//            button.setFocusPainted(false); // Quitar el borde de foco al hacer clic
//            // button.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5)); // Padding interno del botón
//
//            // Construir la clave larga para el mapa `botonesPorNombre`
//            // Esta clave debe ser la misma que se usa en VisorView.aplicarAnimacionBoton
//            // y en cualquier otro lugar donde se necesite buscar un botón por su "identificador largo".
//            // Ejemplo: "interfaz.boton.<categoria>.<nombreIconoSinExtensionYFormateado>"
//            // Es crucial que esta generación de clave sea consistente.
//            String claveLargaBoton = generarClaveLargaParaBoton(def);
//            this.botonesPorNombre.put(claveLargaBoton, button);
//            // System.out.println("      -> Botón creado/configurado. Clave Larga: '" + claveLargaBoton + "', Comando: " + def.comandoCanonico());
//
//            // Añadir el botón a la JToolBar actual
//            if (currentToolBar != null) {
//                currentToolBar.add(button);
//                
//                // Añadir separador si el comando es un marcador de separador (ejemplo)
//                // Necesitarías definir un comando especial para esto en AppActionCommands
//                // if (AppActionCommands.CMD_TOOLBAR_SEPARATOR.equals(def.comandoCanonico())) {
//                //    currentToolBar.addSeparator(new Dimension(6,0)); // Separador con un poco de espacio
//                // }
//            } else {
//            	//FIXME codigo Muerto
//                // Fallback por si algo va mal con la lógica de JToolBar por categoría
//                mainToolbarPanel.add(button); 
//            }
//        }
//        
//        // Añadir la última JToolBar creada al panel principal
//        if (currentToolBar != null) {
//            mainToolbarPanel.add(currentToolBar);
//        }
//        
//        System.out.println("  [ToolbarBuilder] Construcción de toolbar finalizada.");
//        return mainToolbarPanel;
//    }
//
//    /**
//     * Genera una clave larga y única para un botón de la barra de herramientas,
//     * basándose en su definición. Esta clave se usa para almacenar y recuperar
//     * el botón en el mapa `botonesPorNombre`.
//     * Es importante que este método genere claves consistentes con cómo se
//     * referencian estos botones en otras partes del código (ej. para animaciones
//     * o para la configuración de visibilidad/habilitación desde el menú).
//     *
//     * @param def La definición del botón de la barra de herramientas.
//     * @return Una cadena que representa la clave larga del botón.
//     */
//    private String generarClaveLargaParaBoton(ToolbarButtonDefinition def) {
//        String categoria = (def.categoriaLayout() != null && !def.categoriaLayout().isBlank()) 
//                           ? def.categoriaLayout().toLowerCase().replace(" ", "_") 
//                           : "general";
//        
//        String nombreBaseIcono = "desconocido";
//        if (def.claveIcono() != null && !def.claveIcono().isBlank()) {
//            nombreBaseIcono = def.claveIcono()
//                              .replace(".png", "")  // Quitar extensión
//                              .replace(".gif", "")  // Quitar otras extensiones comunes
//                              .replace(".jpg", "")
//                              .replace(".jpeg", "")
//                              .replaceAll("[^a-zA-Z0-9_]", "_"); // Reemplazar caracteres no alfanuméricos por _
//            // Quitar números iniciales y guiones si es el formato "XXXX-Nombre_WxH"
//            nombreBaseIcono = nombreBaseIcono.replaceAll("^\\d+-", ""); 
//        } else if (def.comandoCanonico() != null && !def.comandoCanonico().isBlank()) {
//            // Fallback a usar parte del comando canónico si no hay claveIcono
//            String[] partesCmd = def.comandoCanonico().split("\\.");
//            nombreBaseIcono = partesCmd.length > 0 ? partesCmd[partesCmd.length -1] : "cmd_desconocido";
//        }
//        
//        // Formato: "interfaz.boton.<categoria>.<nombre_limpio_del_icono_o_comando>"
//        return "interfaz.boton." + categoria + "." + nombreBaseIcono;
//    }
//
//
//    /**
//     * Devuelve el mapa de botones creados, donde la clave es la
//     * clave de configuración larga generada y el valor es el JButton.
//     * @return El mapa de botones.
//     */
//    public Map<String, JButton> getBotonesPorNombre() {
//        return this.botonesPorNombre;
//    }
//}