package vista.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import modelo.VisorModel;
import utils.ImageUtils;
import utils.StringUtils;
import vista.theme.ThemeManager;

public class PolaroidDisplayPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final VisorModel model;
    private final ImageDisplayPanel imagePanel;

    private final JLabel valueNombre;
    private final JLabel valueTipo;
    private final JLabel valueTamano;
    private final JLabel valueFecha;
    private final JLabel valueUbicacion;
    private final JLabel valueIdx;
    private final JLabel valueDimensiones;
    private final JLabel valueTags;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm");

    public PolaroidDisplayPanel(ThemeManager themeManager, VisorModel model) {
        super(new BorderLayout(8, 0));
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");

        this.imagePanel = new ImageDisplayPanel(themeManager, model);
        this.imagePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setPreferredSize(new Dimension(290, 0));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 4, 8, 8),
                BorderFactory.createTitledBorder("Información de la imagen")));

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new javax.swing.BoxLayout(rowsPanel, javax.swing.BoxLayout.Y_AXIS));

        this.valueNombre = addInfoRow(rowsPanel, "Nombre");
        this.valueTipo = addInfoRow(rowsPanel, "Tipo (formato)");
        this.valueTamano = addInfoRow(rowsPanel, "Tamaño");
        this.valueFecha = addInfoRow(rowsPanel, "Fecha");
        this.valueUbicacion = addInfoRow(rowsPanel, "Ubicación");
        
        addSeparatorRow(rowsPanel, "----");
        
        this.valueIdx = addInfoRow(rowsPanel, "Idx");
        this.valueDimensiones = addInfoRow(rowsPanel, "Dimensiones");
        
        addSeparatorRow(rowsPanel, "------");
        
        this.valueTags = addInfoRow(rowsPanel, "Tags asignadas");

        infoPanel.add(rowsPanel, BorderLayout.NORTH);

        add(this.imagePanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);

        actualizarInformacionDesdeModelo();
    } // --- FIN de constructor [PolaroidDisplayPanel] ---

    private void addSeparatorRow(JPanel parent, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        JLabel sepLabel = new JLabel(text);
        sepLabel.setForeground(java.awt.Color.GRAY);
        row.add(sepLabel);
        parent.add(row);
    } // --- FIN de metodo [addSeparatorRow] ---

    private JLabel addInfoRow(JPanel parent, String title) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel titleLabel = new JLabel(title + ":");
        JLabel valueLabel = new JLabel("N/A");

        JPanel titleWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleWrapper.add(titleLabel);

        JPanel valueWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        valueWrapper.add(valueLabel);

        row.add(titleWrapper, BorderLayout.NORTH);
        row.add(valueWrapper, BorderLayout.CENTER);
        parent.add(row);
        return valueLabel;
    } // --- FIN de metodo [addInfoRow] ---

    public ImageDisplayPanel getImagePanel() {
        return imagePanel;
    } // --- FIN de metodo [getImagePanel] ---

    public JLabel getInternalLabel() {
        return imagePanel.getInternalLabel();
    } // --- FIN de metodo [getInternalLabel] ---

    public void setNavigationActions(javax.swing.Action prevAction, javax.swing.Action nextAction,
            javax.swing.Icon prevIcon, javax.swing.Icon nextIcon) {
        imagePanel.setNavigationActions(prevAction, nextAction, prevIcon, nextIcon);
    } // --- FIN de metodo [setNavigationActions] ---

    public void actualizarInformacionDesdeModelo() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::actualizarInformacionDesdeModelo);
            return;
        }

        String selectedKey = model.getSelectedImageKey();
        Path ruta = (selectedKey != null) ? model.getRutaCompleta(selectedKey) : null;
        int total = (model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
        int index = (selectedKey != null && model.getModeloLista() != null) ? model.getModeloLista().indexOf(selectedKey) : -1;

        valueNombre.setText(ruta != null && ruta.getFileName() != null ? ruta.getFileName().toString() : "N/A");
        valueTipo.setText(calcularTipo(ruta));
        valueTamano.setText(calcularTamano(ruta));
        valueFecha.setText(calcularFecha(ruta));
        valueUbicacion.setText(ruta != null && ruta.getParent() != null ? ruta.getParent().toString() : "N/A");
        valueIdx.setText(index >= 0 && total > 0 ? (index + 1) + "/" + total : "0/0");
        valueDimensiones.setText(model.getCurrentImage() != null
                ? model.getCurrentImage().getWidth() + "x" + model.getCurrentImage().getHeight()
                : "N/A");

        valueTags.setText("N/A");
        if (ruta != null) {
            try {
                servicios.db.ImagenDAO imagenDAO = new servicios.db.ImagenDAO();
                java.util.Optional<modelo.datos.ImagenInfo> imgOpt = imagenDAO.findImagenByPath(ruta);
                if (imgOpt.isPresent()) {
                    servicios.db.TagDAO tagDAO = new servicios.db.TagDAO();
                    java.util.List<modelo.datos.Tag> tags = tagDAO.getTagsForImage(imgOpt.get().getId());
                    if (tags != null && !tags.isEmpty()) {
                        String tagsStr = tags.stream().map(modelo.datos.Tag::getNombre).collect(java.util.stream.Collectors.joining(", "));
                        valueTags.setText("<html><div style='width:240px;'>" + tagsStr + "</div></html>");
                    }
                }
            } catch (Exception e) {
                valueTags.setText("Error");
            }
        }
    } // --- FIN de metodo [actualizarInformacionDesdeModelo] ---

    private String calcularTipo(Path ruta) {
        if (ruta == null) {
            return "N/A";
        }
        String format = ImageUtils.getImageFormat(ruta);
        if (format != null && !format.isBlank() && !"unknown".equalsIgnoreCase(format)) {
            return format.toUpperCase();
        }

        String nombre = ruta.getFileName() != null ? ruta.getFileName().toString() : ruta.toString();
        int idx = nombre.lastIndexOf('.');
        if (idx >= 0 && idx < nombre.length() - 1) {
            return nombre.substring(idx + 1).toUpperCase();
        }
        return "N/A";
    } // --- FIN de metodo [calcularTipo] ---

    private String calcularTamano(Path ruta) {
        if (ruta == null || !Files.exists(ruta)) {
            return "N/A";
        }
        try {
            return StringUtils.formatFileSize(Files.size(ruta));
        } catch (IOException ex) {
            return "Error";
        }
    } // --- FIN de metodo [calcularTamano] ---

    private String calcularFecha(Path ruta) {
        if (ruta == null || !Files.exists(ruta)) {
            return "N/A";
        }
        try {
            return dateFormat.format(new Date(Files.getLastModifiedTime(ruta).toMillis()));
        } catch (IOException ex) {
            return "Error";
        }
    } // --- FIN de metodo [calcularFecha] ---

} // --- FIN de clase [PolaroidDisplayPanel] ---
