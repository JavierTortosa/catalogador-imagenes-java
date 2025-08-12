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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.VisorModel;
import vista.theme.ThemeManager;

public class ImageDisplayPanel extends JPanel {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageDisplayPanel.class);
	
    private static final long serialVersionUID = 2L; // Versión incrementada

    // --- Dependencias ---
    private final ThemeManager themeManager;
    private final VisorModel model;
    private final JLabel internalLabel;

    // --- Estado del fondo (sin cambios) ---
    private boolean fondoACuadros = false;
    private final Color colorCuadroClaro = new Color(204, 204, 204);
    private final Color colorCuadroOscuro = new Color(255, 255, 255);
    private final int TAMANO_CUADRO = 16;
    private Color colorFondoSolido;
    
    private BufferedImage welcomeImage; // Para almacenar la imagen de bienvenida
    private boolean showingWelcome = false; // Un flag para saber qué dibujar

    public ImageDisplayPanel(ThemeManager themeManager, VisorModel model) {
    	
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
            logger.warn("WARN [ImageDisplayPanel]: ThemeManager es nulo. Usando color de fondo por defecto.");
        }
        
        this.setBackground(this.colorFondoSolido);
        
    } // --- Fin del método ImageDisplayPanel (constructor) ---
    
    
    public void setWelcomeImage(BufferedImage image) {
        this.welcomeImage = image;
    }

    public void showWelcomeMessage() {
        this.showingWelcome = true;
        limpiar(); // Limpia cualquier texto de error/carga
        repaint(); // Pide al panel que se redibuje
    }

    public void hideWelcomeMessage() {
        this.showingWelcome = false;
        repaint();
    }
    
    
    public JLabel getInternalLabel() {
        return this.internalLabel;
    } // --- Fin del método getInternalLabel ---

    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelAncho = getWidth();
        int panelAlto = getHeight();

        // --- DIBUJAR FONDO ---
        if (fondoACuadros) {
            // ... (tu código de fondo a cuadros se mantiene igual)
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
        
        // --- LÓGICA DE DIBUJADO CONDICIONAL ---
        if (showingWelcome && welcomeImage != null) {
            
            // --- INICIO DE LA LÓGICA DE REESCALADO DE BIENVENIDA ---
            
            int imgAncho = welcomeImage.getWidth();
            int imgAlto = welcomeImage.getHeight();
            
            // Calcular el factor de escala para ajustar manteniendo proporciones
            double ratioAncho = (double) panelAncho / imgAncho;
            double ratioAlto = (double) panelAlto / imgAlto;
            double factorEscala = Math.min(ratioAncho, ratioAlto);
            
            // Calcular las nuevas dimensiones de la imagen
            int nuevoAncho = (int) (imgAncho * factorEscala);
            int nuevoAlto = (int) (imgAlto * factorEscala);
            
            // Calcular la posición para centrar la imagen reescalada
            int x = (panelAncho - nuevoAncho) / 2;
            int y = (panelAlto - nuevoAlto) / 2;
            
            // Dibujar la imagen reescalada
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(welcomeImage, x, y, nuevoAncho, nuevoAlto, this);
            g2d.dispose();
            
            // --- FIN DE LA LÓGICA DE REESCALADO DE BIENVENIDA ---

            return; // Salimos para no dibujar la imagen principal.
        }
        
        BufferedImage imagenADibujar = model.getCurrentImage();
        
        if (imagenADibujar != null) {
            if (showingWelcome) {
                this.showingWelcome = false;
            }
            
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

    // --- Este método ahora solo actualiza el label. El controlador limpia el modelo. ---
    public void mostrarError(String mensaje, ImageIcon iconoError) {
    	
        this.internalLabel.setText(mensaje);
        this.internalLabel.setIcon(iconoError);
        this.internalLabel.setForeground(Color.RED);
        
        this.showingWelcome = false; // Desactivar bienvenida si hay un error
        
        repaint(); // Forzamos repintado para que se vea el error y desaparezca la imagen vieja.
    } // --- Fin del método mostrarError ---
    
    
    // --- limpiar() ahora solo limpia el label. El controlador limpia el modelo. ---
    public void limpiar() {
    	
    	
        this.internalLabel.setText(null);
        this.internalLabel.setIcon(null);
        repaint();
    } // --- Fin del método limpiar ---
    
    
    public void mostrarCargando(String mensaje) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrarCargando(mensaje));
            return;
        }
        
        if (this.internalLabel != null) {
            this.internalLabel.setIcon(null);
            this.internalLabel.setText(mensaje);
        }
        
        this.showingWelcome = false;
        
        repaint();
    } // --- Fin del método mostrarCargando ---
    
    
    
    /**
     * Devuelve si el panel está configurado actualmente para mostrar el fondo a cuadros.
     * @return true si el fondo a cuadros está activo, false en caso contrario.
     */
    public boolean isCheckeredBackground() {
        return this.fondoACuadros;
    }
    
    
    /**
     * Actualiza el color de fondo sólido del panel basándose en el tema
     * actualmente activo en el ThemeManager.
     *
     * @param themeManagerRef La referencia al ThemeManager para obtener el color.
     */
    public void actualizarColorDeFondoPorTema(ThemeManager themeManagerRef) {
        if (themeManagerRef != null) {
            // Obtenemos el color correcto del tema.
            Color nuevoColorFondo = themeManagerRef.getTemaActual().colorFondoSecundario();
            
            // Actualizamos tanto la propiedad para el paintComponent...
            this.colorFondoSolido = nuevoColorFondo;
            
            // ...como la propiedad de fondo del propio JPanel.
            this.setBackground(nuevoColorFondo);
            
            // Forzamos un redibujado para que se vea el cambio.
            repaint();
            
            logger.debug("  -> ImageDisplayPanel actualizado al color de fondo del nuevo tema: " + nuevoColorFondo);
        }
    } // --- FIN del método actualizarColorDeFondoPorTema ---
    
    
} // --- FIN DE LA CLASE ImageDisplayPanel ---
