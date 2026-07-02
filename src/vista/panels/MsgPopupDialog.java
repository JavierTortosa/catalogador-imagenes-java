package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import controlador.managers.interfaces.IProjectManager;
import modelo.proyecto.CommentThread;
import modelo.proyecto.Mensaje;

/**
 * Dialogo modal que muestra el historial de mensajes de un hilo
 * en formato "tira de papel", con capacidad de responder.
 */
public class MsgPopupDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final Color NOSOTROS_BG  = new Color(255, 243, 205);
    private static final Color NOSOTROS_FG  = new Color(133, 100, 4);
    private static final Color NOSOTROS_BORDER = new Color(255, 224, 130);
    private static final Color CLIENTE_BG   = new Color(235, 235, 235);
    private static final Color CLIENTE_FG   = new Color(60, 60, 60);
    private static final Color CLIENTE_BORDER = new Color(200, 200, 200);

    private final transient IProjectManager projectManager;
    private final transient CommentThread commentThread;
    private final int currentIteration;
    private final Runnable onModify;

    private final JPanel messagesPanel;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JButton clearButton;
    private final JButton closeButton;


    public MsgPopupDialog(Window owner, String title,
                          CommentThread commentThread,
                          IProjectManager projectManager,
                          int currentIteration,
                          Runnable onModify) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.commentThread = commentThread;
        this.projectManager = projectManager;
        this.currentIteration = currentIteration;
        this.onModify = onModify;

        setSize(380, 420);
        setLocationRelativeTo(owner);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Centro: tira de papel
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Historial"));
        content.add(scrollPane, BorderLayout.CENTER);

        // Sur: area de texto + botones
        JPanel south = new JPanel(new BorderLayout(4, 4));

        inputArea = new JTextArea(3, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarBotones(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarBotones(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarBotones(); }
        });
        JScrollPane taScroll = new JScrollPane(inputArea);
        taScroll.setBorder(BorderFactory.createTitledBorder("Responder"));
        south.add(taScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(ev -> enviarMensaje());

        clearButton = new JButton("\u2716");
        clearButton.setFont(clearButton.getFont().deriveFont(Font.BOLD, 14f));
        clearButton.setForeground(new Color(180, 40, 40));
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setFocusPainted(false);
        clearButton.setToolTipText("Descartar borrador");
        clearButton.addActionListener(ev -> {
            inputArea.setText("");
            inputArea.requestFocusInWindow();
        });

        closeButton = new JButton("Cerrar");
        closeButton.addActionListener(ev -> dispose());

        btnPanel.add(sendButton);
        btnPanel.add(clearButton);
        btnPanel.add(Box.createHorizontalGlue());
        btnPanel.add(closeButton);
        south.add(btnPanel, BorderLayout.SOUTH);

        content.add(south, BorderLayout.SOUTH);

        add(content);

        refrescarMensajes();

        // Pre-rellenar con el ultimo mensaje nuestro
        if (commentThread != null) {
            Mensaje last = commentThread.getLast();
            if (last != null && "nosotros".equals(last.de())) {
                inputArea.setText(last.texto());
            }
        }

        actualizarBotones();
        
        
        // Marcar como leído al abrir la conversación
        if (commentThread != null) {
            // Si el estado de nuestro programa es 3 (Nuevo no leído), lo pasamos a 1 (Leído no contestado)
            if (commentThread.getEstadoPr() == 3) {
                commentThread.setEstadoPr(1);   // Verde para nosotros
                commentThread.setEstadoHtml(1); // Verde para el cliente (sabe que lo hemos leído)
                
                if (projectManager != null) {
                    projectManager.notificarModificacion();
                }
                if (onModify != null) {
                    onModify.run(); // Refresca la tabla detrás del diálogo al instante
                }
            }
        }
        
    } // --- Fin de metodo MsgPopupDialog (constructor) ---


    private void actualizarBotones() {
        boolean tieneTexto = !inputArea.getText().trim().isEmpty();
        sendButton.setEnabled(tieneTexto);
        clearButton.setEnabled(tieneTexto);
    } // --- FIN de metodo actualizarBotones ---


    private void enviarMensaje() {
        String text = inputArea.getText().trim();
        if (text.isEmpty() || commentThread == null) return;
        commentThread.add("nosotros", text, currentIteration);
        
        // ---> ACTUALIZAR ESTADOS AL CONTESTAR <---
        commentThread.setEstadoPr(2);   // 2: mensaje contestado (sobre azul para nosotros)
        commentThread.setEstadoHtml(3); // 3: mensaje nuevo no leido (sobre rojo para el cliente)
        
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
        if (commentThread == null || commentThread.isEmpty()) {
            JLabel empty = new JLabel("  (sin mensajes)  ");
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            messagesPanel.add(Box.createVerticalGlue());
            messagesPanel.add(empty);
            messagesPanel.add(Box.createVerticalGlue());
        } else {
            java.util.List<Mensaje> msgs = commentThread.getMessages();
            for (int i = 0; i < msgs.size(); i++) {
                Mensaje msg = msgs.get(i);
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

        // Click -> copiar al textbox
        bubble.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                inputArea.setText(msg.texto());
            }
        });

        // Btn X para borrar (solo "nosotros" no compartidos)
        if (isNosotros && commentThread != null && !msg.isCompartido(currentIteration)) {
            JButton deleteBtn = new JButton("\u2716");
            deleteBtn.setFont(deleteBtn.getFont().deriveFont(Font.BOLD, 10f));
            deleteBtn.setForeground(new Color(180, 40, 40));
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setBorderPainted(false);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setToolTipText("Borrar este mensaje");
            
            deleteBtn.addActionListener(ev -> {
                if (commentThread.delete(index, currentIteration)) {
                    // ---> SI SE QUEDA VACÍO, ESTADOS A 0 (GRIS) <---
                    if (commentThread.isEmpty()) {
                        commentThread.setEstadoPr(0);
                        commentThread.setEstadoHtml(0);
                    }
                    
                    if (projectManager != null) projectManager.notificarModificacion();
                    if (onModify != null) onModify.run();
                    refrescarMensajes();
                }
            });
            
            
            // Colocar a la derecha del remitente
            bubble.add(deleteBtn, BorderLayout.EAST);
        }

        return bubble;
    } // --- FIN de metodo crearBurbujaMensaje ---

} // --- FIN de clase MsgPopupDialog ---
