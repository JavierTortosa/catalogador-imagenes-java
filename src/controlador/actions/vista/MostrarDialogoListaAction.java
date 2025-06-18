// Archivo: controlador/actions/vista/MostrarDialogoListaAction.java

package controlador.actions.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
// No se necesita importar VisorView

public class MostrarDialogoListaAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final VisorModel modelRef;
    private final VisorController controllerRef;

    public MostrarDialogoListaAction(
            String name,
            ImageIcon icon,
            VisorModel model,
            VisorController controller) {
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar un diálogo con la lista de imágenes cargadas");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_MOSTRAR_DIALOGO_LISTA);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[MostrarDialogoListaAction actionPerformed]");

        if (modelRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [MostrarDialogoListaAction]: Model o Controller nulos.");
            return;
        }

        // Obtener el frame padre desde el controlador
        JFrame mainFrame = controllerRef.getView();
        
        final JDialog dialogoLista = new JDialog(mainFrame, "Lista de Imágenes Cargadas", true);
        dialogoLista.setSize(600, 400);
        dialogoLista.setLocationRelativeTo(mainFrame);
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

        checkBoxMostrarRutas.addActionListener(evt -> {
            actualizarListaEnDialogoInterno(modeloListaDialogo, checkBoxMostrarRutas.isSelected(), modelRef);
        });

        botonCopiarLista.addActionListener(evt -> {
            controllerRef.copiarListaAlPortapapeles(modeloListaDialogo);
        });

        actualizarListaEnDialogoInterno(modeloListaDialogo, checkBoxMostrarRutas.isSelected(), modelRef);
        dialogoLista.setVisible(true);
    }

    private void actualizarListaEnDialogoInterno(DefaultListModel<String> modeloDialogo, boolean mostrarRutas, VisorModel modelFuente) {
        if (modeloDialogo == null || modelFuente == null || modelFuente.getModeloLista() == null || modelFuente.getRutaCompletaMap() == null) {
            System.err.println("ERROR [actualizarListaEnDialogoInterno]: Modelo del diálogo o modelo fuente/mapa nulos.");
            if (modeloDialogo != null) {
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
                    textoAAgregar = (rutaCompleta != null) ? rutaCompleta.toString() : claveArchivo + " (¡Ruta no encontrada!)";
                }
                modeloDialogo.addElement(textoAAgregar);
            }
        }
    }
} // --- FIN de la clase MostrarDialogoListaAction ---