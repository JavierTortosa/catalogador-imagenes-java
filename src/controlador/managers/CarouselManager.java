// REEMPLAZA LA CLASE ENTERA CarouselManager.java CON ESTO

package controlador.managers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

public class CarouselManager {

    // --- Dependencias ---
    private final IListCoordinator listCoordinator;
    private final VisorController controller;
    private final ComponentRegistry registry;
    private final VisorModel model;

    // --- Componentes de UI (Lazy loaded) ---
    private JLabel timerOverlayLabel;
    private JScrollPane carouselThumbnails;
    // NUEVO: Referencias a los botones de avance/retroceso
    private AbstractButton rewindButton;
    private AbstractButton fastForwardButton;

    // --- Estado Interno ---
    private Timer imageChangeTimer;
    private Timer countdownTimer;
    private boolean isRunning = false;
    private int countdown;
    // NUEVO: Timer y estado para el avance/retroceso rápido
    private Timer fastMoveTimer;
    private boolean wasRunningBeforeFastMove = false;
    private static final int FAST_MOVE_INTERVAL_MS = 200; // Avanza 5 imágenes por segundo

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

    private void initComponents() {
        this.timerOverlayLabel = registry.get("label.carousel.timer.overlay");
        this.carouselThumbnails = registry.get("scroll.miniaturas.carousel");
    } // --- Fin del método initComponents ---
    
