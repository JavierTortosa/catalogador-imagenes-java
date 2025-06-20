package controlador.actions.tema;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame; // Importar JFrame
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controlador.VisorController; // Importar VisorController
import vista.theme.ThemeManager;
import vista.theme.Tema;
// No se necesita importar VisorView

public class ToggleThemeAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ThemeManager themeManagerRef;
    private final VisorController controllerRef; // Referencia al controlador
    private final String nombreInternoTemaQueRepresenta;
    private final String nombreDisplayTema;

    public ToggleThemeAction(ThemeManager themeManager, 
                             VisorController controller, // Recibe el controlador
                             String nombreInternoTema, 
                             String displayName,
                             String actionCommandKey) {
        super(displayName, null);

        this.themeManagerRef = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.nombreInternoTemaQueRepresenta = Objects.requireNonNull(nombreInternoTema, "nombreInternoTema no puede ser null");
        this.nombreDisplayTema = (displayName != null && !displayName.isBlank()) ? displayName : nombreInternoTema;

        putValue(Action.SHORT_DESCRIPTION, "Establecer el " + this.nombreDisplayTema);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));
        
        sincronizarEstadoSeleccionConManager();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[ToggleThemeAction actionPerformed] Aplicando tema: " + nombreInternoTemaQueRepresenta);

        if (themeManagerRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleThemeAction]: ThemeManager o VisorController nulos.");
            return;
        }

        boolean temaRealmenteCambiado = themeManagerRef.setTemaActual(this.nombreInternoTemaQueRepresenta);

        if (temaRealmenteCambiado) {
            System.out.println("  -> Tema cambiado en ThemeManager a: " + this.nombreInternoTemaQueRepresenta);
            
            // Notificar al usuario
            SwingUtilities.invokeLater(() -> {
                // Obtenemos el frame principal desde el controlador.
                JFrame mainFrame = controllerRef.getView();
                
                JOptionPane.showMessageDialog(
                    mainFrame, // Usamos el frame obtenido
                    "El tema se ha cambiado a '" + this.nombreDisplayTema + "'.\n" +
                    "Los cambios visuales completos se aplicarán la próxima vez\n" +
                    "que inicie la aplicación.",
                    "Cambio de Tema",
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
        } else {
            System.out.println("  -> El tema '" + this.nombreInternoTemaQueRepresenta + "' ya era el actual. No se realizaron cambios.");
            sincronizarEstadoSeleccionConManager();
        }
    }

    public void sincronizarEstadoSeleccionConManager() {
        if (themeManagerRef != null) {
            Tema temaActivo = themeManagerRef.getTemaActual();
            if (temaActivo != null) {
                boolean deberiaEstarSeleccionada = temaActivo.nombreInterno().equals(this.nombreInternoTemaQueRepresenta);
                if (!Objects.equals(getValue(Action.SELECTED_KEY), deberiaEstarSeleccionada)) {
                    putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
                }
            }
        } else {
            if (!Objects.equals(getValue(Action.SELECTED_KEY), Boolean.FALSE)) {
                 putValue(Action.SELECTED_KEY, Boolean.FALSE);
            }
        }
    }
} // --- FIN de la clase ToggleThemeAction ---