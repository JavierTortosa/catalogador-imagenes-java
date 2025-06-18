package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import vista.theme.ThemeManager;

public class ImageDisplayPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ThemeManager themeManager;
    private final JLabel internalLabel;

    // --- Estado de pintado (simplificado) ---
    private Image imagenParaPintar; // La imagen YA ESCALADA que nos pasarán
    private int offsetX = 0;
    private int offsetY = 0;
    
    private boolean fondoACuadros = false;
    private final Color colorCuadroClaro = new Color(204, 204, 204);
    private final Color colorCuadroOscuro = new Color(255, 255, 255);
    private final int TAMANO_CUADRO = 16;
    private Color colorFondoSolido;

    public ImageDisplayPanel(ThemeManager themeManager) {
        this.themeManager = themeManager;
        this.setLayout(new BorderLayout());
        this.internalLabel = new JLabel();
        this.internalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.internalLabel.setVerticalAlignment(SwingConstants.CENTER);
        this.add(this.internalLabel, BorderLayout.CENTER);
        this.setOpaque(false);
        this.colorFondoSolido = themeManager.getTemaActual().colorFondoSecundario();
    }
    
    public JLabel getInternalLabel() {
        return this.internalLabel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelAncho = getWidth();
        int panelAlto = getHeight();

        // 1. Dibujar el fondo
        if (fondoACuadros) {
        	Graphics2D g2dFondo = (Graphics2D) g.create();
            try {
                for (int row = 0; row < panelAlto; row += TAMANO_CUADRO) {
                    for (int col = 0; col < panelAncho; col += TAMANO_CUADRO) {
                        boolean isLight = ((row / TAMANO_CUADRO) % 2) == ((col / TAMANO_CUADRO) % 2);
                        g2dFondo.setColor(isLight ? colorCuadroClaro : colorCuadroOscuro);
                        g2dFondo.fillRect(col, row, TAMANO_CUADRO, TAMANO_CUADRO);
                    }
                }
            } finally {
                g2dFondo.dispose();
            }
        } else {
            g.setColor(this.colorFondoSolido);
            g.fillRect(0, 0, panelAncho, panelAlto);
        }

        // 2. Dibujar la imagen principal, si existe
        if (imagenParaPintar != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            int finalW = imagenParaPintar.getWidth(null);
            int finalH = imagenParaPintar.getHeight(null);

            int drawX = (panelAncho - finalW) / 2 + this.offsetX;
            int drawY = (panelAlto - finalH) / 2 + this.offsetY;
            
            g2d.drawImage(imagenParaPintar, drawX, drawY, finalW, finalH, null);
            g2d.dispose();
        }
    } // --- FIN del metodo paintComponent ---

    public void setSolidBackgroundColor(Color color) {
        if (color == null) return;
        this.colorFondoSolido = color;
        // Si establecemos un color sólido, el modo a cuadros se desactiva.
        if (this.fondoACuadros) {
            this.fondoACuadros = false;
        }
        repaint();
    } // --- Fin del método setSolidBackgroundColor ---
    
    public void setCheckeredBackground(boolean activado) {
        if (this.fondoACuadros != activado) {
            this.fondoACuadros = activado;
            repaint();
        }
    } // --- Fin del método setCheckeredBackground ---

    public void mostrarError(String mensaje, ImageIcon iconoError) {
        this.imagenParaPintar = null;
        this.internalLabel.setText(mensaje);
        this.internalLabel.setIcon(iconoError);
        this.internalLabel.setForeground(Color.RED);
        repaint();
    }
    
    // <<< MÉTODO DE ENTRADA SIMPLIFICADO >>>
    // Ahora solo recibe la imagen ya escalada y los offsets.
    public void setImagenEscalada(Image img, int offsetX, int offsetY) {
        this.imagenParaPintar = img;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.internalLabel.setText(null);
        this.internalLabel.setIcon(null);
        repaint();
    }

    public void limpiar() {
        setImagenEscalada(null, 0, 0);
        this.internalLabel.setText(null);
        this.internalLabel.setIcon(null);
    }
    
    
    /**
     * Muestra un mensaje de "Cargando..." en el panel.
     * Limpia cualquier imagen anterior y establece un texto simple.
     * Esto proporciona feedback inmediato al usuario mientras una operación
     * en segundo plano (como el escalado) se está ejecutando.
     *
     * @param mensaje El texto a mostrar, por ejemplo "Escalando imagen...".
     */
    public void mostrarCargando(String mensaje) {
        // Asegurarse de que esta modificación de la UI se ejecute en el EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrarCargando(mensaje));
            return;
        }

        // Limpiar la imagen escalada actual
        this.imagenParaPintar = null;
        
        // Configurar la etiqueta interna para mostrar el mensaje
        if (this.internalLabel != null) {
            this.internalLabel.setIcon(null); // Quitar cualquier icono anterior
            this.internalLabel.setText(mensaje); // Poner el nuevo texto
        }
        
        // Solicitar un repintado del panel para que los cambios sean visibles
        this.repaint();
    }// --- FIN del metodo mostrarCargando ---
    
    
} // --- FIN de la clase ImageDisplayPanel ---