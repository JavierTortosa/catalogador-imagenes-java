package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLaf;

import vista.theme.ThemeManager;
import vista.theme.ThemeManager.ThemeCategory;
import vista.theme.ThemeManager.ThemeInfo;
import vista.theme.ThemePreviewPanel;

/**
 * Diálogo de personalización de temas. Muestra una previsualización
 * interactiva del visor con zonas cliqueables y una lista organizada
 * por categorías de todos los colores personalizables de la aplicación.
 * Permite guardar, aplicar y borrar temas personalizados.
 */
public class ThemeCustomizerDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ThemeCustomizerDialog.class);

    private final ThemeManager themeManager;
    private JComboBox<ThemeInfo> baseThemeSelector;
    private JTextField customThemeNameField;

    private final Map<String, Color> customColors = new LinkedHashMap<>();
    private final Map<String, ColorPreviewPanel> colorSwatches = new LinkedHashMap<>();
    private final Map<String, String[]> labelToKeys = new LinkedHashMap<>();
    private final Map<String, String> keyToLabel = new HashMap<>();
    private final Map<String, JPanel> colorRows = new LinkedHashMap<>();

    private ThemePreviewPanel themePreview;
    private JScrollPane colorScrollPane;
    private boolean isInitializing = true;
    private boolean isSaved = false;

    /**
     * Definición interna de una categoría de colores: título y lista
     * de entradas (etiqueta descriptiva + claves de propiedad).
     */
    private static class CategoryDef {
        final String title;
        final String[][] entries;
        CategoryDef(String title, String[][] entries) {
            this.title = title;
            this.entries = entries;
        } // --- Fin del metodo [CategoryDef] ---
    } // --- Fin de clase [CategoryDef] ---


    /**
     * Construye y devuelve la lista completa de categorías con todas
     * las entradas de color que se mostrarán en el editor.
     */
    private static List<CategoryDef> buildCategories() {
        List<CategoryDef> cats = new ArrayList<>();

        cats.add(new CategoryDef("Men\u00FA y Herramientas", new String[][] {
            {"Fondo del men\u00FA", "MenuBar.background"},
            {"Texto del men\u00FA", "MenuBar.foreground"},
            {"Fondo barra herramientas", "ToolBar.background"},
            {"Color de botones", "Button.foreground"},
            {"Borde de bot\u00F3n", "Button.borderColor"},
            {"Borde bot\u00F3n enfocado", "Button.focusedBorderColor"},
            {"Fondo bot\u00F3n por defecto", "Button.default.background"},
            {"Texto bot\u00F3n por defecto", "Button.default.foreground"},
        }));

        cats.add(new CategoryDef("Barra de Estado", new String[][] {
            {"Fondo de la barra", "Visor.statusBarBackground"},
            {"Texto de la barra", "Visor.statusBarForeground"},
            {"Fondo campo de ruta", "TextField.background"},
            {"Texto campo de ruta", "TextField.foreground"},
            {"Borde campo activo", "Component.accentColor"},
            {"Borde de enfoque campo", "Component.focusedBorderColor"},
            {"Fondo \u00E1rea de texto", "TextArea.background"},
        }));

        cats.add(new CategoryDef("Lista de Archivos", new String[][] {
            {"Fondo de la lista", "List.background"},
            {"Texto de la lista", "List.foreground"},
            {"Fondo de selecci\u00F3n", "List.selectionBackground"},
            {"Texto seleccionado", "List.selectionForeground"},
            {"Fondo selec. inactiva", "List.selectionInactiveBackground"},
            {"Texto selec. inactiva", "List.selectionInactiveForeground"},
            {"Borde de celda", "List.focusCellHighlightBorder"},
        }));

        cats.add(new CategoryDef("Visor de Im\u00E1genes", new String[][] {
            {"Fondo del visor", "windowBackground"},
            {"Borde imagen marcada", "Visor.markedImageBorder"},
            {"Borde imagen enfocada", "Visor.borderColor.focused"},
        }));

        cats.add(new CategoryDef("Miniaturas", new String[][] {
            {"Fondo de miniaturas", "Panel.background"},
            {"Texto de miniaturas", "Label.foreground"},
            {"Fondo de campo combo", "ComboBox.background"},
            {"Fondo de contrase\u00F1a", "PasswordField.background"},
            {"Fondo de spinner", "Spinner.background"},
        }));

        cats.add(new CategoryDef("Grid del Proyecto", new String[][] {
            {"Fondo selec. del \u00E1rbol", "Tree.selectionBackground"},
            {"Texto selec. del \u00E1rbol", "Tree.selectionForeground"},
            {"Borde celda \u00E1rbol", "Tree.focusCellHighlightBorder"},
            {"Fondo selec. de tabla", "Table.selectionBackground"},
            {"Texto selec. de tabla", "Table.selectionForeground"},
            {"Borde celda tabla", "Table.focusCellHighlightBorder"},
            {"Fondo encabezado tabla", "TableHeader.background"},
            {"Fondo barra de estado", "StatusBar.background"},
        }));

        cats.add(new CategoryDef("Acentos y Bordes", new String[][] {
            {"Color de acento", "Component.accentColor"},
            {"Color de enfoque", "Component.focusColor", "JTextField.focusColor",
             "ComboBox.focusColor", "TabbedPane.focusColor",
             "ScrollPane.focusColor", "SplitPane.focusColor", "Panel.focusColor"},
            {"Borde de enfoque", "Component.focusedBorderColor"},
            {"Borde general", "Component.borderColor"},
            {"Subrayado de pesta\u00F1a", "TabbedPane.underlineColor"},
            {"Fondo \u00E1rea pesta\u00F1as", "TabbedPane.contentAreaColor"},
            {"Texto de pesta\u00F1as", "TabbedPane.foreground"},
            {"Color del separador", "Separator.foreground", "Separator.color"},
            {"Color borde de t\u00EDtulo", "TitledBorder.titleColor"},
            {"Barra de progreso", "ProgressBar.foreground"},
        }));

        cats.add(new CategoryDef("Varios", new String[][] {
            {"Texto de checkbox", "CheckBox.foreground"},
            {"Texto de radio", "RadioButton.foreground"},
            {"Fondo t\u00EDtulo ventana", "TitlePane.background"},
            {"Texto de campo inactivo", "TextField.inactiveForeground"},
            {"Texto etiqueta deshabilitada", "Label.disabledForeground"},
            {"Fondo campo deshabilitado", "TextField.disabledBackground"},
        }));

        return cats;
    } // --- Fin del metodo [buildCategories] ---


    /**
     * Constructor. Configura la interfaz del editor: panel de
     * previsualización a la izquierda, categorías de colores a la
     * derecha, selector de tema base y botones de acción.
     */
    public ThemeCustomizerDialog(JFrame owner, ThemeManager themeManager) {
        super(owner, "Editor de Temas Personalizados", true);
        this.themeManager = themeManager;

        setSize(1100, 800);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // --- 1. Preview panel (left) ---
        themePreview = new ThemePreviewPanel();
        themePreview.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 10, 10, 10),
            BorderFactory.createTitledBorder("Vista Previa Interactiva")
        ));
        themePreview.setOnZoneClicked(this::handleZoneClicked);

        // --- 2. Control panel (right) ---
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(10, 0, 10, 10));

        // Header: name + base theme
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Configuraci\u00F3n General"),
            new EmptyBorder(5, 5, 5, 5)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        headerPanel.add(new JLabel("Nombre del Tema:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        customThemeNameField = new JTextField();
        headerPanel.add(customThemeNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        headerPanel.add(new JLabel("Basado en:"), gbc);
        gbc.gridx = 1;
        List<ThemeInfo> themeList = new ArrayList<>(themeManager.getAvailableThemes().values());
        themeList.removeIf(t -> t.category() == ThemeCategory.CUSTOM);
        themeList.sort(java.util.Comparator.comparing(ThemeInfo::nombreDisplay));
        baseThemeSelector = new JComboBox<>(themeList.toArray(new ThemeInfo[0]));
        baseThemeSelector.setRenderer(new ThemeInfoRenderer());
        baseThemeSelector.addActionListener(e -> updateColorPreviews());
        headerPanel.add(baseThemeSelector, gbc);

        controlPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Color categories (scrollable) ---
        JPanel categoriesContainer = new JPanel();
        categoriesContainer.setLayout(new BoxLayout(categoriesContainer, BoxLayout.Y_AXIS));

        for (CategoryDef cat : buildCategories()) {
            categoriesContainer.add(buildCategoryPanel(cat));
            categoriesContainer.add(Box.createVerticalStrut(4));
        }

        colorScrollPane = new JScrollPane(categoriesContainer);
        colorScrollPane.setBorder(BorderFactory.createTitledBorder("Colores Personalizables"));
        colorScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        controlPanel.add(colorScrollPane, BorderLayout.CENTER);

        // --- Split pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            themePreview, controlPanel);
        splitPane.setDividerLocation(550);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        // --- Action buttons ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JButton saveButton = new JButton("Guardar Tema");
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
        saveButton.addActionListener(e -> saveCustomTheme());

        JButton applyButton = new JButton("Aplicar");
        applyButton.addActionListener(e -> {
            ThemeInfo selectedBase = (ThemeInfo) baseThemeSelector.getSelectedItem();
            if (selectedBase != null) {
                String baseId = null;
                for (Map.Entry<String, ThemeInfo> entry
                     : themeManager.getAvailableThemes().entrySet()) {
                    if (entry.getValue() == selectedBase) {
                        baseId = entry.getKey();
                        break;
                    }
                }
                if (baseId != null) {
                    themeManager.setTemaActual(baseId, false);
                }
            }
            themeManager.applyLiveCustomizations(customColors);
            if (getOwner() != null) {
                javax.swing.SwingUtilities.updateComponentTreeUI(getOwner());
            }
            dispose();
        });

        JButton deleteButton = new JButton("Borrar Tema...");
        deleteButton.putClientProperty("JButton.buttonType", "roundRect");
        deleteButton.addActionListener(e -> deleteCustomTheme());

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> {
            if (confirmCloseIfUnsaved()) {
                dispose();
            }
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Preguntar al cerrar la ventana si hay cambios sin guardar
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmCloseIfUnsaved()) {
                    dispose();
                }
            }
        });

        // Select current theme
        String currentThemeId = themeManager.getTemaActual().nombreInterno();
        ThemeInfo currentInfo = themeManager.getAvailableThemes().get(currentThemeId);

        if (currentInfo != null) {
            if (currentInfo.category() == ThemeCategory.CUSTOM
                    || currentInfo.category() == ThemeCategory.CUSTOM_INTERNAL) {
                try {
                    String baseClassName = UIManager.getLookAndFeel().getClass().getName();
                    for (int i = 0; i < baseThemeSelector.getItemCount(); i++) {
                        ThemeInfo info = baseThemeSelector.getItemAt(i);
                        if (info.lafSupplier().get().getClass().getName().equals(baseClassName)) {
                            baseThemeSelector.setSelectedIndex(i);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error al identificar el tema base", e);
                }
            } else {
                for (int i = 0; i < baseThemeSelector.getItemCount(); i++) {
                    if (baseThemeSelector.getItemAt(i).nombreDisplay()
                            .equals(currentInfo.nombreDisplay())) {
                        baseThemeSelector.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        initializeFromCurrentTheme();
        isInitializing = false;
    } // --- Fin del metodo [ThemeCustomizerDialog] ---


    /**
     * Construye el panel visual de una categoría con borde titulado
     * y todas sus filas de color.
     */
    private JPanel buildCategoryPanel(CategoryDef cat) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), cat.title));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = -1;

        for (String[] entry : cat.entries) {
            gbc.gridy++;
            String label = entry[0];
            String[] keys = new String[entry.length - 1];
            System.arraycopy(entry, 1, keys, 0, keys.length);
            panel.add(createColorRow(label, keys), gbc);
        }

        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    } // --- Fin del metodo [buildCategoryPanel] ---


    /**
     * Crea una fila cliqueable con una etiqueta descriptiva y una
     * muestra de color. Al hacer clic se abre el selector de color.
     */
    private JPanel createColorRow(String labelText, String[] propertyKeys) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(11f));

        ColorPreviewPanel swatch = new ColorPreviewPanel();
        swatch.setPreferredSize(new Dimension(60, 18));
        swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        row.add(label, BorderLayout.CENTER);
        row.add(swatch, BorderLayout.EAST);

        colorSwatches.put(labelText, swatch);
        labelToKeys.put(labelText, propertyKeys);
        for (String key : propertyKeys) {
            keyToLabel.put(key, labelText);
        }
        colorRows.put(labelText, row);

        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                openColorChooser(labelText);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                row.setBackground(new Color(230, 235, 245));
                row.setOpaque(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                row.setBackground(null);
                row.setOpaque(false);
            }
        };
        row.addMouseListener(handler);
        label.addMouseListener(handler);

        return row;
    } // --- Fin del metodo [createColorRow] ---


    /**
     * Abre el selector de color del sistema para la fila indicada.
     * Si el usuario elige un nuevo color, actualiza la muestra,
     * el mapa de colores personalizados y la previsualización.
     */
    private void openColorChooser(String labelText) {
        ColorPreviewPanel swatch = colorSwatches.get(labelText);
        String[] keys = labelToKeys.get(labelText);
        if (swatch == null || keys == null) return;

        Color current = swatch.getColor();
        Color newColor = JColorChooser.showDialog(this,
            "Color para: " + labelText, current != null ? current : Color.WHITE);
        if (newColor != null) {
            swatch.setColor(newColor);
            for (String key : keys) {
                customColors.put(key, newColor);
            }
            refreshPreview();
        }
    } // --- Fin del metodo [openColorChooser] ---


    /**
     * Maneja el clic en una zona de la previsualización. Busca la
     * fila de color correspondiente a la clave de propiedad, hace
     * scroll hasta ella y abre el selector de color.
     */
    private void handleZoneClicked(String propertyKey) {
        String label = keyToLabel.get(propertyKey);
        if (label != null) {
            JPanel row = colorRows.get(label);
            if (row != null) {
                row.scrollRectToVisible(row.getBounds());
            }
            openColorChooser(label);
        }
    } // --- Fin del metodo [handleZoneClicked] ---


    /**
     * Refresca la previsualización con los colores actuales de todas
     * las filas del editor.
     */
    private void refreshPreview() {
        if (themePreview == null) return;
        Map<String, Color> previewColors = new HashMap<>();
        for (Map.Entry<String, String[]> entry : labelToKeys.entrySet()) {
            ColorPreviewPanel swatch = colorSwatches.get(entry.getKey());
            Color c = (swatch != null) ? swatch.getColor() : null;
            if (c != null) {
                for (String key : entry.getValue()) {
                    previewColors.put(key, c);
                }
            }
        }
        themePreview.setColors(previewColors);
    } // --- Fin del metodo [refreshPreview] ---


    /**
     * Actualiza todas las muestras de color con los valores por
     * defecto del tema base seleccionado. Se llama al cambiar el
     * selector de tema base.
     */
    private void updateColorPreviews() {
        if (isInitializing) return;

        ThemeInfo selectedThemeInfo =
            (ThemeInfo) baseThemeSelector.getSelectedItem();
        if (selectedThemeInfo == null) return;

        customColors.clear();

        try {
            LookAndFeel laf = selectedThemeInfo.lafSupplier().get();
            if (laf instanceof FlatLaf) {
                UIDefaults defaults = ((FlatLaf) laf).getDefaults();
                for (Map.Entry<String, String[]> entry : labelToKeys.entrySet()) {
                    String labelText = entry.getKey();
                    String[] keys = entry.getValue();
                    Color foundColor = null;
                    for (String key : keys) {
                        Color color = defaults.getColor(key);
                        if (color != null) {
                            foundColor = color;
                            break;
                        }
                    }
                    ColorPreviewPanel swatch = colorSwatches.get(labelText);
                    if (swatch != null) {
                        swatch.setColor(foundColor);
                    }
                }
                refreshPreview();
            }
        } catch (Exception e) {
            logger.error("No se pudieron cargar los colores para el tema: "
                + selectedThemeInfo.nombreDisplay(), e);
        }
    } // --- Fin del metodo [updateColorPreviews] ---


    /**
     * Inicializa todas las muestras de color con los valores del
     * tema actualmente aplicado en la interfaz. Se llama una vez al
     * abrir el diálogo.
     */
    private void initializeFromCurrentTheme() {
        for (Map.Entry<String, String[]> entry : labelToKeys.entrySet()) {
            String labelText = entry.getKey();
            String[] keys = entry.getValue();
            Color foundColor = null;
            for (String key : keys) {
                foundColor = UIManager.getColor(key);
                if (foundColor != null) break;
            }
            ColorPreviewPanel swatch = colorSwatches.get(labelText);
            if (swatch != null) {
                swatch.setColor(foundColor);
            }
            if (foundColor != null) {
                for (String key : keys) {
                    customColors.put(key, foundColor);
                }
            }
        }
        refreshPreview();
    } // --- Fin del metodo [initializeFromCurrentTheme] ---


    /**
     * Muestra un diálogo para seleccionar y borrar un tema
     * personalizado del disco y del mapa interno de temas.
     */
    private void deleteCustomTheme() {
        File customThemesDir = new File(".temas_personalizados");
        if (!customThemesDir.exists() || !customThemesDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                "No hay temas personalizados para borrar.",
                "Informaci\u00F3n", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File[] files = customThemesDir.listFiles(
            (dir, name) -> name.toLowerCase().endsWith(".properties"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay temas personalizados para borrar.",
                "Informaci\u00F3n", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultListModel<FileEntry> model = new DefaultListModel<>();
        for (File f : files) {
            String displayName = f.getName().replace(".properties", "");
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f),
                        StandardCharsets.UTF_8))) {
                String line = r.readLine();
                if (line != null && line.startsWith("#=")) {
                    displayName = line.substring(2).trim();
                }
            } catch (IOException e) {
                logger.warn("No se pudo leer {} para obtener nombre", f.getName());
            }
            model.addElement(new FileEntry(displayName, f));
        }

        JList<FileEntry> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(300, 200));

        int result = JOptionPane.showConfirmDialog(this, scroll,
            "Selecciona un tema para borrar",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            FileEntry selected = list.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "\u00bfEst\u00E1s seguro de borrar el tema \""
                    + selected.name + "\"?\nEsta acci\u00F3n no se puede deshacer.",
                    "Confirmar borrado",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (selected.file.delete()) {
                        String themeId = "custom_"
                            + selected.file.getName()
                                .replace(".properties", "")
                                .toLowerCase()
                                .replaceAll("\\s+", "_");
                        themeManager.removeCustomTheme(themeId);
                        logger.info("Tema personalizado borrado: {}",
                            selected.file.getName());
                        JOptionPane.showMessageDialog(this,
                            "Tema \"" + selected.name + "\" borrado con \u00E9xito.",
                            "Informaci\u00F3n", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        logger.error("No se pudo borrar: {}",
                            selected.file.getAbsolutePath());
                        JOptionPane.showMessageDialog(this,
                            "No se pudo borrar el tema. Comprueba los permisos del archivo.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    } // --- Fin del metodo [deleteCustomTheme] ---


    /**
     * Guarda el tema personalizado actual en un archivo .properties
     * dentro de la carpeta .temas_personalizados/.
     */
    private void saveCustomTheme() {
        String newThemeName = customThemeNameField.getText().trim();
        if (newThemeName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor, introduce un nombre para tu tema.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (customColors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No has personalizado ning\u00FAn color. El tema no se guardar\u00E1.",
                "Informaci\u00F3n", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ThemeInfo baseTheme = (ThemeInfo) baseThemeSelector.getSelectedItem();
        if (baseTheme == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor, selecciona un tema base.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String baseThemeClassName;
        try {
            baseThemeClassName = baseTheme.lafSupplier().get().getClass().getName();
        } catch (Exception e) {
            logger.error("Error al obtener la clase del tema base", e);
            JOptionPane.showMessageDialog(this,
                "Error al obtener el tema base.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File customThemesDir = new File(".temas_personalizados");
        if (!customThemesDir.exists()) {
            if (!customThemesDir.mkdir()) {
                logger.error("No se pudo crear el directorio .temas_personalizados");
                JOptionPane.showMessageDialog(this,
                    "No se pudo crear el directorio de temas personalizados.",
                    "Error de Archivo", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String fileName = newThemeName.toLowerCase()
            .replaceAll("[^a-z0-9_\u00E1\u00E9\u00ED\u00F3\u00FA\u00F1]", "")
            .replaceAll("\\s+", "_") + ".properties";
        File themeFile = new File(customThemesDir, fileName);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(themeFile),
                    StandardCharsets.UTF_8))) {
            writer.write("#= " + newThemeName);
            writer.newLine();
            writer.write("# Basado en=" + baseThemeClassName);
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, Color> entry : customColors.entrySet()) {
                String hexColor = String.format("#%02x%02x%02x",
                    entry.getValue().getRed(),
                    entry.getValue().getGreen(),
                    entry.getValue().getBlue());
                writer.write(entry.getKey() + "=" + hexColor);
                writer.newLine();
            }

            logger.info("Tema personalizado guardado en: " + themeFile.getAbsolutePath());

            // Convertir colores a Properties y registrar el tema en vivo
            Properties themeProps = new Properties();
            customColors.forEach((key, color) -> {
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                themeProps.put(key, hex);
            });
            String newThemeId = themeManager.registerCustomTheme(newThemeName, baseThemeClassName, themeProps);
            themeManager.setTemaActual(newThemeId, true);
            isSaved = true;

            JOptionPane.showMessageDialog(this,
                "\u00A1Tema guardado y aplicado con \u00E9xito!",
                "\u00C9xito", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (IOException ex) {
            logger.error("Error al guardar el tema personalizado", ex);
            JOptionPane.showMessageDialog(this,
                "No se pudo guardar el archivo del tema.",
                "Error de Archivo", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del metodo [saveCustomTheme] ---


    // Devuelve true si se puede cerrar; si hay cambios sin guardar, pregunta al usuario
    private boolean confirmCloseIfUnsaved() {
        if (!customColors.isEmpty() && !isSaved) {
            int response = JOptionPane.showConfirmDialog(this,
                "Hay cambios sin guardar.\nSi cierras ahora, el tema personalizado se perder\u00E1\n"
                + "y la pr\u00F3xima vez que inicies la aplicaci\u00F3n\n"
                + "no se aplicar\u00E1n estas modificaciones.\n\n"
                + "\u00BFEst\u00E1s seguro de que quieres salir sin guardar?",
                "Tema sin guardar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            return response == JOptionPane.YES_OPTION;
        }
        return true;
    } // --- Fin del metodo [confirmCloseIfUnsaved] ---


    /**
     * Renderizador personalizado para el selector de temas base.
     * Muestra el nombre descriptivo de cada tema.
     */
    private static class ThemeInfoRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ThemeInfo) {
                setText(((ThemeInfo) value).nombreDisplay());
            }
            return this;
        } // --- Fin del metodo [getListCellRendererComponent] ---
    } // --- Fin de clase [ThemeInfoRenderer] ---


    /**
     * Panel cuadrado que muestra una muestra de color. Si no hay
     * color definido, dibuja un patrón de cuadros (checkerboard).
     */
    private static class ColorPreviewPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Color definedColor;
        private static final int SQUARE_SIZE = 4;
        private static final Color CHECKER_COLOR_1 = new Color(255, 255, 255);
        private static final Color CHECKER_COLOR_2 = new Color(224, 224, 224);

        public void setColor(Color color) {
            this.definedColor = color;
            repaint();
        } // --- Fin del metodo [setColor] ---


        public Color getColor() {
            return definedColor;
        } // --- Fin del metodo [getColor] ---

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (definedColor != null) {
                g.setColor(definedColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            } else {
                for (int row = 0; row < getHeight(); row += SQUARE_SIZE) {
                    for (int col = 0; col < getWidth(); col += SQUARE_SIZE) {
                        int rowIdx = row / SQUARE_SIZE;
                        int colIdx = col / SQUARE_SIZE;
                        g.setColor((rowIdx + colIdx) % 2 == 0
                            ? CHECKER_COLOR_1 : CHECKER_COLOR_2);
                        g.fillRect(col, row, SQUARE_SIZE, SQUARE_SIZE);
                    }
                }
            }
        } // --- Fin del metodo [paintComponent] ---
    } // --- Fin de clase [ColorPreviewPanel] ---


    /**
     * Registro que asocia el nombre mostrado de un tema con el
     * archivo .properties que lo contiene.
     */
    private record FileEntry(String name, File file) {
        @Override
        public String toString() { return name; }
    } // --- Fin de clase [FileEntry] ---

} // --- Fin de clase [ThemeCustomizerDialog] ---
