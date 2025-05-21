package controlador.actions.tema;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon; // Aunque probablemente sea null para JRadioButtonMenuItem
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands; // Para el ACTION_COMMAND_KEY
import vista.VisorView; // Para ser el padre del JOptionPane
import vista.theme.ThemeManager; // Para cambiar el tema
import vista.theme.Tema; // Para obtener el nombre del tema actual

public class ToggleThemeAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ThemeManager themeManagerRef;
    private VisorView viewRef; // Para el JOptionPane
    private String nombreInternoTemaQueRepresenta; // Ej: "clear", "dark"
    private String nombreDisplayTema; // Ej: "Tema Claro", "Tema Oscuro" (para el JOptionPane)

    public ToggleThemeAction(ThemeManager themeManager, 
                             VisorView view, 
                             String nombreInternoTema, 
                             String displayName, // Este es el texto del JRadioButtonMenuItem
                             String actionCommandKey) { // El CMD_TEMA_...
        super(displayName, null); // Icono es null para JRadioButtonMenuItem generalmente

        this.themeManagerRef = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null");
        this.nombreInternoTemaQueRepresenta = Objects.requireNonNull(nombreInternoTema, "nombreInternoTema no puede ser null");
        this.nombreDisplayTema = (displayName != null && !displayName.isBlank()) ? displayName : nombreInternoTema;

        putValue(Action.SHORT_DESCRIPTION, "Establecer el " + this.nombreDisplayTema);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // Inicializar el estado SELECTED_KEY basado en el tema actual del ThemeManager
        sincronizarEstadoSeleccionConManager();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[ToggleThemeAction actionPerformed] Aplicando tema: " + nombreInternoTemaQueRepresenta + ", Comando: " + e.getActionCommand());

        if (themeManagerRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleThemeAction]: ThemeManager o VisorView nulos.");
            return;
        }

        // Intentar establecer el nuevo tema
        boolean temaRealmenteCambiado = themeManagerRef.setTemaActual(this.nombreInternoTemaQueRepresenta);

        if (temaRealmenteCambiado) {
            System.out.println("  -> Tema cambiado en ThemeManager a: " + this.nombreInternoTemaQueRepresenta);
            
            // Notificar a TODAS las ToggleThemeAction para que actualicen su SELECTED_KEY
            // Esto se hará desde VisorController después de que esta action termine.
            // Aquí, solo nos aseguramos que ESTA action esté seleccionada.
            // En realidad, el JRadioButtonMenuItem ya habrá cambiado el estado de esta Action
            // a seleccionado (true) porque el usuario hizo clic en él.
            // Así que esta línea es más para asegurar consistencia si la acción se llama programáticamente.
            // putValue(Action.SELECTED_KEY, Boolean.TRUE);


            // Notificar al usuario
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    viewRef.getFrame(),
                    "El tema se ha cambiado a '" + this.nombreDisplayTema + "'.\n" +
                    "Los cambios visuales completos se aplicarán la próxima vez\n" +
                    "que inicie la aplicación.",
                    "Cambio de Tema",
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
            
            // Es responsabilidad del VisorController (o un UIManager) llamar a un método que
            // itere sobre todas las ToggleThemeAction y llame a su sincronizarEstadoSeleccionConManager()
            // para deseleccionar las otras.

        } else {
            System.out.println("  -> El tema '" + this.nombreInternoTemaQueRepresenta + "' ya era el actual o no es válido. No se realizaron cambios en ThemeManager.");
            // Asegurar que el estado SELECTED_KEY de esta Action sea el correcto.
            sincronizarEstadoSeleccionConManager();
        }
    }

    /**
     * Actualiza el estado de selección (Action.SELECTED_KEY) de esta Action
     * basándose en si el tema que representa coincide con el tema
     * actualmente activo en el ThemeManager.
     */
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
            // Si themeManager es null, por seguridad, deseleccionar.
            if (!Objects.equals(getValue(Action.SELECTED_KEY), Boolean.FALSE)) {
                 putValue(Action.SELECTED_KEY, Boolean.FALSE);
            }
        }
    }
}