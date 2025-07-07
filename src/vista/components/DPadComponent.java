package vista.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Un componente de UI altamente configurable que representa una imagen base
 * con múltiples zonas interactivas (Hotspots).
 * Utiliza métodos de fábrica estáticos para su construcción.
 */
public class DPadComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    // --- Imágenes ORIGINALES, sin escalar ---
    private final Image originalBaseImage;
    private final Image originalPressedImage;
    private final Map<String, Image> originalHoverImages; // <-- CORRECCIÓN: Reintroducido

    // --- Estado Interno ---
    private final List<Hotspot> hotspots;
    private Hotspot hoveredHotspot = null;
    private boolean isPressed = false;

    // --- Imágenes en CACHÉ, escaladas al tamaño actual ---
    private transient Image scaledBaseImage;
    private transient Image scaledPressedImage;
    private transient Map<String, Image> scaledHoverImages; // <-- CORRECCIÓN: Reintroducido

    // ===================================================================
    // --- CONSTRUCTOR Y MÉTODOS DE FÁBRICA ESTÁTICOS ---
    // ===================================================================

    /**
     * Constructor privado. La instanciación se gestiona a través de los métodos de fábrica.
     */
    private DPadComponent(Dimension size, Image baseImage, Image pressedImage, List<Hotspot> hotspots) {
        this.originalBaseImage = Objects.requireNonNull(baseImage, "La imagen base no puede ser nula.");
        this.originalPressedImage = pressedImage; // Puede ser nulo
        this.hotspots = Objects.requireNonNull(hotspots, "La lista de hotspots no puede ser nula.");
        
        // --- CORRECCIÓN: INICIO bloque para inicializar mapas de imágenes ---
        this.originalHoverImages = new HashMap<>();
        this.scaledHoverImages = new HashMap<>();
        for (Hotspot hotspot : this.hotspots) {
            if (hotspot.hoverImage() != null) {
                this.originalHoverImages.put(hotspot.key(), hotspot.hoverImage());
            }
        }
        // --- CORRECCIÓN: FIN bloque para inicializar mapas de imágenes ---

        // Configurar propiedades del componente
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(size);
        setMaximumSize(size);
        setMinimumSize(size);
        
        setupListeners();
    } // --- FIN del método DPadComponent (constructor) ---

    /**
     * Método de fábrica para crear un D-Pad con layout en CRUZ.
     * Espera exactamente 4 elementos en cada lista, en el orden: UP, DOWN, LEFT, RIGHT.
     *
     * @param size La dimensión del componente.
     * @param baseImage La imagen de fondo del D-Pad.
     * @param pressedImage La imagen que se superpone cuando CUALQUIER zona está presionada.
     * @param hotspotKeys Una lista de 4 Strings para las claves de los hotspots.
     * @param hoverImages Una lista de 4 Images para el estado hover.
     * @param actions Una lista de 4 Actions a ejecutar.
     * @return una instancia de DPadComponent.
     */
    public static DPadComponent createCrossLayout(Dimension size, Image baseImage, Image pressedImage, List<String> hotspotKeys, List<Image> hoverImages, List<Action> actions) {
        if (hotspotKeys.size() != 4 || hoverImages.size() != 4 || actions.size() != 4) {
            throw new IllegalArgumentException("El layout en cruz requiere exactamente 4 elementos para claves, imágenes y acciones.");
        }
        
        int zoneSize = size.width / 3; // Asume una cuadrícula de 3x3

        // Se definen los rectángulos para la cruz
        Rectangle upBounds = new Rectangle(zoneSize, 0, zoneSize, zoneSize);
        Rectangle downBounds = new Rectangle(zoneSize, zoneSize * 2, zoneSize, zoneSize);
        Rectangle leftBounds = new Rectangle(0, zoneSize, zoneSize, zoneSize);
        Rectangle rightBounds = new Rectangle(zoneSize * 2, zoneSize, zoneSize, zoneSize);

        List<Rectangle> boundsList = List.of(upBounds, downBounds, leftBounds, rightBounds);
        List<Hotspot> finalHotspots = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            // Creamos los Hotspots aquí, asegurando que bounds nunca es nulo.
            finalHotspots.add(new Hotspot(
                hotspotKeys.get(i),
                boundsList.get(i), 
                hoverImages.get(i),
                actions.get(i)
            ));
        }

        return new DPadComponent(size, baseImage, pressedImage, finalHotspots);
    } // --- FIN del método createCrossLayout ---

    /**
     * (Futuro) Método de fábrica para crear un D-Pad con layout en CUADRÍCULA.
     * @param size La dimensión del componente.
     * @param baseImage La imagen de fondo.
     * @param pressedImage La imagen de 'presionado'.
     * @param predefinedHotspots La lista de hotspots a distribuir en la cuadrícula.
     * @param rows Número de filas.
     * @param cols Número de columnas.
     * @return una instancia de DPadComponent.
     */
    public static DPadComponent createGridLayout(Dimension size, Image baseImage, Image pressedImage, List<Hotspot> predefinedHotspots, int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("Las filas y columnas deben ser al menos 1.");
        }
        if (predefinedHotspots.size() != rows * cols) {
            throw new IllegalArgumentException("El número de hotspots (" + predefinedHotspots.size() + ") no coincide con la cuadrícula (" + rows + "x" + cols + ").");
        }

        int cellWidth = size.width / cols;
        int cellHeight = size.height / rows;
        List<Hotspot> finalHotspots = new ArrayList<>();
        int hotspotIndex = 0;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Rectangle bounds = new Rectangle(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
                Hotspot predefined = predefinedHotspots.get(hotspotIndex++);
                finalHotspots.add(new Hotspot(
                    predefined.key(),
                    bounds,
                    predefined.hoverImage(),
                    predefined.action()
                ));
            }
        }
        return new DPadComponent(size, baseImage, pressedImage, finalHotspots);
    } // --- FIN del método createGridLayout ---


    // ===================================================================
    // --- LÓGICA DE DIBUJO Y ESCALADO ---
    // ===================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Image imageToDraw = getScaledBaseImage();
            if (imageToDraw != null) {
                g2d.drawImage(imageToDraw, 0, 0, getWidth(), getHeight(), this);
            }

            Image overlayImage = null;
            if (isPressed && hoveredHotspot != null && originalPressedImage != null) {
                overlayImage = getScaledPressedImage();
            } else if (hoveredHotspot != null) {
                // --- CORRECCIÓN: Usar el mapa de caché local ---
                overlayImage = getScaledHoverImage(hoveredHotspot.key());
            }

            if (overlayImage != null) {
                g2d.drawImage(overlayImage, 0, 0, getWidth(), getHeight(), this);
            }
        } finally {
            g2d.dispose();
        }
    } // --- FIN del método paintComponent ---
    
    private Image getScaledImage(Image originalImage) {
        if (originalImage == null) return null;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return null;
        return originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    } // --- FIN del método getScaledImage ---
    
    private Image getScaledBaseImage() {
        if (scaledBaseImage == null && originalBaseImage != null) {
            scaledBaseImage = getScaledImage(originalBaseImage);
        }
        return scaledBaseImage;
    } // --- FIN del método getScaledBaseImage ---
    
    private Image getScaledPressedImage() {
        if (scaledPressedImage == null && originalPressedImage != null) {
            scaledPressedImage = getScaledImage(originalPressedImage);
        }
        return scaledPressedImage;
    } // --- FIN del método getScaledPressedImage ---
    
    // --- CORRECCIÓN: Reintroducido método para cachear imágenes de hover ---
    private Image getScaledHoverImage(String key) {
        if (!scaledHoverImages.containsKey(key) && originalHoverImages.containsKey(key)) {
            Image scaled = getScaledImage(originalHoverImages.get(key));
            if (scaled != null) {
                scaledHoverImages.put(key, scaled);
            }
        }
        return scaledHoverImages.get(key);
    } // --- FIN del método getScaledHoverImage ---

    // ===================================================================
    // --- LÓGICA DE INTERACCIÓN (Listeners) ---
    // ===================================================================

    private void setupListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { updateHoveredHotspot(e.getPoint()); }
            @Override
            public void mouseDragged(MouseEvent e) { updateHoveredHotspot(e.getPoint()); }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (findHotspotAtPoint(e.getPoint()) != null && SwingUtilities.isLeftMouseButton(e)) {
                    isPressed = true;
                    repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isPressed) {
                    isPressed = false;
                    Hotspot hotspotAtRelease = findHotspotAtPoint(e.getPoint());
                    if (hotspotAtRelease != null && hotspotAtRelease == hoveredHotspot) {
                        executeHotspotAction(hotspotAtRelease);
                    }
                    repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredHotspot != null || isPressed) {
                    hoveredHotspot = null;
                    isPressed = false;
                    repaint();
                }
            }
        });
    } // --- FIN del método setupListeners ---

    private void updateHoveredHotspot(Point p) {
        Hotspot newHoveredHotspot = findHotspotAtPoint(p);
        if (newHoveredHotspot != hoveredHotspot) {
            hoveredHotspot = newHoveredHotspot;
            repaint();
        }
    } // --- FIN del método updateHoveredHotspot ---

    private Hotspot findHotspotAtPoint(Point p) {
        for (int i = hotspots.size() - 1; i >= 0; i--) {
            Hotspot hs = hotspots.get(i);
            if (hs.bounds().contains(p)) {
                return hs;
            }
        }
        return null;
    } // --- FIN del método findHotspotAtPoint ---

    private void executeHotspotAction(Hotspot hotspot) {
        Action action = hotspot.action();
        if (action != null && action.isEnabled()) {
            action.actionPerformed(new ActionEvent(
                this, ActionEvent.ACTION_PERFORMED, (String) action.getValue(Action.ACTION_COMMAND_KEY)
            ));
        }
    } // --- FIN del método executeHotspotAction ---
    
