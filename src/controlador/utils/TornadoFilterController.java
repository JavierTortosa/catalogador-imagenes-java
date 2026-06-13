package controlador.utils;

import java.util.function.BooleanSupplier;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador reutilizable que implementa el patrón "Tornado" para un campo de texto
 * con un botón de activación/desactivación de filtro en vivo:
 * <ul>
 *   <li><b>Toggle ON</b>: filtra en vivo al escribir (con debounce configurable).</li>
 *   <li><b>Toggle OFF</b>: Enter busca la siguiente coincidencia.</li>
 *   <li>Al activar toggle: ejecuta el filtro inmediatamente.</li>
 *   <li>Al desactivar toggle: restaura la lista completa (vía restoreCallback).</li>
 * </ul>
 */
public class TornadoFilterController {

    private static final Logger logger = LoggerFactory.getLogger(TornadoFilterController.class);

    private final JTextField textField;
    private final JToggleButton toggleButton;
    private final BooleanSupplier popupVisibleSupplier;
    private final Runnable liveFilterCallback;
    private final Runnable findNextCallback;
    private final Runnable restoreCallback;
    private final Timer debounceTimer;

    /**
     * @param textField            Campo de texto asociado.
     * @param toggleButton         Botón toggle (ON = filtro en vivo).
     * @param popupVisibleSupplier Proveedor opcional que indica si el campo muestra un popup
     *                              (ej. autocompletar); si devuelve true, se omite findNext en Enter.
     * @param liveFilterCallback   Se invoca con debounce cuando cambia el texto y toggle está ON.
     * @param findNextCallback     Se invoca al pulsar Enter con toggle OFF.
     * @param restoreCallback      Se invoca al pasar toggle de ON a OFF.
     * @param debounceMs           Milisegundos de debounce para filtro en vivo.
     */
    public TornadoFilterController(JTextField textField, JToggleButton toggleButton,
                                   BooleanSupplier popupVisibleSupplier,
                                   Runnable liveFilterCallback, Runnable findNextCallback,
                                   Runnable restoreCallback, int debounceMs) {
        this.textField = textField;
        this.toggleButton = toggleButton;
        this.popupVisibleSupplier = popupVisibleSupplier;
        this.liveFilterCallback = liveFilterCallback;
        this.findNextCallback = findNextCallback;
        this.restoreCallback = restoreCallback;

        this.debounceTimer = new Timer(debounceMs, e -> {
            if (toggleButton.isSelected() && liveFilterCallback != null) {
                liveFilterCallback.run();
            }
        });
        this.debounceTimer.setRepeats(false);

        setupListeners();
    }

    private void setupListeners() {
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                String text = textField.getText();
                if (!text.isEmpty() && liveFilterCallback != null) {
                    liveFilterCallback.run();
                }
            } else {
                if (restoreCallback != null) {
                    restoreCallback.run();
                }
            }
        });

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChange(); }
            @Override public void removeUpdate(DocumentEvent e)  { onTextChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onTextChange(); }
            private void onTextChange() {
                if (toggleButton.isSelected()) {
                    debounceTimer.restart();
                }
            }
        });

        textField.addActionListener(e -> {
            if (!toggleButton.isSelected() && !textField.getText().isEmpty()) {
                if (popupVisibleSupplier != null && popupVisibleSupplier.getAsBoolean()) {
                    return;
                }
                if (findNextCallback != null) {
                    findNextCallback.run();
                }
            }
        });
    }

    public boolean isTornadoActive() {
        return toggleButton.isSelected();
    }

    /**
     * Aplica el filtro inmediatamente (sin debounce). Útil al refrescar datos.
     */
    public void applyImmediate() {
        if (toggleButton.isSelected() && liveFilterCallback != null) {
            liveFilterCallback.run();
        }
    }

    /**
     * Detiene el debounce pendiente. Útil al destruir el controlador.
     */
    public void dispose() {
        if (debounceTimer != null && debounceTimer.isRunning()) {
            debounceTimer.stop();
        }
    }
}
