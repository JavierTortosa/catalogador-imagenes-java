package controlador.managers;

import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class CarouselManager {

    // --- Dependencias ---
    private final IListCoordinator listCoordinator;
    private final VisorController controller;
    private final ComponentRegistry registry;
    private final VisorModel model;

    // --- Componentes de UI ---
    private JLabel timerLabel;
    private JScrollPane carouselThumbnails;

    // --- Estado Interno ---
    private Timer imageChangeTimer;
    private Timer countdownTimer;
    private boolean isRunning = false;
    private int delay;
    private int countdown;

    public CarouselManager(
            IListCoordinator listCoordinator, 
            VisorController controller,
            ComponentRegistry registry,
            VisorModel model) {
        
        this.listCoordinator = Objects.requireNonNull(listCoordinator, "ListCoordinator no puede ser null");
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en CarouselManager");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en CarouselManager");
        
    } // --- Fin del método CarouselManager (constructor) --- // --- Fin del método CarouselManager (constructor) ---

    private void initComponents() {
        if (this.timerLabel == null) {
            this.timerLabel = registry.get("label.estado.carouselTimer");
        }
        if (this.carouselThumbnails == null) {
            this.carouselThumbnails = registry.get("scroll.miniaturas.carousel");
        }
    } // --- Fin del método initComponents ---

    
    public void play() {
        if (isRunning) return;
        
        System.out.println("[CarouselManager] Iniciando carrusel...");
        initComponents();
        
        this.delay = model.getCarouselDelay();
        
        // --- INICIO DE LA ÚNICA MODIFICACIÓN ---
        // Si el delay es 3000ms (3s), la cuenta atrás debe empezar en 2.
        this.countdown = (delay / 1000) - 1; 
        // --- FIN DE LA ÚNICA MODIFICACIÓN ---

        // Timer principal para cambiar la imagen
        imageChangeTimer = new Timer(delay, e -> listCoordinator.seleccionarSiguiente());
        imageChangeTimer.setInitialDelay(0);

        // Timer secundario para actualizar el contador CADA segundo
        countdownTimer = new Timer(1000, e -> {
            if (timerLabel != null) {
                // Se muestra el valor actual de countdown (ej. 2, 1, 0)
                timerLabel.setText(String.format("%02d", countdown));
            }
            countdown--;
            if (countdown < 0) {
                // Cuando llega a -1, se resetea al valor inicial (ej. 2)
                countdown = (delay / 1000) - 1;
            }
        });

        imageChangeTimer.start();
        countdownTimer.start();
        isRunning = true;

        if (timerLabel != null) timerLabel.setVisible(true);
        if (carouselThumbnails != null) carouselThumbnails.setVisible(false);

        actualizarEstadoDeAcciones();
    } // --- Fin del método play ---

    public void pause() {
        if (!isRunning) return;
        
        System.out.println("[CarouselManager] Pausando carrusel.");
        if (imageChangeTimer != null) imageChangeTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        isRunning = false;

        if (carouselThumbnails != null) carouselThumbnails.setVisible(true);

        actualizarEstadoDeAcciones();
    } // --- Fin del método pause ---

    public void stop() {
        System.out.println("[CarouselManager] Deteniendo carrusel.");
        if (imageChangeTimer != null) imageChangeTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        isRunning = false;

        if (timerLabel != null) {
            timerLabel.setText("--:--");
            timerLabel.setVisible(false);
        }
        if (carouselThumbnails != null) carouselThumbnails.setVisible(true);

        actualizarEstadoDeAcciones();
    } // --- Fin del método stop ---

    private void actualizarEstadoDeAcciones() {
        if (controller.getActionMap() != null) {
            boolean hayImagenes = model.getModeloLista() != null && !model.getModeloLista().isEmpty();
            
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PLAY).setEnabled(!isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PAUSE).setEnabled(isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_STOP).setEnabled(isRunning && hayImagenes);
        }
    } // --- Fin del método actualizarEstadoDeAcciones ---

    public void onCarouselModeChanged(boolean isEntering) {
        if (isEntering) {
            stop();
        } else {
            stop();
        }
    } // --- Fin del método onCarouselModeChanged ---

} // --- FIN de la clase CarouselManager ---