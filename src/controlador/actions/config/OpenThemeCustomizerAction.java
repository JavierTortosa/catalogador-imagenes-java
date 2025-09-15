package controlador.actions.config;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import vista.dialogos.ThemeCustomizerDialog; // Crearemos esta clase en el siguiente paso
import vista.theme.ThemeManager;

public class OpenThemeCustomizerAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame owner;
    private final ThemeManager themeManager;

    public OpenThemeCustomizerAction(String name, JFrame owner, ThemeManager themeManager) {
        super(name);
        this.owner = owner;
        this.themeManager = themeManager;
    } // ---FIN de metodo [Constructor OpenThemeCustomizerAction]---

    @Override
    public void actionPerformed(ActionEvent e) {
        ThemeCustomizerDialog dialog = new ThemeCustomizerDialog(owner, themeManager);
        dialog.setVisible(true);
    } // ---FIN de metodo [actionPerformed]---

} // --- FIN de clase [OpenThemeCustomizerAction]---