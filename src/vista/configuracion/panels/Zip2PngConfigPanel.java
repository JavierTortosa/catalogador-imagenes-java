package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

/**
 * Panel de configuración para el sistema Zip2PNG (Modo Render).
 * Aparece como sección "Renderizado" en el diálogo de configuración avanzada.
 */
public class Zip2PngConfigPanel extends JPanel implements ConfigurationPanel {

    private final JComboBox<String> cmbMotor;
    private final JTextField txtRutaOpenscad;
    private final JTextField txtRutaBlender;
    private final JSpinner spnLimiteMb;
    private final JSpinner spnResolucion;
    private final JTextField txtCarpetaTemp;

    private String initialMotor;
    private String initialRutaOpenscad;
    private String initialRutaBlender;
    private int initialLimiteMb;
    private int initialResolucion;
    private String initialCarpetaTemp;

    public Zip2PngConfigPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Motor por defecto:"), c);

        cmbMotor = new JComboBox<>(new String[]{"awt", "openscad", "blender"});
        c.gridx = 1; c.gridy = row;
        add(cmbMotor, c);
        row++;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Ruta OpenSCAD:"), c);

        txtRutaOpenscad = new JTextField(30);
        c.gridx = 1; c.gridy = row;
        add(txtRutaOpenscad, c);
        row++;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Ruta Blender:"), c);

        txtRutaBlender = new JTextField(30);
        c.gridx = 1; c.gridy = row;
        add(txtRutaBlender, c);
        row++;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Límite MB:"), c);

        spnLimiteMb = new JSpinner(new SpinnerNumberModel(512, 1, 99999, 64));
        c.gridx = 1; c.gridy = row;
        add(spnLimiteMb, c);
        row++;

        JLabel lblResolucion = new JLabel("Resolución salida (px):");
        lblResolucion.setToolTipText(
                "Lado mayor de la imagen de salida. Aplica al render batch (Zip2PNG) "
                + "y al snapshot supersampleado del preview 3D al asignar/guardar/exportar. "
                + "Se mantiene el aspect ratio actual; no genera salida cuadrada.");
        c.gridx = 0; c.gridy = row;
        add(lblResolucion, c);

        spnResolucion = new JSpinner(new SpinnerNumberModel(1024, 512, 4096, 256));
        c.gridx = 1; c.gridy = row;
        add(spnResolucion, c);
        row++;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Carpeta temporal:"), c);

        txtCarpetaTemp = new JTextField(30);
        c.gridx = 1; c.gridy = row;
        add(txtCarpetaTemp, c);
    }

    @Override
    public void load(ConfigurationManager config) {
        initialMotor = config.getString(ConfigKeys.ZIP2PNG_MOTOR, "awt");
        cmbMotor.setSelectedItem(initialMotor);

        initialRutaOpenscad = config.getString(ConfigKeys.ZIP2PNG_RUTA_OPENSCAD, "");
        txtRutaOpenscad.setText(initialRutaOpenscad);

        initialRutaBlender = config.getString(ConfigKeys.ZIP2PNG_RUTA_BLENDER, "");
        txtRutaBlender.setText(initialRutaBlender);

        initialLimiteMb = config.getInt(ConfigKeys.ZIP2PNG_LIMITE_MB, 512);
        spnLimiteMb.setValue(initialLimiteMb);

        initialResolucion = config.getInt(ConfigKeys.ZIP2PNG_RESOLUCION_SALIDA, 1024);
        spnResolucion.setValue(initialResolucion);

        initialCarpetaTemp = config.getString(ConfigKeys.ZIP2PNG_CARPETA_TEMP,
                System.getProperty("java.io.tmpdir") + File.separator + "visor_zip2png");
        txtCarpetaTemp.setText(initialCarpetaTemp);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;

        String motor = (String) cmbMotor.getSelectedItem();
        if (!motor.equals(initialMotor)) {
            config.setString(ConfigKeys.ZIP2PNG_MOTOR, motor);
            initialMotor = motor;
            changed = true;
        }

        String rutaOpenscad = txtRutaOpenscad.getText().trim();
        if (!rutaOpenscad.equals(initialRutaOpenscad)) {
            config.setString(ConfigKeys.ZIP2PNG_RUTA_OPENSCAD, rutaOpenscad);
            initialRutaOpenscad = rutaOpenscad;
            changed = true;
        }

        String rutaBlender = txtRutaBlender.getText().trim();
        if (!rutaBlender.equals(initialRutaBlender)) {
            config.setString(ConfigKeys.ZIP2PNG_RUTA_BLENDER, rutaBlender);
            initialRutaBlender = rutaBlender;
            changed = true;
        }

        int limiteMb = (Integer) spnLimiteMb.getValue();
        if (limiteMb != initialLimiteMb) {
            config.setString(ConfigKeys.ZIP2PNG_LIMITE_MB, String.valueOf(limiteMb));
            initialLimiteMb = limiteMb;
            changed = true;
        }

        int resolucion = (Integer) spnResolucion.getValue();
        if (resolucion != initialResolucion) {
            config.setString(ConfigKeys.ZIP2PNG_RESOLUCION_SALIDA, String.valueOf(resolucion));
            initialResolucion = resolucion;
            changed = true;
        }

        String carpetaTemp = txtCarpetaTemp.getText().trim();
        if (!carpetaTemp.equals(initialCarpetaTemp)) {
            config.setString(ConfigKeys.ZIP2PNG_CARPETA_TEMP, carpetaTemp);
            initialCarpetaTemp = carpetaTemp;
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean isModified() {
        return !((String) cmbMotor.getSelectedItem()).equals(initialMotor)
                || !txtRutaOpenscad.getText().trim().equals(initialRutaOpenscad)
                || !txtRutaBlender.getText().trim().equals(initialRutaBlender)
                || !((Integer) spnLimiteMb.getValue()).equals(initialLimiteMb)
                || !((Integer) spnResolucion.getValue()).equals(initialResolucion)
                || !txtCarpetaTemp.getText().trim().equals(initialCarpetaTemp);
    }

    @Override
    public String getTitle() {
        return "Renderizado";
    }

} // --- Fin de la clase Zip2PngConfigPanel ---
