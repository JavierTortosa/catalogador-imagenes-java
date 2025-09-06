package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vista.theme.ThemeManager;

public class ThemeCustomizerDialog extends JDialog {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ThemeCustomizerDialog.class);
    private final ThemeManager themeManager;
    private JComboBox<ThemeManager.ThemeInfo> baseThemeSelector;
    private JTextField customThemeNameField;
    private final Map<String, Color> customColors = new HashMap<>();
    private final Map<String, JLabel> colorPreviewLabels = new HashMap<>();

    public ThemeCustomizerDialog(JFrame owner, ThemeManager themeManager) {
        super(owner, "Editor de Temas Personalizados", true);
        this.themeManager = themeManager;

        setSize(450, 250);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: Tema Base
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Tema Base:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        baseThemeSelector = new JComboBox<>(themeManager.getAvailableThemes().values().toArray(new ThemeManager.ThemeInfo[0]));
        baseThemeSelector.setRenderer(new ThemeInfoRenderer());
        mainPanel.add(baseThemeSelector, gbc);

        // Fila 2: Nombre del nuevo tema
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Nombre del Nuevo Tema:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        customThemeNameField = new JTextField();
        mainPanel.add(customThemeNameField, gbc);

        // Fila 3: Borde de Foco
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        mainPanel.add(createColorPickerRow("Borde de Foco:", "List.focusCellHighlightBorder"), gbc);

        // Fila 4: Fondo de Selección
        gbc.gridy++;
        mainPanel.add(createColorPickerRow("Fondo de Selección:", "List.selectionBackground"), gbc);
        
        // Botones de acción
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Guardar y Aplicar al Reiniciar");
        saveButton.addActionListener(e -> saveCustomTheme());
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    } // ---FIN de metodo [Constructor ThemeCustomizerDialog]---

    private JPanel createColorPickerRow(String labelText, String propertyKey) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(new JLabel(labelText), BorderLayout.WEST);

        JLabel colorPreview = new JLabel();
        colorPreview.setOpaque(true);
        colorPreview.setBorder(BorderFactory.createEtchedBorder());
        colorPreview.setBackground(Color.GRAY);
        colorPreviewLabels.put(propertyKey, colorPreview);
        panel.add(colorPreview, BorderLayout.CENTER);

        JButton changeButton = new JButton("Cambiar...");
        changeButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Elige un color para: " + labelText, colorPreview.getBackground());
            if (newColor != null) {
                colorPreview.setBackground(newColor);
                customColors.put(propertyKey, newColor);
            }
        });
        panel.add(changeButton, BorderLayout.EAST);
        return panel;
    } // ---FIN de metodo [createColorPickerRow]---

    private void saveCustomTheme() {
        String newThemeName = customThemeNameField.getText().trim();
        if (newThemeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce un nombre para tu tema.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (customColors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No has personalizado ningún color.", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ThemeManager.ThemeInfo baseTheme = (ThemeManager.ThemeInfo) baseThemeSelector.getSelectedItem();
        String baseThemeClassName = baseTheme.lafSupplier().get().getClass().getName();
        
        // Crear el archivo .properties
        Properties props = new Properties();
        props.setProperty("#", " Tema personalizado '" + newThemeName + "'");
        props.setProperty("# Basado en", baseThemeClassName);
        for (Map.Entry<String, Color> entry : customColors.entrySet()) {
            String hexColor = String.format("#%02x%02x%02x", entry.getValue().getRed(), entry.getValue().getGreen(), entry.getValue().getBlue());
            props.setProperty(entry.getKey(), hexColor);
        }

        File customThemesDir = new File(".temas_personalizados");
        if (!customThemesDir.exists()) {
            customThemesDir.mkdir();
        }

        String fileName = newThemeName.toLowerCase().replaceAll("\\s+", "_") + ".properties";
        File themeFile = new File(customThemesDir, fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(themeFile), StandardCharsets.UTF_8)) {
            props.store(writer, null);
            logger.info("Tema personalizado guardado en: " + themeFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "¡Tema guardado con éxito!\n'" + newThemeName + "' estará disponible la próxima vez que inicies la aplicación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            logger.error("Error al guardar el tema personalizado", ex);
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo del tema.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
        }
    } // ---FIN de metodo [saveCustomTheme]---

    // Clase interna para mostrar nombres amigables en el JComboBox
    private static class ThemeInfoRenderer extends DefaultListCellRenderer {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ThemeManager.ThemeInfo) {
                value = ((ThemeManager.ThemeInfo) value).nombreDisplay();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    } // ---FIN de clase [ThemeInfoRenderer]---

} // --- FIN de clase [ThemeCustomizerDialog]---