package vista.dialogos; // O el paquete que elijas

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects; // Import necesario

public class ProgresoCargaDialog extends JDialog {

    private static final long serialVersionUID = 1L; // Añadir SUID
    private JLabel etiquetaMensaje;
    private JLabel etiquetaContador;
    private JProgressBar barraProgreso;
    private JButton botonCancelar;
    private SwingWorker<?, ?> workerAsociado; // Ya no es final

    /**
     * Constructor del diálogo.
     * @param owner El frame padre (puede ser null).
     * @param worker El SwingWorker asociado (puede ser null inicialmente y asignado luego).
     */
    public ProgresoCargaDialog(Frame owner, SwingWorker<?, ?> worker) {
        // Título del diálogo y modalidad (true = bloquea la ventana padre)
        super(owner, "Cargando Lista de Archivos", true);
        // Asignación inicial del worker (puede ser null)
        this.workerAsociado = worker;

        // Crear y configurar los componentes internos
        initComponents();
        // Organizar los componentes en el layout
        setupLayout();

        // Ajustar tamaño del diálogo al contenido
        pack();
        // Centrar el diálogo respecto a la ventana padre (o pantalla si owner es null)
        setLocationRelativeTo(owner);
        // Evitar que el usuario cierre el diálogo con la 'X' (debe usar Cancelar)
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        // Impedir que el usuario cambie el tamaño del diálogo
        setResizable(false);
    }

    /**
     * Asigna el SwingWorker al diálogo, principalmente para que el botón
     * Cancelar pueda interactuar con él. También habilita/deshabilita el botón.
     * @param worker El worker que se está ejecutando o null si no hay worker.
     */
    public void setWorkerAsociado(SwingWorker<?, ?> worker) {
        System.out.println("[Dialogo Progreso] Asignando worker: " + (worker != null ? worker.getClass().getSimpleName() : "null"));
        this.workerAsociado = worker;
        // Habilitar el botón Cancelar solo si hay un worker asociado y activo
        if (this.botonCancelar != null) {
            this.botonCancelar.setEnabled(this.workerAsociado != null && !this.workerAsociado.isDone());
        }
    }

