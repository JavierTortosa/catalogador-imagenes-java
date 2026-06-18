package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import modelo.VisorModel;

public class ImagePropertiesDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public ImagePropertiesDialog(Frame owner, VisorModel model, List<String> tags) {
        super(owner, "Propiedades de la imagen", true);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 8, 3, 8);
        c.gridx = 0;
        c.weightx = 1;

        String key = model.getSelectedImageKey();
        Path ruta = (key != null) ? model.getRutaCompleta(key) : null;

        java.awt.image.BufferedImage img = model.getCurrentImage();
        if (img != null) {
            int thumbSize = 200;
            double scale = Math.min((double) thumbSize / img.getWidth(), (double) thumbSize / img.getHeight());
            int tw = (int) (img.getWidth() * scale);
            int th = (int) (img.getHeight() * scale);
            Image scaled = img.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
            JLabel thumbLabel = new JLabel(new ImageIcon(scaled));
            thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
            thumbLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            c.gridwidth = 2;
            c.gridy++;
            c.fill = GridBagConstraints.NONE;
            content.add(thumbLabel, c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 1;
        }

        addSection(content, c, "Archivo");
        addRow(content, c, "Nombre:", key != null ? key : "N/A");
        addRow(content, c, "Ruta:", ruta != null ? ruta.toAbsolutePath().toString() : "N/A");

        if (ruta != null && Files.exists(ruta)) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(ruta, BasicFileAttributes.class);
                long bytes = attrs.size();
                String sizeStr = formatSize(bytes);
                addRow(content, c, "Tamaño:", sizeStr + " (" + bytes + " bytes)");
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                addRow(content, c, "Modificado:", sdf.format(new Date(attrs.lastModifiedTime().toMillis())));
                addRow(content, c, "Creado:", sdf.format(new Date(attrs.creationTime().toMillis())));
            } catch (Exception ignored) {
            }
        }

        addSection(content, c, "Imagen");
        if (img != null) {
            addRow(content, c, "Dimensiones:", img.getWidth() + " × " + img.getHeight() + " px");
        }
        if (ruta != null) {
            String name = ruta.getFileName().toString();
            int dot = name.lastIndexOf('.');
            String ext = (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1).toUpperCase() : "N/A";
            addRow(content, c, "Formato:", ext);
        }

        if (tags != null && !tags.isEmpty()) {
            addSection(content, c, "Etiquetas (Tags)");
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(tag);
            }
            JTextArea tagsArea = new JTextArea(sb.toString());
            tagsArea.setEditable(false);
            tagsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            tagsArea.setBackground(content.getBackground());
            tagsArea.setLineWrap(true);
            tagsArea.setWrapStyleWord(true);
            c.gridy++;
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0.3;
            JScrollPane sp = new JScrollPane(tagsArea);
            sp.setBorder(BorderFactory.createEmptyBorder());
            content.add(sp, c);
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
        }

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        add(mainScroll, BorderLayout.CENTER);

        setSize(420, 460);
        setLocationRelativeTo(owner);
    }

    private void addSection(JPanel panel, GridBagConstraints c, String title) {
        c.gridy++;
        c.gridwidth = 2;
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
        panel.add(label, c);
        c.gridwidth = 1;
    }

    private void addRow(JPanel panel, GridBagConstraints c, String label, String value) {
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1;
        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(val, c);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

}
