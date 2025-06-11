package vista.builders;

import java.awt.Color;
import java.awt.Insets;
import java.util.Collections; // Para Collections.emptyMap() si es necesario
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import controlador.VisorController; // Para ActionListener fallback
import servicios.ConfigKeys;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.util.IconUtils;

public class ToolbarBuilder {

    // --- SECCIÓN 1: CAMPOS DE INSTANCIA Y CONSTANTES ---

    // 1.1. Resultados del Builder
//    private JPanel mainToolbarPanel;         // El panel principal que contendrá todas las barras/secciones
    private Map<String, JButton> botonesPorNombre; // Mapa: Clave Larga de Config -> JButton

//    // 1.2. Paneles Internos para Alineación
//    private JPanel panelBotonesIzquierda;
//    private JPanel panelBotonesCentro;
//    private JPanel panelBotonesDerecha;
    
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
            Color _colorBotonActivadoIgnorado,
            Color _colorBotonAnimacionIgnorado,
            int iconoAncho,
            int iconoAlto,
            IconUtils iconUtils,
            VisorController controller
    ) {
        System.out.println("[ToolbarBuilder Constructor] Iniciando...");

        // 1. Asignación de Dependencias
        this.actionMap = (actionMap != null) ? actionMap : Collections.emptyMap();
        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        
        // 2. Configuración de Apariencia por Defecto
        this.colorBotonFondoDefault = (colorBotonFondo != null) ? colorBotonFondo : new Color(238, 238, 238);
        this.colorBotonTextoDefault = (colorBotonTexto != null) ? colorBotonTexto : Color.BLACK;
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto <= 0) ? this.iconoAncho : iconoAlto;

        // 3. Inicialización del Mapa de Botones
        this.botonesPorNombre = new HashMap<>();
        
        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    }
    