    /**
     * NUEVO: Busca y configura los botones de avance/retroceso rápido.
     * Este método debe ser llamado desde GeneralController DESPUÉS de que la UI
     * del carrusel y sus barras de herramientas hayan sido construidas.
     */
    public void findAndWireUpFastMoveButtons() {
        System.out.println("[CarouselManager] Buscando y configurando botones de avance/retroceso rápido...");
        
        // La clave se construye a partir del nombre de la toolbar y el comando
        String rewindButtonKey = ConfigKeys.buildKey("interfaz.boton", "carrousel", ConfigKeys.keyPartFromCommand(AppActionCommands.CMD_CAROUSEL_REWIND));
        this.rewindButton = registry.get(rewindButtonKey);
        
        String fastForwardButtonKey = ConfigKeys.buildKey("interfaz.boton", "carrousel", ConfigKeys.keyPartFromCommand(AppActionCommands.CMD_CAROUSEL_FAST_FORWARD));
        this.fastForwardButton = registry.get(fastForwardButtonKey);

        if (rewindButton != null) {
            // Limpiamos listeners antiguos por si acaso
            for (java.awt.event.MouseListener ml : rewindButton.getMouseListeners()) {
                if (ml.getClass().getName().contains("CarouselManager")) rewindButton.removeMouseListener(ml);
            }
            // Añadimos el listener para "mantener pulsado"
            rewindButton.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { if (rewindButton.isEnabled()) startRewind(); }
                @Override public void mouseReleased(MouseEvent e) { stopFastMove(); }
            });
            System.out.println("  -> Botón Rewind encontrado y configurado.");
        } else {
            System.err.println("  -> WARN: Botón Rewind no encontrado con clave: " + rewindButtonKey);
        }

        if (fastForwardButton != null) {
            for (java.awt.event.MouseListener ml : fastForwardButton.getMouseListeners()) {
                 if (ml.getClass().getName().contains("CarouselManager")) fastForwardButton.removeMouseListener(ml);
            }
            fastForwardButton.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { if (fastForwardButton.isEnabled()) startFastForward(); }
                @Override public void mouseReleased(MouseEvent e) { stopFastMove(); }
            });
            System.out.println("  -> Botón Fast Forward encontrado y configurado.");
        } else {
            System.err.println("  -> WARN: Botón Fast Forward no encontrado con clave: " + fastForwardButtonKey);
        }
    } // --- Fin del método findAndWireUpFastMoveButtons ---

    // --- Métodos play/pause/stop (con formato de reloj corregido) ---

    public void play() {
        if (isRunning || model.getCurrentWorkMode() != WorkMode.CARROUSEL) return;
        System.out.println("[CarouselManager] Iniciando carrusel...");
        int delay = model.getCarouselDelay();
        this.countdown = delay / 1000;
        imageChangeTimer = new Timer(delay, e -> { if (model.getCurrentWorkMode() == WorkMode.CARROUSEL) listCoordinator.seleccionarSiguiente(); });
        imageChangeTimer.setInitialDelay(delay);
        countdownTimer = new Timer(1000, e -> {
            countdown--;
            if (countdown < 0) countdown = (delay / 1000);
            if (timerOverlayLabel != null) {
                int minutos = Math.max(0, countdown) / 60;
                int segundos = Math.max(0, countdown) % 60;
                timerOverlayLabel.setText(String.format("%02d:%02d", minutos, segundos));
            }
        });
        if (timerOverlayLabel != null) {
            int minutos = Math.max(0, countdown) / 60;
            int segundos = Math.max(0, countdown) % 60;
            timerOverlayLabel.setText(String.format("%02d:%02d", minutos, segundos));
            timerOverlayLabel.setVisible(true);
        }
        imageChangeTimer.start();
        countdownTimer.start();
        isRunning = true;
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
        if (imageChangeTimer != null) { imageChangeTimer.stop(); imageChangeTimer = null; }
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
        isRunning = false;
        if (timerOverlayLabel != null) timerOverlayLabel.setVisible(false);
        if (carouselThumbnails != null) carouselThumbnails.setVisible(true);
        actualizarEstadoDeAcciones();
    } // --- Fin del método stop ---

    // --- NUEVOS MÉTODOS PARA AVANCE/RETROCESO RÁPIDO ---

    public void startFastForward() {
        System.out.println("[CarouselManager] Iniciando Avance Rápido.");
        this.wasRunningBeforeFastMove = isRunning;
        if (isRunning) pause();
        listCoordinator.seleccionarSiguiente(); // Primer avance instantáneo
        fastMoveTimer = new Timer(FAST_MOVE_INTERVAL_MS, e -> listCoordinator.seleccionarSiguiente());
        fastMoveTimer.start();
    } // --- Fin del método startFastForward ---
    
    public void startRewind() {
        System.out.println("[CarouselManager] Iniciando Retroceso Rápido.");
        this.wasRunningBeforeFastMove = isRunning;
        if (isRunning) pause();
        listCoordinator.seleccionarAnterior(); // Primer retroceso instantáneo
        fastMoveTimer = new Timer(FAST_MOVE_INTERVAL_MS, e -> listCoordinator.seleccionarAnterior());
        fastMoveTimer.start();
    } // --- Fin del método startRewind ---
    
    public void stopFastMove() {
        System.out.println("[CarouselManager] Deteniendo Avance/Retroceso Rápido.");
        if (fastMoveTimer != null) {
            fastMoveTimer.stop();
            fastMoveTimer = null;
        }
        if (wasRunningBeforeFastMove) {
            play(); // Reanuda si estaba en play antes
        }
    } // --- Fin del método stopFastMove ---
    
    private void actualizarEstadoDeAcciones() {
        if (controller.getActionMap() != null) {
            boolean hayImagenes = model.getCurrentListContext() != null && !model.getCurrentListContext().getModeloLista().isEmpty();
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PLAY).setEnabled(!isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_PAUSE).setEnabled(isRunning && hayImagenes);
            controller.getActionMap().get(AppActionCommands.CMD_CAROUSEL_STOP).setEnabled(isRunning && hayImagenes);
            
            // Habilitamos o deshabilitamos los botones físicos también
            if (rewindButton != null) rewindButton.setEnabled(hayImagenes);
            if (fastForwardButton != null) fastForwardButton.setEnabled(hayImagenes);
        }
    } // --- Fin del método actualizarEstadoDeAcciones ---

    public void onCarouselModeChanged(boolean isEntering) {
        if (isEntering) {
            System.out.println("[CarouselManager] Entrando en modo Carrusel...");
            initComponents();
            stop();
        } else {
            System.out.println("[CarouselManager] Saliendo del modo Carrusel...");
            stop();
            if (fastMoveTimer != null) { // Asegurarnos de parar el timer rápido si salimos del modo
                fastMoveTimer.stop();
                fastMoveTimer = null;
            }
        }
    } // --- Fin del método onCarouselModeChanged ---

} // --- FIN de la clase CarouselManager ---