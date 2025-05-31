package controlador.actions.vista;

import java.awt.BorderLayout; // Para el layout del diálogo
import java.awt.FlowLayout;  // Para el panel superior del diálogo
import java.awt.event.ActionEvent;
import java.nio.file.Path; // Para el mapa de rutas
import java.util.Map;      // Para el mapa de rutas
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import controlador.VisorController; // Para ClipboardOwner y los métodos de ayuda
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import vista.VisorView;

public class MostrarDialogoListaAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private VisorView viewRef;
    private VisorModel modelRef;
    // Si copiarListaAlPortapapeles y actualizarListaEnDialogo se quedan en VisorController,
    // necesitaríamos una referencia a él o mover esos métodos.
    // Por ahora, para simplificar, y dado que VisorController implementa ClipboardOwner,
    // vamos a asumir que esta Action puede necesitar una referencia al controller para eso.
    // ¡PERO esto es un punto a refactorizar más adelante!
    // Lo ideal sería que la lógica de copiar y actualizar el diálogo estuviera aquí o en una clase de diálogo.
    private VisorController controllerRef; // TEMPORAL para copiarListaAlPortapapeles

    public MostrarDialogoListaAction(String name, 
                                     ImageIcon icon, 
                                     VisorView view, 
                                     VisorModel model,
                                     VisorController controller) { 
        super(name, icon);
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null para ClipboardOwner"); 

        putValue(Action.SHORT_DESCRIPTION, "Mostrar un diálogo con la lista de imágenes cargadas");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[MostrarDialogoListaAction actionPerformed]");

        if (viewRef == null || modelRef == null || controllerRef == null) { // TEMPORAL: controllerRef
            System.err.println("ERROR CRÍTICO [MostrarDialogoListaAction]: View, Model o Controller (temporal) nulos.");
            return;
        }

        // --- Lógica copiada y adaptada de VisorController.mostrarDialogoListaImagenes ---
        final JDialog dialogoLista = new JDialog(viewRef.getFrame(), "Lista de Imágenes Cargadas", true);
        dialogoLista.setSize(600, 400);
        dialogoLista.setLocationRelativeTo(viewRef.getFrame());
        dialogoLista.setLayout(new BorderLayout(5, 5));

        final DefaultListModel<String> modeloListaDialogo = new DefaultListModel<>();
        JList<String> listaImagenesDialogo = new JList<>(modeloListaDialogo);
        JScrollPane scrollPaneListaDialogo = new JScrollPane(listaImagenesDialogo);
        final JCheckBox checkBoxMostrarRutas = new JCheckBox("Mostrar Rutas Completas");
        JButton botonCopiarLista = new JButton("Copiar Lista");

        JPanel panelSuperiorDialog = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSuperiorDialog.add(botonCopiarLista);
        panelSuperiorDialog.add(checkBoxMostrarRutas);

        dialogoLista.add(panelSuperiorDialog, BorderLayout.NORTH);
        dialogoLista.add(scrollPaneListaDialogo, BorderLayout.CENTER);

        // Listener para el CheckBox
        checkBoxMostrarRutas.addActionListener(evt -> {
            actualizarListaEnDialogoInterno(modeloListaDialogo, checkBoxMostrarRutas.isSelected(), modelRef);
        });

        // Listener para el Botón Copiar
        botonCopiarLista.addActionListener(evt -> {
            // Usamos el controllerRef TEMPORALMENTE porque implementa ClipboardOwner
            // y tiene el método copiarListaAlPortapapeles.
            controllerRef.copiarListaAlPortapapeles(modeloListaDialogo); 
        });

        actualizarListaEnDialogoInterno(modeloListaDialogo, checkBoxMostrarRutas.isSelected(), modelRef);
        dialogoLista.setVisible(true);
        // --- Fin lógica copiada ---
    }

    // Método helper PRIVADO para actualizar el contenido del diálogo (antes en VisorController)
    private void actualizarListaEnDialogoInterno(DefaultListModel<String> modeloDialogo, boolean mostrarRutas, VisorModel modelFuente) {
        if (modeloDialogo == null || modelFuente == null || modelFuente.getModeloLista() == null || modelFuente.getRutaCompletaMap() == null) {
            System.err.println("ERROR [actualizarListaEnDialogoInterno]: Modelo del diálogo o modelo fuente/mapa nulos.");
            if(modeloDialogo != null) {
                modeloDialogo.clear();
                modeloDialogo.addElement("Error: Datos no disponibles.");
            }
            return;
        }

        DefaultListModel<String> modeloPrincipal = modelFuente.getModeloLista();
        Map<String, Path> mapaRutas = modelFuente.getRutaCompletaMap();
        modeloDialogo.clear();

        if (modeloPrincipal.isEmpty()) {
            modeloDialogo.addElement("(La lista principal está vacía)");
        } else {
            for (int i = 0; i < modeloPrincipal.getSize(); i++) {
                String claveArchivo = modeloPrincipal.getElementAt(i);
                String textoAAgregar = claveArchivo;
                if (mostrarRutas) {
                    Path rutaCompleta = mapaRutas.get(claveArchivo);
                    if (rutaCompleta != null) {
                        textoAAgregar = rutaCompleta.toString();
                    } else {
                        textoAAgregar = claveArchivo + " (¡Ruta no encontrada!)";
                    }
                }
                modeloDialogo.addElement(textoAAgregar);
            }
        }
    }
    
    // NOTA: Si mueves copiarListaAlPortapapeles aquí, necesitarías que MostrarDialogoListaAction
    // implemente ClipboardOwner. Por ahora, lo dejamos en VisorController y lo llamamos
    // a través de controllerRef (temporal).
}