//    public ToolbarBuilder(
//            Map<String, Action> actionMap,
//            Color colorBotonFondo,
//            Color colorBotonTexto,
//            Color _colorBotonActivadoIgnorado,  // Parámetro no usado directamente aquí, lo usa ViewUIConfig
//            Color _colorBotonAnimacionIgnorado, // Parámetro no usado directamente aquí, lo usa ViewUIConfig
//            int iconoAncho,
//            int iconoAlto,
//            IconUtils iconUtils,
//            VisorController controller
//    ) {
//        System.out.println("[ToolbarBuilder Constructor] Iniciando...");
//
//        // 2.1. Asignación de Dependencias
//        this.actionMap = (actionMap != null) ? actionMap : Collections.emptyMap();
//        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null en ToolbarBuilder");
//        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en ToolbarBuilder");
//        
//        // 2.2. Configuración de Apariencia por Defecto para Botones
//        this.colorBotonFondoDefault = (colorBotonFondo != null) ? colorBotonFondo : new Color(238, 238, 238);
//        this.colorBotonTextoDefault = (colorBotonTexto != null) ? colorBotonTexto : Color.BLACK;
//        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
//        this.iconoAlto = (iconoAlto <= 0) ? ((this.iconoAncho > 0) ? -1 : 24) : iconoAlto; // Si alto <= 0, usa -1 (mantener proporción) o 24 si ancho también es inválido
//
//        // 2.3. Inicialización del Mapa de Botones Creados
//        this.botonesPorNombre = new HashMap<>();
//
//        // 2.4. Inicialización de Paneles para Alineación
//        // Estos paneles usarán FlowLayout para alinear los botones DENTRO de su sección.
////        panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // (alineación, hgap, vgap)
////        panelBotonesCentro = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
////        panelBotonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
//
//        // 2.5. Configuración de Apariencia de los Paneles de Alineación
//        // Es importante que sean opacos si tienen un color de fondo diferente al panel principal,
//        // o no opacos si quieres que tomen el color del panel principal.
//        // Asumimos que la toolbar tiene un fondo uniforme.
////        panelBotonesIzquierda.setBackground(this.colorBotonFondoDefault); // O el color de fondo de la toolbar principal
////        panelBotonesCentro.setBackground(this.colorBotonFondoDefault);
////        panelBotonesDerecha.setBackground(this.colorBotonFondoDefault);
////        panelBotonesIzquierda.setOpaque(true);
////        panelBotonesCentro.setOpaque(true);
////        panelBotonesDerecha.setOpaque(true);
//
////        // 2.6. Creación del Panel Principal de la Toolbar
////        // Este panel usa BorderLayout para posicionar los paneles de alineación.
////        mainToolbarPanel = new JPanel(new BorderLayout());
////        mainToolbarPanel.setBackground(this.colorBotonFondoDefault); // O el color de fondo general de la UI
////        mainToolbarPanel.setOpaque(true);
//        
//        // 2.7. Añadir Paneles de Alineación al Panel Principal
////        mainToolbarPanel.add(panelBotonesIzquierda, BorderLayout.WEST);
////        mainToolbarPanel.add(panelBotonesCentro, BorderLayout.CENTER);
////        mainToolbarPanel.add(panelBotonesDerecha, BorderLayout.EAST);
//        
//        System.out.println("[ToolbarBuilder Constructor] Paneles de alineación configurados.");
//        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
//    }
    

    // --- SECCIÓN 3: MÉTODO PRINCIPAL DE CONSTRUCCIÓN DE LA TOOLBAR ---
    /**
     * Construye y devuelve una única JToolBar basada en una ToolbarDefinition.
     * Este método es ahora el núcleo de la fábrica de barras de herramientas.
     *
     * @param toolbarDef La definición completa de la barra a construir.
     * @return una nueva instancia de JToolBar, poblada con sus botones.
     */
    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) {
        System.out.println("\n--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");

        // 1. Crear la JToolBar
        JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra()); // Identificador interno
        toolbar.setFloatable(false); // Opcional: para que no se pueda mover
        toolbar.setRollover(true);   // Opcional: para efecto visual al pasar el ratón

        // 2. Iterar sobre los botones de ESTA definición
        if (toolbarDef.botones() == null || toolbarDef.botones().isEmpty()) {
            System.out.println("    -> Definición no contiene botones. Devolviendo barra vacía.");
            return toolbar;
        }

        for (ToolbarButtonDefinition botonDef : toolbarDef.botones()) {
            // 3. Crear el botón individual
            JButton boton = crearBotonIndividual(botonDef); // Usaremos un nuevo método helper
            
            // 4. Añadir el botón a la barra
            if (boton != null) {
                toolbar.add(boton);
            }
        }
        
        System.out.println("--- [ToolbarBuilder] Barra '" + toolbarDef.titulo() + "' construida con " + toolbar.getComponentCount() + " componentes.");
        return toolbar;
    }
