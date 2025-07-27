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
import modelo.VisorModel.WorkMode;

public class CarouselManager {

    // --- Dependencias ---
    private final IListCoordinator listCoordinator;
    private final VisorController controller;
    private final ComponentRegistry registry;
    private final VisorModel model;

    // --- Componentes de UI (Lazy loaded) ---
    private JLabel timerLabel;
    private JScrollPane carouselThumbnails;

    // --- Estado Interno ---
    private Timer imageChangeTimer;
    private Timer countdownTimer;
    private boolean isRunning = false;
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
        
    } // --- Fin del constructor CarouselManager ---

    /**
     * Inicializa las referencias a los componentes de la UI.
     * Se llama una vez cuando se entra en el modo carrusel.
     */
    private void initComponents() {
        if (this.timerLabel == null) {
            this.timerLabel = registry.get("label.estado.carouselTimer");
        }
        if (this.carouselThumbnails == null) {
            // Usamos la clave correcta que definimos en el ViewBuilder
            this.carouselThumbnails = registry.get("scroll.miniaturas.carousel");
        }
    } // --- Fin del método initComponents ---
    
    public void play() {
        if (isRunning || model.getCurrentWorkMode() != WorkMode.CARROUSEL) return;
        
        System.out.println("[CarouselManager] Iniciando carrusel...");
        
        int delay = model.getCarouselDelay();
        this.countdown = (delay / 1000) - 1;

        // Timer principal para cambiar la imagen
        imageChangeTimer = new Timer(delay, e -> {
            if (model.getCurrentWorkMode() == WorkMode.CARROUSEL) { // Doble chequeo de seguridad
                listCoordinator.seleccionarSiguiente();
            }
        });
        imageChangeTimer.setInitialDelay(delay); // Inicia después del primer delay completo

        // Timer secundario para actualizar el contador CADA segundo
        countdownTimer = new Timer(1000, e -> {
            if (timerLabel != null) {
                timerLabel.setText(String.format("%02d", Math.max(0, countdown)));
            }
            countdown--;
            if (countdown < 0) {
                countdown = (delay / 1000) - 1;
            }
        });

        // Iniciar el contador inmediatamente
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d", Math.max(0, countdown)));
            countdown--;
            if (countdown < 0) {
                countdown = (delay / 1000) - 1;
            }
        }
        
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
        
        if (imageChangeTimer != null) {
            imageChangeTimer.stop();
            imageChangeTimer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
        isRunning = false;

        if (timerLabel != null) {
            timerLabel.setText("--:--");
            timerLabel.setVisible(false);
        }
        if (carouselThumbnails != null) {
            carouselThumbnails.setVisible(true);
        }

        actualizarEstadoDeAcciones();
    } // --- Fin del método stop ---

    private void actualizarEstadoDeAcciones() {
        if (controller.getActionMap() != null) {
            boolean hayImagenes = model.getCurrentListContext() != null && !model.getCurrentListContext().getModeloLista().isEmpty();
            
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PLAY).setEnabled(!isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PAUSE).setEnabled(isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_STOP).setEnabled(isRunning && hayImagenes);
        }
    } // --- Fin del método actualizarEstadoDeAcciones ---

    /**
     * Punto de entrada/salida que se llama desde GeneralController cuando
     * el modo de trabajo cambia hacia o desde el Carrusel.
     * @param isEntering true si estamos entrando en el modo, false si estamos saliendo.
     */
    public void onCarouselModeChanged(boolean isEntering) {
        if (isEntering) {
            System.out.println("[CarouselManager] Entrando en modo Carrusel...");
            initComponents(); // Se asegura de que tenemos las referencias a la UI
            stop(); // Garantiza un estado limpio al entrar
        } else {
            System.out.println("[CarouselManager] Saliendo del modo Carrusel...");
            stop(); // Detiene cualquier reproducción activa al salir
        }
    } // --- Fin del método onCarouselModeChanged ---

} // --- FIN de la clase CarouselManager ---