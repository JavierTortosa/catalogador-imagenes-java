package vista.dialogos; // O el paquete que elijas

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProgresoCargaDialog extends JDialog {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel etiquetaMensaje;
    private JLabel etiquetaContador;
    private JProgressBar barraProgreso;
    private JButton botonCancelar;

    // --- INICIO CÓDIGO CORREGIDO ---
    // Quitar 'final' para poder asignarlo en setWorkerAsociado
    private SwingWorker<?, ?> workerAsociado;
    // --- FIN CÓDIGO CORREGIDO ---

    /**
     * Constructor del diálogo.
     * @param owner El frame padre.
     * @param worker El SwingWorker (puede ser null inicialmente).
     */
    public ProgresoCargaDialog(Frame owner, SwingWorker<?, ?> worker) {
        super(owner, "Cargando Lista de Archivos", true);
        // La asignación inicial aquí está bien, incluso si es null
        this.workerAsociado = worker; 

        initComponents();
        setupLayout();

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Asigna el SwingWorker al diálogo, principalmente para que el botón
     * Cancelar pueda interactuar con él.
     * @param worker El worker que se está ejecutando.
     */
    public void setWorkerAsociado(SwingWorker<?, ?> worker) {
        // Ahora esta asignación es válida porque el campo no es final
        this.workerAsociado = worker;
        if (this.botonCancelar != null) {
            this.botonCancelar.setEnabled(this.workerAsociado != null); // Habilitar si worker no es null
        }
    }

    private void initComponents() {
        // ... (igual que antes)
        etiquetaMensaje = new JLabel("Iniciando escaneo...", SwingConstants.CENTER);
        etiquetaContador = new JLabel("Archivos encontrados: 0", SwingConstants.CENTER);
        barraProgreso = new JProgressBar();
        barraProgreso.setIndeterminate(true);
        barraProgreso.setStringPainted(false);

        botonCancelar = new JButton("Cancelar");
        // Deshabilitar inicialmente por si acaso, setWorkerAsociado lo habilitará
        botonCancelar.setEnabled(false); 
        botonCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (workerAsociado != null && !workerAsociado.isDone()) {
                    System.out.println("[Dialogo Progreso] Botón Cancelar presionado.");
                    workerAsociado.cancel(true);
                    botonCancelar.setEnabled(false);
                    botonCancelar.setText("Cancelando...");
                    etiquetaMensaje.setText("Cancelando operación...");
                }
            }
        });
    }

    // ... (setupLayout, actualizarContador, setMensaje, cerrar sin cambios) ...
     private void setupLayout() { /* ... igual que antes ... */ 
        setLayout(new BorderLayout(10, 10)); 
        JPanel panelCentral = new JPanel(new GridLayout(3, 1, 5, 5)); 
        panelCentral.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); 
        panelCentral.add(etiquetaMensaje);
        panelCentral.add(etiquetaContador);
        panelCentral.add(barraProgreso);
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.add(botonCancelar);
        add(panelCentral, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);
    }
    public void actualizarContador(int contador) { /* ... igual que antes ... */
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> etiquetaContador.setText("Archivos encontrados: " + contador));
        } else {
            etiquetaContador.setText("Archivos encontrados: " + contador);
        }
    }
    public void setMensaje(String mensaje) { /* ... igual que antes ... */
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> etiquetaMensaje.setText(mensaje));
        } else {
            etiquetaMensaje.setText(mensaje);
        }
    }
     public void cerrar() { /* ... igual que antes ... */
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> { setVisible(false); dispose(); }); // Combinar en un invokeLater
        } else {
            setVisible(false);
            dispose(); 
        }
    }
}