//    /**
//     * Construye la barra de herramientas completa basada en una lista de definiciones
//     * y el mapa de acciones.
//     *
//     * @param toolbarStructure Lista de {@link ToolbarButtonDefinition} que define los botones.
//     * @return El {@link JPanel} que contiene la barra de herramientas construida.
//     */
//    public JPanel buildToolbar(List<ToolbarButtonDefinition> toolbarStructure) {
//        System.out.println("\n--- [ToolbarBuilder buildToolbar] Iniciando construcción de la toolbar ---");
//
//        // 3.1. Reiniciar los paneles y el mapa de botones (por si se reutiliza el builder)
//        panelBotonesIzquierda.removeAll();
//        panelBotonesCentro.removeAll();
//        panelBotonesDerecha.removeAll();
//        this.botonesPorNombre.clear();
//        System.out.println("  -> Paneles internos y mapa de botones reiniciados.");
//
//        // 3.2. Validar la estructura de la toolbar proporcionada
//        if (toolbarStructure == null || toolbarStructure.isEmpty()) {
//            System.err.println("WARN [ToolbarBuilder buildToolbar]: La estructura de la toolbar (toolbarStructure) está vacía o es nula.");
//            // Revalidar paneles vacíos y devolver el panel principal vacío
//            mainToolbarPanel.revalidate();
//            mainToolbarPanel.repaint();
//            System.out.println("--- [ToolbarBuilder buildToolbar] Finalizado (toolbar vacía) ---");
//            return this.mainToolbarPanel;
//        }
//        System.out.println("  -> Procesando " + toolbarStructure.size() + " definiciones de botones.");
//
//        // 3.3. Iterar sobre las definiciones y procesar cada botón
//        String ultimaCategoriaLayoutProcesada = null;
//        for (ToolbarButtonDefinition definition : toolbarStructure) {
//            // 3.3.1. (Opcional) Añadir separadores entre diferentes categorías de layout
//            String categoriaActual = definition.categoriaLayout();
//            if (ultimaCategoriaLayoutProcesada != null && !ultimaCategoriaLayoutProcesada.equals(categoriaActual)) {
//                // Si la categoría cambió, decidimos a qué panel añadir el separador.
//                // Podríamos añadirlo al panel del grupo ANTERIOR o al del NUEVO.
//                // Por simplicidad, lo añadimos al panel que corresponde a la categoría ANTERIOR.
//                addSeparatorToPanel(ultimaCategoriaLayoutProcesada);
//            }
//            
//            // 3.3.2. Procesar la definición actual para crear y añadir el botón
//            procesarToolbarButtonDefinition(definition);
//            
//            ultimaCategoriaLayoutProcesada = categoriaActual;
//        }
//
//        // 3.4. Revalidar y repintar los paneles al final para asegurar la correcta visualización
//        panelBotonesIzquierda.revalidate();
//        panelBotonesIzquierda.repaint();
//        panelBotonesCentro.revalidate();
//        panelBotonesCentro.repaint();
//        panelBotonesDerecha.revalidate();
//        panelBotonesDerecha.repaint();
//        mainToolbarPanel.revalidate();
//        mainToolbarPanel.repaint();
//
//        System.out.println("--- [ToolbarBuilder buildToolbar] Toolbar construida. Total botones en mapa: " + this.botonesPorNombre.size() + " ---");
//        return mainToolbarPanel;
//    }

    // --- SECCIÓN 4: MÉTODOS HELPER INTERNOS ---

    // 4.1. Método para procesar una definición de botón individual
    
    /**
     * Crea y configura un único JButton a partir de su definición.
     * Asigna la acción, icono, tooltip y lo registra en el mapa de botones
     * usando una clave canónica basada en el comando de la acción.
     *
     * @param definition La definición del botón a crear.
     * @return El JButton configurado, o null si la definición es inválida.
     */
    private JButton crearBotonIndividual(ToolbarButtonDefinition definition) {
        if (definition == null || definition.comandoCanonico() == null) {
            System.err.println("WARN [crearBotonIndividual]: Definición de botón inválida o sin comando.");
            return null;
        }

        // 1. Crear el JButton y aplicar estilos básicos.
        JButton button = new JButton();
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(this.colorBotonFondoDefault);
        button.setOpaque(true);
        button.setToolTipText(definition.textoTooltip());

        // 2. Asignar la Action desde el mapa.
        Action action = this.actionMap.get(definition.comandoCanonico());
        if (action != null) {
            button.setAction(action);
            // Ocultamos el texto por defecto si la Action lo tuviera,
            // ya que en la toolbar priorizamos el icono.
            button.setText(null); 
        } else {
            // Fallback si no se encuentra una Action predefinida.
            System.err.println("WARN [crearBotonIndividual]: No se encontró Action para comando: '" + definition.comandoCanonico() + "'");
            button.setText("?"); // Placeholder visual de error.
            button.setActionCommand(definition.comandoCanonico());
            button.addActionListener(this.controllerRef); // Asignar el listener global.
        }

        // 3. Asegurar que el icono esté presente.
        // Si la Action no tenía un icono asignado, lo cargamos desde la definición.
        if (button.getIcon() == null && definition.claveIcono() != null) {
            ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
            button.setIcon(icono);
        }

        // 4. Generar la clave de configuración canónica y registrar el botón.
        // Se basa en la CATEGORÍA y el COMANDO CANÓNICO.
        String nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
        String claveBaseBoton = ConfigKeys.buildKey(
            "interfaz.boton",
            definition.categoriaLayout(),
            nombreBotonParaClave
        );
        
        this.botonesPorNombre.put(claveBaseBoton, button);
        System.out.println("    -> Botón '" + definition.comandoCanonico() + "' creado y mapeado a clave base: " + claveBaseBoton);
        
        return button;
    } // --- FIN del metodo crearBotonIndividual ---


    private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null) return "desconocido";
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;
        return resultado.replace('.', '_');
    }
    
