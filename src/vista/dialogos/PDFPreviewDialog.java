package vista.dialogos;

import modelo.proyecto.ExportItem;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class PDFPreviewDialog extends JDialog {

    private final List<ExportItem> items;
    private final JPanel listPanel;
    private final JLabel infoLabel;
    private boolean confirmed = false;

    private static final int THUMB_W = 140;
    private static final int THUMB_H = 105;
    private static final int ITEMS_PER_PAGE = 4;

    public PDFPreviewDialog(JFrame owner, List<ExportItem> items) {
        super(owner, "Vista previa del catálogo PDF", true);
        this.items = items;

        setLayout(new BorderLayout(10, 10));

        infoLabel = new JLabel();
        updateInfo();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(infoLabel, BorderLayout.WEST);

        JButton btnRemoveAll = new JButton("Quitar todas");
        btnRemoveAll.addActionListener(e -> {
            items.clear();
            rebuildList();
            updateInfo();
        });
        topPanel.add(btnRemoveAll, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        rebuildList();

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnGenerate = new JButton("Generar PDF");
        btnGenerate.addActionListener(e -> {
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay imágenes para generar el PDF.");
                return;
            }
            confirmed = true;
            dispose();
        });
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        bottomPanel.add(btnGenerate);
        bottomPanel.add(btnCancel);
        add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(950, 650));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateInfo() {
        int paginas = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);
        infoLabel.setText(String.format("Imágenes: %d  |  Páginas estimadas: %d", items.size(), paginas));
    }

    private void rebuildList() {
        listPanel.removeAll();
        for (int i = 0; i < items.size(); i++) {
            listPanel.add(new ItemCard(items.get(i)));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private class ItemCard extends JPanel {
        private final JTextField piezasField;
        private final JTextField lvlField;
        private final JTextField pvpField;
        private final JTextField notasField;
        private final ExportItem item;

        ItemCard(ExportItem item) {
            this.item = item;
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, THUMB_H + 30));

            // Imagen
            ImageIcon icon = loadThumbnail(item.getRutaImagen().toFile(), THUMB_W, THUMB_H);
            JLabel thumbLabel = new JLabel(icon, SwingConstants.CENTER);
            thumbLabel.setPreferredSize(new Dimension(THUMB_W + 10, THUMB_H));
            add(thumbLabel, BorderLayout.WEST);

            // Campos
            JPanel fieldsPanel = new JPanel(new GridLayout(0, 4, 8, 4));
            fieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

            fieldsPanel.add(new JLabel("Código:"));
            JLabel codeLabel = new JLabel(item.getCodigoCatalogo() != null ? item.getCodigoCatalogo() : "---");
            codeLabel.setFont(codeLabel.getFont().deriveFont(Font.BOLD, 12f));
            fieldsPanel.add(codeLabel);

            fieldsPanel.add(new JLabel("Piezas:"));
            piezasField = new JTextField(item.getPiezas() > 0 ? String.valueOf(item.getPiezas()) : "");
            fieldsPanel.add(piezasField);

            fieldsPanel.add(new JLabel("LVL:"));
            lvlField = new JTextField(item.getLvl() != null ? item.getLvl() : "");
            fieldsPanel.add(lvlField);

            fieldsPanel.add(new JLabel("PVP:"));
            pvpField = new JTextField(item.getPvp() != null ? item.getPvp() : "");
            fieldsPanel.add(pvpField);

            fieldsPanel.add(new JLabel("Notas:"));
            notasField = new JTextField(item.getNotas() != null ? item.getNotas() : "");
            fieldsPanel.add(notasField);

            DocumentListener dl = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { saveFields(); }
                public void removeUpdate(DocumentEvent e) { saveFields(); }
                public void changedUpdate(DocumentEvent e) { saveFields(); }
            };
            piezasField.getDocument().addDocumentListener(dl);
            lvlField.getDocument().addDocumentListener(dl);
            pvpField.getDocument().addDocumentListener(dl);
            notasField.getDocument().addDocumentListener(dl);

            add(fieldsPanel, BorderLayout.CENTER);

            // Botón quitar
            JButton removeBtn = new JButton("✕");
            removeBtn.setFont(removeBtn.getFont().deriveFont(Font.BOLD, 12f));
            removeBtn.setPreferredSize(new Dimension(26, 26));
            removeBtn.setToolTipText("Quitar del PDF");
            removeBtn.addActionListener(e -> {
                items.remove(item);
                rebuildList();
                updateInfo();
            });
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            btnPanel.setOpaque(false);
            btnPanel.add(removeBtn);
            add(btnPanel, BorderLayout.EAST);
        }

        private void saveFields() {
            try {
                item.setPiezas(piezasField.getText().isEmpty() ? 0 : Integer.parseInt(piezasField.getText()));
            } catch (NumberFormatException e) {
            }
            item.setLvl(lvlField.getText());
            item.setPvp(pvpField.getText());
            item.setNotas(notasField.getText());
        }
    }

    private ImageIcon loadThumbnail(File file, int maxW, int maxH) {
        try {
            BufferedImage bi = ImageIO.read(file);
            if (bi == null) return createPlaceholder(maxW, maxH, "S/V");

            float scale = Math.min((float) maxW / bi.getWidth(), (float) maxH / bi.getHeight());
            if (scale > 1) scale = 1;

            int w = (int) (bi.getWidth() * scale);
            int h = (int) (bi.getHeight() * scale);

            Image scaled = bi.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return createPlaceholder(maxW, maxH, "ERROR");
        }
    }

    private ImageIcon createPlaceholder(int w, int h, String text) {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        g.drawString(text, (w - tw) / 2, h / 2 + fm.getAscent() / 2);
        g.dispose();
        return new ImageIcon(bi);
    }
}
