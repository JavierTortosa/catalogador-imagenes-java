package vista.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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

    
    public CustomGridCellPanel() {
        setOpaque(true); // El panel es opaco, él mismo gestiona su fondo.
        
        // --- INICIO DE LA PRUEBA DE DIAGNÓSTICO ---
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Usamos System.out.println para que sea imposible que se pierda en los logs.
                System.out.println(">>> CLIC DETECTADO DIRECTAMENTE EN CustomGridCellPanel (clicks: " + e.getClickCount() + ") <<<");
                logger.debug(">>> CLIC DETECTADO DIRECTAMENTE EN CustomGridCellPanel (clicks: " + e.getClickCount() + ") <<<");
            }
        });
        // --- FIN DE LA PRUEBA DE DIAGNÓSTICO ---

    } // ---FIN de metodo---
    

    /**
     * Almacena los datos a pintar y solicita un repintado.
     */
    public void setData(ImageIcon image, String overlayText, boolean isSelected, Color cellBackground, Color borderColor) {
        this.image = image;
        this.overlayText = overlayText;
        this.isSelected = isSelected;
        this.borderColor = borderColor;
        setBackground(cellBackground);
        // No llamamos a repaint() aquí, el renderer ya lo gestiona la JList.
    } // ---FIN de metodo---

    /**
     * El corazón del componente. Aquí dibujamos todo manualmente.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // 1. Pintar el fondo (MUY IMPORTANTE)
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // =========================================================================
        // === INICIO DE MODIFICACIÓN: AÑADIR HINT PARA CALIDAD DE ESCALADO ===
        // =========================================================================
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // =========================================================================
        // === FIN DE MODIFICACIÓN ===
        // =========================================================================

        if (isSelected) {
        	 if (this.borderColor != null) {
                 g2d.setColor(this.borderColor);
             } else {
                 g2d.setColor(new Color(0, 150, 255));
             }
            
            int grosorBorde = 5;
            g2d.setStroke(new java.awt.BasicStroke(grosorBorde));
            g2d.drawRect(grosorBorde / 2, grosorBorde / 2, getWidth() - grosorBorde, getHeight() - grosorBorde);
        }
        
        // 2. Dibujar la imagen (centrada y ESCALADA)
        if (image != null) {
            // =========================================================================
            // === INICIO DE MODIFICACIÓN: LÓGICA DE ESCALADO DE IMAGEN ===
            // =========================================================================
            int padding = 10; // Espacio entre la imagen y el borde de la celda
            int panelWidth = getWidth() - padding;
            int panelHeight = getHeight() - padding;
            
            int imgWidth = image.getIconWidth();
            int imgHeight = image.getIconHeight();
            
            double imgAspect = (double) imgWidth / imgHeight;
            double panelAspect = (double) panelWidth / panelHeight;
            
            int newWidth;
            int newHeight;
            
            if (imgAspect > panelAspect) {
                // La imagen es más ancha que el panel, ajustamos al ancho
                newWidth = panelWidth;
                newHeight = (int) (newWidth / imgAspect);
            } else {
                // La imagen es más alta o igual en proporción, ajustamos al alto
                newHeight = panelHeight;
                newWidth = (int) (newHeight * imgAspect);
            }

            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;

            // Usamos el método drawImage que permite escalar
            g2d.drawImage(image.getImage(), x, y, newWidth, newHeight, this);
            // =========================================================================
            // === FIN DE MODIFICACIÓN ===
            // =========================================================================
        }
        
        // 3. Si está seleccionado, dibujar el overlay semitransparente encima
        if (isSelected) {
            g2d.setColor(selectionColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // 4. Si hay texto, dibujar su fondo y luego el texto encima de todo
        if (overlayText != null && !overlayText.isBlank()) {
            g2d.setFont(textFont);
            FontMetrics fm = g2d.getFontMetrics();
            int stringWidth = fm.stringWidth(overlayText);
            int stringHeight = fm.getHeight();
            int padding = 5;

            // Dibuja el fondo del texto
            g2d.setColor(textBackgroundColor);
            g2d.fillRect(0, getHeight() - (stringHeight + padding), getWidth(), stringHeight + padding);
            
            // Dibuja el texto
            g2d.setColor(Color.WHITE);
            int textX = (getWidth() - stringWidth) / 2;
            int textY = getHeight() - fm.getDescent() - (padding / 2);
            g2d.drawString(overlayText, textX, textY);
        }
        
        g2d.dispose();
    } // ---FIN de metodo---

} // --- FIN de clase CustomGridCellPanel---

//package vista.components;
//
//import java.awt.Color;
//import java.awt.Font;
//import java.awt.FontMetrics;
//import java.awt.Graphics;
//import java.awt.Graphics2D;
//import java.awt.RenderingHints;
//
//import javax.swing.ImageIcon;
//import javax.swing.JPanel;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class CustomGridCellPanel extends JPanel {
//
//	private static final Logger logger = LoggerFactory.getLogger(CustomGridCellPanel.class); 
//    private static final long serialVersionUID = 2L; // Versión incrementada
//
//    // Campos para almacenar el estado que debemos pintar
//    private ImageIcon image;
//    private String overlayText;
//    private Color borderColor;
//    
//    private boolean isSelected;
//    
//    // Colores y fuentes pre-calculados para eficiencia
//    private final Color selectionColor = new Color(0, 150, 255, 70);
//    private final Color textBackgroundColor = new Color(0, 0, 0, 150);
//    private final Font textFont = new Font("Arial", Font.BOLD, 14);
//
//    
//    public CustomGridCellPanel() {
//        setOpaque(true); // El panel es opaco, él mismo gestiona su fondo.
//        
//        // --- INICIO DE LA PRUEBA DE DIAGNÓSTICO ---
//        addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseClicked(java.awt.event.MouseEvent e) {
//                // Usamos System.out.println para que sea imposible que se pierda en los logs.
//                System.out.println(">>> CLIC DETECTADO DIRECTAMENTE EN CustomGridCellPanel (clicks: " + e.getClickCount() + ") <<<");
//                logger.debug(">>> CLIC DETECTADO DIRECTAMENTE EN CustomGridCellPanel (clicks: " + e.getClickCount() + ") <<<");
//            }
//        });
//        // --- FIN DE LA PRUEBA DE DIAGNÓSTICO ---
//
//    } // end of constructor
//    
//    
////    public CustomGridCellPanel() {
////        setOpaque(true); // El panel es opaco, él mismo gestiona su fondo.
////    } // end of constructor
//
//    /**
//     * Almacena los datos a pintar y solicita un repintado.
//     */
//    public void setData(ImageIcon image, String overlayText, boolean isSelected, Color cellBackground, Color borderColor) {
//        this.image = image;
//        this.overlayText = overlayText;
//        this.isSelected = isSelected;
//        this.borderColor = borderColor;
//        setBackground(cellBackground);
//        // No llamamos a repaint() aquí, el renderer ya lo gestiona la JList.
//    } // end of setData
//
//    /**
//     * El corazón del componente. Aquí dibujamos todo manualmente.
//     */
//    @Override
//    protected void paintComponent(Graphics g) {
//        // 1. Pintar el fondo (MUY IMPORTANTE)
//        super.paintComponent(g);
//
//        Graphics2D g2d = (Graphics2D) g.create();
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//     // --- INICIO DE LA MODIFICACIÓN ---
//        // Si está seleccionado, ANTES de pintar la imagen, dibujamos un borde.
//        if (isSelected) {
//        	
//        	 if (this.borderColor != null) {
//                 g2d.setColor(this.borderColor);
//             } else {
//                 // Fallback por si acaso
//                 g2d.setColor(new Color(0, 150, 255));
//             }
//            
//            // Dibujamos un rectángulo con un grosor de 2 píxeles.
//            int grosorBorde = 5;
//            g2d.setStroke(new java.awt.BasicStroke(grosorBorde));
//            // Dibujamos el rectángulo justo dentro de los límites del panel.
//            g2d.drawRect(grosorBorde / 2, grosorBorde / 2, getWidth() - grosorBorde, getHeight() - grosorBorde);
//        }
//        // --- FIN DE LA MODIFICACIÓN ---
//        
//        // 2. Dibujar la imagen (centrada)
//        if (image != null) {
//            int x = (getWidth() - image.getIconWidth()) / 2;
//            int y = (getHeight() - image.getIconHeight()) / 2;
//            g2d.drawImage(image.getImage(), x, y, this);
//        }
//        
//        // 3. Si está seleccionado, dibujar el overlay semitransparente encima
//        if (isSelected) {
//            g2d.setColor(selectionColor);
//            g2d.fillRect(0, 0, getWidth(), getHeight());
//        }
//
//        // 4. Si hay texto, dibujar su fondo y luego el texto encima de todo
//        if (overlayText != null && !overlayText.isBlank()) {
//            g2d.setFont(textFont);
//            FontMetrics fm = g2d.getFontMetrics();
//            int stringWidth = fm.stringWidth(overlayText);
//            int stringHeight = fm.getHeight();
//            int padding = 5;
//
//            // Dibuja el fondo del texto
//            g2d.setColor(textBackgroundColor);
//            g2d.fillRect(0, getHeight() - (stringHeight + padding), getWidth(), stringHeight + padding);
//            
//            // Dibuja el texto
//            g2d.setColor(Color.WHITE);
//            int textX = (getWidth() - stringWidth) / 2;
//            int textY = getHeight() - fm.getDescent() - (padding / 2);
//            g2d.drawString(overlayText, textX, textY);
//        }
//        
//        g2d.dispose();
//    } // end of paintComponent
//
//} // end of class
//