//    /**
//     * Crea un JButton a partir de una {@link ToolbarButtonDefinition}, lo configura
//     * con su Action, icono, tooltip, y lo añade al panel de alineación correcto
//     * y al mapa `botonesPorNombre`.
//     *
//     * @param definition La definición del botón a procesar.
//     */
//    private void procesarToolbarButtonDefinition(ToolbarButtonDefinition definition) {
//        // 4.1.1. Validación de la definición
//        if (definition == null) {
//            System.err.println("WARN [TB procesarDef]: Se recibió una ToolbarButtonDefinition nula. Saltando botón.");
//            return;
//        }
//        if (definition.comandoCanonico() == null || definition.claveIcono() == null) {
//             System.err.println("WARN [TB procesarDef]: Definición de botón inválida (comando o claveIcono nulos). Tooltip: " + definition.textoTooltip());
//             return;
//        }
//        System.out.println("  [TB procesarDef] Def: Comando='" + definition.comandoCanonico() + "', Icono='" + definition.claveIcono() + "', Cat='" + definition.categoriaLayout() + "'");
//
//        // 4.1.2. Creación y configuración básica del JButton
//        JButton button = new JButton();
//        int anchoBotonCalculado = this.iconoAncho; // Asumimos que el ancho del botón es el del icono
//        int altoBotonCalculado = (this.iconoAlto <= 0) ? anchoBotonCalculado : this.iconoAlto; // Si alto es -1, usar ancho (cuadrado) o el alto especificado
//
//        button.setPreferredSize(new Dimension(anchoBotonCalculado + 6, altoBotonCalculado + 6)); // Pequeño padding visual
//        button.setMargin(new Insets(1, 1, 1, 1)); // Margen mínimo
//        button.setBorderPainted(false);          // Generalmente false para botones de toolbar con iconos
//        button.setFocusPainted(false);
//        button.setContentAreaFilled(true);       // Necesario para que setBackground funcione
//        button.setBackground(this.colorBotonFondoDefault);
//        // button.setForeground(this.colorBotonTextoDefault); // No necesario si es solo icono
//        button.setOpaque(true);                  // Asegurar que el fondo se pinte
//
//        // 4.1.3. Asignar Tooltip
//        if (definition.textoTooltip() != null && !definition.textoTooltip().isBlank()) {
//            button.setToolTipText(definition.textoTooltip());
//        } else {
//            button.setToolTipText(definition.comandoCanonico()); // Fallback
//        }
//
//        // 4.1.4. Conectar Acción e Icono
//        Action action = this.actionMap.get(definition.comandoCanonico());
//        if (definition.comandoCanonico().equals(AppActionCommands.CMD_TOGGLE_SUBCARPETAS)) {
//            System.out.println("ToolbarBuilder: Obtenida Action@" + Integer.toHexString(System.identityHashCode(action)) + 
//                               " para CMD_TOGGLE_SUBCARPETAS. Asignando a botón.");
//        }
//        button.setAction(action);
//        
//        ImageIcon iconoParaBoton = null;
//
//        if (action != null) {
//            button.setAction(action); // Asigna la Action, esto puede configurar texto, icono, tooltip, enabled
//            System.out.println("    -> Action '" + definition.comandoCanonico() + "' asignada al botón.");
//
//            // Intentar obtener el icono desde la Action si ya fue configurado allí
//            Object iconFromAction = action.getValue(Action.SMALL_ICON);
//            if (iconFromAction instanceof ImageIcon) {
//                iconoParaBoton = (ImageIcon) iconFromAction;
//                 System.out.println("    -> Icono obtenido de la Action.");
//            } else {
//                // Si la Action no tiene icono, o no es ImageIcon, cargarlo usando claveIcono
//                if (definition.claveIcono() != null) {
//                    iconoParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
//                    if (iconoParaBoton != null) {
//                        System.out.println("    -> Icono '" + definition.claveIcono() + "' cargado y escalado vía IconUtils (Action no lo tenía).");
//                    } else {
//                        System.err.println("ERROR [TB procesarDef]: Icono '" + definition.claveIcono() + "' NO ENCONTRADO para comando " + definition.comandoCanonico());
//                    }
//                }
//            }
//        } else {
//            // Si no se encontró Action para el comando (podría ser un botón no funcional o de config)
//            System.err.println("WARN [TB procesarDef]: No se encontró Action para comando: '" + definition.comandoCanonico() + "'. Configurando manualmente.");
//            if (definition.comandoCanonico() != null) { // Establecer action command para identificación
//                button.setActionCommand(definition.comandoCanonico());
//            }
//            // Cargar icono directamente, ya que no hay Action de donde tomarlo
//            if (definition.claveIcono() != null) {
//                iconoParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
//                 if (iconoParaBoton == null) {
//                     System.err.println("ERROR [TB procesarDef]: Icono '" + definition.claveIcono() + "' NO ENCONTRADO (botón sin Action).");
//                 }
//            }
//            // Añadir ActionListener fallback (VisorController)
//            if (this.controllerRef != null) {
//                button.addActionListener(this.controllerRef);
//                 System.out.println("    -> ActionListener fallback (VisorController) añadido a botón sin Action.");
//            }
//        }
//
//        // 4.1.5. Establecer Icono y Ocultar Texto si hay Icono
//        if (iconoParaBoton != null) {
//            button.setIcon(iconoParaBoton);
//            button.setHideActionText(true); // Si hay icono, el texto de la Action no se muestra
//            button.setText(null);           // Asegurar que no haya texto literal
//        } else {
//            // Si no hay icono, el botón mostrará el texto de la Action (si se asignó Action y tiene NAME)
//            // o el texto literal si se puso button.setText("Algo") y no hay Action.
//            button.setHideActionText(false);
//            // Podrías poner un texto placeholder si es un error y no hay Action ni icono
//            // if (action == null) button.setText("?");
//        }
//
//        // 4.1.6. Generar Clave Larga de Configuración y Añadir al Mapa del Builder
//        //         (Esta clave se usa para que VisorView.getBotonesPorNombre() funcione
//        //          y para que ConfigurationManager pueda guardar/leer estado de visibilidad/enabled)
//        //         Usamos una combinación de categoría y el nombre base del icono.
//        String nombreBaseDelIcono;
//        if (definition.claveIcono() != null && !definition.claveIcono().isBlank()) {
//            nombreBaseDelIcono = definition.claveIcono().replace(".png", ""); // Quitar extensión
//            // Quitar prefijo numérico si existe (ej. "1001-")
//            int dashIndex = nombreBaseDelIcono.indexOf('-');
//            if (dashIndex != -1 && dashIndex < nombreBaseDelIcono.length() - 1) {
//                nombreBaseDelIcono = nombreBaseDelIcono.substring(dashIndex + 1);
//            }
//        } else {
//            // Fallback si no hay claveIcono (usar parte del comando)
//            nombreBaseDelIcono = definition.comandoCanonico().replace("cmd.", "").replace(".", "_");
//        }
//        String fullConfigKey = "interfaz.boton." + definition.categoriaLayout() + "." + nombreBaseDelIcono;
//        this.botonesPorNombre.put(fullConfigKey, button);
//        System.out.println("    -> Botón añadido a mapa botonesPorNombre con clave: " + fullConfigKey);
//
//
//        // 4.1.7. Añadir el Botón al Panel de Alineación Correspondiente
//        int alignmentTarget = getAlignmentForCategory(definition.categoriaLayout());
//        if (alignmentTarget == FlowLayout.LEFT) {
//            panelBotonesIzquierda.add(button);
//            System.out.println("    -> Botón añadido a panelBotonesIzquierda.");
//        } else if (alignmentTarget == FlowLayout.CENTER) {
//            panelBotonesCentro.add(button);
//            System.out.println("    -> Botón añadido a panelBotonesCentro.");
//        } else if (alignmentTarget == FlowLayout.RIGHT) {
//            panelBotonesDerecha.add(button);
//            System.out.println("    -> Botón añadido a panelBotonesDerecha.");
//        } else { // Fallback
//            System.err.println("WARN [TB procesarDef]: CategoríaLayout no mapeada a panel. Añadiendo a CENTRO por defecto: " + definition.categoriaLayout());
//            panelBotonesCentro.add(button);
//        }
//    }