// ***************************************************************************************** CLASE RECORD  

	/**
	 * Representa un "punto caliente" o zona interactiva dentro de un componente de imagen.
	 * Cada hotspot tiene una clave única, una región definida, una imagen para mostrar
	 * cuando el ratón está sobre él, y una acción asociada para ejecutar al ser clicado.
	 *
	 * @param key        Un identificador único para este hotspot (ej. "up", "on").
	 * @param bounds     La región rectangular dentro del componente donde este hotspot es activo.
	 * @param hoverImage La imagen que se dibujará sobre la imagen base cuando el ratón
	 *                   esté sobre este hotspot. Puede ser null si no hay efecto visual especial.
	 * @param action     La acción de Swing asociada a este hotspot, que se ejecutará al hacer clic.
	 */
	public static record Hotspot(
	    String key,             // Identificador único del hotspot (ej. "up", "down", "on")
	    Rectangle bounds,       // Las coordenadas y dimensiones de la zona activa dentro del componente
	    Image hoverImage,       // La imagen a dibujar cuando el ratón está sobre este hotspot
	    Action action           // La Action de Swing asociada a este hotspot
	) {
	    // Los records generan automáticamente constructor, getters, equals(), hashCode() y toString().
	    // Puedes añadir validaciones si son necesarias, por ejemplo:
	    public Hotspot {
	        if (key == null || key.trim().isEmpty()) {
	            throw new IllegalArgumentException("La clave del Hotspot no puede ser nula o vacía.");
	        }
	        if (bounds == null) {
	            throw new IllegalArgumentException("Los límites (bounds) del Hotspot no pueden ser nulos.");
	        }
	        // La imagen de hover y la acción pueden ser null, dependiendo del caso de uso.
	    }
	} // --- FIN de la clase record Hotspot ---

} // --- FIN de la clase DPadComponent ---
