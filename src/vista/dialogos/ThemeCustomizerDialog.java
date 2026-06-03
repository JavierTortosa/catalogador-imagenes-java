package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLaf;

import vista.theme.ThemeManager;
import vista.theme.ThemeManager.ThemeCategory;
import vista.theme.ThemePreviewPanel;

public class ThemeCustomizerDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ThemeCustomizerDialog.class);
    private final ThemeManager themeManager;
    private JComboBox<ThemeManager.ThemeInfo> baseThemeSelector;
    private JTextField customThemeNameField;
    private final Map<String, Color> customColors = new HashMap<>();
    
    private final Map<String, ColorPreviewPanel> colorPreviewPanels = new HashMap<>();
    private final Map<String, String[]> labelToKeysMap = new HashMap<>();
    private final Map<String, JButton> labelToButtonMap = new HashMap<>();
    private final Map<String, Integer> labelToTabIndexMap = new HashMap<>();
    private ThemePreviewPanel themePreview;
    private JTabbedPane tabbedPane;
    /** Evita que el listener del combo de tema base dispare durante la inicialización */
    private boolean isInitializing = true;

    public ThemeCustomizerDialog(JFrame owner, ThemeManager themeManager) {
        super(owner, "Editor de Temas Personalizados", true);
        this.themeManager = themeManager;

        setSize(1100, 800);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // --- 1. Panel de Previsualización (Izquierda) ---
        themePreview = new ThemePreviewPanel();
        themePreview.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 10, 10, 10),
            BorderFactory.createTitledBorder("Vista Previa Interactiva")
        ));
        themePreview.setOnZoneClicked(this::handleZoneClicked);

        // --- 2. Panel de Controles (Derecha) ---
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(10, 0, 10, 10));

        // Cabecera: Nombre y Tema Base
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Configuración General"),
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
        java.util.List<ThemeManager.ThemeInfo> themeList = new java.util.ArrayList<>(themeManager.getAvailableThemes().values());
        themeList.removeIf(t -> t.category() == ThemeCategory.CUSTOM);
        themeList.sort(java.util.Comparator.comparing(ThemeManager.ThemeInfo::nombreDisplay));
        baseThemeSelector = new JComboBox<>(themeList.toArray(new ThemeManager.ThemeInfo[0]));
        baseThemeSelector.setRenderer(new ThemeInfoRenderer());
        baseThemeSelector.addActionListener(e -> updateColorPreviews());
        headerPanel.add(baseThemeSelector, gbc);

        controlPanel.add(headerPanel, BorderLayout.NORTH);

        // Pestañas de Colores
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Barras Sup.", createBarsTab());
        tabbedPane.addTab("Status Bars", createStatusBarsTab());
        tabbedPane.addTab("Cuerpo", createMainTab());
        tabbedPane.addTab("Miniaturas", createThumbTab());
        tabbedPane.addTab("Acento y Bordes", createAccentTab());
        
        controlPanel.add(tabbedPane, BorderLayout.CENTER);

        // --- 3. JSplitPane para unir ambos ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, themePreview, controlPanel);
        splitPane.setDividerLocation(550);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        // Botones de acción
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        JButton saveButton = new JButton("Guardar Tema");
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
        saveButton.addActionListener(e -> saveCustomTheme());
        
        JButton applyButton = new JButton("Aplicar");
        applyButton.addActionListener(e -> {
            // 1. Obtener y aplicar el tema base seleccionado
            ThemeManager.ThemeInfo selectedBase = (ThemeManager.ThemeInfo) baseThemeSelector.getSelectedItem();
            if (selectedBase != null) {
                // Buscamos el ID del tema base en el manager
                String baseId = null;
                for (Map.Entry<String, ThemeManager.ThemeInfo> entry : themeManager.getAvailableThemes().entrySet()) {
                    if (entry.getValue() == selectedBase) {
                        baseId = entry.getKey();
                        break;
                    }
                }
                
                if (baseId != null) {
                    // Aplicar el tema base (sin notificar todavía para evitar doble refresco)
                    themeManager.setTemaActual(baseId, false);
                }
            }

            // 2. Aplicar las personalizaciones de color acumuladas (esto notifica y refresca)
            themeManager.applyLiveCustomizations(customColors);
            
            // Refrescar toda la aplicación
            if (getOwner() != null) {
                javax.swing.SwingUtilities.updateComponentTreeUI(getOwner());
            }
            
            // Cerrar la ventana
            dispose();
        });

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Seleccionar el tema actual por defecto
        String currentThemeId = themeManager.getTemaActual().nombreInterno();
        ThemeManager.ThemeInfo currentInfo = themeManager.getAvailableThemes().get(currentThemeId);
        
        if (currentInfo != null) {
            if (currentInfo.category() == ThemeCategory.CUSTOM
                    || currentInfo.category() == ThemeCategory.CUSTOM_INTERNAL) {
                // Si es personalizado, intentar buscar el tema base por el nombre de la clase del LAF
                try {
                    String baseClassName = UIManager.getLookAndFeel().getClass().getName();
                    for (int i = 0; i < baseThemeSelector.getItemCount(); i++) {
                        ThemeManager.ThemeInfo info = baseThemeSelector.getItemAt(i);
                        if (info.lafSupplier().get().getClass().getName().equals(baseClassName)) {
                            baseThemeSelector.setSelectedIndex(i);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error al identificar el tema base del tema personalizado", e);
                }
            } else {
                // Si es un tema estándar, seleccionarlo directamente
                for (int i = 0; i < baseThemeSelector.getItemCount(); i++) {
                    if (baseThemeSelector.getItemAt(i).nombreDisplay().equals(currentInfo.nombreDisplay())) {
                        baseThemeSelector.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        // Inicializar los selectores de color con el ESTADO ACTUAL del UIManager
        // (no con los defaults de un LAF fresco), para que el diálogo refleje el tema aplicado.
        initializeFromCurrentTheme();
        isInitializing = false;
    } // ---FIN de metodo [Constructor ThemeCustomizerDialog]---

    private JPanel createBarsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = -1;

        addColorPickerRow(panel, gbc, "Menú Fondo:", 0, "MenuBar.background");
        addColorPickerRow(panel, gbc, "Menú Texto:", 0, "MenuBar.foreground");
        gbc.gridy++; panel.add(new javax.swing.JSeparator(), gbc);
        addColorPickerRow(panel, gbc, "Botones Fondo:", 0, "ToolBar.background");
        addColorPickerRow(panel, gbc, "Botones Texto:", 0, "Button.foreground");
        
        gbc.gridy++; gbc.weighty = 1.0; panel.add(new JPanel(), gbc);
        return panel;
    }

    private JPanel createStatusBarsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = -1;

        addColorPickerRow(panel, gbc, "Status Bar Fondo:", 1, "Visor.statusBarBackground");
        addColorPickerRow(panel, gbc, "Status Bar Texto:", 1, "Visor.statusBarForeground");
        gbc.gridy++; panel.add(new javax.swing.JSeparator(), gbc);
        addColorPickerRow(panel, gbc, "TextBox Fondo:", 1, "TextField.background");
        addColorPickerRow(panel, gbc, "TextBox Texto:", 1, "TextField.foreground");
        
        gbc.gridy++; gbc.weighty = 1.0; panel.add(new JPanel(), gbc);
        return panel;
    }

    private JPanel createMainTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = -1;

        addColorPickerRow(panel, gbc, "Lista Fondo:", 2, "List.background");
        addColorPickerRow(panel, gbc, "Lista Texto:", 2, "List.foreground");
        gbc.gridy++; panel.add(new javax.swing.JSeparator(), gbc);
        addColorPickerRow(panel, gbc, "Visor Fondo:", 2, "windowBackground");
        
        gbc.gridy++; gbc.weighty = 1.0; panel.add(new JPanel(), gbc);
        return panel;
    }

    private JPanel createThumbTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = -1;

        addColorPickerRow(panel, gbc, "Miniaturas Fondo:", 3, "Panel.background");
        addColorPickerRow(panel, gbc, "Miniaturas Texto:", 3, "Label.foreground");
        
        gbc.gridy++; gbc.weighty = 1.0; panel.add(new JPanel(), gbc);
        return panel;
    }

    private JPanel createAccentTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = -1;

        addColorPickerRow(panel, gbc, "Color de Acento:", 4, "Component.accentColor");
        addColorPickerRow(panel, gbc, "Fondo Selección:", 4, "List.selectionBackground");
        addColorPickerRow(panel, gbc, "Texto Selección:", 4, "List.selectionForeground");
        addColorPickerRow(panel, gbc, "Borde de Foco:", 4, "Component.focusColor", "Component.focusedBorderColor");
        addColorPickerRow(panel, gbc, "Imagen Marcada:", 4, "Visor.markedImageBorder");
        
        gbc.gridy++; gbc.weighty = 1.0; panel.add(new JPanel(), gbc);
        return panel;
    }
    
    private void handleZoneClicked(String propertyKey) {
        String labelToFind = null;
        for (Map.Entry<String, String[]> entry : labelToKeysMap.entrySet()) {
            for (String key : entry.getValue()) {
                if (key.equals(propertyKey)) {
                    labelToFind = entry.getKey();
                    break;
                }
            }
            if (labelToFind != null) break;
        }

        if (labelToFind != null) {
            // Cambiar a la pestaña correcta
            int tabIndex = labelToTabIndexMap.getOrDefault(labelToFind, 0);
            tabbedPane.setSelectedIndex(tabIndex);
            
            JButton button = labelToButtonMap.get(labelToFind);
            if (button != null) {
                button.doClick();
            }
        }
    }
    
    // ... (el resto de la clase no necesita cambios)
    private void updateColorPreviews() {
        // No disparar durante la inicialización del diálogo
        if (isInitializing) return;

        ThemeManager.ThemeInfo selectedThemeInfo = (ThemeManager.ThemeInfo) baseThemeSelector.getSelectedItem();
        if (selectedThemeInfo == null) return;

        // El usuario ha cambiado el tema BASE: limpiamos las personalizaciones anteriores
        // y mostramos los colores por defecto del nuevo tema base.
        customColors.clear();

        try {
            LookAndFeel laf = selectedThemeInfo.lafSupplier().get();
            if (laf instanceof FlatLaf) {
                UIDefaults defaults = ((FlatLaf) laf).getDefaults();
                for (Map.Entry<String, String[]> entry : labelToKeysMap.entrySet()) {
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

                    ColorPreviewPanel rowPreview = colorPreviewPanels.get(labelText);
                    if (rowPreview != null) {
                        rowPreview.setColor(foundColor);
                    }
                }
                refreshThemePreviewFromPickers();
            }
        } catch (Exception e) {
            logger.error("No se pudieron cargar los colores por defecto para el tema: " + selectedThemeInfo.nombreDisplay(), e);
        }
    } // ---FIN de metodo [updateColorPreviews]---

    /**
     * Inicializa los selectores de color con el estado ACTUAL del UIManager,
     * que refleja el tema realmente aplicado en la aplicación en este momento.
     * Se llama una única vez al abrir el diálogo.
     */
    private void initializeFromCurrentTheme() {
        for (Map.Entry<String, String[]> entry : labelToKeysMap.entrySet()) {
            String labelText = entry.getKey();
            String[] keys = entry.getValue();
            Color foundColor = null;

            // Leer el color actual del UIManager (estado real aplicado)
            for (String key : keys) {
                foundColor = UIManager.getColor(key);
                if (foundColor != null) break;
            }

            ColorPreviewPanel rowPreview = colorPreviewPanels.get(labelText);
            if (rowPreview != null) {
                rowPreview.setColor(foundColor);
            }

            // Pre-poblar customColors con el estado actual para que "Aplicar"
            // preserve todos los colores incluso sin cambiar nada
            if (foundColor != null) {
                for (String key : keys) {
                    customColors.put(key, foundColor);
                }
            }
        }
        refreshThemePreviewFromPickers();
    } // ---FIN de metodo [initializeFromCurrentTheme]---

    /** Actualiza el panel de vista previa con los colores actuales de los selectores. */
    private void refreshThemePreviewFromPickers() {
        if (themePreview == null) return;
        Map<String, Color> previewColors = new HashMap<>();
        for (Map.Entry<String, String[]> entry : labelToKeysMap.entrySet()) {
            ColorPreviewPanel panel = colorPreviewPanels.get(entry.getKey());
            Color c = (panel != null) ? panel.getColor() : null;
            if (c != null) {
                for (String key : entry.getValue()) {
                    previewColors.put(key, c);
                }
            }
        }
        themePreview.setColors(previewColors);
    } // ---FIN de metodo [refreshThemePreviewFromPickers]---


    private void addColorPickerRow(JPanel parent, GridBagConstraints gbc, String labelText, int tabIndex, String... propertyKeys) {
        labelToTabIndexMap.put(labelText, tabIndex);
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.LINE_END;
        parent.add(new JLabel(labelText), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.LINE_START;
        
        JPanel colorPanel = new JPanel(new BorderLayout(10, 0));
        
        ColorPreviewPanel colorPreview = new ColorPreviewPanel();
        colorPreview.setBorder(BorderFactory.createEtchedBorder());
        colorPreview.setPreferredSize(new Dimension(80, 20));
        
        colorPreviewPanels.put(labelText, colorPreview);
        labelToKeysMap.put(labelText, propertyKeys);

        JButton changeButton = new JButton("Cambiar...");
        labelToButtonMap.put(labelText, changeButton);
        changeButton.addActionListener(e -> {
            Color currentColor = colorPreview.getColor();
            Color newColor = JColorChooser.showDialog(this, "Elige un color para: " + labelText, currentColor);
            if (newColor != null) {
                colorPreview.setColor(newColor);
                for (String key : propertyKeys) {
                    customColors.put(key, newColor);
                }
                // Solo actualizamos la vista previa interna; la app real no se toca hasta "Aplicar"
                refreshThemePreviewFromPickers();
            }
        });

        colorPanel.add(colorPreview, BorderLayout.CENTER);
        colorPanel.add(changeButton, BorderLayout.EAST);
        parent.add(colorPanel, gbc);
    } // ---FIN de metodo [addColorPickerRow]---

    private JPanel createSeparator(String text) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), text, TitledBorder.LEFT, TitledBorder.TOP));
        return panel;
    } // ---FIN de metodo [createSeparator]---

    private void saveCustomTheme() {
        String newThemeName = customThemeNameField.getText().trim();
        if (newThemeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce un nombre para tu tema.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (customColors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No has personalizado ningún color. El tema no se guardará.", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ThemeManager.ThemeInfo baseTheme = (ThemeManager.ThemeInfo) baseThemeSelector.getSelectedItem();
        if (baseTheme == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un tema base.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String baseThemeClassName = baseTheme.lafSupplier().get().getClass().getName();

        File customThemesDir = new File(".temas_personalizados");
        if (!customThemesDir.exists()) {
            if (!customThemesDir.mkdir()) {
                logger.error("No se pudo crear el directorio .temas_personalizados");
                 JOptionPane.showMessageDialog(this, "No se pudo crear el directorio de temas personalizados.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String fileName = newThemeName.toLowerCase().replaceAll("[^a-z0-9_]", "").replaceAll("\\s+", "_") + ".properties";
        File themeFile = new File(customThemesDir, fileName);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(themeFile), StandardCharsets.UTF_8))) {
            writer.write("#= " + newThemeName);
            writer.newLine();
            writer.write("# Basado en=" + baseThemeClassName);
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, Color> entry : customColors.entrySet()) {
                String hexColor = String.format("#%02x%02x%02x", entry.getValue().getRed(), entry.getValue().getGreen(), entry.getValue().getBlue());
                writer.write(entry.getKey() + "=" + hexColor);
                writer.newLine();
            }
            
            logger.info("Tema personalizado guardado en: " + themeFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "¡Tema guardado con éxito!\n'" + newThemeName + "' estará disponible la próxima vez que inicies la aplicación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (IOException ex) {
            logger.error("Error al guardar el tema personalizado", ex);
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo del tema.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
        }
    } // ---FIN de metodo [saveCustomTheme]---

    
    private static class ThemeInfoRenderer extends DefaultListCellRenderer {
    	
        private static final long serialVersionUID = 1L;
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // Primero llamamos al método de la superclase para que configure los colores de fondo, bordes, etc.
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            // AHORA, si el valor es un ThemeInfo, sobrescribimos SOLO el texto.
            if (value instanceof ThemeManager.ThemeInfo) {
                setText(((ThemeManager.ThemeInfo) value).nombreDisplay());
            }
            
            // Devolvemos el componente (this) ya modificado.
            return this;
        } // ---FIN de metodo [getListCellRendererComponent]---
    } // ---FIN de clase [ThemeInfoRenderer]---
    
    private static class ColorPreviewPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Color definedColor;
        private static final int SQUARE_SIZE = 4;
        private static final Color CHECKER_COLOR_1 = new Color(255, 255, 255);
        private static final Color CHECKER_COLOR_2 = new Color(224, 224, 224);

        public void setColor(Color color) {
            this.definedColor = color;
            repaint();
        } // ---FIN de metodo [setColor]---

        public Color getColor() {
            return definedColor;
        } // ---FIN de metodo [getColor]---

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
                        if ((rowIdx + colIdx) % 2 == 0) {
                            g.setColor(CHECKER_COLOR_1);
                        } else {
                            g.setColor(CHECKER_COLOR_2);
                        }
                        g.fillRect(col, row, SQUARE_SIZE, SQUARE_SIZE);
                    }
                }
            }
        } // ---FIN de metodo [paintComponent]---
    } // ---FIN de clase [ColorPreviewPanel]---

} // --- FIN de clase [ThemeCustomizerDialog]---