//    // 4.2. Método para determinar la alineación basada en la categoría
//    /**
//     * Devuelve la constante de alineación de FlowLayout (LEFT, CENTER, RIGHT)
//     * basada en la categoría de layout proporcionada.
//     *
//     * @param category La categoría del layout del botón (ej. "movimiento", "edicion").
//     * @return La constante de alineación de FlowLayout.
//     */
//    private int getAlignmentForCategory(String category) {
//        System.out.println("  [TB getAlignment] Para categoría: '" + category + "'");
//        if (category == null) {
//            System.err.println("WARN [TB getAlignment]: Categoría es null. Usando CENTER por defecto.");
//            return FlowLayout.CENTER;
//        }
//        int alignment;
//        switch (category.toLowerCase()) { // Usar toLowerCase para ser más robusto
//            case "movimiento":
//                alignment = FlowLayout.LEFT;
//                break;
//            case "edicion":
//            case "zoom":
//            case "vista":
//            case "control":
//            case "proyecto": 
//            case "toggle":
//                alignment = FlowLayout.CENTER;
//                break;
//            case "especiales":
//                alignment = FlowLayout.RIGHT;
//                break;
//            default:
//                System.err.println("WARN [TB getAlignment]: Categoría de layout desconocida '" + category + "'. Usando CENTER.");
//                alignment = FlowLayout.CENTER;
//        }
//        System.out.println("    -> Alineación determinada: " + (alignment == FlowLayout.LEFT ? "LEFT" : alignment == FlowLayout.CENTER ? "CENTER" : "RIGHT"));
//        return alignment;
//    }

