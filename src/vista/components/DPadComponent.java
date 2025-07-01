package vista.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter; // Para los listeners del ratón
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List; // Para almacenar los Hotspots
import java.util.Objects;

import javax.swing.Action; // Necesario para la Action del Hotspot
import javax.swing.JComponent;
import javax.swing.SwingUtilities; // Para invocar en el EDT si es necesario

/**
 * Componente de UI personalizado que representa un D-Pad para controlar el paneo
 * de una imagen. Detecta interacciones de ratón sobre sus zonas predefinidas
 * y ejecuta la acción asociada a cada zona.
 *
 * NOTA: Este componente es un prototipo del futuro MultiActionImageComponent,
 *      con su API adaptada específicamente para el caso del D-Pad.
 */
public class DPadComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    // --- Imágenes Base del Componente ---
    // Estas imágenes son la "piel" visual del D-Pad.
    // baseImage: La imagen de fondo del D-Pad (todo "apagado").
    // pressedImage: La imagen que se muestra cuando el usuario hace clic y mantiene.
    private Image baseImage;
    private Image pressedImage;

    // --- Definición de Hotspots (Zonas Interactivas) ---
    // Cada Hotspot define un área clicable, una imagen de hover y una Action.
    private final List<Hotspot> hotspots;

    // --- Estado Interno de Interacción ---
    // hoveredHotspot: El Hotspot sobre el que está el ratón en este momento.
    //                 Se usa para dibujar el efecto de "hover".
    private Hotspot hoveredHotspot = null;
    // isPressed: Indica si el botón del ratón está presionado actualmente sobre una zona.
    private boolean isPressed = false;

    /**
     * Constructor del DPadComponent.
     * Inicializa las colecciones internas, carga las imágenes base y configura los listeners.
     */
    public DPadComponent() {
        this.hotspots = new ArrayList<>();
        
        // TODO: Cargar las imágenes base (baseImage y pressedImage).
        // Por ahora, usamos null. Más adelante, las cargarás aquí o las pasarás por un setter.
        // Ejemplo de carga (asumiendo que las imágenes están en tu carpeta de recursos):
        // try {
        //     this.baseImage = new ImageIcon(getClass().getResource("/icons/common/dpad_base.png")).getImage();
        //     this.pressedImage = new ImageIcon(getClass().getResource("/icons/common/dpad_all_on.png")).getImage();
        // } catch (Exception e) {
        //     System.err.println("Error cargando imágenes del DPadComponent: " + e.getMessage());
        // }

        // Configura el tamaño preferido del componente.
        // Debería coincidir con el tamaño de tus imágenes del D-Pad (ej. 48x48 o 64x64).
        setPreferredSize(new Dimension(48, 48)); // Ajusta esto al tamaño real de tus iconos.

        setupListeners();
        
        // Asegurarse de que el componente pueda recibir eventos del ratón.
        setOpaque(false); // Puede ser transparente para ver el fondo del contenedor.
        setFocusable(false); // No queremos que reciba el foco para navegación con teclado por ahora.
    } // --- Fin del método DPadComponent (constructor) ---

    // --- Métodos de Configuración Pública (API) ---

    /**
     * Establece la imagen base que se dibujará como fondo del D-Pad cuando
     * no hay interacción.
     * @param baseImage La imagen base (no nula).
     */
    public void setBaseImage(Image baseImage) {
        this.baseImage = Objects.requireNonNull(baseImage, "La imagen base no puede ser nula.");
        // Ajustar el tamaño del componente al de la imagen base si no se ha establecido
        // o si queremos que siempre se ajuste.
        setPreferredSize(new Dimension(baseImage.getWidth(null), baseImage.getHeight(null)));
        repaint();
    } // --- Fin del método setBaseImage ---

    /**
     * Establece la imagen que se dibujará cuando el D-Pad esté en estado "presionado" (clic mantenido).
     * @param pressedImage La imagen de presionado (puede ser null si no hay efecto visual).
     */
    public void setPressedImage(Image pressedImage) {
        this.pressedImage = pressedImage;
        repaint();
    } // --- Fin del método setPressedImage ---

    /**
     * Añade un Hotspot (zona interactiva) al D-Pad.
     * @param hotspot El objeto Hotspot a añadir (no nulo).
     */
    public void addHotspot(Hotspot hotspot) {
        this.hotspots.add(Objects.requireNonNull(hotspot, "El Hotspot a añadir no puede ser nulo."));
    } // --- Fin del método addHotspot ---

    // --- Lógica de Dibujo (Rendering) ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Importante para la cadena de pintado de Swing

        Graphics2D g2d = (Graphics2D) g.create(); // Creamos una copia del contexto gráfico

        try {
            // 1. Dibujar la imagen base del componente
            if (baseImage != null) {
                g2d.drawImage(baseImage, 0, 0, this);
            }

            // 2. Si el componente está presionado, dibujar la imagen de "todos encendidos"
            //    Esta tiene prioridad sobre el efecto de hover.
            if (isPressed && pressedImage != null) {
                g2d.drawImage(pressedImage, 0, 0, this);
            } else if (hoveredHotspot != null && hoveredHotspot.hoverImage() != null) {
                // 3. Si no está presionado, pero hay un hotspot en hover, dibujar su imagen de hover
                g2d.drawImage(hoveredHotspot.hoverImage(), 0, 0, this);
            }

        } finally {
            g2d.dispose(); // Liberar recursos del contexto gráfico
        }
    } // --- Fin del método paintComponent ---

    // --- Lógica de Interacción (Listeners de Ratón) ---

    private void setupListeners() {
        // Listener para detectar el movimiento del ratón y el efecto de hover
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoveredHotspot(e.getPoint());
            } // --- Fin del método mouseMoved ---

            @Override
            public void mouseDragged(MouseEvent e) {
                // También actualizamos el hover durante el arrastre, por si el usuario
                // arrastra sobre otra zona y luego la suelta.
                updateHoveredHotspot(e.getPoint());
            } // --- Fin del método mouseDragged ---
        });

        // Listener para detectar clics y el estado de presionado
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Al presionar, si hay un hotspot bajo el ratón, marcamos el estado 'isPressed'
                // y forzamos un repintado para mostrar la imagen de "todos encendidos".
                Hotspot hotspotAtPress = findHotspotAtPoint(e.getPoint());
                if (hotspotAtPress != null && SwingUtilities.isLeftMouseButton(e)) {
                    isPressed = true;
                    repaint();
                }
            } // --- Fin del método mousePressed ---

            @Override
            public void mouseReleased(MouseEvent e) {
                // Al soltar el botón, si el componente estaba presionado y
                // el ratón sigue sobre el mismo hotspot, ejecutamos la acción.
                if (isPressed) {
                    isPressed = false; // Resetear el estado de presionado
                    Hotspot hotspotAtRelease = findHotspotAtPoint(e.getPoint());
                    if (hotspotAtRelease != null && hotspotAtRelease == hoveredHotspot) {
                        // Ejecutar la acción si es válida
                        executeHotspotAction(hotspotAtRelease);
                    }
                    repaint(); // Forzar repintado para quitar la imagen de "todos encendidos"
                }
            } // --- Fin del método mouseReleased ---

            @Override
            public void mouseExited(MouseEvent e) {
                // Cuando el ratón sale del componente, no hay ningún hotspot en hover.
                hoveredHotspot = null;
                isPressed = false; // Asegurarse de que el estado presionado se resetea
                repaint();
            } // --- Fin del método mouseExited ---
        });
    } // --- Fin del método setupListeners ---

    /**
     * Actualiza el 'hoveredHotspot' buscando qué zona está bajo el punto del ratón.
     * Forzará un repintado si el hotspot en hover ha cambiado.
     * @param p El punto actual del ratón.
     */
    private void updateHoveredHotspot(Point p) {
        Hotspot newHoveredHotspot = findHotspotAtPoint(p);
        if (newHoveredHotspot != hoveredHotspot) {
            hoveredHotspot = newHoveredHotspot;
            repaint(); // Solo repintar si el estado de hover ha cambiado
        }
    } // --- Fin del método updateHoveredHotspot ---

    /**
     * Busca y devuelve el Hotspot cuya área de 'bounds' contiene el punto dado.
     * @param p El punto (coordenadas X, Y) a comprobar.
     * @return El Hotspot que contiene el punto, o null si ninguno lo contiene.
     */
    private Hotspot findHotspotAtPoint(Point p) {
        // Itera sobre los hotspots de forma inversa para que el último Hotspot añadido (si hay solapamiento)
        // tenga prioridad en la detección, o según el orden que quieras darles.
        for (int i = hotspots.size() - 1; i >= 0; i--) {
            Hotspot hs = hotspots.get(i);
            if (hs.bounds().contains(p)) {
                return hs;
            }
        }
        return null;
    } // --- Fin del método findHotspotAtPoint ---

    /**
     * Ejecuta la Action asociada a un Hotspot dado.
     * @param hotspot El Hotspot cuya acción se debe ejecutar.
     */
    private void executeHotspotAction(Hotspot hotspot) {
        if (hotspot != null && hotspot.action() != null && hotspot.action().isEnabled()) {
            // Creamos un ActionEvent, similar a cómo lo hace Swing para JButtons.
            hotspot.action().actionPerformed(new ActionEvent(
                this, // Fuente del evento es este DPadComponent
                ActionEvent.ACTION_PERFORMED,
                (String) hotspot.action().getValue(Action.ACTION_COMMAND_KEY) // El comando de la Action
            ));
            System.out.println("[DPadComponent] Acción ejecutada para Hotspot: " + hotspot.key());
        } else if (hotspot != null) {
            System.out.println("[DPadComponent] Hotspot '" + hotspot.key() + "' clicado, pero su acción es nula o deshabilitada.");
        }
    } // --- Fin del método executeHotspotAction ---

} // --- FIN de la clase DPadComponent ---