    /**
     * Inicializa los componentes Swing del diálogo (etiquetas, barra, botón).
     */
    private void initComponents() {
        // Etiqueta para mostrar mensajes de estado (ej. "Escaneando...")
        etiquetaMensaje = new JLabel("Iniciando escaneo...", SwingConstants.CENTER);
        etiquetaMensaje.setFont(etiquetaMensaje.getFont().deriveFont(Font.PLAIN)); // Fuente normal

        // Etiqueta para mostrar el contador de archivos encontrados
        etiquetaContador = new JLabel("Archivos encontrados: 000000", SwingConstants.CENTER); // Inicializar a 0
        etiquetaContador.setFont(etiquetaContador.getFont().deriveFont(Font.ITALIC, 10f)); // Fuente pequeña e itálica

        // Barra de progreso
        barraProgreso = new JProgressBar();
        barraProgreso.setIndeterminate(true); // Modo indeterminado (animación) ya que no sabemos el total al inicio
        barraProgreso.setStringPainted(false); // No mostrar texto sobre la barra

        // Botón para cancelar la operación
        botonCancelar = new JButton("Cancelar");
        // Deshabilitado inicialmente; se habilita cuando se asocia un worker activo
        botonCancelar.setEnabled(false);
        botonCancelar.setToolTipText("Detener la búsqueda de archivos");
        // Añadir ActionListener para manejar el clic en Cancelar
        botonCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Comprobar si hay un worker asociado y si aún no ha terminado
                if (workerAsociado != null && !workerAsociado.isDone()) {
                    System.out.println("[Dialogo Progreso] Botón Cancelar presionado.");
                    // Intentar cancelar el worker (true = intentar interrumpir el hilo)
                    boolean cancelado = workerAsociado.cancel(true);
                    System.out.println("  -> worker.cancel(true) llamado. Resultado: " + cancelado);
                    // Deshabilitar el botón y cambiar texto para feedback visual
                    botonCancelar.setEnabled(false);
                    botonCancelar.setText("Cancelando...");
                    // Actualizar mensaje principal
                    etiquetaMensaje.setText("Cancelando operación...");
                    // Poner la barra de progreso en modo determinado (opcional)
                    barraProgreso.setIndeterminate(false);
                    barraProgreso.setStringPainted(true);
                    barraProgreso.setString("Cancelado");
                } else {
                     System.out.println("[Dialogo Progreso] Botón Cancelar presionado, pero no hay worker activo.");
                }
            }
        });
    }

    /**
     * Organiza los componentes inicializados dentro del layout del diálogo.
     */
    private void setupLayout() {
        // Usar BorderLayout como layout principal del JDialog
        setLayout(new BorderLayout(10, 10)); // Márgenes horizontal y vertical

        // Panel central para agrupar mensajes y barra de progreso
        // Usar GridLayout para que tengan el mismo ancho y se apilen verticalmente
        JPanel panelCentral = new JPanel(new GridLayout(3, 1, 5, 5)); // 3 filas, 1 columna, espaciado V y H
        // Añadir padding alrededor del panel central
        panelCentral.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); // Arriba, Izq, Abajo, Der

        // Añadir componentes al panel central
        panelCentral.add(etiquetaMensaje);
        panelCentral.add(etiquetaContador);
        panelCentral.add(barraProgreso);

        // Panel sur para el botón Cancelar
        // Usar FlowLayout centrado
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.add(botonCancelar);

        // Añadir paneles al BorderLayout del diálogo
        add(panelCentral, BorderLayout.CENTER); // El panel con mensajes y barra va al centro
        add(panelSur, BorderLayout.SOUTH);     // El panel con el botón va abajo
    }

    /**
     * Actualiza el texto de la etiqueta del contador de forma segura
     * desde cualquier hilo (usando invokeLater si es necesario).
     * Formatea el número para tener un ancho consistente.
     * @param contador El número de archivos encontrados.
     */
    public void actualizarContador(int contador) {
        // Formatear el número (ej. con ceros a la izquierda, opcional)
        // String textoContador = String.format("Archivos encontrados: %06d", contador);
        String textoContador = "Archivos encontrados: " + contador; // Versión simple

        // Asegurar que la actualización se haga en el EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                if (etiquetaContador != null) { // Comprobar nulidad por si se cierra rápido
                     etiquetaContador.setText(textoContador);
                }
            });
        } else {
             if (etiquetaContador != null) {
                 etiquetaContador.setText(textoContador);
             }
        }
    }

    /**
     * Actualiza el mensaje principal del diálogo (ej. "Escaneando carpeta X...")
     * de forma segura desde cualquier hilo.
     * @param mensaje El nuevo mensaje a mostrar.
     */
    public void setMensaje(String mensaje) {
        // Usar Objects.requireNonNullElse para evitar null
        final String mensajeSeguro = Objects.requireNonNullElse(mensaje, "");

        // Asegurar que la actualización se haga en el EDT
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                 if (etiquetaMensaje != null) { // Comprobar nulidad
                     etiquetaMensaje.setText(mensajeSeguro);
                 }
            });
        } else {
             if (etiquetaMensaje != null) {
                 etiquetaMensaje.setText(mensajeSeguro);
             }
        }
    }

     /**
      * Cierra y libera los recursos del diálogo.
      * Debe llamarse desde el EDT. Se encarga de llamar a setVisible(false)
      * y dispose().
      */
     public void cerrar() {
         System.out.println("[Dialogo Progreso] Solicitud de cierre.");
         // Asegurar ejecución en el EDT
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::cerrar); // Llama a sí mismo en el EDT
        } else {
            // Código ejecutado en el EDT
            System.out.println("  -> [EDT] Cerrando diálogo...");
            setVisible(false); // Ocultar la ventana
            dispose();         // Liberar recursos de la ventana
            System.out.println("  -> [EDT] Diálogo cerrado y desechado.");
        }
    }
}

