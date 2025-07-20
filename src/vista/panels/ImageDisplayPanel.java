package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import modelo.VisorModel;
import vista.theme.ThemeManager;

public class ImageDisplayPanel extends JPanel {
    private static final long serialVersionUID = 2L; // Versión incrementada

    // --- Dependencias ---
    private final ThemeManager themeManager;
    private final VisorModel model;
    private final JLabel internalLabel;

    // --- CAMBIO: El panel YA NO ALMACENA LA IMAGEN ---
    // private BufferedImage imagenOriginal; // <--- ELIMINADO

    // --- Estado del fondo (sin cambios) ---
    private boolean fondoACuadros = false;
    private final Color colorCuadroClaro = new Color(204, 204, 204);
    private final Color colorCuadroOscuro = new Color(255, 255, 255);
    private final int TAMANO_CUADRO = 16;
    private Color colorFondoSolido;

    public ImageDisplayPanel(ThemeManager themeManager, VisorModel model) {
//        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
    	
    	this.themeManager = themeManager; 
    	
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.setLayout(new BorderLayout());
        this.internalLabel = new JLabel();
        this.internalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.internalLabel.setVerticalAlignment(SwingConstants.CENTER);
        this.add(this.internalLabel, BorderLayout.CENTER);
        this.setOpaque(false);
        
        // Comprobamos si themeManager es nulo ANTES de usarlo.
        if (themeManager != null) {
            // Si no es nulo (caso normal), usamos el color del tema.
            this.colorFondoSolido = themeManager.getTemaActual().colorFondoSecundario();
        } else {
            // Si es nulo (caso del ThumbnailPreviewer), usamos un color por defecto seguro.
            this.colorFondoSolido = new Color(40, 40, 40); // Gris oscuro
            System.out.println("WARN [ImageDisplayPanel]: ThemeManager es nulo. Usando color de fondo por defecto.");
        }
        
        this.setBackground(this.colorFondoSolido);
        
//        this.colorFondoSolido = themeManager.getTemaActual().colorFondoSecundario();
        
    } // --- Fin del método ImageDisplayPanel (constructor) ---
    
    public JLabel getInternalLabel() {
        return this.internalLabel;
    } // --- Fin del método getInternalLabel ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelAncho = getWidth();
        int panelAlto = getHeight();

        // --- PASO 1: Dibujar el fondo (lógica sin cambios) ---
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

        // --- CAMBIO: Obtener la imagen directamente del modelo CADA VEZ que se pinta ---
        BufferedImage imagenADibujar = model.getCurrentImage();
        
        if (imagenADibujar != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            AffineTransform at = new AffineTransform();
            
            double scaleX, scaleY;

            if (model.getCurrentZoomMode() == servicios.zoom.ZoomModeEnum.FILL) {
                scaleX = (double) panelAncho / imagenADibujar.getWidth();
                scaleY = (double) panelAlto / imagenADibujar.getHeight();
                at.scale(scaleX, scaleY);
            } else {
                scaleX = model.getZoomFactor();
                scaleY = model.getZoomFactor();
                
                double xBase = (double) (panelAncho - imagenADibujar.getWidth() * scaleX) / 2;
                double yBase = (double) (panelAlto - imagenADibujar.getHeight() * scaleY) / 2;
                at.translate(xBase, yBase);
                at.translate(model.getImageOffsetX(), model.getImageOffsetY());
                at.scale(scaleX, scaleY);
            }

            // Dibuja la imagen que acabamos de obtener del modelo
            g2d.drawImage(imagenADibujar, at, null);
            g2d.dispose();
        }
    } // --- Fin del método paintComponent ---

    public void setSolidBackgroundColor(Color color) {
        if (color == null) return;
        this.colorFondoSolido = color;
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

    // --- CAMBIO: Este método ahora solo actualiza el label. El controlador limpia el modelo. ---
    public void mostrarError(String mensaje, ImageIcon iconoError) {
        this.internalLabel.setText(mensaje);
        this.internalLabel.setIcon(iconoError);
        this.internalLabel.setForeground(Color.RED);
        repaint(); // Forzamos repintado para que se vea el error y desaparezca la imagen vieja.
    } // --- Fin del método mostrarError ---
    
    // --- CAMBIO: ELIMINAMOS setImagen() ya que el panel leerá directamente del modelo ---
    
    // --- CAMBIO: limpiar() ahora solo limpia el label. El controlador limpia el modelo. ---
    public void limpiar() {
        this.internalLabel.setText(null);
        this.internalLabel.setIcon(null);
        repaint();
    } // --- Fin del método limpiar ---
    
    // --- CAMBIO: mostrarCargando() ya no necesita tocar la imagen, solo el label. ---
    public void mostrarCargando(String mensaje) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrarCargando(mensaje));
            return;
        }
        
        if (this.internalLabel != null) {
            this.internalLabel.setIcon(null);
            this.internalLabel.setText(mensaje);
        }
        
        repaint();
    } // --- Fin del método mostrarCargando ---
    
    /**
     * Devuelve si el panel está configurado actualmente para mostrar el fondo a cuadros.
     * @return true si el fondo a cuadros está activo, false en caso contrario.
     */
    public boolean isCheckeredBackground() {
        return this.fondoACuadros;
    }
    
} // --- FIN DE LA CLASE ImageDisplayPanel ---