//    // 4.3. Método para añadir separadores
//    /**
//     * Añade un separador visual (un espacio horizontal) al panel con la
//     * alineación correspondiente a la categoría proporcionada.
//     *
//     * @param categoriaDelGrupoAnterior La categoría del grupo de botones ANTES del cual se quiere
//     *                                  poner el separador.
//     */
//    private void addSeparatorToPanel(String categoriaDelGrupoAnterior) {
//        if (categoriaDelGrupoAnterior == null) return;
//
//        int alignmentTarget = getAlignmentForCategory(categoriaDelGrupoAnterior);
//        Component separator = Box.createHorizontalStrut(10); // Espacio de 10px
//        
//        System.out.println("  [TB addSeparator] Añadiendo separador después de categoría: '" + categoriaDelGrupoAnterior + "'");
//
//        if (alignmentTarget == FlowLayout.LEFT) {
//            panelBotonesIzquierda.add(separator);
//        } else if (alignmentTarget == FlowLayout.CENTER) {
//            // Si el grupo anterior era central, el separador también va al central.
//            // Si el siguiente grupo es central, el separador también va al central.
//            // Generalmente, un separador entre un grupo central y otro (izq o der) se añade al panel central.
//            panelBotonesCentro.add(separator);
//        } else if (alignmentTarget == FlowLayout.RIGHT) {
//            // Si el grupo anterior era derecho, el separador también va al derecho.
//            panelBotonesDerecha.add(separator);
//        }
//        // Nota: La lógica de dónde poner el separador si hay un cambio de LEFT a CENTER,
//        // o de CENTER a RIGHT puede necesitar ajustes finos.
//        // Esta implementación simple añade el separador al panel que correspondía
//        // a la *categoría anterior*.
//    }

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

