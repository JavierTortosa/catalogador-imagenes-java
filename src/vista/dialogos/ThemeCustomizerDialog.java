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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLaf;

import vista.theme.ThemeManager;
import vista.theme.ThemeManager.ThemeCategory;

public class ThemeCustomizerDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ThemeCustomizerDialog.class);
    private final ThemeManager themeManager;
    private JComboBox<ThemeManager.ThemeInfo> baseThemeSelector;
    private JTextField customThemeNameField;
    private final Map<String, Color> customColors = new HashMap<>();
    
    private final Map<String, ColorPreviewPanel> colorPreviewPanels = new HashMap<>();
    private final Map<String, String[]> labelToKeysMap = new HashMap<>();

    public ThemeCustomizerDialog(JFrame owner, ThemeManager themeManager) {
        super(owner, "Editor de Temas Personalizados", true);
        this.themeManager = themeManager;

        setSize(550, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(new JLabel("Tema Base:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.LINE_START;
        
     // --- INICIO DE LA CORRECCIÓN ---
        java.util.List<ThemeManager.ThemeInfo> themeList = new java.util.ArrayList<>(
                themeManager.getAvailableThemes().values()
            );
            
            // Filtramos para no poder basar un tema personalizado en otro.
            themeList.removeIf(t -> t.category() == ThemeCategory.CUSTOM);
            
            // Ordenamos la lista.
            themeList.sort(java.util.Comparator.comparing(ThemeManager.ThemeInfo::nombreDisplay));
            
            // Creamos el JComboBox a partir de un array del tipo correcto.
            baseThemeSelector = new JComboBox<>(themeList.toArray(new ThemeManager.ThemeInfo[0]));
            // --- FIN DE LA CORRECCIÓN ---

            baseThemeSelector.setRenderer(new ThemeInfoRenderer());
            baseThemeSelector.addActionListener(e -> updateColorPreviews());
            mainPanel.add(baseThemeSelector, gbc);
        
            
//        ThemeManager.ThemeInfo[] themes = themeManager.getAvailableThemes().values().stream()
//                .filter(t -> t.category() != ThemeCategory.CUSTOM) // No basar un tema personalizado en otro
//                .sorted(Comparator.comparing(ThemeManager.ThemeInfo::nombreDisplay))
//                .toArray(ThemeManager.ThemeInfo[]::new);
//        baseThemeSelector = new JComboBox<>(themes);
//        
//        mainPanel.add(baseThemeSelector, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.weightx = 0; gbc.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(new JLabel("Nombre del Nuevo Tema:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.LINE_START;
        customThemeNameField = new JTextField();
        mainPanel.add(customThemeNameField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        mainPanel.add(createSeparator("Acento y Selección"), gbc);
        
        addColorPickerRow(mainPanel, gbc, "Color de Acento:", "List.selectionBackground", "Component.accentColor");
        addColorPickerRow(mainPanel, gbc, "Fondo de Selección:", "List.selectionBackground", "Table.selectionBackground", "Tree.selectionBackground");
        addColorPickerRow(mainPanel, gbc, "Texto de Selección:", "List.selectionForeground", "Table.selectionForeground", "Tree.selectionForeground");
        
        
        addColorPickerRow(mainPanel, gbc, "Borde de Foco:", 
        	    "Component.focusColor",               // Color de foco general (el principal)
        	    "Component.focusedBorderColor",       // Color para bordes cuando el componente tiene foco
        	    "List.focusCellHighlightBorder",      // ¡La clave para el borde de las celdas de JList!
        	    "Table.focusCellHighlightBorder",     // La clave para el borde de las celdas de JTable
        	    "Tree.focusCellHighlightBorder",      // La clave para el borde de los nodos de JTree
        	    "TabbedPane.focusColor",              // Color de foco para las pestañas
        	    "ComboBox.focusColor",                // Color de foco para los ComboBox
        	    "Button.focusedBorderColor",          // Color de borde de foco específico para botones
        	    "ToggleButton.focusedBorderColor"     // Color de borde de foco específico para toggle buttons
        	);
        
//        // --- INICIO DE LA CORRECCIÓN ---
//        // Añadimos la clave específica para el borde de foco de la JList.
//     // Hacemos que "Borde de Foco" sea mucho más completo, incluyendo JTable y JTabbedPane.
//        addColorPickerRow(mainPanel, gbc, "Borde de Foco:", 
//                "Component.focusColor", 
//                "Component.focusedBorderColor", 
//                "List.focusCellHighlightBorder",
//                "Table.focusCellHighlightBorder", // Clave para tablas
//                "TabbedPane.focusColor",          // Clave para pestañas
//                "ComboBox.focusColor"             // Clave para ComboBoxes
//            );
//        // --- FIN DE LA CORRECCIÓN ---
        
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        mainPanel.add(createSeparator("Fondos"), gbc);
        addColorPickerRow(mainPanel, gbc, "Fondo Principal:", "Panel.background", "windowBackground");
        addColorPickerRow(mainPanel, gbc, "Fondo Secundario (Barras):", "ToolBar.background", "MenuBar.background");
        addColorPickerRow(mainPanel, gbc, "Fondo Componentes (Texto):", "TextField.background", "ComboBox.background", "Spinner.background", "TextArea.background");

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        mainPanel.add(createSeparator("Texto y Bordes"), gbc);
        addColorPickerRow(mainPanel, gbc, "Texto Principal (Etiquetas):", "Label.foreground", "CheckBox.foreground", "RadioButton.foreground");
        addColorPickerRow(mainPanel, gbc, "Texto de Botones:", "Button.foreground");
        addColorPickerRow(mainPanel, gbc, "Borde Título:", "TitledBorder.titleColor");

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        mainPanel.add(createSeparator("Personalizados de la Aplicación"), gbc);
        addColorPickerRow(mainPanel, gbc, "Fondo Barra de Estado:", "Visor.statusBarBackground");
        addColorPickerRow(mainPanel, gbc, "Texto Barra de Estado:", "Visor.statusBarForeground");
        
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Guardar y Aplicar al Reiniciar");
        saveButton.addActionListener(e -> saveCustomTheme());
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        updateColorPreviews();
    } // ---FIN de metodo [Constructor ThemeCustomizerDialog]---
    
    // ... (el resto de la clase no necesita cambios)
    private void updateColorPreviews() {
        ThemeManager.ThemeInfo selectedThemeInfo = (ThemeManager.ThemeInfo) baseThemeSelector.getSelectedItem();
        if (selectedThemeInfo == null) return;

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
                    
                    if (foundColor == null) {
                        if (labelText.equals("Fondo Barra de Estado:")) foundColor = defaults.getColor("ToolBar.background");
                        else if (labelText.equals("Texto Barra de Estado:")) foundColor = defaults.getColor("List.selectionBackground");
                    }

                    ColorPreviewPanel previewPanel = colorPreviewPanels.get(labelText);
                    if (previewPanel != null) {
                        previewPanel.setColor(foundColor);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("No se pudieron cargar los colores por defecto para el tema: " + selectedThemeInfo.nombreDisplay(), e);
        }
    } // ---FIN de metodo [updateColorPreviews]---


    private void addColorPickerRow(JPanel parent, GridBagConstraints gbc, String labelText, String... propertyKeys) {
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
        changeButton.addActionListener(e -> {
            Color currentColor = colorPreview.getColor();
            Color newColor = JColorChooser.showDialog(this, "Elige un color para: " + labelText, currentColor);
            if (newColor != null) {
                colorPreview.setColor(newColor);
                for (String key : propertyKeys) {
                    customColors.put(key, newColor);
                }
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