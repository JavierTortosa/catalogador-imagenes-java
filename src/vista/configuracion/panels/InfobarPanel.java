package vista.configuracion.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.configuracion.ConfigurationPanel;

public class InfobarPanel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
	// Superior
    private final JCheckBox chkSupVisible = new JCheckBox("Mostrar Panel de Información");
    private final JCheckBox chkSupNombreRuta = new JCheckBox("Nombre/Ruta Archivo");
    private final JComboBox<String> cmbSupFormato = new JComboBox<>(new String[]{"solo_nombre", "ruta_completa"});
    private final JCheckBox chkSupIndice = new JCheckBox("Índice/Total Imágenes");
    private final JCheckBox chkSupDimensiones = new JCheckBox("Dimensiones Originales");
    private final JCheckBox chkSupTamArchivo = new JCheckBox("Tamaño de Archivo");
    private final JCheckBox chkSupFecha = new JCheckBox("Fecha de Archivo");
    private final JCheckBox chkSupFormato = new JCheckBox("Formato de Imagen");
    private final JCheckBox chkSupModoZoom = new JCheckBox("Modo de Zoom");
    private final JCheckBox chkSupZoomReal = new JCheckBox("% Zoom Real");

    // Inferior
    private final JCheckBox chkInfVisible = new JCheckBox("Mostrar Panel de Control");
    private final JCheckBox chkInfNombreRuta = new JCheckBox("Nombre/Ruta Archivo");
    private final JComboBox<String> cmbInfFormato = new JComboBox<>(new String[]{"solo_nombre", "ruta_completa"});
    private final JCheckBox chkInfIconoZm = new JCheckBox("Icono Zoom Manual");
    private final JCheckBox chkInfIconoProp = new JCheckBox("Icono Proporciones");
    private final JCheckBox chkInfIconoSubc = new JCheckBox("Icono Subcarpetas");
    private final JCheckBox chkInfCtrlZoomPct = new JCheckBox("Control % Zoom");
    private final JCheckBox chkInfCtrlModoZoom = new JCheckBox("Control Modo Zoom");
    private final JCheckBox chkInfMensajes = new JCheckBox("Área de Mensajes");

    // Initial state
    private boolean iSupVisible, iSupNombreRuta, iSupIndice, iSupDimensiones;
    private boolean iSupTamArchivo, iSupFecha, iSupFormato, iSupModoZoom, iSupZoomReal;
    private String iSupFormatoStr;
    private boolean iInfVisible, iInfNombreRuta, iInfIconoZm, iInfIconoProp, iInfIconoSubc;
    private boolean iInfCtrlZoomPct, iInfCtrlModoZoom, iInfMensajes;
    private String iInfFormatoStr;

    public InfobarPanel(ConfigurationManager config) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Paneles de Información"));
        initComponents();
        load(config);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Barra Superior", createSuperiorPanel());
        tabs.addTab("Barra Inferior", createInferiorPanel());

        gbc.gridx = 0; gbc.gridy = 0;
        add(tabs, gbc);
    }

    private JPanel createSuperiorPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        p.add(chkSupVisible, g);

        g.gridy = 1;
        p.add(chkSupNombreRuta, g);
        g.gridy = 2; g.gridx = 1; g.gridwidth = 1;
        p.add(new JLabel("Formato:"), g);
        g.gridx = 2;
        cmbSupFormato.setPreferredSize(new java.awt.Dimension(140, 24));
        p.add(cmbSupFormato, g);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 3;
        p.add(chkSupIndice, g);
        g.gridy = 4;
        p.add(chkSupDimensiones, g);
        g.gridy = 5;
        p.add(chkSupTamArchivo, g);
        g.gridy = 6;
        p.add(chkSupFecha, g);
        g.gridy = 7;
        p.add(chkSupFormato, g);
        g.gridy = 8;
        p.add(chkSupModoZoom, g);
        g.gridy = 9;
        p.add(chkSupZoomReal, g);

        // spacer
        g.gridy = 10; g.weighty = 1.0;
        p.add(new JPanel(), g);

        return p;
    }

    private JPanel createInferiorPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        p.add(chkInfVisible, g);

        g.gridy = 1;
        p.add(chkInfNombreRuta, g);
        g.gridy = 2; g.gridx = 1; g.gridwidth = 1;
        p.add(new JLabel("Formato:"), g);
        g.gridx = 2;
        cmbInfFormato.setPreferredSize(new java.awt.Dimension(140, 24));
        p.add(cmbInfFormato, g);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 3;
        p.add(chkInfIconoZm, g);
        g.gridy = 4;
        p.add(chkInfIconoProp, g);
        g.gridy = 5;
        p.add(chkInfIconoSubc, g);
        g.gridy = 6;
        p.add(chkInfCtrlZoomPct, g);
        g.gridy = 7;
        p.add(chkInfCtrlModoZoom, g);
        g.gridy = 8;
        p.add(chkInfMensajes, g);

        g.gridy = 9; g.weighty = 1.0;
        p.add(new JPanel(), g);

        return p;
    }

    @Override
    public void load(ConfigurationManager config) {
        // Superior
        iSupVisible = config.getBoolean(ConfigKeys.INFOBAR_SUP_VISIBLE, true);
        iSupNombreRuta = config.getBoolean(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, true);
        iSupFormatoStr = config.getString(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, "solo_nombre");
        iSupIndice = config.getBoolean(ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, true);
        iSupDimensiones = config.getBoolean(ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, true);
        iSupTamArchivo = config.getBoolean(ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, true);
        iSupFecha = config.getBoolean(ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, true);
        iSupFormato = config.getBoolean(ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, true);
        iSupModoZoom = config.getBoolean(ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, true);
        iSupZoomReal = config.getBoolean(ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, true);

        chkSupVisible.setSelected(iSupVisible);
        chkSupNombreRuta.setSelected(iSupNombreRuta);
        cmbSupFormato.setSelectedItem(iSupFormatoStr);
        chkSupIndice.setSelected(iSupIndice);
        chkSupDimensiones.setSelected(iSupDimensiones);
        chkSupTamArchivo.setSelected(iSupTamArchivo);
        chkSupFecha.setSelected(iSupFecha);
        chkSupFormato.setSelected(iSupFormato);
        chkSupModoZoom.setSelected(iSupModoZoom);
        chkSupZoomReal.setSelected(iSupZoomReal);

        // Inferior
        iInfVisible = config.getBoolean(ConfigKeys.INFOBAR_INF_VISIBLE, true);
        iInfNombreRuta = config.getBoolean(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, true);
        iInfFormatoStr = config.getString(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "solo_nombre");
        iInfIconoZm = config.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, true);
        iInfIconoProp = config.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, true);
        iInfIconoSubc = config.getBoolean(ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, true);
        iInfCtrlZoomPct = config.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, true);
        iInfCtrlModoZoom = config.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, true);
        iInfMensajes = config.getBoolean(ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE, true);

        chkInfVisible.setSelected(iInfVisible);
        chkInfNombreRuta.setSelected(iInfNombreRuta);
        cmbInfFormato.setSelectedItem(iInfFormatoStr);
        chkInfIconoZm.setSelected(iInfIconoZm);
        chkInfIconoProp.setSelected(iInfIconoProp);
        chkInfIconoSubc.setSelected(iInfIconoSubc);
        chkInfCtrlZoomPct.setSelected(iInfCtrlZoomPct);
        chkInfCtrlModoZoom.setSelected(iInfCtrlModoZoom);
        chkInfMensajes.setSelected(iInfMensajes);
    }

    @Override
    public boolean save(ConfigurationManager config) {
        boolean changed = false;
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_VISIBLE, chkSupVisible.isSelected(), iSupVisible);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_VISIBLE, chkSupNombreRuta.isSelected(), iSupNombreRuta);
        String supFmt = (String) cmbSupFormato.getSelectedItem();
        if (!supFmt.equals(iSupFormatoStr)) {
            config.setString(ConfigKeys.INFOBAR_SUP_NOMBRE_RUTA_FORMATO, supFmt);
            iSupFormatoStr = supFmt; changed = true;
        }
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_INDICE_TOTAL_VISIBLE, chkSupIndice.isSelected(), iSupIndice);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_DIMENSIONES_VISIBLE, chkSupDimensiones.isSelected(), iSupDimensiones);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_TAMANO_ARCHIVO_VISIBLE, chkSupTamArchivo.isSelected(), iSupTamArchivo);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_FECHA_ARCHIVO_VISIBLE, chkSupFecha.isSelected(), iSupFecha);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_FORMATO_IMAGEN_VISIBLE, chkSupFormato.isSelected(), iSupFormato);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_MODO_ZOOM_VISIBLE, chkSupModoZoom.isSelected(), iSupModoZoom);
        changed |= saveBool(config, ConfigKeys.INFOBAR_SUP_ZOOM_REAL_PCT_VISIBLE, chkSupZoomReal.isSelected(), iSupZoomReal);

        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_VISIBLE, chkInfVisible.isSelected(), iInfVisible);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, chkInfNombreRuta.isSelected(), iInfNombreRuta);
        String infFmt = (String) cmbInfFormato.getSelectedItem();
        if (!infFmt.equals(iInfFormatoStr)) {
            config.setString(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, infFmt);
            iInfFormatoStr = infFmt; changed = true;
        }
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, chkInfIconoZm.isSelected(), iInfIconoZm);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, chkInfIconoProp.isSelected(), iInfIconoProp);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, chkInfIconoSubc.isSelected(), iInfIconoSubc);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, chkInfCtrlZoomPct.isSelected(), iInfCtrlZoomPct);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, chkInfCtrlModoZoom.isSelected(), iInfCtrlModoZoom);
        changed |= saveBool(config, ConfigKeys.INFOBAR_INF_MENSAJES_APP_VISIBLE, chkInfMensajes.isSelected(), iInfMensajes);
        return changed;
    }

    private boolean saveBool(ConfigurationManager cfg, String key, boolean newVal, boolean oldVal) {
        if (newVal != oldVal) {
            cfg.setString(key, String.valueOf(newVal));
            return true;
        }
        return false;
    }

    @Override
    public boolean isModified() {
        return chkSupVisible.isSelected() != iSupVisible
            || chkSupNombreRuta.isSelected() != iSupNombreRuta
            || !cmbSupFormato.getSelectedItem().equals(iSupFormatoStr)
            || chkSupIndice.isSelected() != iSupIndice
            || chkSupDimensiones.isSelected() != iSupDimensiones
            || chkSupTamArchivo.isSelected() != iSupTamArchivo
            || chkSupFecha.isSelected() != iSupFecha
            || chkSupFormato.isSelected() != iSupFormato
            || chkSupModoZoom.isSelected() != iSupModoZoom
            || chkSupZoomReal.isSelected() != iSupZoomReal
            || chkInfVisible.isSelected() != iInfVisible
            || chkInfNombreRuta.isSelected() != iInfNombreRuta
            || !cmbInfFormato.getSelectedItem().equals(iInfFormatoStr)
            || chkInfIconoZm.isSelected() != iInfIconoZm
            || chkInfIconoProp.isSelected() != iInfIconoProp
            || chkInfIconoSubc.isSelected() != iInfIconoSubc
            || chkInfCtrlZoomPct.isSelected() != iInfCtrlZoomPct
            || chkInfCtrlModoZoom.isSelected() != iInfCtrlModoZoom
            || chkInfMensajes.isSelected() != iInfMensajes;
    }

    @Override
    public String getTitle() {
        return "Paneles de Información";
    }

}
