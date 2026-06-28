package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;
import modelo.proyecto.Mensaje;

/**
 * Di&aacute;logo modal que muestra el historial de mensajes de un thread
 * en formato &quot;tira de papel&quot;, con capacidad de responder.
 * <p>
 * Uso:
 * <pre>
 * List&lt;Mensaje&gt; thread = imageCheckboxOverlay.getCommentThread();
 * MsgPopupDialog dlg = new MsgPopupDialog(ownerFrame, &quot;Mensajes del checkbox&quot;, thread, projectManager, () -&gt; {
 *     projectManager.notificarModificacion();
 *     tableModel.fireTableDataChanged();
 * });
 * dlg.setVisible(true);
 * </pre>
 */
public class MsgPopupDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MsgPopupDialog.class);

    private static final Color NOSOTROS_BG  = new Color(255, 243, 205);
    private static final Color NOSOTROS_FG  = new Color(133, 100, 4);
    private static final Color NOSOTROS_BORDER = new Color(255, 224, 130);
    private static final Color CLIENTE_BG   = new Color(235, 235, 235);
    private static final Color CLIENTE_FG   = new Color(60, 60, 60);
    private static final Color CLIENTE_BORDER = new Color(200, 200, 200);

    private final transient IProjectManager projectManager;
    private final transient List<Mensaje> thread;
    private final Runnable onModify;

    private final JPanel messagesPanel;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JButton closeButton;


    /**
     * @param owner           ventana padre (JFrame o JDialog)
     * @param title           t&iacute;tulo del di&aacute;logo
     * @param thread          lista viva de Mensaje (se modifica directamente)
     * @param projectManager  para notificar modificaciones
     * @param onModify        callback tras a&ntilde;adir/editar/borrar mensajes
     */
    public MsgPopupDialog(Window owner, String title,
                          List<Mensaje> thread,
                          IProjectManager projectManager,
                          Runnable onModify) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.thread = thread;
        this.projectManager = projectManager;
        this.onModify = onModify;

        setSize(380, 420);
        setLocationRelativeTo(owner);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Layout
        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Centro: tira de papel con los mensajes
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Historial"));
        content.add(scrollPane, BorderLayout.CENTER);

        // Sur: �rea de texto + botones
        JPanel south = new JPanel(new BorderLayout(4, 4));

        inputArea = new JTextArea(3, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane taScroll = new JScrollPane(inputArea);
        taScroll.setBorder(BorderFactory.createTitledBorder("Responder"));
        south.add(taScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new BorderLayout(4, 0));
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(ev -> enviarMensaje());
        closeButton = new JButton("Cerrar");
        closeButton.addActionListener(ev -> dispose());
        btnPanel.add(sendButton, BorderLayout.WEST);
        btnPanel.add(closeButton, BorderLayout.EAST);
        south.add(btnPanel, BorderLayout.SOUTH);

        content.add(south, BorderLayout.SOUTH);

        add(content);

        // Pre-cargar mensajes
        refrescarMensajes();
    } // --- Fin de metodo MsgPopupDialog (constructor) ---


    private void enviarMensaje() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;
        thread.add(new Mensaje("nosotros", text));
        inputArea.setText("");
        if (projectManager != null) {
            projectManager.notificarModificacion();
        }
        if (onModify != null) {
            onModify.run();
        }
        refrescarMensajes();
    } // --- FIN de metodo enviarMensaje ---


    private void refrescarMensajes() {
        messagesPanel.removeAll();
        if (thread == null || thread.isEmpty()) {
            JLabel empty = new JLabel("  (sin mensajes)  ");
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            messagesPanel.add(Box.createVerticalGlue());
            messagesPanel.add(empty);
            messagesPanel.add(Box.createVerticalGlue());
        } else {
            for (int i = 0; i < thread.size(); i++) {
                Mensaje msg = thread.get(i);
                messagesPanel.add(crearBurbujaMensaje(msg, i));
                messagesPanel.add(Box.createVerticalStrut(4));
            }
        }
        messagesPanel.revalidate();
        messagesPanel.repaint();
        // Scroll al final
        var parent = messagesPanel.getParent();
        if (parent instanceof JScrollPane sp) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());
            });
        }

        // Pre-fill textarea si el �ltimo mensaje es del cliente
        if (thread != null && !thread.isEmpty()) {
            Mensaje last = thread.get(thread.size() - 1);
            if ("cliente".equals(last.de())) {
                inputArea.setText(last.texto());
            }
        }
    } // --- FIN de metodo refrescarMensajes ---


    private Component crearBurbujaMensaje(Mensaje msg, int index) {
        boolean isNosotros = "nosotros".equals(msg.de());

        JPanel bubble = new JPanel(new BorderLayout(4, 2));
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isNosotros ? NOSOTROS_BORDER : CLIENTE_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        bubble.setBackground(isNosotros ? NOSOTROS_BG : CLIENTE_BG);
        bubble.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));

        // Etiqueta del remitente
        JLabel senderLabel = new JLabel(isNosotros ? "  TALLER" : "CLIENTE  ");
        senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 11f));
        senderLabel.setForeground(isNosotros ? NOSOTROS_FG : CLIENTE_FG);
        senderLabel.setHorizontalAlignment(isNosotros ? JLabel.LEFT : JLabel.RIGHT);
        bubble.add(senderLabel, BorderLayout.NORTH);

        // Texto del mensaje
        JTextArea textArea = new JTextArea(msg.texto());
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setBackground(isNosotros ? NOSOTROS_BG : CLIENTE_BG);
        textArea.setForeground(isNosotros ? NOSOTROS_FG : CLIENTE_FG);
        textArea.setFont(textArea.getFont().deriveFont(12f));
        textArea.setBorder(null);
        textArea.setRows(1);
        bubble.add(textArea, BorderLayout.CENTER);

        // Click → copiar al textbox
        bubble.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                inputArea.setText(msg.texto());
            }
        });

        // Popup para editar/borrar mensajes "nosotros"
        if (isNosotros) {
            bubble.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) mostrarPopup(e, index);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) mostrarPopup(e, index);
                }
            });
        }

        return bubble;
    } // --- FIN de metodo crearBurbujaMensaje ---


    private void mostrarPopup(MouseEvent e, int index) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Editar");
        editItem.addActionListener(ev -> {
            Mensaje msg = thread.get(index);
            String nuevo = JOptionPane.showInputDialog(this,
                    "Editar mensaje:", msg.texto());
            if (nuevo != null) {
                if (nuevo.trim().isEmpty()) {
                    thread.remove(index);
                } else {
                    thread.set(index, new Mensaje("nosotros", nuevo.trim()));
                }
                if (projectManager != null) projectManager.notificarModificacion();
                if (onModify != null) onModify.run();
                refrescarMensajes();
            }
        });
        popup.add(editItem);

        JMenuItem deleteItem = new JMenuItem("Borrar");
        deleteItem.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "\u00bfBorrar este mensaje?",
                    "Borrar mensaje", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                thread.remove(index);
                if (projectManager != null) projectManager.notificarModificacion();
                if (onModify != null) onModify.run();
                refrescarMensajes();
            }
        });
        popup.add(deleteItem);

        popup.show(e.getComponent(), e.getX(), e.getY());
    } // --- FIN de metodo mostrarPopup ---

} // --- FIN de clase MsgPopupDialog ---
