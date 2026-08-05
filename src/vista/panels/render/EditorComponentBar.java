package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import controlador.commands.AppActionCommands;
import controlador.tools.CanvasController;
import controlador.tools.ShapeTool;
import controlador.tools.TextTool;
import modelo.editor.CanvasModel;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.TextLayer;
import vista.config.SeparatorDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.util.IconUtils;

public class EditorComponentBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JComboBox<ToolItem> toolCombo;
    private final DefaultComboBoxModel<ToolItem> comboModel;
    private final JSpinner spinnerX;
    private final JSpinner spinnerY;
    private final JSpinner spinnerW;
    private final JSpinner spinnerH;
    private final JSpinner spinnerAngle;
    private final JLabel labelX;
    private final JLabel labelY;
    private final JLabel labelW;
    private final JLabel labelH;
    private final JLabel labelAngle;
    private final JPanel toolControlsPanel;
    private final CardLayout cardLayout;
    private final Map<String, JPanel> toolOptionsMap;
    private final JPanel row1Right;
    private final JPanel leftPartHome;
    private final java.util.List<Component> leftPartToolComponents;

    private IconUtils iconUtils;
    private UIDefinitionService uiDefinitionService;
    private Color fgToolbar = Color.WHITE;

    // Home mode state
    private boolean homeActive;
    private boolean homeCanvasSubMode; // true=medidas, false=nuevo
    private final JPanel leftPart;

    // EditTool checkboxes
    private final JCheckBox chkAutoSelect = new JCheckBox("Selec.auto");
    private final JCheckBox chkShowGizmo = new JCheckBox("Tiradores");
    private final JCheckBox chkKeepAspect = new JCheckBox("Proporci\u00F3n");
    private final JCheckBox chkAutoZoom = new JCheckBox("Zoom auto");

    // Icon combos (sustituyen a los D-Pad)

    // Widgets compartidos del home lienzo (para leer valores desde handleHomeAction)
    private JSpinner homeWSpinner;
    private JSpinner homeHSpinner;
    private JButton homeColorSwatch;
    private JPanel homeLienzoPanel;
    private JPanel homeImagePanel;

    // Mapa de constructores de paneles (Strategy) — elimina el if-chain de buildPanelForTool
    @FunctionalInterface
    private interface PanelBuilder {
        JPanel build(Color bg);
    }

    // Referencia para actualizar el label de capa en el panel de edición
    private JLabel editLayerNameLabel;

    private final Map<String, PanelBuilder> toolPanelBuilders = new HashMap<>();

    private String currentToolCommandKey;

    // Referencia al CanvasController (para conectar controles de texto)
    private CanvasController canvasController;

    // Se dispara cuando cambia cualquier propiedad de texto (para sincronizar panel derecho)
    private Runnable onTextChange;

    public void setOnTextChange(Runnable r) { this.onTextChange = r; }

    // --- Retención de componentes del panel de texto ---
    private JComboBox<String> textFontCombo;
    private JComboBox<Integer> textSizeCombo;
    private JButton textColorBtn;
    private JToggleButton textBoldBtn;
    private JToggleButton textItalicBtn;
    private JToggleButton textUnderlineBtn;
    private JToggleButton textStrikethroughBtn;
    private JToggleButton textAlignLeftBtn;
    private JToggleButton textAlignCenterBtn;
    private JToggleButton textAlignRightBtn;
    private JToggleButton textJustifiedBtn;
    private JToggleButton textFlowRowsBtn;
    private JToggleButton textFlowColumnsBtn;
    private JToggleButton textHorizontalBtn;
    private JToggleButton textVerticalBtn;

    // ===== Opciones de herramientas (Parte B) conectadas a las Tools =====
    private JSlider featherSlider;
    private JSpinner wandToleranceSpinner;
    private JCheckBox wandContiguousChk;
    private JCheckBox cropKeepOriginalChk;
    private int eyedropperSampleSize = 1;
    private Color eyedropperColor = Color.WHITE;
    private JLabel eyedropperSwatch;
    private JLabel eyedropperRgbLabel;
    private Color paintBucketColor = new Color(255, 0, 0);
    private JButton paintBucketSwatch;
    private JSpinner paintToleranceSpinner;
    private JCheckBox paintContiguousChk;
    private Color gradientStartColor = new Color(255, 0, 0);
    private Color gradientEndColor = new Color(0, 0, 255);
    private JButton gradientStartSwatch;
    private JButton gradientEndSwatch;
    private JPanel gradientPreviewPanel;
    private JComboBox<String> gradientTypeCombo;
    private JSlider gradientOpacitySlider;
    private Color shapeFillColor = new Color(200, 200, 200);
    private Color shapeStrokeColor = Color.BLACK;
    private JSpinner shapeStrokeWidthSpinner;
    private String selectedShapeType = "rect";

    // Referencias para sincronizar el panel de formas al editar una capa existente
    private JButton shapeFillSwatch;
    private JButton shapeStrokeSwatch;
    private final Map<String, JToggleButton> shapeTypeButtons = new HashMap<>();
    private Runnable onShapeChange;

    public void setOnShapeChange(Runnable r) { this.onShapeChange = r; }
    private String transformMode = "mover";
    private String transformTarget = "capa";
    private String cropMode = "nueva_capa";
    private String zoomMode = "cursor";
    private JToggleButton transformTargetLayerBtn;
    private boolean syncAngleGuard;

    {
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION,           this::buildEditPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR, this::buildTransformPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO, this::buildFeatherPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA, this::buildFeatherPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_VARITA, this::buildWandPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR, this::buildCropPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS, this::buildEyedropperPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA, this::buildPaintBucketPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO, this::buildGradientPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO, this::buildTextPanel);
        toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS, this::buildShapesPanel);
