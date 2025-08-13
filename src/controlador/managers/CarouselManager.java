package controlador.managers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import vista.util.IconUtils;

public class CarouselManager {
	
	private static final Logger logger = LoggerFactory.getLogger(CarouselManager.class);

    // --- Dependencias ---
    private final IListCoordinator listCoordinator;
    private final VisorController controller;
    private final ComponentRegistry registry;
    private final VisorModel model;
    

    // --- Componentes de UI (Lazy loaded) ---
    private JLabel timerOverlayLabel;
    private JLabel statusIndicatorLabel;
    private JScrollPane carouselThumbnails;
    
    // Referencias a los botones de avance/retroceso
    private AbstractButton rewindButton;
    private AbstractButton fastForwardButton;
    private final IconUtils iconUtils;
    
    // --- Estado Interno ---
    private Timer imageChangeTimer;
    private Timer countdownTimer;
    private boolean isRunning = false;
    private int countdown;
    // Timer y estado para el avance/retroceso rápido
    private Timer fastMoveTimer;
    private boolean wasRunningBeforeFastMove = false;
    private static final int FAST_MOVE_INTERVAL_MS = 200; // Avanza 5 imágenes por segundo

    private Icon playIcon;
    private Icon pauseIcon;
    
    public CarouselManager(
            IListCoordinator listCoordinator, 
            VisorController controller,
            ComponentRegistry registry,
            VisorModel model,
            IconUtils iconUtils) {
        
        this.listCoordinator = Objects.requireNonNull(listCoordinator, "ListCoordinator no puede ser null");
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en CarouselManager");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en CarouselManager");
        this.iconUtils = iconUtils;
        
    } // --- Fin del constructor CarouselManager ---

    private void initComponents() {
        this.timerOverlayLabel = registry.get("label.carousel.timer.overlay");
        this.statusIndicatorLabel = registry.get("label.carousel.status.indicator");
        this.carouselThumbnails = registry.get("scroll.miniaturas.carousel");
        
        if (playIcon == null) {
            playIcon = iconUtils.getScaledIcon("9004-play_48x48.png", 24, 24);
        }
        if (pauseIcon == null) {
            pauseIcon = iconUtils.getScaledIcon("9005-pausa_48x48.png", 24, 24);
        }
        
    } // --- Fin del método initComponents ---
    
    /**
     * Busca y configura los botones de avance/retroceso rápido.
     * Este método debe ser llamado desde GeneralController DESPUÉS de que la UI
     * del carrusel y sus barras de herramientas hayan sido construidas.
     */
    public void findAndWireUpFastMoveButtons() {
        logger.debug("[CarouselManager] Buscando y configurando botones de avance/retroceso rápido...");
        
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
            logger.debug("  -> Botón Rewind encontrado y configurado.");
        } else {
            logger.warn("  -> WARN: Botón Rewind no encontrado con clave: " + rewindButtonKey);
        }

        if (fastForwardButton != null) {
            for (java.awt.event.MouseListener ml : fastForwardButton.getMouseListeners()) {
                 if (ml.getClass().getName().contains("CarouselManager")) fastForwardButton.removeMouseListener(ml);
            }
            fastForwardButton.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { if (fastForwardButton.isEnabled()) startFastForward(); }
                @Override public void mouseReleased(MouseEvent e) { stopFastMove(); }
            });
            logger.debug("  -> Botón Fast Forward encontrado y configurado.");
        } else {
            logger.warn("  -> WARN: Botón Fast Forward no encontrado con clave: " + fastForwardButtonKey);
        }
    } // --- Fin del método findAndWireUpFastMoveButtons ---

    // --- Métodos play/pause/stop (con formato de reloj corregido) ---

    
    public void play() {
        if (isRunning || model.getCurrentWorkMode() != WorkMode.CARROUSEL) return;
        logger.debug("[CarouselManager] Iniciando carrusel...");
        
        int delay = model.getCarouselDelay();
        int absoluteDelay = Math.abs(delay); // <-- Usaremos el valor absoluto para el timer
        
        this.countdown = absoluteDelay / 1000;

        imageChangeTimer = new Timer(absoluteDelay, e -> { // <-- El timer usa el valor absoluto
            if (model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
            	if (model.isCarouselShuffleEnabled()) {
                    // Si el modo aleatorio está activo, seleccionamos una al azar
                    listCoordinator.seleccionarAleatorio();
                } else {
                    // Si no, usamos la lógica secuencial (hacia adelante o atrás)
                    if (model.getCarouselDelay() >= 0) {
                        listCoordinator.seleccionarSiguiente();
                    } else {
                        listCoordinator.seleccionarAnterior();
                    }
                }
            }
        });
        imageChangeTimer.setInitialDelay(absoluteDelay);

        countdownTimer = new Timer(1000, e -> {
            countdown--;
            if (countdown < 0) countdown = (absoluteDelay / 1000);
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
        
        if (statusIndicatorLabel != null) {
            statusIndicatorLabel.setIcon(playIcon);
            statusIndicatorLabel.setVisible(true);
        }
        
        actualizarEstadoDeAcciones();
    } // --- Fin del método play ---
    
    
    public void pause() {
        if (!isRunning) return;
        logger.debug("[CarouselManager] Pausando carrusel.");
        if (imageChangeTimer != null) imageChangeTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        isRunning = false;
        if (carouselThumbnails != null) carouselThumbnails.setVisible(true);
        
        if (statusIndicatorLabel != null) {
            statusIndicatorLabel.setIcon(pauseIcon);
            statusIndicatorLabel.setVisible(true); // Se queda visible en pausa
        }
        
        actualizarEstadoDeAcciones();
    } // --- Fin del método pause ---

    public void stop() {
        logger.debug("[CarouselManager] Deteniendo carrusel.");
        if (imageChangeTimer != null) { imageChangeTimer.stop(); imageChangeTimer = null; }
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
        isRunning = false;
        if (timerOverlayLabel != null) timerOverlayLabel.setVisible(false);
        if (carouselThumbnails != null) carouselThumbnails.setVisible(true);
        
        if (statusIndicatorLabel != null) {
            statusIndicatorLabel.setVisible(false); // Se oculta al parar
        }
        
        actualizarEstadoDeAcciones();
    } // --- Fin del método stop ---

    // --- NUEVOS MÉTODOS PARA AVANCE/RETROCESO RÁPIDO ---

    public void startFastForward() {
        logger.debug("[CarouselManager] Iniciando Avance Rápido.");
        this.wasRunningBeforeFastMove = isRunning;
        if (isRunning) pause();
        listCoordinator.seleccionarSiguiente(); // Primer avance instantáneo
        fastMoveTimer = new Timer(FAST_MOVE_INTERVAL_MS, e -> listCoordinator.seleccionarSiguiente());
        fastMoveTimer.start();
    } // --- Fin del método startFastForward ---
    
    public void startRewind() {
        logger.debug("[CarouselManager] Iniciando Retroceso Rápido.");
        this.wasRunningBeforeFastMove = isRunning;
        if (isRunning) pause();
        listCoordinator.seleccionarAnterior(); // Primer retroceso instantáneo
        fastMoveTimer = new Timer(FAST_MOVE_INTERVAL_MS, e -> listCoordinator.seleccionarAnterior());
        fastMoveTimer.start();
    } // --- Fin del método startRewind ---
    
    public void stopFastMove() {
        logger.debug("[CarouselManager] Deteniendo Avance/Retroceso Rápido.");
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
            logger.debug("[CarouselManager] Entrando en modo Carrusel...");
            initComponents();
            stop();
            updateSpeedLabel(); // <-- AÑADE ESTA LÍNEA para mostrar el valor inicial
        } else {
            logger.debug("[CarouselManager] Saliendo del modo Carrusel...");
            stop();
            if (fastMoveTimer != null) {
                fastMoveTimer.stop();
                fastMoveTimer = null;
            }
        }
    } // --- Fin del método onCarouselModeChanged ---
    
    
    /**
     * Busca los componentes interactivos del carrusel (que no son botones de Action)
     * y les añade los listeners de comportamiento específico.
     * Se debe llamar después de que la UI del carrusel esté construida.
     */
    public void wireUpEventListeners() {
        logger.debug("[CarouselManager] Configurando listeners de eventos especiales...");
        
        // 1. Buscar el JLabel de velocidad
        javax.swing.JLabel speedLabel = registry.get("label.velocidad.carrusel");

        if (speedLabel != null) {
            // 2. Limpiar listeners antiguos para evitar duplicados
            for (java.awt.event.MouseListener ml : speedLabel.getMouseListeners()) {
                speedLabel.removeMouseListener(ml);
            }

            // 3. Añadir el nuevo MouseListener
            speedLabel.setToolTipText("Clic para seleccionar una velocidad predefinida");
            speedLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Delegamos la creación y muestra del menú al VisorController
                    controller.showCarouselSpeedMenu(speedLabel);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    speedLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    speedLabel.setCursor(java.awt.Cursor.getDefaultCursor());
                }
            });
            logger.debug("  -> MouseListener para menú de velocidad añadido a 'label.velocidad.carrusel'.");
        } else {
            logger.warn("  -> WARN: No se encontró 'label.velocidad.carrusel' para añadir listener de menú.");
        }
    } // --- Fin del método wireUpEventListeners ---
    
    
    /**
     * Actualiza el texto del JLabel que muestra la velocidad del carrusel.
     * Lee el valor actual del modelo, lo formatea y lo establece en el label.
     */
    public void updateSpeedLabel() {
        // 1. Buscar la etiqueta en el registro
        javax.swing.JLabel speedLabel = registry.get("label.velocidad.carrusel");
        if (speedLabel == null) {
            // Si no existe (quizás en una versión futura de la UI), no hacemos nada.
            return;
        }

        // 2. Obtener el valor actual del modelo (en milisegundos)
        double delayInSeconds = model.getCarouselDelay() / 1000.0;
        
        // 3. Formatear y actualizar el texto de la etiqueta (ej: "5.0s")
        speedLabel.setText(String.format("%.1fs", delayInSeconds));
        
    } // --- Fin del método updateSpeedLabel ---
    

} // --- FIN de la clase CarouselManager ---