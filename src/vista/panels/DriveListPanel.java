package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import modelo.datos.Disco;

/**
 * Panel que muestra la lista de unidades de disco registradas y su estado actual.
 */
public class DriveListPanel extends JPanel {

    private static final long serialVersionUID = 1L;
	private final JList<Disco> driveList;
    private final DefaultListModel<Disco> listModel;
    private Map<String, Path> connectedDrives;
    private javax.swing.JButton refreshButton;

    public DriveListPanel() {
        setLayout(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Unidades Catalogadas");
        setBorder(border);

        listModel = new DefaultListModel<>();
        driveList = new JList<>(listModel);
        driveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driveList.setCellRenderer(new DriveCellRenderer());

        JScrollPane scrollPane = new JScrollPane(driveList);
        add(scrollPane, BorderLayout.CENTER);

        // Barra de herramientas inferior
        JPanel toolbar = new JPanel(new BorderLayout());
        refreshButton = new javax.swing.JButton("Redetectar Unidades");
        toolbar.add(refreshButton, BorderLayout.CENTER);
        add(toolbar, BorderLayout.SOUTH);
    } // ---FIN de constructor [DriveListPanel]---

    public void setOnRefresh(Runnable action) {
        refreshButton.addActionListener(e -> action.run());
    }

    /**
     * Actualiza la lista de discos mostrada y el mapa de conexiones actuales.
     * @param registeredDisks Lista de discos en la BD.
     * @param connectedDrives Mapa de NumeroSerie -> Path de raíces conectadas.
     */
    public void updateDrives(List<Disco> registeredDisks, Map<String, Path> connectedDrives) {
        this.connectedDrives = connectedDrives;
        listModel.clear();
        for (Disco d : registeredDisks) {
            listModel.addElement(d);
        }
    } // ---FIN de metodo [updateDrives]---

    public JList<Disco> getDriveList() {
        return driveList;
    }

    /**
     * Renderizador personalizado para cada celda de disco.
     */
    private class DriveCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

		@Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Disco) {
                Disco disco = (Disco) value;
                boolean isConnected = connectedDrives != null && connectedDrives.containsKey(disco.getNumeroSerie());
                
                String status = isConnected ? " [CONECTADO]" : " [DESCONECTADO]";
                String root = isConnected ? "(" + connectedDrives.get(disco.getNumeroSerie()) + ")" : "";
                
                // Formatear la cantidad de imágenes para que sea más legible
                String countStr;
                long count = disco.getCantidadImagenes();
                if (count >= 1000) {
                    countStr = String.format("%.1fk", count / 1000.0);
                } else {
                    countStr = String.valueOf(count);
                }
                
                label.setText(String.format("%s %s %s (%s imgs)", disco.getNombreEtiqueta(), root, status, countStr));
                
                if (isConnected) {
                    label.setForeground(new Color(0, 150, 0)); // Verde oscuro
                } else {
                    label.setForeground(Color.GRAY);
                }
                
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
            }
            
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return label;
        }
    } // --- FIN de clase DriveCellRenderer ---

} // --- FIN de clase DriveListPanel ---
