package vista.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomGridCellPanel extends JPanel {

	private static final Logger logger = LoggerFactory.getLogger(CustomGridCellPanel.class); 
    private static final long serialVersionUID = 2L; // Versión incrementada

    // Campos para almacenar el estado que debemos pintar
    private ImageIcon image;
    private String overlayText;
    private Color borderColor;
    
    private boolean isSelected;
    
    // Colores y fuentes pre-calculados para eficiencia
    private final Color selectionColor = new Color(0, 150, 255, 70);
    private final Color textBackgroundColor = new Color(0, 0, 0, 150);
    private final Font textFont = new Font("Arial", Font.BOLD, 14);

    private static int GROSOR_BORDE = 6;
    private static final int GROSOR_BORDE_SELECCION = 3;
    
    public CustomGridCellPanel() {
        // --- LA CLAVE DE LA SEPARACIÓN ESTÁ AQUÍ ---
        // 1. Creamos un borde transparente alrededor del panel. Este será el espacio de separación.
        //    Cambia el número '5' para más o menos separación. 5px por cada lado = 10px de separación total.
//        int separacion = 10; 
//        setBorder(BorderFactory.createEmptyBorder(separacion, separacion, separacion, separacion));

        // 2. El panel es opaco, porque va a dibujar su propio fondo (el color de estado).
        setOpaque(true);

    } // ---FIN de metodo CustomGridCellPanel ---
    

    /**
     * Almacena los datos a pintar y solicita un repintado.
     */
    public void setData(ImageIcon image, String overlayText, boolean isSelected, Color cellBackground, Color borderColor) {
        this.image = image;
        this.overlayText = overlayText;
        this.isSelected = isSelected;
        this.borderColor = borderColor;
        setBackground(cellBackground); // El fondo del área de la imagen

        // --- DEFINICIÓN DE LA "TORRE" DE PANELES ---
        int separacionExterna = 2; // Capa 1: Espacio entre celdas
        int grosorBordeEstado = 4; // Capa 2: Grosor del borde verde/rojo
        int paddingInterno = 6;    // Capa 3: Pequeño espacio entre el borde y la imagen

        Color colorSeparador = javax.swing.UIManager.getColor("List.background");

        if (borderColor != null) {
            // --- CONSTRUIMOS LA TORRE DE 3 NIVELES ---
            // Nivel 3 (El más interno): El padding entre el borde y la imagen.
            javax.swing.border.Border bordePadding = BorderFactory.createEmptyBorder(
                paddingInterno, paddingInterno, paddingInterno, paddingInterno);

            // Nivel 2 (Intermedio): El borde de estado.
            javax.swing.border.Border bordeEstado = BorderFactory.createLineBorder(
                borderColor, grosorBordeEstado);
            
            // Combinamos Nivel 2 y 3: Borde de estado + Padding interior
            javax.swing.border.Border bordeInteriorCompuesto = BorderFactory.createCompoundBorder(
                bordeEstado, bordePadding);

            // Nivel 1 (El más externo): La separación entre celdas.
            javax.swing.border.Border bordeSeparacion = BorderFactory.createLineBorder(
                colorSeparador, separacionExterna);
            
            // Combinamos Nivel 1 con el resto: Separación + (Borde de estado + Padding)
            setBorder(BorderFactory.createCompoundBorder(bordeSeparacion, bordeInteriorCompuesto));

        } else {
            // --- SIN BORDE DE ESTADO ---
            // Creamos un borde simple que simula el grosor total de la torre
            // para que el contenido no se mueva de sitio.
            int paddingTotal = separacionExterna + grosorBordeEstado + paddingInterno;
            setBorder(BorderFactory.createEmptyBorder(
                paddingTotal, paddingTotal, paddingTotal, paddingTotal));
        }
    } // --- FIN de metodo setData ---

    /**
     * El corazón del componente. Aquí dibujamos todo manualmente.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // 1. PINTAR FONDO Y LA TORRE DE BORDES
        // Esta única línea pinta el color de fondo de la celda (el que está bajo la imagen)
        // y luego dibuja nuestra torre de 3 bordes perfectamente encima.
        super.paintComponent(g);

        // 2. PREPARAR PARA DIBUJAR EL CONTENIDO
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 3. OBTENER EL ÁREA LIBRE DENTRO DE LA TORRE
        // getInsets() nos da el espacio total ocupado por los 3 bordes.
        java.awt.Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;

        // 4. DIBUJAR LA IMAGEN DENTRO DEL ÁREA LIBRE
        if (image != null) {
            int imgWidth = image.getIconWidth();
            int imgHeight = image.getIconHeight();
            double imgAspect = (double) imgWidth / imgHeight;
            double panelAspect = (double) width / height;
            
            int newWidth, newHeight;
            if (imgAspect > panelAspect) {
                newWidth = width;
                newHeight = (int) (newWidth / imgAspect);
            } else {
                newHeight = height;
                newWidth = (int) (newHeight * imgAspect);
            }

            int imgX = x + (width - newWidth) / 2;
            int imgY = y + (height - newHeight) / 2;
            
            g2d.drawImage(image.getImage(), imgX, imgY, newWidth, newHeight, this);
        }
        
        // 5. SI ESTÁ SELECCIONADO, DIBUJAR EL INDICADOR
        if (isSelected) {
            g2d.setColor(selectionColor);
            g2d.fillRect(x, y, width, height);

            g2d.setColor(new Color(0, 150, 255));
            g2d.setStroke(new java.awt.BasicStroke(GROSOR_BORDE_SELECCION));
            g2d.drawRect(
                x + (GROSOR_BORDE_SELECCION / 2), 
                y + (GROSOR_BORDE_SELECCION / 2), 
                width - GROSOR_BORDE_SELECCION, 
                height - GROSOR_BORDE_SELECCION
            );
        }

        // 6. DIBUJAR TEXTO
        if (overlayText != null && !overlayText.isBlank()) {
            g2d.setFont(textFont);
            FontMetrics fm = g2d.getFontMetrics();
            int stringWidth = fm.stringWidth(overlayText);
            int stringHeight = fm.getHeight();
            int padding = 5;

            g2d.setColor(textBackgroundColor);
            g2d.fillRect(x, y + height - (stringHeight + padding), width, stringHeight + padding);
            
            g2d.setColor(Color.WHITE);
            int textX = x + (width - stringWidth) / 2;
            int textY = y + height - fm.getDescent() - (padding / 2);
            g2d.drawString(overlayText, textX, textY);
        }
        
        g2d.dispose();
    } // ---FIN de metodo paintComponent ---

} // --- FIN de clase CustomGridCellPanel---