toolPanelBuilders.put(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM, this::buildZoomTypePanel);
    }


    public record ToolItem(String commandKey, String iconKey, String tooltip) {

        @Override
        public String toString() {
            return tooltip;
        }

    } // --- Fin del record ToolItem ---


    private static Color clr(String key, int r, int g, int b) {
        Color c = UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    public EditorComponentBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(clr("Panel.background", 45, 45, 50));

        toolOptionsMap = new HashMap<>();

        // ===== FILA 1: izquierda (combo + spinners) + derecha (layer selection) =====
        JPanel row1 = new JPanel(new BorderLayout(0, 0));
        row1.setBackground(getBackground());

        leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPart.setBackground(getBackground());

        comboModel = new DefaultComboBoxModel<>();
        toolCombo = new JComboBox<>(comboModel);
        toolCombo.setPreferredSize(new Dimension(180, 22));
        toolCombo.setMinimumSize(new Dimension(180, 22));
        toolCombo.setMaximumSize(new Dimension(180, 22));
        toolCombo.setRenderer(new ToolListRenderer());
        toolCombo.addActionListener(e -> {
            ToolItem selected = (ToolItem) toolCombo.getSelectedItem();
            if (selected != null) {
                showOptionsFor(selected.commandKey());
            }
        });
        leftPart.add(Box.createHorizontalStrut(8));
        leftPart.add(toolCombo);

        leftPart.add(Box.createHorizontalStrut(4));

        labelX = createDimLabel("X:");
        spinnerX = createDimsSpinner();
        labelY = createDimLabel("Y:");
        spinnerY = createDimsSpinner();
        labelW = createDimLabel("W:");
        spinnerW = createDimsSpinner();
        labelH = createDimLabel("H:");
        spinnerH = createDimsSpinner();
        labelAngle = createDimLabel("\u00C1ngulo:");
        spinnerAngle = createAngleSpinner();

        // Guardar componentes del modo herramienta
        leftPartToolComponents = new java.util.ArrayList<>();
        for (Component c : leftPart.getComponents()) {
            leftPartToolComponents.add(c);
        }

        // Panel home para la fila 1 (Lienzo / Control de la Imagen)
        leftPartHome = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPartHome.setBackground(getBackground());
        leftPartHome.setVisible(false);
        buildHomeRow1();
        leftPart.add(leftPartHome);

        row1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        row1Right.setBackground(getBackground());

        row1.add(leftPart, BorderLayout.CENTER);
        row1.add(row1Right, BorderLayout.EAST);

        // ===== FILA 2: tool controls (Parte B) =====
        cardLayout = new CardLayout();
        toolControlsPanel = new JPanel(cardLayout);
        toolControlsPanel.setBackground(getBackground());
        // Margen izquierdo para que no empiece encima del panel de botones
        toolControlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        // Altura mínima para que la fila 2 no colapse
        toolControlsPanel.setPreferredSize(new Dimension(0, 24));

        add(row1);
        add(toolControlsPanel);
    } // --- Fin del constructor EditorComponentBar ---


    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        repaint();
    } // --- Fin del metodo setIconUtils ---


    public JPanel getToolControlsPanel() {
        return toolControlsPanel;
    } // --- Fin del metodo getToolControlsPanel ---


    public JComboBox<ToolItem> getToolCombo() {
        return toolCombo;
    } // --- Fin del metodo getToolCombo ---


    public void setUiDefinitionService(UIDefinitionService service) {
        this.uiDefinitionService = service;
        reloadTools();
    } // --- Fin del metodo setUiDefinitionService ---


    public void setCanvasController(CanvasController cc) {
        this.canvasController = cc;
    } // --- Fin del metodo setCanvasController ---

    public CanvasController getCanvasController() {
        return canvasController;
    } // --- Fin del metodo getCanvasController ---

    public JSpinner getSpinnerX() { return spinnerX; }
    public JSpinner getSpinnerY() { return spinnerY; }
    public JSpinner getSpinnerW() { return spinnerW; }
    public JSpinner getSpinnerH() { return spinnerH; }
    public JSpinner getSpinnerAngle() { return spinnerAngle; }
    public boolean isAutoSelect()  { return chkAutoSelect.isSelected(); }
    public boolean isShowGizmo()   { return chkShowGizmo.isSelected(); }
    public boolean isKeepAspect()  { return chkKeepAspect.isSelected(); }
    public boolean isAutoZoom()    { return chkAutoZoom.isSelected(); }


    public void reloadTools() {
        comboModel.removeAllElements();
        if (uiDefinitionService == null) return;

        ToolbarDefinition tbDef = uiDefinitionService.getToolbarDefinition("editoravanzado");
        if (tbDef == null) return;

        for (ToolbarComponentDefinition comp : tbDef.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
            	
                if (AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE.equals(btnDef.comandoCanonico())) continue;
//            	System.out.println(btnDef.comandoCanonico());
                
            	comboModel.addElement(new ToolItem(
                        btnDef.comandoCanonico(),
                        btnDef.claveIcono(),
                        btnDef.textoTooltip()));
            }
        }

        buildRow1RightSide();
        buildHomeRow1();
        buildAllToolPanels();

        if (comboModel.getSize() > 0) {
            toolCombo.setSelectedIndex(0);
        }
    } // --- Fin del metodo reloadTools ---


    public void selectToolByCommand(String commandKey) {
        for (int i = 0; i < comboModel.getSize(); i++) {
            ToolItem item = comboModel.getElementAt(i);
            if (item.commandKey().equals(commandKey)) {
                toolCombo.setSelectedIndex(i);
                return;
            }
        }
    } // --- Fin del metodo selectToolByCommand ---


    private void buildRow1RightSide() {
        row1Right.removeAll();
        if (uiDefinitionService == null) return;
        Color bg = getBackground();
        // Separador
        row1Right.add(new JSeparator(SwingConstants.VERTICAL));
        // Checkboxes
        chkAutoSelect.setSelected(true);
        chkShowGizmo.setSelected(true);
        chkKeepAspect.setSelected(true);
        for (JCheckBox chk : new JCheckBox[]{chkAutoSelect, chkShowGizmo, chkKeepAspect, chkAutoZoom}) {
            chk.setBackground(bg);
            chk.setForeground(fgToolbar);
            chk.setFont(chk.getFont().deriveFont(10f));
            chk.setFocusPainted(false);
            chk.setOpaque(false);
            row1Right.add(chk);
        }
        chkShowGizmo.addItemListener(e -> {
            AdvanceEditPanel aep2 = findAdvanceEditPanel();
            if (aep2 != null && aep2.getCanvas() != null) aep2.getCanvas().repaint();
        });
        // Separador
        row1Right.add(new JSeparator(SwingConstants.VERTICAL));
        // Combo selector Alinear
        JButton btnAlign = createIconComboButton(bg, true);
        if (btnAlign != null) row1Right.add(btnAlign);
        // Combo selector Distribuir
        JButton btnDistrib = createIconComboButton(bg, false);
        if (btnDistrib != null) row1Right.add(btnDistrib);
        row1Right.revalidate();
        row1Right.repaint();
    } // --- Fin del metodo buildRow1RightSide ---


    private JButton createIconComboButton(Color bg, boolean isAlign) {
        if (iconUtils == null) return null;
        String[][] options = isAlign
            ? new String[][]{
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_SUPERIOR,   "70403-align-borde-superior.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_VERTICAL,  "70402-align-centro-vertical.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_INFERIOR,   "70401-align-borde-inferior.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_IZQUIERDO,  "70404-align-borde-izquierdo.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_HORIZONTAL,"70405-align-centro-horizontal.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_DERECHO,    "70406-align-borde-derecho.png"}}
            : new String[][]{
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_TOP_BORDER,      "70503-distribute-top-border.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_VERTICAL, "70502-distribute-center-vertical.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_BOTTOM_BORDER,   "70501-distribute-bottom-border.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_LEFT_BORDER,     "70504-distribute-left-border.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_HORIZONTAL,"70505-distribute-center-horizontal.png"},
                {AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_RIGHT_BORDER,    "70506-distribute-right-border.png"}};
        Consumer<String> executor = isAlign ? this::executeAlignFromCommand : this::executeDistributeFromCommand;
        JButton btn = new JButton();
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(9f));
        btn.setText("\u25BC");
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        // Icono inicial
        ImageIcon initIcon = iconUtils.getScaledIcon(options[0][1], 14, 14);
        if (initIcon != null) btn.setIcon(initIcon);
        btn.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            popup.setLayout(new GridLayout(2, 3, 1, 1));
            popup.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            for (String[] opt : options) {
                String cmd = opt[0];
                String iconKey = opt[1];
                JLabel item = new JLabel();
                item.setHorizontalAlignment(SwingConstants.CENTER);
                item.setPreferredSize(new Dimension(28, 28));
                item.setOpaque(true);
                item.setBackground(bg);
                ImageIcon itemIcon = iconUtils.getScaledIcon(iconKey, 20, 20);
                if (itemIcon != null) item.setIcon(itemIcon);
                item.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent ev) {
                        executor.accept(cmd);
                        ImageIcon newIcon = iconUtils.getScaledIcon(iconKey, 14, 14);
                        if (newIcon != null) btn.setIcon(newIcon);
                        popup.setVisible(false);
                    }
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent ev) {
                        item.setBackground(item.getBackground().darker());
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent ev) {
                        item.setBackground(bg);
                    }
                });
                popup.add(item);
            }
            popup.show(btn, 0, btn.getHeight());
        });
        return btn;
    } // --- Fin del metodo createIconComboButton ---


    private void executeAlign(int hPos, int vPos) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;
        CanvasPanel canvas = aep.getCanvas();
        if (canvas == null) return;
        CanvasModel cm = canvas.getCanvasModel();
        if (cm == null) return;
        LayerModel lm = canvas.getLayerModel();
        if (lm == null) return;
        int cw = cm.getWidth();
        int ch = cm.getHeight();
        Layer layer = lm.getActiveLayer();
        if (layer == null) return;
        Rectangle b = layer.getBounds();
        int nx = b.x;
        int ny = b.y;
        if (hPos == 0) nx = 0;
        else if (hPos == 1) nx = (cw - b.width) / 2;
        else if (hPos == 2) nx = cw - b.width;
        if (vPos == 0) ny = 0;
        else if (vPos == 1) ny = (ch - b.height) / 2;
        else if (vPos == 2) ny = ch - b.height;
        layer.setBounds(new Rectangle(nx, ny, b.width, b.height));
        canvas.repaint();
    } // --- Fin del metodo executeAlign ---


    private void executeDistribute(int hPos, int vPos) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;
        CanvasPanel canvas = aep.getCanvas();
        if (canvas == null) return;
        CanvasModel cm = canvas.getCanvasModel();
        if (cm == null) return;
        LayerModel lm = canvas.getLayerModel();
        if (lm == null) return;
        List<Layer> layers = new ArrayList<>();
        for (Layer l : lm.getLayers()) {
            if (l.isVisible() && !l.isLocked()) layers.add(l);
        }
        if (layers.size() < 2) return;
        int cw = cm.getWidth();
        int ch = cm.getHeight();
        if (hPos == 1 || vPos == 1) {
            // Center along the specified axis
            int refX = hPos == 1 ? cw / 2 : 0;
            int refY = vPos == 1 ? ch / 2 : 0;
            for (Layer layer : layers) {
                Rectangle b = layer.getBounds();
                int nx = b.x;
                int ny = b.y;
                if (hPos == 1) nx = refX - b.width / 2;
                if (vPos == 1) ny = refY - b.height / 2;
                layer.setBounds(new Rectangle(nx, ny, b.width, b.height));
            }
        } else {
            // Distribute evenly across canvas width/height
            distributeEvenly(layers, cw, ch, hPos == 0, vPos == 0);
        }
        canvas.repaint();
    } // --- Fin del metodo executeDistribute ---


    private void distributeEvenly(List<Layer> layers, int cw, int ch, boolean hDist, boolean vDist) {
        int n = layers.size();
        if (n < 2) return;
        if (hDist) {
            layers.sort((a, b) -> Integer.compare(a.getBounds().x, b.getBounds().x));
            int totalW = layers.stream().mapToInt(l -> l.getBounds().width).sum();
            int gap = (cw - totalW) / (n - 1);
            int cx = 0;
            for (Layer layer : layers) {
                Rectangle b = layer.getBounds();
                layer.setBounds(new Rectangle(cx, b.y, b.width, b.height));
                cx += b.width + gap;
            }
        }
        if (vDist) {
            layers.sort((a, b) -> Integer.compare(a.getBounds().y, b.getBounds().y));
            int totalH = layers.stream().mapToInt(l -> l.getBounds().height).sum();
            int gap = (ch - totalH) / (n - 1);
            int cy = 0;
            for (Layer layer : layers) {
                Rectangle b = layer.getBounds();
                layer.setBounds(new Rectangle(b.x, cy, b.width, b.height));
                cy += b.height + gap;
            }
        }
    } // --- Fin del metodo distributeEvenly ---


    private void executeAlignFromCommand(String cmd) {
        int hPos = 1, vPos = 1; // default center
        if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_SUPERIOR.equals(cmd))       { vPos = 0; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_VERTICAL.equals(cmd)) { vPos = 1; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_INFERIOR.equals(cmd))  { vPos = 2; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_IZQUIERDO.equals(cmd)) { hPos = 0; vPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_CENTRO_HORIZONTAL.equals(cmd)){ hPos = 1; vPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_ALIGN_BORDE_DERECHO.equals(cmd))   { hPos = 2; vPos = 1; }
        executeAlign(hPos, vPos);
    } // --- Fin del metodo executeAlignFromCommand ---


    private void executeDistributeFromCommand(String cmd) {
        int hPos = 1, vPos = 1;
        if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_TOP_BORDER.equals(cmd))        { vPos = 0; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_VERTICAL.equals(cmd)) { vPos = 1; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_BOTTOM_BORDER.equals(cmd))   { vPos = 2; hPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_LEFT_BORDER.equals(cmd))     { hPos = 0; vPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_CENTER_HORIZONTAL.equals(cmd)){ hPos = 1; vPos = 1; }
        else if (AppActionCommands.CMD_PREVIEW_RENDER_DISTRIBUTE_RIGHT_BORDER.equals(cmd))    { hPos = 2; vPos = 1; }
        executeDistribute(hPos, vPos);
    } // --- Fin del metodo executeDistributeFromCommand ---



    public void buildAllToolPanels() {
        toolControlsPanel.removeAll();
        toolOptionsMap.clear();
        Color bg = getBackground();

        for (int i = 0; i < comboModel.getSize(); i++) {
            ToolItem item = comboModel.getElementAt(i);
            JPanel panel = buildPanelForTool(item.commandKey(), bg);
            toolControlsPanel.add(panel, item.commandKey());
            toolOptionsMap.put(item.commandKey(), panel);
        }

        buildHomePanels();

        toolControlsPanel.revalidate();
        toolControlsPanel.repaint();
    } // --- Fin del metodo buildAllToolPanels ---


    public void showOptionsFor(String commandKey) {
        if (toolOptionsMap.containsKey(commandKey)) {
            currentToolCommandKey = commandKey;
            cardLayout.show(toolControlsPanel, commandKey);
        }
    } // --- Fin del metodo showOptionsFor ---


    public void showEditToolPanel(Layer layer) {
        String editKey = AppActionCommands.CMD_ADVANCED_EDITOR_EDICION;
        String textKey = AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO;
        String shapeKey = AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS;
        if (layer instanceof ImageLayer il && il.getType() == ImageLayer.LayerType.SHAPE
                && toolOptionsMap.containsKey(shapeKey)) {
            loadShapeProperties(il);
            cardLayout.show(toolControlsPanel, shapeKey);
        } else if (layer instanceof TextLayer && toolOptionsMap.containsKey(textKey)) {
            cardLayout.show(toolControlsPanel, textKey);
        } else if (toolOptionsMap.containsKey(editKey)) {
            cardLayout.show(toolControlsPanel, editKey);
        }
    } // --- Fin del metodo showEditToolPanel ---


    private JPanel buildPanelForTool(String commandKey, Color bg) {
        PanelBuilder builder = toolPanelBuilders.get(commandKey);
        return builder != null ? builder.build(bg) : newEmptyPanel(bg);
    } // --- Fin del metodo buildPanelForTool ---


    // ===================== PANELES PARTE B =====================


    private JPanel buildTransformPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup modeGroup = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadotransform", bg, modeGroup);
        p.add(createSubSeparator(bg));
        ButtonGroup targetGroup = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadolayerselection", bg, targetGroup);

        JCheckBox keepAspect = new JCheckBox("Mantener proporción");
        keepAspect.setBackground(bg);
        keepAspect.setForeground(fgToolbar);
        keepAspect.setSelected(chkKeepAspect.isSelected());
        keepAspect.addItemListener(e -> chkKeepAspect.setSelected(keepAspect.isSelected()));
        p.add(Box.createHorizontalStrut(4));
        p.add(keepAspect);

        return p;
    } // --- Fin del metodo buildTransformPanel ---


    private JPanel buildEditPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        p.setBackground(bg);
        JLabel capaLabel = new JLabel("Capa:");
        capaLabel.setForeground(fgToolbar);
        p.add(capaLabel);
        editLayerNameLabel = new JLabel("\u2014");
        editLayerNameLabel.setForeground(fgToolbar);
        editLayerNameLabel.setPreferredSize(new Dimension(70, 20));
        p.add(editLayerNameLabel);
        p.add(Box.createHorizontalStrut(4));
        JLabel lbX = new JLabel("X:");
        lbX.setForeground(fgToolbar);
        p.add(lbX);
        p.add(spinnerX);
        JLabel lbY = new JLabel("Y:");
        lbY.setForeground(fgToolbar);
        p.add(lbY);
        p.add(spinnerY);
        JLabel lbW = new JLabel("W:");
        lbW.setForeground(fgToolbar);
        p.add(lbW);
        p.add(spinnerW);
        JLabel lbH = new JLabel("H:");
        lbH.setForeground(fgToolbar);
        p.add(lbH);
        p.add(spinnerH);
        p.add(Box.createHorizontalStrut(4));
        JLabel lbAngle = new JLabel("\u00C1ngulo:");
        lbAngle.setForeground(fgToolbar);
        p.add(lbAngle);
        p.add(spinnerAngle);
        return p;
    } // --- Fin del metodo buildEditPanel ---


    private JPanel buildFeatherPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Desvanecer:");
        lb.setForeground(fgToolbar);
        p.add(lb);
        JSlider slider = new JSlider(0, 50, 0);
        slider.setBackground(bg);
        slider.setPreferredSize(new Dimension(80, 20));
        p.add(slider);
        JLabel val = new JLabel("0");
        val.setForeground(fgToolbar);
        p.add(val);
        slider.addChangeListener(e -> val.setText(String.valueOf(slider.getValue())));
        featherSlider = slider;
        return p;
    } // --- Fin del metodo buildFeatherPanel ---


    private JPanel buildWandPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Tolerancia:");
        lb.setForeground(fgToolbar);
        p.add(lb);
        JSpinner tol = new JSpinner(new SpinnerNumberModel(32, 0, 255, 1));
        tol.setPreferredSize(new Dimension(50, 20));
        p.add(tol);
        JCheckBox contiguo = new JCheckBox("Contiguo");
        contiguo.setBackground(bg);
        contiguo.setForeground(fgToolbar);
        contiguo.setSelected(true);
        p.add(Box.createHorizontalStrut(4));
        p.add(contiguo);
        wandToleranceSpinner = tol;
        wandContiguousChk = contiguo;
        return p;
    } // --- Fin del metodo buildWandPanel ---


    private JPanel buildCropPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadocrop", bg, group);
        JCheckBox mantener = new JCheckBox("Mantener original");
        mantener.setBackground(bg);
        mantener.setForeground(fgToolbar);
        p.add(Box.createHorizontalStrut(4));
        p.add(mantener);
        cropKeepOriginalChk = mantener;
        return p;
    } // --- Fin del metodo buildCropPanel ---


    private JPanel buildEyedropperPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        JLabel lb = new JLabel("Tamaño de muestra:");
        lb.setForeground(fgToolbar);
        p.add(lb);
        ButtonGroup group = new ButtonGroup();
        for (String s : new String[]{"1×1", "3×3", "5×5"}) {
            JRadioButton rb = new JRadioButton(s);
            rb.setBackground(bg);
            rb.setForeground(fgToolbar);
            group.add(rb);
            p.add(rb);
            rb.addActionListener(e -> {
                String sel = rb.getText();
                eyedropperSampleSize = "3×3".equals(sel) ? 3 : "5×5".equals(sel) ? 5 : 1;
            });
        }
        group.getElements().nextElement().setSelected(true);

        p.add(Box.createHorizontalStrut(8));
        JLabel lbM = new JLabel("Capturado:");
        lbM.setForeground(fgToolbar);
        p.add(lbM);
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setPreferredSize(new Dimension(20, 20));
        swatch.setBackground(eyedropperColor);
        swatch.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        p.add(swatch);
        JLabel rgb = new JLabel("RGB(255,255,255)");
        rgb.setForeground(fgToolbar);
        p.add(rgb);
        eyedropperSwatch = swatch;
        eyedropperRgbLabel = rgb;

        return p;
    } // --- Fin del metodo buildEyedropperPanel ---


    private JPanel buildPaintBucketPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        // Color swatch button
        paintBucketSwatch = createColorSwatch(new Color(255, 0, 0), "Color de relleno", bg, c -> paintBucketColor = c);
        p.add(paintBucketSwatch);
        p.add(Box.createHorizontalStrut(6));
        // Tolerancia
        JLabel lb = new JLabel("Tolerancia:");
        lb.setForeground(fgToolbar);
        p.add(lb);
        JSpinner tol = new JSpinner(new SpinnerNumberModel(32, 0, 255, 1));
        tol.setPreferredSize(new Dimension(50, 20));
        p.add(tol);
        p.add(Box.createHorizontalStrut(4));
        JCheckBox contiguo = new JCheckBox("Contiguo");
        contiguo.setBackground(bg);
        contiguo.setForeground(fgToolbar);
        contiguo.setSelected(true);
        p.add(contiguo);
        paintToleranceSpinner = tol;
        paintContiguousChk = contiguo;
        return p;
    } // --- Fin del metodo buildPaintBucketPanel ---


    private JPanel buildGradientPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);

        JButton colorStart = createColorSwatch(Color.RED, "Color inicial", bg,
                c -> gradientStartColor = c);
        JButton colorEnd = createColorSwatch(Color.BLUE, "Color final", bg,
                c -> gradientEndColor = c);
        gradientStartSwatch = colorStart;
        gradientEndSwatch = colorEnd;
        JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Lineal", "Radial", "Angular", "Reflejado", "Diamante"});
        typeCombo.setPreferredSize(new Dimension(90, 22));

        // Preview que lee colores y tipo en tiempo real
        JPanel preview = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                if (w > 0 && h > 0) {
                    Color c1 = colorStart.getBackground();
                    Color c2 = colorEnd.getBackground();
                    String tipo = (String) typeCombo.getSelectedItem();
                    if ("Radial".equals(tipo)) {
                        float r = Math.min(w, h) / 2f;
                        g2.setPaint(new java.awt.RadialGradientPaint(
                                w / 2f, h / 2f, r,
                                new float[]{0f, 1f},
                                new java.awt.Color[]{c1, c2}));
                    } else if ("Lineal".equals(tipo)) {
                        g2.setPaint(new java.awt.GradientPaint(0, 0, c1, w, h, c2));
                    } else if ("Angular".equals(tipo)) {
                        // Abanico cónico alrededor del centro del preview
                        int cx = w / 2, cy = h / 2;
                        double maxR = Math.hypot(cx, cy) + 4;
                        int n = 120;
                        double prevA = 0;
                        for (int i = 1; i <= n; i++) {
                            double a = 2 * Math.PI * i / n;
                            g2.setColor(lerpColor(c1, c2, (double) i / n));
                            java.awt.geom.Path2D.Double tri = new java.awt.geom.Path2D.Double();
                            tri.moveTo(cx, cy);
                            tri.lineTo(cx + maxR * Math.cos(prevA), cy + maxR * Math.sin(prevA));
                            tri.lineTo(cx + maxR * Math.cos(a), cy + maxR * Math.sin(a));
                            tri.closePath();
                            g2.fill(tri);
                            prevA = a;
                        }
                        g2.dispose();
                        return;
                    } else if ("Reflejado".equals(tipo)) {
                        // Espejo horizontal: c1 → c2 en la mitad izquierda y c2 → c1 en la derecha
                        int mid = w / 2;
                        g2.setPaint(new java.awt.GradientPaint(0, h / 2f, c1, mid, h / 2f, c2));
                        g2.fillRect(0, 0, mid, h);
                        g2.setPaint(new java.awt.GradientPaint(mid, h / 2f, c2, w, h / 2f, c1));
                        g2.fillRect(mid, 0, w - mid, h);
                        g2.dispose();
                        return;
                    } else if ("Diamante".equals(tipo)) {
                        float r = Math.max(w, h) * 0.7f;
                        g2.setPaint(new java.awt.RadialGradientPaint(
                                w / 2f, h / 2f, r,
                                new float[]{0f, 0.5f, 1f},
                                new java.awt.Color[]{c1, c2, c1}));
                    }
                    g2.fillRect(0, 0, w, h);
                }
                g2.dispose();
            }
        };
        preview.setPreferredSize(new Dimension(28, 20));
        preview.setBackground(bg);
        preview.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        gradientPreviewPanel = preview;

        // Forzar repintado del preview al cambiar colores o tipo
        colorStart.addActionListener(e -> preview.repaint());
        colorEnd.addActionListener(e -> preview.repaint());
        typeCombo.addActionListener(e -> preview.repaint());

        p.add(preview);
        p.add(Box.createHorizontalStrut(2));
        p.add(colorStart);
        p.add(Box.createHorizontalStrut(2));
        p.add(colorEnd);

        p.add(Box.createHorizontalStrut(4));
        p.add(typeCombo);

        p.add(Box.createHorizontalStrut(4));
        JLabel lb = new JLabel("Opacidad:");
        lb.setForeground(fgToolbar);
        p.add(lb);
        JSlider op = new JSlider(0, 100, 100);
        op.setBackground(bg);
        op.setPreferredSize(new Dimension(50, 20));
        p.add(op);

        gradientTypeCombo = typeCombo;
        gradientOpacitySlider = op;

        return p;
    } // --- Fin del metodo buildGradientPanel ---


    public void applyTextProperty(Consumer<modelo.editor.TextLayer> action) {
        if (canvasController == null) return;

        // 1. Sincronizar TextTool si es la herramienta activa
        if (canvasController.getActiveTool() instanceof TextTool tt) {
            tt.syncFromComponentBar();
            tt.syncInlineStyle();
        }

        // 2. Aplicar la propiedad directamente sobre la capa de texto activa si existe
        var layerModel = canvasController.getContext().layerModel();
        if (layerModel != null && layerModel.getActiveLayer() instanceof modelo.editor.TextLayer tl) {
            action.accept(tl);
            canvasController.getContext().canvasPanel().repaint();
        }

        // 3. Notificar al panel derecho para que se sincronice
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo applyTextProperty ---


    /**
     * Aplica una propiedad de texto convirtiendo previamente las capas auto-size
     * a caja fija, para que el justificado y el flujo en columnas sean visibles
     * incluso en texto creado con un solo clic.
     */
    public void applyTextPropertyFixedBox(Consumer<modelo.editor.TextLayer> action, float widthFactor, int minWidth) {
        applyTextProperty(tl -> {
            if (tl.isAutoSize()) {
                tl.setAutoSize(false);
                Rectangle b = tl.getBounds();
                int w = Math.max(Math.round(b.width * widthFactor), minWidth);
                tl.setBounds(new Rectangle(b.x, b.y, w, b.height));
            }
            action.accept(tl);
        });
    } // --- Fin del metodo applyTextPropertyFixedBox ---


    /**
     * Re-renderiza la capa de forma activa con los parámetros actuales del panel
     * de formas (tipo, relleno, borde, grosor). No hace nada si la capa activa no
     * es una forma.
     */
    public void applyShapeProperty() {
        if (canvasController == null) return;

        var layerModel = canvasController.getContext().layerModel();
        if (layerModel != null && layerModel.getActiveLayer() instanceof ImageLayer il
                && il.getType() == ImageLayer.LayerType.SHAPE) {
            reRenderShape(il);
            updateDimensionSpinners(il.getBounds().x, il.getBounds().y,
                    il.getBounds().width, il.getBounds().height);
            canvasController.getContext().canvasPanel().repaint();
        }

        if (onShapeChange != null) onShapeChange.run();
    } // --- Fin del metodo applyShapeProperty ---


    /**
     * Renderiza de nuevo la forma de la capa en su tamaño de render original
     * (shapeRenderW/H), recorta al contenido y actualiza imagen y bounds.
     */
    private void reRenderShape(ImageLayer il) {
        String type = il.getShapeType() != null ? il.getShapeType() : "rect";
        int iw = Math.max(1, il.getShapeRenderW() > 0 ? il.getShapeRenderW() : il.getBounds().width);
        int ih = Math.max(1, il.getShapeRenderH() > 0 ? il.getShapeRenderH() : il.getBounds().height);
        Color fill = getShapeFillColor();
        Color stroke = getShapeStrokeColor();
        float width = getShapeStrokeWidth();

        BufferedImage img = ShapeTool.renderShape(iw, ih, type, fill, stroke, width);
        Rectangle bounds = new Rectangle(il.getBounds().x, il.getBounds().y, img.getWidth(), img.getHeight());
        Rectangle content = ShapeTool.contentBounds(img);
        if (content != null && (content.x != 0 || content.y != 0
                || content.width != img.getWidth() || content.height != img.getHeight())) {
            BufferedImage trimmed = new BufferedImage(content.width, content.height,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = trimmed.createGraphics();
            tg.drawImage(img, -content.x, -content.y, null);
            tg.dispose();
            img = trimmed;
            bounds = new Rectangle(il.getBounds().x + content.x, il.getBounds().y + content.y,
                    content.width, content.height);
        }

        il.setImage(img);
        il.setBounds(bounds);
        il.setShapeType(type);
        il.setShapeFill(fill);
        il.setShapeStroke(stroke);
        il.setShapeStrokeWidth(width);
        il.setShapeRenderW(iw);
        il.setShapeRenderH(ih);
    } // --- Fin del metodo reRenderShape ---


    /**
     * Sincroniza el panel de formas con los parámetros guardados en una capa
     * de forma seleccionada (tipo, relleno, borde, grosor).
     */
    public void loadShapeProperties(ImageLayer il) {
        if (il == null) return;
        if (il.getShapeType() != null) selectedShapeType = il.getShapeType();
        if (il.getShapeFill() != null) shapeFillColor = il.getShapeFill();
        if (il.getShapeStroke() != null) shapeStrokeColor = il.getShapeStroke();
        if (shapeStrokeWidthSpinner != null) {
            shapeStrokeWidthSpinner.setValue((int) Math.round(il.getShapeStrokeWidth()));
        }
        if (shapeFillSwatch != null) shapeFillSwatch.setBackground(shapeFillColor);
        if (shapeStrokeSwatch != null) shapeStrokeSwatch.setBackground(shapeStrokeColor);
        JToggleButton btn = shapeTypeButtons.get(il.getShapeType());
        if (btn != null) btn.setSelected(true);
    } // --- Fin del metodo loadShapeProperties ---


    private JPanel buildTextPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);

        ButtonGroup alignGroup = new ButtonGroup();
        ButtonGroup flowGroup = new ButtonGroup();
        ButtonGroup orientationGroup = new ButtonGroup();

        ToolbarDefinition def = getSubDef("editoravanzadotexto");
        if (def != null) {
            for (ToolbarComponentDefinition comp : def.componentes()) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    String iconKey = btnDef.claveIcono();

                    // 1. Icono informativo + combo de fuentes (con FontListCellRenderer)
                    if (AppActionCommands.CMD_EDITOR_TEXTO_FUENTE.equals(cmd)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        String[] fonts = java.awt.GraphicsEnvironment
                                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        textFontCombo = new JComboBox<>(fonts);
                        textFontCombo.setPreferredSize(new Dimension(140, 22));
                        textFontCombo.setRenderer(new vista.renderers.FontListCellRenderer());
                        textFontCombo.addActionListener(e -> {
                            String sel = (String) textFontCombo.getSelectedItem();
                            if (sel != null) {
                                applyTextProperty(tl -> {
                                    Font curr = tl.getFont();
                                    tl.setFont(new Font(sel, curr.getStyle(), curr.getSize()));
                                });
                            }
                        });
                        p.add(textFontCombo);

                    // 2. Icono informativo + combo de tamaños
                    } else if (AppActionCommands.CMD_EDITOR_TEXTO_TAMANO.equals(cmd)) {
                        JLabel iconLb = createIconLabel(btnDef, bg);
                        p.add(iconLb);
                        Integer[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
                        textSizeCombo = new JComboBox<>(sizes);
                        textSizeCombo.setSelectedItem(24);
                        textSizeCombo.setPreferredSize(new Dimension(55, 22));
                        textSizeCombo.addActionListener(e -> {
                            Integer sel = (Integer) textSizeCombo.getSelectedItem();
                            if (sel != null) {
                                applyTextProperty(tl -> {
                                    Font curr = tl.getFont();
                                    tl.setFont(curr.deriveFont((float) sel));
                                });
                            }
                        });
                        p.add(textSizeCombo);

                    // 3. Botones de texto (estilos, alineación, flujo, orientación)
                    } else {
                        String tooltip = btnDef.textoTooltip();
                        ButtonGroup targetGroup = null;
                        if (AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_LEFT.equals(cmd)
                                || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_CENTER.equals(cmd)
                                || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_RIGHT.equals(cmd)
                                || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_JUSTIFY.equals(cmd)) {
                            targetGroup = alignGroup;
                        } else if (AppActionCommands.CMD_EDITOR_TEXTO_FLOW_ROWS.equals(cmd)
                                || AppActionCommands.CMD_EDITOR_TEXTO_FLOW_COLUMNS.equals(cmd)) {
                            targetGroup = flowGroup;
                        } else if (AppActionCommands.CMD_EDITOR_TEXTO_HORIZONTAL.equals(cmd)
                                || AppActionCommands.CMD_EDITOR_TEXTO_VERTICAL.equals(cmd)) {
                            targetGroup = orientationGroup;
                        }

                        JToggleButton tb = createTextToggleButton(cmd, iconKey, tooltip, bg, targetGroup);
                        p.add(tb);
                    }

                } else if (comp instanceof SeparatorDefinition) {
                    p.add(createSubSeparator(bg));
                }
            }
        }

        // Color picker de texto
        p.add(createSubSeparator(bg));
        textColorBtn = createColorSwatch(Color.BLACK, "Color del texto", bg, nuevo -> {
            applyTextProperty(tl -> tl.setColor(nuevo));
        });
        p.add(textColorBtn);

        // Ángulo de rotación (no destructiva)
        p.add(createSubSeparator(bg));
        JLabel lbAngle = new JLabel("\u00C1ngulo:");
        lbAngle.setForeground(fgToolbar);
        p.add(lbAngle);
        p.add(spinnerAngle);

        // Botones ✓ (aceptar) y ✗ (cancelar) para edición inline
        p.add(createSubSeparator(bg));
        JButton btnAccept = new JButton();
        btnAccept.setToolTipText("Aceptar edición");
        btnAccept.setFocusPainted(false);
        btnAccept.setPreferredSize(new Dimension(24, 24));
        btnAccept.setBackground(bg);
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon("82001-aceptar.png", 18, 18);
            if (icon != null) btnAccept.setIcon(icon);
        }
        btnAccept.addActionListener(e -> {
            if (canvasController != null && canvasController.getActiveTool() instanceof TextTool tt) {
                tt.commitInlineText();
            }
        });
        p.add(btnAccept);

        JButton btnCancel = new JButton();
        btnCancel.setToolTipText("Cancelar edición");
        btnCancel.setFocusPainted(false);
        btnCancel.setPreferredSize(new Dimension(24, 24));
        btnCancel.setBackground(bg);
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon("82002-cancelar.png", 18, 18);
            if (icon != null) btnCancel.setIcon(icon);
        }
        btnCancel.addActionListener(e -> {
            if (canvasController != null && canvasController.getActiveTool() instanceof TextTool tt) {
                tt.cancelInlineEdit();
            }
        });
        p.add(btnCancel);

        return p;
    } // --- Fin del metodo buildTextPanel ---


    private JToggleButton createTextToggleButton(String cmd, String iconKey,
            String tooltip, Color bg, ButtonGroup group) {
        JToggleButton btn = new JToggleButton();
        btn.setActionCommand(cmd);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(iconKey, 18, 18);
            if (icon != null) btn.setIcon(icon);
        }
        if (group != null) {
            group.add(btn);
        }

        // Mapear listeners según la clave del icono o comando
        if ("80901-bold-text.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_NEGRITA.equals(cmd)) {
            textBoldBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> {
                Font curr = tl.getFont();
                int style = btn.isSelected() ? (curr.getStyle() | Font.BOLD) : (curr.getStyle() & ~Font.BOLD);
                tl.setFont(curr.deriveFont(style));
            }));
        } else if ("80902-italic-text.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_CURSIVA.equals(cmd)) {
            textItalicBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> {
                Font curr = tl.getFont();
                int style = btn.isSelected() ? (curr.getStyle() | Font.ITALIC) : (curr.getStyle() & ~Font.ITALIC);
                tl.setFont(curr.deriveFont(style));
            }));
        } else if ("80903-underline-text.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_SUBRAYADO.equals(cmd)) {
            textUnderlineBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setUnderline(btn.isSelected())));
        } else if ("80904-tachado.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_TACHADO.equals(cmd)) {
            textStrikethroughBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setStrikethrough(btn.isSelected())));
        } else if ("80906-align-left.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_LEFT.equals(cmd)) {
            textAlignLeftBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setAlignment(SwingConstants.LEFT)));
        } else if ("80907-align-center.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_CENTER.equals(cmd)) {
            textAlignCenterBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setAlignment(SwingConstants.CENTER)));
        } else if ("80908-align-right.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_RIGHT.equals(cmd)) {
            textAlignRightBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setAlignment(SwingConstants.RIGHT)));
        } else if ("80909-justified.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_ALIGN_JUSTIFY.equals(cmd)) {
            textJustifiedBtn = btn;
            btn.addActionListener(e -> applyTextPropertyFixedBox(
                    tl -> tl.setAlignment(modelo.editor.TextLayer.ALIGN_JUSTIFY), 1.5f, 200));
        } else if ("80910-text-flow-rows.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_FLOW_ROWS.equals(cmd)) {
            textFlowRowsBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setFlowColumns(false)));
        } else if ("80911-text-flow-columns.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_FLOW_COLUMNS.equals(cmd)) {
            textFlowColumnsBtn = btn;
            btn.addActionListener(e -> applyTextPropertyFixedBox(
                    tl -> tl.setFlowColumns(true), 2f, 240));
        } else if ("80912-horizontal-text.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_HORIZONTAL.equals(cmd)) {
            textHorizontalBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setVertical(false)));
        } else if ("80913-vertical-text.png".equals(iconKey) || AppActionCommands.CMD_EDITOR_TEXTO_VERTICAL.equals(cmd)) {
            textVerticalBtn = btn;
            btn.addActionListener(e -> applyTextProperty(tl -> tl.setVertical(true)));
        }

        return btn;
    } // --- Fin del metodo createTextToggleButton ---


    // ==================== Sincronización bidireccional texto ====================


    public String getTextFontFamily() {
        return textFontCombo != null ? (String) textFontCombo.getSelectedItem() : null;
    } // --- Fin del metodo getTextFontFamily ---


    public void setTextFontFamily(String ff) {
        if (textFontCombo != null) textFontCombo.setSelectedItem(ff);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextFontFamily ---


    public int getTextFontSize() {
        if (textSizeCombo != null && textSizeCombo.getSelectedItem() != null) {
            return (Integer) textSizeCombo.getSelectedItem();
        }
        return 0;
    } // --- Fin del metodo getTextFontSize ---


    public void setTextFontSize(int sz) {
        if (textSizeCombo != null) textSizeCombo.setSelectedItem(sz);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextFontSize ---


    public Boolean isTextBold() {
        return textBoldBtn != null ? textBoldBtn.isSelected() : null;
    } // --- Fin del metodo isTextBold ---


    public void setTextBold(boolean b) {
        if (textBoldBtn != null) textBoldBtn.setSelected(b);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextBold ---


    public Boolean isTextItalic() {
        return textItalicBtn != null ? textItalicBtn.isSelected() : null;
    } // --- Fin del metodo isTextItalic ---


    public void setTextItalic(boolean i) {
        if (textItalicBtn != null) textItalicBtn.setSelected(i);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextItalic ---


    public Color getTextColor() {
        return textColorBtn != null ? textColorBtn.getBackground() : null;
    } // --- Fin del metodo getTextColor ---


    public void setTextColor(Color c) {
        if (textColorBtn != null) textColorBtn.setBackground(c);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextColor ---


    public int getTextAlignment() {
        if (textAlignLeftBtn != null && textAlignLeftBtn.isSelected())  return javax.swing.SwingConstants.LEFT;
        if (textAlignCenterBtn != null && textAlignCenterBtn.isSelected()) return javax.swing.SwingConstants.CENTER;
        if (textAlignRightBtn != null && textAlignRightBtn.isSelected()) return javax.swing.SwingConstants.RIGHT;
        if (textJustifiedBtn != null && textJustifiedBtn.isSelected()) return modelo.editor.TextLayer.ALIGN_JUSTIFY;
        return -1;
    } // --- Fin del metodo getTextAlignment ---


    public void setTextAlignment(int align) {
        if (textAlignLeftBtn != null)   textAlignLeftBtn.setSelected(align == javax.swing.SwingConstants.LEFT);
        if (textAlignCenterBtn != null) textAlignCenterBtn.setSelected(align == javax.swing.SwingConstants.CENTER);
        if (textAlignRightBtn != null)  textAlignRightBtn.setSelected(align == javax.swing.SwingConstants.RIGHT);
        if (textJustifiedBtn != null)   textJustifiedBtn.setSelected(align == modelo.editor.TextLayer.ALIGN_JUSTIFY);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextAlignment ---


    public Boolean isTextVertical() {
        return textVerticalBtn != null ? textVerticalBtn.isSelected() : null;
    } // --- Fin del metodo isTextVertical ---


    public void setTextVertical(boolean v) {
        if (textHorizontalBtn != null) textHorizontalBtn.setSelected(!v);
        if (textVerticalBtn != null)   textVerticalBtn.setSelected(v);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextVertical ---


    public Boolean isTextUnderline() {
        return textUnderlineBtn != null ? textUnderlineBtn.isSelected() : null;
    } // --- Fin del metodo isTextUnderline ---


    public void setTextUnderline(boolean u) {
        if (textUnderlineBtn != null) textUnderlineBtn.setSelected(u);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextUnderline ---


    public Boolean isTextStrikethrough() {
        return textStrikethroughBtn != null ? textStrikethroughBtn.isSelected() : null;
    } // --- Fin del metodo isTextStrikethrough ---


    public void setTextStrikethrough(boolean s) {
        if (textStrikethroughBtn != null) textStrikethroughBtn.setSelected(s);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextStrikethrough ---


    public Boolean isTextFlowColumns() {
        return textFlowColumnsBtn != null ? textFlowColumnsBtn.isSelected() : null;
    } // --- Fin del metodo isTextFlowColumns ---


    public void setTextFlowColumns(boolean fc) {
        if (textFlowRowsBtn != null)    textFlowRowsBtn.setSelected(!fc);
        if (textFlowColumnsBtn != null) textFlowColumnsBtn.setSelected(fc);
        if (onTextChange != null) onTextChange.run();
    } // --- Fin del metodo setTextFlowColumns ---


    // ==================== Opciones de herramientas (Parte B) ====================


    public int getFeatherAmount() {
        return featherSlider != null ? featherSlider.getValue() : 0;
    } // --- Fin del metodo getFeatherAmount ---


    public int getWandTolerance() {
        if (wandToleranceSpinner != null) {
            return ((Number) wandToleranceSpinner.getValue()).intValue();
        }
        return 32;
    } // --- Fin del metodo getWandTolerance ---


    public boolean isWandContiguous() {
        return wandContiguousChk == null || wandContiguousChk.isSelected();
    } // --- Fin del metodo isWandContiguous ---


    public boolean isCropKeepOriginal() {
        return cropKeepOriginalChk != null && cropKeepOriginalChk.isSelected();
    } // --- Fin del metodo isCropKeepOriginal ---


    public int getEyedropperSampleSize() {
        return eyedropperSampleSize;
    } // --- Fin del metodo getEyedropperSampleSize ---


    public Color getEyedropperColor() {
        return eyedropperColor != null ? eyedropperColor : Color.WHITE;
    } // --- Fin del metodo getEyedropperColor ---


    /**
     * Muestra el color capturado por el cuentagotas en el swatch y en el label RGB.
     */
    public void setEyedropperColor(Color c) {
        if (c == null) return;
        eyedropperColor = c;
        if (eyedropperSwatch != null) {
            eyedropperSwatch.setBackground(c);
        }
        if (eyedropperRgbLabel != null) {
            eyedropperRgbLabel.setText(String.format("RGB(%d,%d,%d)",
                    c.getRed(), c.getGreen(), c.getBlue()));
        }
    } // --- Fin del metodo setEyedropperColor ---


    public Color getPaintBucketColor() {
        return paintBucketColor != null ? paintBucketColor : Color.RED;
    } // --- Fin del metodo getPaintBucketColor ---


    public void setPaintBucketColor(Color c) {
        if (c == null) return;
        paintBucketColor = c;
        if (paintBucketSwatch != null) paintBucketSwatch.setBackground(c);
    } // --- Fin del metodo setPaintBucketColor ---


    public void setShapeFillColor(Color c) {
        if (c == null) return;
        shapeFillColor = c;
        if (shapeFillSwatch != null) shapeFillSwatch.setBackground(c);
    } // --- Fin del metodo setShapeFillColor ---


    public void setGradientStartColor(Color c) {
        if (c == null) return;
        gradientStartColor = c;
        if (gradientStartSwatch != null) gradientStartSwatch.setBackground(c);
        if (gradientPreviewPanel != null) gradientPreviewPanel.repaint();
    } // --- Fin del metodo setGradientStartColor ---


    public void setGradientEndColor(Color c) {
        if (c == null) return;
        gradientEndColor = c;
        if (gradientEndSwatch != null) gradientEndSwatch.setBackground(c);
        if (gradientPreviewPanel != null) gradientPreviewPanel.repaint();
    } // --- Fin del metodo setGradientEndColor ---


    public int getPaintBucketTolerance() {
        if (paintToleranceSpinner != null) {
            return ((Number) paintToleranceSpinner.getValue()).intValue();
        }
        return 32;
    } // --- Fin del metodo getPaintBucketTolerance ---


    public boolean isPaintBucketContiguous() {
        return paintContiguousChk == null || paintContiguousChk.isSelected();
    } // --- Fin del metodo isPaintBucketContiguous ---


    public Color getGradientStartColor() {
        return gradientStartColor != null ? gradientStartColor : Color.RED;
    } // --- Fin del metodo getGradientStartColor ---


    public Color getGradientEndColor() {
        return gradientEndColor != null ? gradientEndColor : Color.BLUE;
    } // --- Fin del metodo getGradientEndColor ---


    public String getGradientType() {
        return gradientTypeCombo != null
                ? (String) gradientTypeCombo.getSelectedItem() : "Lineal";
    } // --- Fin del metodo getGradientType ---


    public int getGradientOpacity() {
        return gradientOpacitySlider != null ? gradientOpacitySlider.getValue() : 100;
    } // --- Fin del metodo getGradientOpacity ---


    public Color getShapeFillColor() {
        return shapeFillColor != null ? shapeFillColor : new Color(200, 200, 200);
    } // --- Fin del metodo getShapeFillColor ---


    public Color getShapeStrokeColor() {
        return shapeStrokeColor != null ? shapeStrokeColor : Color.BLACK;
    } // --- Fin del metodo getShapeStrokeColor ---


    public int getShapeStrokeWidth() {
        if (shapeStrokeWidthSpinner != null) {
            return ((Number) shapeStrokeWidthSpinner.getValue()).intValue();
        }
        return 1;
    } // --- Fin del metodo getShapeStrokeWidth ---


    public String getSelectedShapeType() {
        return selectedShapeType != null ? selectedShapeType : "rect";
    } // --- Fin del metodo getSelectedShapeType ---


    public String getTransformMode() {
        return transformMode != null ? transformMode : "mover";
    } // --- Fin del metodo getTransformMode ---


    public String getTransformTarget() {
        return transformTarget != null ? transformTarget : "capa";
    } // --- Fin del metodo getTransformTarget ---


    public String getCropMode() {
        return cropMode != null ? cropMode : "nueva_capa";
    } // --- Fin del metodo getCropMode ---


    public String getZoomMode() {
        return zoomMode != null ? zoomMode : "cursor";
    } // --- Fin del metodo getZoomMode ---


    public void updateDimensionSpinners(int x, int y, int w, int h) {
        spinnerX.setValue(x);
        spinnerY.setValue(y);
        spinnerW.setValue(w);
        spinnerH.setValue(h);
    } // --- Fin del metodo updateDimensionSpinners ---


    public void setEditToolSpinnerListener(Consumer<int[]> listener) {
        javax.swing.event.ChangeListener sync = e -> {
            int x = (Integer) spinnerX.getValue();
            int y = (Integer) spinnerY.getValue();
            int w = (Integer) spinnerW.getValue();
            int h = (Integer) spinnerH.getValue();
            listener.accept(new int[]{x, y, w, h});
        };
        for (JSpinner s : new JSpinner[]{spinnerX, spinnerY, spinnerW, spinnerH}) {
            for (javax.swing.event.ChangeListener cl : s.getChangeListeners()) {
                s.removeChangeListener(cl);
            }
            s.addChangeListener(sync);
        }
    } // --- Fin del metodo setEditToolSpinnerListener ---


    /**
     * Registra el listener del spinner de ángulo (un solo listener activo).
     */
    public void setAngleSpinnerListener(Consumer<Double> listener) {
        for (javax.swing.event.ChangeListener cl : spinnerAngle.getChangeListeners()) {
            spinnerAngle.removeChangeListener(cl);
        }
        spinnerAngle.addChangeListener(e -> {
            if (syncAngleGuard) return;
            listener.accept(((Number) spinnerAngle.getValue()).doubleValue());
        });
    } // --- Fin del metodo setAngleSpinnerListener ---


    public void updateEditLayerFields(Layer layer) {
        if (editLayerNameLabel != null) {
            editLayerNameLabel.setText(layer != null ? layer.getName() : "\u2014");
        }
        if (layer != null && layer.getBounds() != null) {
            Rectangle b = layer.getBounds();
            updateDimensionSpinners(b.x, b.y, b.width, b.height);
            setAngleSpinnerValue((int) Math.round(layer.getRotation()));
        } else {
            updateDimensionSpinners(0, 0, 0, 0);
            setAngleSpinnerValue(0);
        }
        showEditToolPanel(layer);
    } // --- Fin del metodo updateEditLayerFields ---


    /**
     * Fija el valor del spinner de ángulo sin disparar los listeners de
     * sincronización de edición.
     */
    private void setAngleSpinnerValue(int angle) {
        if (syncAngleGuard) return;
        syncAngleGuard = true;
        try {
            spinnerAngle.setValue(angle);
        } finally {
            syncAngleGuard = false;
        }
    } // --- Fin del metodo setAngleSpinnerValue ---


    /**
     * Sincroniza el spinner de ángulo con la rotación de la capa indicada
     * (sin disparar los listeners de edición).
     */
    public void syncAngleSpinner(Layer layer) {
        if (layer != null && layer.getBounds() != null) {
            setAngleSpinnerValue((int) Math.round(layer.getRotation()));
        } else {
            setAngleSpinnerValue(0);
        }
    } // --- Fin del metodo syncAngleSpinner ---


    private JPanel buildShapesPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        shapeTypeButtons.clear();
        ButtonGroup group = new ButtonGroup();
        ToolbarDefinition def = getSubDef("editoravanzadoformas");
        if (def != null) {
            for (ToolbarComponentDefinition comp : def.componentes()) {
                if (comp instanceof ToolbarButtonDefinition btnDef) {
                    String cmd = btnDef.comandoCanonico();
                    if (AppActionCommands.CMD_ADVANCED_EDITOR_FILL_COLOR.equals(cmd)) {
                        JLabel lb = new JLabel("Relleno:");
                        lb.setForeground(fgToolbar);
                        p.add(lb);
                        shapeFillSwatch = createColorSwatch(new Color(200, 200, 200), "Color de relleno", bg,
                                c -> {
                                    shapeFillColor = c;
                                    applyShapeProperty();
                                });
                        p.add(shapeFillSwatch);
                    } else if (AppActionCommands.CMD_ADVANCED_EDITOR_STROKE_COLOR.equals(cmd)) {
                        p.add(Box.createHorizontalStrut(4));
                        JLabel lb = new JLabel("Borde:");
                        lb.setForeground(fgToolbar);
                        p.add(lb);
                        shapeStrokeSwatch = createColorSwatch(Color.BLACK, "Color del borde", bg,
                                c -> {
                                    shapeStrokeColor = c;
                                    applyShapeProperty();
                                });
                        p.add(shapeStrokeSwatch);
                    } else if (AppActionCommands.CMD_ADVANCED_EDITOR_STROKE_WIDTH.equals(cmd)) {
                        p.add(Box.createHorizontalStrut(4));
                        JLabel lb = new JLabel("Grosor:");
                        lb.setForeground(fgToolbar);
                        p.add(lb);
                        JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
                        sp.setPreferredSize(new Dimension(50, 20));
                        p.add(sp);
                        sp.addChangeListener(e -> applyShapeProperty());
                        shapeStrokeWidthSpinner = sp;
                    } else {
                        JToggleButton shapeBtn = createSubToolButton(btnDef, bg, group);
                        hookShapeSelection(shapeBtn, btnDef);
                        p.add(shapeBtn);
                    }
                }
            }
        }
        return p;
    } // --- Fin del metodo buildShapesPanel ---


    /**
     * Vincula el tipo de forma (rect, ellipse, line, triangle, polygon) al
     * subtool correspondiente del panel de formas.
     */
    private void hookShapeSelection(JToggleButton btn, ToolbarButtonDefinition btnDef) {
        String cmd = btnDef.comandoCanonico();
        String type = switch (cmd) {
            case AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_ELLIPSE -> "ellipse";
            case AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_LINE -> "line";
            case AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_TRIANGLE -> "triangle";
            case AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_POLYGON -> "polygon";
            default -> "rect";
        };
        btn.addActionListener(e -> {
            if (btn.isSelected()) {
                selectedShapeType = type;
                applyShapeProperty();
            }
        });
        shapeTypeButtons.put(type, btn);
        if ("rect".equals(type)) {
            btn.setSelected(true);
        }
    } // --- Fin del metodo hookShapeSelection ---


    private JPanel buildZoomTypePanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        ButtonGroup group = new ButtonGroup();
        addButtonsFromDef(p, "editoravanzadozoomtype", bg, group);
        JLabel hint = new JLabel("Clic: acercar · Clic derecho: alejar");
        hint.setForeground(fgToolbar);
        hint.setFont(hint.getFont().deriveFont(10f));
        p.add(Box.createHorizontalStrut(6));
        p.add(hint);
        return p;
    } // --- Fin del metodo buildZoomTypePanel ---


    // ===================== HELPERS =====================


    private void addButtonsFromDef(JPanel panel, String toolbarKey, Color bg, ButtonGroup group) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createSubToolButton(btnDef, bg, group));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addButtonsFromDef ---


    private ToolbarDefinition getSubDef(String key) {
        return uiDefinitionService != null ? uiDefinitionService.getToolbarDefinition(key) : null;
    } // --- Fin del metodo getSubDef ---


    private JPanel newEmptyPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setBackground(bg);
        return p;
    } // --- Fin del metodo newEmptyPanel ---


    private static Color swatchBorderColor() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : new Color(120, 120, 120);
    }


    /**
     * Interpola linealmente entre dos colores (incluyendo alfa). t = 0 → a, t = 1 → b.
     */
    private static Color lerpColor(Color a, Color b, double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        return new Color(
                a.getRed() + (int) Math.round((b.getRed() - a.getRed()) * t),
                a.getGreen() + (int) Math.round((b.getGreen() - a.getGreen()) * t),
                a.getBlue() + (int) Math.round((b.getBlue() - a.getBlue()) * t),
                a.getAlpha() + (int) Math.round((b.getAlpha() - a.getAlpha()) * t));
    }


    private JButton createColorSwatch(Color initial, String tooltip, Color bg) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setBackground(initial);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(btn, tooltip, btn.getBackground());
            if (nuevo != null) {
                btn.setBackground(nuevo);
            }
        });
        return btn;
    } // --- Fin del metodo createColorSwatch ---


    /**
     * Crea un boton de color que ademas ejecuta un callback con el color elegido,
     * evitando el orden inverso de AbstractButton.fireActionPerformed.
     */
    private JButton createColorSwatch(Color initial, String tooltip, Color bg,
                                       Consumer<Color> onColorSelected) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setBackground(initial);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(swatchBorderColor()));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(btn, tooltip, btn.getBackground());
            if (nuevo != null) {
                btn.setBackground(nuevo);
                if (onColorSelected != null) {
                    onColorSelected.accept(nuevo);
                }
            }
        });
        return btn;
    } // --- Fin del metodo createColorSwatch ---


    private JLabel createIconLabel(ToolbarButtonDefinition btnDef, Color bg) {
        JLabel lb = new JLabel();
        lb.setBackground(bg);
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) lb.setIcon(icon);
        }
        lb.setToolTipText(btnDef.textoTooltip());
        return lb;
    } // --- Fin del metodo createIconLabel ---


    private JButton createCanvasResizeButton(ToolbarButtonDefinition btnDef, Color bg) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        btn.setFocusPainted(false);
        btn.setToolTipText(btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 18, 18);
            if (icon != null) btn.setIcon(icon);
        }
        btn.addActionListener(e -> showCanvasResizeDialog());
        return btn;
    } // --- Fin del metodo createCanvasResizeButton ---


    private void showCanvasResizeDialog() {
        // Find the advance edit panel ancestor
        java.awt.Container parent = getParent();
        while (parent != null && !(parent instanceof vista.panels.render.AdvanceEditPanel)) {
            parent = parent.getParent();
        }
        if (!(parent instanceof vista.panels.render.AdvanceEditPanel aep)) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        javax.swing.JSpinner wSpinner = new javax.swing.JSpinner(
                new javax.swing.SpinnerNumberModel(cm.getWidth(), 1, 99999, 1));
        javax.swing.JSpinner hSpinner = new javax.swing.JSpinner(
                new javax.swing.SpinnerNumberModel(cm.getHeight(), 1, 99999, 1));
        wSpinner.setPreferredSize(new java.awt.Dimension(80, 22));
        hSpinner.setPreferredSize(new java.awt.Dimension(80, 22));

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        var gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new javax.swing.JLabel("Anchura:"), gbc);
        gbc.gridx = 1;
        panel.add(wSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new javax.swing.JLabel("Altura:"), gbc);
        gbc.gridx = 1;
        panel.add(hSpinner, gbc);

        int result = javax.swing.JOptionPane.showConfirmDialog(this, panel,
                "Tama\u00F1o del Lienzo", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (result == javax.swing.JOptionPane.OK_OPTION) {
            int w = (Integer) wSpinner.getValue();
            int h = (Integer) hSpinner.getValue();
            cm.setSize(w, h);
            // Also resize existing layers to fit
            var lm = aep.getCanvas().getLayerModel();
            if (lm != null) {
                for (var layer : lm.getLayers()) {
                    var b = layer.getBounds();
                    if (b != null) {
                        layer.setBounds(new java.awt.Rectangle(
                                Math.min(b.x, w - 1), Math.min(b.y, h - 1),
                                Math.min(b.width, w), Math.min(b.height, h)));
                    }
                }
            }
            aep.getCanvas().repaint();
        }
    } // --- Fin del metodo showCanvasResizeDialog ---


    private JToggleButton createSubToolButton(ToolbarButtonDefinition btnDef, Color bg, ButtonGroup group) {
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                // Sub-herramienta — la acción real la conecta hookSubToolState
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, btnDef.comandoCanonico());
        action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 18, 18);
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, icon);
            }
        }

        JToggleButton btn = new JToggleButton(action);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        group.add(btn);
        hookSubToolState(btn, btnDef);
        return btn;
    } // --- Fin del metodo createSubToolButton ---


    /**
     * Conecta los botones sub-herramienta (modo transform, target de
     * transformación, modo de recorte, tipo de zoom) con el estado interno de
     * la barra.
     */
    private void hookSubToolState(JToggleButton btn, ToolbarButtonDefinition btnDef) {
        String cmd = btnDef.comandoCanonico();

        switch (cmd) {
            case AppActionCommands.CMD_ADVANCED_EDITOR_MOVE,
                 AppActionCommands.CMD_ADVANCED_EDITOR_SCALE,
                 AppActionCommands.CMD_ADVANCED_EDITOR_ROTATE -> {
                String mode = switch (cmd) {
                    case AppActionCommands.CMD_ADVANCED_EDITOR_SCALE -> "escalar";
                    case AppActionCommands.CMD_ADVANCED_EDITOR_ROTATE -> "rotar";
                    default -> "mover";
                };
                btn.addActionListener(e -> {
                    if (btn.isSelected()) transformMode = mode;
                });
                if ("mover".equals(mode)) btn.setSelected(true);
            }
            case AppActionCommands.CMD_ADVANCED_EDITOR_LAYER,
                 AppActionCommands.CMD_ADVANCED_EDITOR_MARCO -> {
                String target = AppActionCommands.CMD_ADVANCED_EDITOR_MARCO.equals(cmd) ? "marco" : "capa";
                btn.addActionListener(e -> {
                    if (btn.isSelected()) transformTarget = target;
                });
                if ("capa".equals(target)) {
                    transformTargetLayerBtn = btn;
                    btn.setSelected(true);
                }
            }
            case AppActionCommands.CMD_ADVANCED_EDITOR_PAGE -> {
                btn.addActionListener(e -> {
                    if (btn.isSelected()) {
                        showCanvasResizeDialog();
                        if (transformTargetLayerBtn != null) {
                            transformTargetLayerBtn.setSelected(true);
                        }
                    }
                });
            }
            case AppActionCommands.CMD_ADVANCED_EDITOR_CROP_SELECTION,
                 AppActionCommands.CMD_ADVANCED_EDITOR_CROP_LAYER -> {
                String mode = AppActionCommands.CMD_ADVANCED_EDITOR_CROP_SELECTION.equals(cmd) ? "eliminar" : "nueva_capa";
                btn.addActionListener(e -> {
                    if (btn.isSelected()) cropMode = mode;
                });
                if ("nueva_capa".equals(mode)) btn.setSelected(true);
            }
            case AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM_CENTER,
                 AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM_CURSOR -> {
                String mode = AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM_CENTER.equals(cmd) ? "centro" : "cursor";
                btn.addActionListener(e -> {
                    if (btn.isSelected()) zoomMode = mode;
                });
                if ("cursor".equals(mode)) btn.setSelected(true);
            }
            case AppActionCommands.CMD_ADVANCED_EDITOR_SHAPE_RECT -> { /* el tipo de forma lo gestiona hookShapeSelection */ }
            default -> { /* sin estado asociado */ }
        }
    } // --- Fin del metodo hookSubToolState ---


    private Component createSubSeparator(Color bg) {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 20));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        wrap.setBackground(bg);
        wrap.add(sep);
        return wrap;
    } // --- Fin del metodo createSubSeparator ---


    private JLabel createDimLabel(String text) {
        JLabel lb = new JLabel(text);
        lb.setForeground(fgToolbar);
        lb.setFont(lb.getFont().deriveFont(10f));
        return lb;
    } // --- Fin del metodo createDimLabel ---


    private JSpinner createDimsSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
        sp.setPreferredSize(new Dimension(48, 20));
        sp.setMaximumSize(new Dimension(48, 20));
        sp.setMinimumSize(new Dimension(48, 20));
        sp.setEnabled(false);
        var editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setColumns(4);
            de.getTextField().setHorizontalAlignment(JLabel.RIGHT);
        }
        return sp;
    } // --- Fin del metodo createDimsSpinner ---


    private JSpinner createAngleSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, -360, 360, 1));
        sp.setPreferredSize(new Dimension(52, 20));
        sp.setMaximumSize(new Dimension(52, 20));
        sp.setMinimumSize(new Dimension(52, 20));
        sp.setEnabled(false);
        var editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setColumns(4);
            de.getTextField().setHorizontalAlignment(JLabel.RIGHT);
        }
        return sp;
    } // --- Fin del metodo createAngleSpinner ---


    // ===================== HOME MODE =====================


    public void setHomeMode(boolean active) {
        this.homeActive = active;

        for (Component c : leftPartToolComponents) {
            c.setVisible(!active);
        }
        leftPartHome.setVisible(active);

        if (active) {
            // Deseleccionar todos los toggles del home
            for (Component c : leftPartHome.getComponents()) {
                if (c instanceof JToggleButton tb) {
                    tb.setSelected(false);
                }
            }
            cardLayout.show(toolControlsPanel, "home_empty");
        } else {
            ToolItem sel = (ToolItem) toolCombo.getSelectedItem();
            if (sel != null) {
                showOptionsFor(sel.commandKey());
            }
        }

        revalidate();
        repaint();
    } // --- Fin del metodo setHomeMode ---


    private void buildHomeRow1() {
        leftPartHome.removeAll();
        Color bg = getBackground();
        leftPartHome.add(Box.createHorizontalStrut(8));
        addHomeToggleButtonsFromDef(leftPartHome, "editoravanzadohometools", bg);
    } // --- Fin del metodo buildHomeRow1 ---


    private void buildHomePanels() {
        Color bg = getBackground();
        homeLienzoPanel = buildHomeLienzoPanel(bg);
        homeImagePanel = buildHomeImagePanel(bg);
        toolControlsPanel.add(homeLienzoPanel, "home_lienzo_medidas");
        toolOptionsMap.put("home_lienzo_medidas", homeLienzoPanel);
        toolControlsPanel.add(homeImagePanel, "home_imagen");
        toolOptionsMap.put("home_imagen", homeImagePanel);
        // Panel vacío para cuando ningún toggle está seleccionado
        toolControlsPanel.add(newEmptyPanel(bg), "home_empty");
    } // --- Fin del metodo buildHomePanels ---


    private JPanel buildHomeLienzoPanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);

        p.add(Box.createHorizontalStrut(6));

        // Combo medidas standard
        String[] presets = {"Personalizado", "A3 (3508\u00D74961)", "A4 (2480\u00D73508)", "A5 (1748\u00D72480)",
                "Full HD (1920\u00D71080)", "HD (1280\u00D7720)", "Facebook (1200\u00D7630)",
                "Instagram (1080\u00D71080)", "YouTube (1280\u00D7720)"};
        int[][] presetValues = {{0, 0}, {3508, 4961}, {2480, 3508}, {1748, 2480},
                {1920, 1080}, {1280, 720}, {1200, 630},
                {1080, 1080}, {1280, 720}};
        JComboBox<String> presetCombo = new JComboBox<>(presets);
        presetCombo.setPreferredSize(new Dimension(140, 22));
        p.add(presetCombo);

        p.add(Box.createHorizontalStrut(4));

        // Spinner ancho + unidad
        JLabel lbW = new JLabel("Ancho:");
        lbW.setForeground(fgToolbar);
        p.add(lbW);
        homeWSpinner = new JSpinner(new SpinnerNumberModel(1920, 1, 99999, 1));
        homeWSpinner.setPreferredSize(new Dimension(65, 20));
        p.add(homeWSpinner);

        JToggleButton unitBtn = new JToggleButton("px");
        unitBtn.setFont(unitBtn.getFont().deriveFont(9f));
        unitBtn.setPreferredSize(new Dimension(30, 20));
        unitBtn.setFocusPainted(false);
        p.add(unitBtn);

        p.add(Box.createHorizontalStrut(4));

        // Spinner alto + unidad
        JLabel lbH = new JLabel("Alto:");
        lbH.setForeground(fgToolbar);
        p.add(lbH);
        homeHSpinner = new JSpinner(new SpinnerNumberModel(1080, 1, 99999, 1));
        homeHSpinner.setPreferredSize(new Dimension(65, 20));
        p.add(homeHSpinner);

        JToggleButton unitBtnH = new JToggleButton("px");
        unitBtnH.setFont(unitBtnH.getFont().deriveFont(9f));
        unitBtnH.setPreferredSize(new Dimension(30, 20));
        unitBtnH.setFocusPainted(false);
        p.add(unitBtnH);

        p.add(Box.createHorizontalStrut(4));

        // Color de fondo
        homeColorSwatch = new JButton();
        homeColorSwatch.setPreferredSize(new Dimension(20, 20));
        homeColorSwatch.setBackground(clr("Panel.background", 200, 200, 200));
        homeColorSwatch.setOpaque(true);
        homeColorSwatch.setBorder(BorderFactory.createLineBorder(clr("Component.borderColor", 120, 120, 120)));
        homeColorSwatch.setToolTipText("Color de fondo");
        homeColorSwatch.addActionListener(e -> {
            Color nuevo = JColorChooser.showDialog(homeColorSwatch, "Color de fondo", homeColorSwatch.getBackground());
            if (nuevo != null) homeColorSwatch.setBackground(nuevo);
        });
        p.add(homeColorSwatch);

        p.add(Box.createHorizontalStrut(8));

        // Acciones de lienzo (nuevo, medidas, ajustar al contenido)
        addHomeButtonsFromDef(p, "editoravanzadohomecanvas", bg, null);

        p.add(Box.createHorizontalStrut(4));

        // Aceptar + Cancelar
        addHomeButtonsFromDef(p, "editoravanzadoconfirmacion", bg, null);

        // Preset listener
        presetCombo.addActionListener(e -> {
            int idx = presetCombo.getSelectedIndex();
            if (idx > 0 && idx < presetValues.length) {
                homeWSpinner.setValue(presetValues[idx][0]);
                homeHSpinner.setValue(presetValues[idx][1]);
            }
        });

        // Unidad toggle listener
        final boolean[] isPx = {true};
        ActionListener unitListener = ev -> {
            isPx[0] = !isPx[0];
            String text = isPx[0] ? "px" : "mm";
            unitBtn.setText(text);
            unitBtnH.setText(text);
        };
        unitBtn.addActionListener(unitListener);
        unitBtnH.addActionListener(unitListener);

        return p;
    } // --- Fin del metodo buildHomeLienzoPanel ---


    private JPanel buildHomeImagePanel(Color bg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(bg);
        addHomeButtonsFromDef(p, "editoravanzadohomeimage", bg, null);
        return p;
    } // --- Fin del metodo buildHomeImagePanel ---


    // ===================== HOME HELPERS =====================


    private void addHomeButtonsFromDef(JPanel panel, String toolbarKey, Color bg, ButtonGroup group) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createHomeButton(btnDef, bg, group));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addHomeButtonsFromDef ---


    // ===================== HOME BUTTON CREATION =====================


    /** Crea botones toggle independientes (sin ButtonGroup) para home */
    private void addHomeToggleButtonsFromDef(JPanel panel, String toolbarKey, Color bg) {
        ToolbarDefinition def = getSubDef(toolbarKey);
        if (def == null) return;
        for (ToolbarComponentDefinition comp : def.componentes()) {
            if (comp instanceof ToolbarButtonDefinition btnDef) {
                panel.add(createHomeToggleButton(btnDef, bg));
            } else if (comp instanceof SeparatorDefinition) {
                panel.add(createSubSeparator(bg));
            }
        }
    } // --- Fin del metodo addHomeToggleButtonsFromDef ---


    private JToggleButton createHomeToggleButton(ToolbarButtonDefinition btnDef, Color bg) {
        String cmd = btnDef.comandoCanonico();
        JToggleButton btn = new JToggleButton();
        btn.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHomeToggleAction(cmd, btn.isSelected());
            }
        });
        btn.getAction().putValue(Action.ACTION_COMMAND_KEY, cmd);
        btn.getAction().putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) btn.getAction().putValue(Action.SMALL_ICON, icon);
        }
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 22));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return btn;
    } // --- Fin del metodo createHomeToggleButton ---


    /** Crea botones de acción (JButton) para home */
    private AbstractButton createHomeButton(ToolbarButtonDefinition btnDef, Color bg, ButtonGroup group) {
        String cmd = btnDef.comandoCanonico();
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHomeAction(cmd);
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, cmd);
        action.putValue(Action.SHORT_DESCRIPTION, btnDef.textoTooltip());
        if (iconUtils != null) {
            var icon = iconUtils.getScaledIcon(btnDef.claveIcono(), 16, 16);
            if (icon != null) action.putValue(Action.SMALL_ICON, icon);
        }

        AbstractButton btn;
        if (group != null) {
            JToggleButton tb = new JToggleButton(action);
            group.add(tb);
            btn = tb;
        } else {
            btn = new JButton(action);
        }
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(24, 22));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        return btn;
    } // --- Fin del metodo createHomeButton ---


    // ===================== HOME ACTION HANDLERS =====================


    private void handleHomeToggleAction(String cmd, boolean selected) {
        if (!selected) {
            cardLayout.show(toolControlsPanel, "home_empty");
            return;
        }
        // Al seleccionar uno, deseleccionar el otro
        for (Component c : leftPartHome.getComponents()) {
            if (c instanceof JToggleButton tb && tb != getToggleButtonFor(cmd)) {
                tb.setSelected(false);
            }
        }
        switch (cmd) {
            case AppActionCommands.CMD_HOME_LIENZO -> {
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_CONTROL_IMAGEN -> {
                if (toolOptionsMap.containsKey("home_imagen")) {
                    cardLayout.show(toolControlsPanel, "home_imagen");
                }
            }
            default -> {}
        }
    } // --- Fin del metodo handleHomeToggleAction ---


    private AbstractButton getToggleButtonFor(String cmd) {
        for (Component c : leftPartHome.getComponents()) {
            if (c instanceof AbstractButton ab && cmd.equals(ab.getActionCommand())) {
                return ab;
            }
        }
        return null;
    } // --- Fin del metodo getToggleButtonFor ---


    private void handleHomeAction(String cmd) {
        if (cmd == null) return;

        AdvanceEditPanel aep = findAdvanceEditPanel();

        switch (cmd) {
            case AppActionCommands.CMD_HOME_LIENZO -> {
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_CONTROL_IMAGEN -> {
                if (toolOptionsMap.containsKey("home_imagen")) {
                    cardLayout.show(toolControlsPanel, "home_imagen");
                }
            }
            case AppActionCommands.CMD_HOME_LIENZO_MEDIDAS -> {
                // Cambio de medidas: los controles ya están visibles en el panel
                if (toolOptionsMap.containsKey("home_lienzo_medidas")) {
                    cardLayout.show(toolControlsPanel, "home_lienzo_medidas");
                }
            }
            case AppActionCommands.CMD_HOME_LIENZO_NUEVO -> {
                int w = homeWSpinner != null ? (Integer) homeWSpinner.getValue() : 1920;
                int h = homeHSpinner != null ? (Integer) homeHSpinner.getValue() : 1080;
                Color bc = homeColorSwatch != null ? homeColorSwatch.getBackground() : new Color(200, 200, 200);
                crearNuevoLienzo(w, h, bc);
            }
            case AppActionCommands.CMD_HOME_LIENZO_AJUSTAR_CONTENIDO -> ajustarLienzoAlContenido();
            case AppActionCommands.CMD_HOME_ACEPTAR -> {
                int w = homeWSpinner != null ? (Integer) homeWSpinner.getValue() : 1920;
                int h = homeHSpinner != null ? (Integer) homeHSpinner.getValue() : 1080;
                Color bc = homeColorSwatch != null ? homeColorSwatch.getBackground() : new Color(200, 200, 200);
                aplicarMedidasLienzo(w, h, bc);
            }
            case AppActionCommands.CMD_HOME_CANCELAR -> cerrarHome();
            case AppActionCommands.CMD_HOME_IMPORTAR -> importarImagen();
            case AppActionCommands.CMD_HOME_EXPORTAR -> exportarImagen();
            case AppActionCommands.CMD_HOME_EXPORTAR_PREVIEW -> exportarAPreview();
            default -> {
                if (aep != null) aep.setActiveTool(cmd);
            }
        }
    } // --- Fin del metodo handleHomeAction ---


    // ===================== HOME ACTIONS =====================


    private AdvanceEditPanel findAdvanceEditPanel() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof AdvanceEditPanel)) {
            parent = parent.getParent();
        }
        return (AdvanceEditPanel) parent;
    } // --- Fin del metodo findAdvanceEditPanel ---


    private void crearNuevoLienzo(int w, int h, Color bgColor) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        // Limpiar capas existentes
        var lm = aep.getCanvas().getLayerModel();
        if (lm != null) {
            lm.clear();
        }

        cm.setSize(w, h);
        cm.setBackgroundColor(bgColor);
        cm.setTransparent(false);
        aep.getCanvas().repaint();

        cerrarHome();
    } // --- Fin del metodo crearNuevoLienzo ---


    private void aplicarMedidasLienzo(int w, int h, Color bgColor) {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        var cm = aep.getCanvas().getCanvasModel();
        if (cm == null) return;

        cm.setSize(w, h);
        cm.setBackgroundColor(bgColor);
        aep.getCanvas().repaint();

        cerrarHome();
    } // --- Fin del metodo aplicarMedidasLienzo ---


    private void ajustarLienzoAlContenido() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        var cm = aep.getCanvas().getCanvasModel();
        var lm = aep.getCanvas().getLayerModel();
        if (cm == null || lm == null) return;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hayContenido = false;

        for (Layer layer : lm.getLayers()) {
            if (!layer.isVisible()) continue;
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            hayContenido = true;
            minX = Math.min(minX, b.x);
            minY = Math.min(minY, b.y);
            maxX = Math.max(maxX, b.x + b.width);
            maxY = Math.max(maxY, b.y + b.height);
        }

        if (!hayContenido) return;

        // Trasladar el contenido al origen sin modificar sus tamaños
        for (Layer layer : lm.getLayers()) {
            if (!layer.isVisible()) continue;
            Rectangle b = layer.getBounds();
            if (b == null) continue;
            layer.setBounds(new Rectangle(b.x - minX, b.y - minY, b.width, b.height));
        }

        cm.setSize(maxX - minX, maxY - minY);
        aep.getCanvas().repaint();

        cerrarHome();
    } // --- Fin del metodo ajustarLienzoAlContenido ---


    private void cerrarHome() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep != null) {
            // Esto deselecciona la casa y vuelve al modo herramienta
            aep.setActiveTool(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION);
        }
    } // --- Fin del metodo cerrarHome ---


    private void importarImagen() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (fc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (file == null) return;

        try {
            BufferedImage img = javax.imageio.ImageIO.read(file);
            if (img == null) return;

            var cm = aep.getCanvas().getCanvasModel();
            var lm = aep.getCanvas().getLayerModel();
            if (cm == null || lm == null) return;

            if (lm.size() == 0) {
                // Lienzo vacío → redimensionar canvas al tamaño de la imagen
                cm.setSize(img.getWidth(), img.getHeight());
            }

            modelo.editor.ImageLayer layer = new modelo.editor.ImageLayer(
                    file.getName(), img,
                    new java.awt.Rectangle(0, 0, img.getWidth(), img.getHeight()));
            lm.addLayer(layer);
            aep.getCanvas().repaint();
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al importar la imagen: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        cerrarHome();
    } // --- Fin del metodo importarImagen ---


    private void exportarImagen() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return;

        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG (*.png)", "png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG (*.jpg)", "jpg", "jpeg"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "GIF (*.gif)", "gif"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "BMP (*.bmp)", "bmp"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (file == null) return;

        String name = file.getName().toLowerCase();
        String format;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            format = "jpg";
        } else if (name.endsWith(".gif")) {
            format = "gif";
        } else if (name.endsWith(".bmp")) {
            format = "bmp";
        } else {
            format = "png";
            if (!name.endsWith(".png")) {
                file = new java.io.File(file.getAbsolutePath() + ".png");
            }
        }

        try {
            BufferedImage out = composeBufferedImage();
            if (out != null) javax.imageio.ImageIO.write(out, format, file);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al exportar: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        cerrarHome();
    } // --- Fin del metodo exportarImagen ---


    private void exportarAPreview() {
        BufferedImage out = composeBufferedImage();
        if (out == null) return;

        AdvanceEditPanel aep = findAdvanceEditPanel();
        Container parent = aep.getParent();
        while (parent != null && !(parent instanceof RenderPanel)) {
            parent = parent.getParent();
        }
        if (parent instanceof RenderPanel rp) {
            rp.set2DImage(out);
            rp.show2DView();
        }

        cerrarHome();
    } // --- Fin del metodo exportarAPreview ---


    /**
     * Devuelve una imagen plana de lo visible en el lienzo del editor (fondos
     * m\u00E1s capas visibles), lista para copiar al portapapeles.
     *
     * @return la imagen aplanada, o {@code null} si no hay lienzo disponible
     */
    public BufferedImage flattenVisibleEditorImage() {
        return composeBufferedImage();
    } // --- Fin del metodo flattenVisibleEditorImage ---


    private BufferedImage composeBufferedImage() {
        AdvanceEditPanel aep = findAdvanceEditPanel();
        if (aep == null) return null;

        var cm = aep.getCanvas().getCanvasModel();
        var lm = aep.getCanvas().getLayerModel();
        if (cm == null) return null;

        int w = cm.getWidth();
        int h = cm.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = out.createGraphics();
        if (!cm.isTransparent()) {
            g2.setColor(cm.getBackgroundColor());
            g2.fillRect(0, 0, w, h);
        }
        if (lm != null) {
            for (var layer : lm.getLayers()) {
                if (layer.isVisible() && layer.getOpacity() >= 0.01f) {
                    layer.paint(g2);
                }
            }
        }
        g2.dispose();
        return out;
    } // --- Fin del metodo composeBufferedImage ---


    public void updateTheme(Color bg, Color fg, Color border) {
        fgToolbar = fg;
        setBackground(bg);
        setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, border));
        updateChildColors(bg, fg, this);
        toolControlsPanel.setBackground(bg);
        row1Right.setBackground(bg);
        toolCombo.setBackground(bg);
        labelX.setForeground(fg);
        labelY.setForeground(fg);
        labelW.setForeground(fg);
        labelH.setForeground(fg);
        labelAngle.setForeground(fg);

        ToolItem sel = (ToolItem) toolCombo.getSelectedItem();
        buildRow1RightSide();
        buildHomeRow1();
        buildAllToolPanels();
        if (sel != null) {
            showOptionsFor(sel.commandKey());
        }
    } // --- Fin del metodo updateTheme ---


    private void updateChildColors(Color bg, Color fg, Container parent) {
        parent.setBackground(bg);
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(fg);
            }
            if (c instanceof Container && !(c instanceof Box.Filler) && c != toolCombo) {
                updateChildColors(bg, fg, (Container) c);
            }
        }
    } // --- Fin del metodo updateChildColors ---


    // -----------------------------------------------------------------------
    // Renderer personalizado para mostrar icono + texto en el desplegable
    // -----------------------------------------------------------------------


    private class ToolListRenderer extends JLabel implements ListCellRenderer<ToolItem> {

        private static final long serialVersionUID = 1L;


        ToolListRenderer() {
            setOpaque(true);
            setFont(getFont().deriveFont(11f));
        } // --- Fin del constructor ToolListRenderer ---


        @Override
        public Component getListCellRendererComponent(JList<? extends ToolItem> list,
                ToolItem value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(fgToolbar);
            }
            if (value != null) {
                setText(value.tooltip());
                setToolTipText(value.tooltip());
                if (iconUtils != null) {
                    var icon = iconUtils.getScaledIcon(value.iconKey(), 16, 16);
                    setIcon(icon);
                } else {
                    setIcon(null);
                }
            } else {
                setText("");
                setIcon(null);
                setToolTipText(null);
            }
            return this;
        } // --- Fin del metodo getListCellRendererComponent ---

    } // --- Fin de la clase ToolListRenderer ---

} // --- Fin de la clase EditorComponentBar ---
