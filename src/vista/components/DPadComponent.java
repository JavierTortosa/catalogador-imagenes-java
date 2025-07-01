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
 * Utiliza un patrón Builder para una configuración limpia y declarativa.
 */
public class DPadComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    // --- Imágenes ORIGINALES, sin escalar ---
    private final Image originalBaseImage;
    private final Image originalPressedImage;
    private final Map<String, Image> originalHoverImages;

    // --- Imágenes en CACHÉ, escaladas al tamaño actual ---
    private transient Image scaledBaseImage;
    private transient Image scaledPressedImage;
    private transient Map<String, Image> scaledHoverImages;
    
    // --- Estado Interno ---
    private final List<Hotspot> hotspots;
    private Hotspot hoveredHotspot = null;
    private boolean isPressed = false;

    /**
     * Constructor privado. Solo se puede llamar desde el Builder.
     */
    private DPadComponent(Builder builder) {
        this.originalBaseImage = builder.baseImage;
        this.originalPressedImage = builder.pressedImage;
        this.hotspots = builder.hotspots;
        
        // Inicializar mapas internos
        this.originalHoverImages = new HashMap<>();
        this.scaledHoverImages = new HashMap<>();
        for (Hotspot hotspot : this.hotspots) {
            if (hotspot.hoverImage() != null) {
                this.originalHoverImages.put(hotspot.key(), hotspot.hoverImage());
            }
        }
        
        // Configurar propiedades del componente
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(builder.size);
        setMaximumSize(builder.size);
        setMinimumSize(builder.size);
        
        setupListeners();
    }

    // --- LÓGICA DE DIBUJO Y ESCALADO ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            Image imageToDraw = getScaledBaseImage();
            if (imageToDraw != null) {
                g2d.drawImage(imageToDraw, 0, 0, getWidth(), getHeight(), this);
            }

            Image overlayImage = null;
            if (isPressed && originalPressedImage != null) {
                overlayImage = getScaledPressedImage();
            } else if (hoveredHotspot != null) {
                overlayImage = getScaledHoverImage(hoveredHotspot.key());
            }

            if (overlayImage != null) {
                g2d.drawImage(overlayImage, 0, 0, getWidth(), getHeight(), this);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    private Image getScaledImage(Image originalImage) {
        if (originalImage == null) return null;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return null;
        return originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
    
    private Image getScaledBaseImage() {
        if (scaledBaseImage == null && originalBaseImage != null) {
            scaledBaseImage = getScaledImage(originalBaseImage);
        }
        return scaledBaseImage;
    }
    
    private Image getScaledPressedImage() {
        if (scaledPressedImage == null && originalPressedImage != null) {
            scaledPressedImage = getScaledImage(originalPressedImage);
        }
        return scaledPressedImage;
    }
    
    private Image getScaledHoverImage(String key) {
        if (!scaledHoverImages.containsKey(key) && originalHoverImages.containsKey(key)) {
            Image scaled = getScaledImage(originalHoverImages.get(key));
            scaledHoverImages.put(key, scaled);
        }
        return scaledHoverImages.get(key);
    }

    // --- LÓGICA DE INTERACCIÓN (Listeners) ---

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
                hoveredHotspot = null;
                isPressed = false;
                repaint();
            }
        });
    }

    private void updateHoveredHotspot(Point p) {
        Hotspot newHoveredHotspot = findHotspotAtPoint(p);
        if (newHoveredHotspot != hoveredHotspot) {
            hoveredHotspot = newHoveredHotspot;
            repaint();
        }
    }

    private Hotspot findHotspotAtPoint(Point p) {
        for (int i = hotspots.size() - 1; i >= 0; i--) {
            Hotspot hs = hotspots.get(i);
            if (hs.bounds().contains(p)) {
                return hs;
            }
        }
        return null;
    }

    private void executeHotspotAction(Hotspot hotspot) {
        Action action = hotspot.action();
        if (action != null && action.isEnabled()) {
            action.actionPerformed(new ActionEvent(
                this, ActionEvent.ACTION_PERFORMED, (String) action.getValue(Action.ACTION_COMMAND_KEY)
            ));
        }
    }

    // ===================================================================
    // --- CLASE ANIDADA ESTÁTICA: BUILDER ---
    // ===================================================================
    public static class Builder {
        private static final int MINIMUM_CELL_SIZE = 8;

        // --- Parámetros de configuración del Builder ---
        private Dimension size;
        private int rows = 1;
        private int cols = 1;
        private Image baseImage;
        private Image pressedImage;
        private final List<Hotspot> hotspots = new ArrayList<>();
        
        public Builder() {
            this.size = new Dimension(48, 48); 
        }

        public Builder withSize(int width, int height) {
            this.size = new Dimension(width, height);
            return this;
        }

        public Builder withGrid(int rows, int cols) {
            if (rows < 1 || cols < 1) {
                throw new IllegalArgumentException("Las filas y columnas deben ser al menos 1.");
            }
            this.rows = rows;
            this.cols = cols;
            return this;
        }

        public Builder withBaseImage(Image baseImage) {
            this.baseImage = baseImage;
            return this;
        }

        public Builder withPressedImage(Image pressedImage) {
            this.pressedImage = pressedImage;
            return this;
        }

        /**
         * Añade un Hotspot basado en coordenadas de cuadrícula.
         * Calcula el rectángulo del hotspot automáticamente.
         */
        public Builder withHotspot(int row, int col, Image hoverImage, Action action) {
            if (row < 0 || row >= rows || col < 0 || col >= cols) {
                throw new IllegalArgumentException("Coordenadas de hotspot ("+row+","+col+") fuera de la cuadrícula ("+rows+"x"+cols+").");
            }
            
            int cellWidth = size.width / cols;
            int cellHeight = size.height / rows;
            int x = col * cellWidth;
            int y = row * cellHeight;
            
            String key = "hotspot-" + row + "-" + col;
            Rectangle bounds = new Rectangle(x, y, cellWidth, cellHeight);
            
            this.hotspots.add(new Hotspot(key, bounds, hoverImage, action));
            return this;
        }
        
        /**
         * Añade un Hotspot con un rectángulo definido manualmente.
         * Útil para formas no rectangulares o D-Pads complejos.
         */
        public Builder withManualHotspot(String key, Rectangle bounds, Image hoverImage, Action action) {
            this.hotspots.add(new Hotspot(key, bounds, hoverImage, action));
            return this;
        }

        /**
         * Construye y devuelve la instancia final del DPadComponent.
         */
        public DPadComponent build() {
            if (baseImage == null) {
                throw new IllegalStateException("La imagen base (baseImage) no puede ser nula.");
            }
            int minWidth = cols * MINIMUM_CELL_SIZE;
            int minHeight = rows * MINIMUM_CELL_SIZE;
            if (size.width < minWidth || size.height < minHeight) {
                System.err.println("WARN: El tamaño solicitado ("+size+") es muy pequeño para la cuadrícula ("+rows+"x"+cols+"). El componente podría no funcionar bien.");
            }
            
            return new DPadComponent(this);
        }
    }
} // --- FIN de la clase DPadComponent ---