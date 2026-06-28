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

import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import modelo.proyecto.CommentOverlay;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
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
    private boolean isMarcada = false; // Indica si la imagen actual está marcada
    private boolean isPlaceholderSinImagen = false; // Indica si estamos mostrando el placeholder de "sin imagen"

    private javax.swing.JPanel navArrowsLeftPanel;
    private javax.swing.JPanel navArrowsRightPanel;

    private boolean editorOverlayInstance = false;
    private boolean useEditorZoom = false;
    private double editorZoomFactor = 1.0;
    private double editorOffsetX = 0;
    private double editorOffsetY = 0;

    private IProjectManager projectManager;

    private transient AffineTransform currentImageTransform;

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

    public void setImagenMarcada(boolean marcada) {
        if (this.isMarcada != marcada) {
            this.isMarcada = marcada;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelAncho = getWidth();
        int panelAlto = getHeight();

        // --- DIBUJAR FONDO ---
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
                if (useEditorZoom) {
                    scaleX = editorZoomFactor;
                    scaleY = editorZoomFactor;
                    double xBase = (double) (panelAncho - imagenADibujar.getWidth() * scaleX) / 2;
                    double yBase = (double) (panelAlto - imagenADibujar.getHeight() * scaleY) / 2;
                    at.translate(xBase + editorOffsetX, yBase + editorOffsetY);
                } else {
                    scaleX = model.getZoomFactor();
                    scaleY = model.getZoomFactor();
                    double xBase = (double) (panelAncho - imagenADibujar.getWidth() * scaleX) / 2;
                    double yBase = (double) (panelAlto - imagenADibujar.getHeight() * scaleY) / 2;
                    at.translate(xBase, yBase);
                    at.translate(model.getImageOffsetX(), model.getImageOffsetY());
                }
                at.scale(scaleX, scaleY);
            }

            g2d.drawImage(imagenADibujar, at, null);
            this.currentImageTransform = new AffineTransform(at);

            // --- DIBUJAR CHECKBOXES CLIENTE (modo revision o editor) ---
            if (model.getCurrentWorkMode() == WorkMode.CLIENTE
                    && (model.isClienteCheckboxVisible() || this.editorOverlayInstance)
                    && projectManager != null && projectManager.getCurrentProject() != null) {
                String imageKey = getCurrentImageKey();
                if (imageKey != null) {
                    String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
                    var pi = projectManager.getCurrentProject().getMasterImages().get(canonicalKey);
                    if (pi == null) return;
                    var overlays = pi.getCheckboxes();
                    if (overlays != null && !overlays.isEmpty()) {
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        for (var overlay : overlays) {
                            java.awt.geom.Point2D src = new java.awt.geom.Point2D.Double(
                                    overlay.getImageX(), overlay.getImageY());
                            java.awt.geom.Point2D dst = new java.awt.geom.Point2D.Double();
                            at.transform(src, dst);
                            int cx = (int) Math.round(dst.getX());
                            int cy = (int) Math.round(dst.getY());
                            int size = overlay.getSize();
                            int halfSize = size / 2;
                            int sqX = cx - halfSize;
                            int sqY = cy - halfSize;

                            // Fondo del checkbox
                            g2d.setColor(new Color(255, 255, 255, 230));
                            g2d.fillRect(sqX, sqY, size, size);

                            // Resaltar si es el checkbox seleccionado en la tabla cliente
                            String selCbCode = model.getSelectedCheckboxCode();
                            boolean isHighlighted = selCbCode != null
                                    && selCbCode.equals(overlay.getCheckboxCode());
                            if (isHighlighted) {
                                g2d.setStroke(new java.awt.BasicStroke(3f));
                                g2d.setColor(new Color(255, 153, 51)); // naranja
                                g2d.drawRect(sqX - 2, sqY - 2, size + 4, size + 4);
                                g2d.setStroke(new java.awt.BasicStroke(1f));
                                g2d.setColor(Color.BLACK);
                            } else {
                                g2d.setColor(Color.BLACK);
                                g2d.drawRect(sqX, sqY, size, size);
                            }

                            // Marca: ✓ SELECTED, ✗ DISCARDED, ○ UNDEFINED
                            g2d.setStroke(new java.awt.BasicStroke(Math.max(2f, size / 10f)));
                            int pad = size / 4;
                            SelectionState ovState = overlay.getState();
                            if (ovState == SelectionState.SELECTED) {
                                g2d.setColor(new Color(0, 140, 0));
                                g2d.drawLine(sqX + pad, sqY + halfSize,
                                        sqX + halfSize, sqY + size - pad);
                                g2d.drawLine(sqX + halfSize, sqY + size - pad,
                                        sqX + size - pad, sqY + pad);
                            } else if (ovState == SelectionState.DISCARDED) {
                                g2d.setColor(new Color(160, 0, 0));
                                g2d.drawLine(sqX + pad, sqY + pad,
                                        sqX + size - pad, sqY + size - pad);
                                g2d.drawLine(sqX + size - pad, sqY + pad,
                                        sqX + pad, sqY + size - pad);
                            } else {
                                g2d.setColor(new Color(128, 128, 128));
                                g2d.drawOval(sqX + pad / 2, sqY + pad / 2,
                                        size - pad, size - pad);
                            }

                            // Código del checkbox encima
                            if (overlay.getCheckboxCode() != null && !overlay.getCheckboxCode().isEmpty()) {
                                g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
                                String code = overlay.getCheckboxCode();
                                java.awt.font.FontRenderContext frc = g2d.getFontRenderContext();
                                java.awt.geom.Rectangle2D cbBounds = g2d.getFont().getStringBounds(code, frc);
                                int cbLabelW = (int) cbBounds.getWidth() + 8;
                                int cbLabelH = (int) cbBounds.getHeight() + 4;
                                int cbLabelX = cx - cbLabelW / 2;
                                int cbLabelY = sqY - cbLabelH - 2;
                                g2d.setColor(Color.BLACK);
                                g2d.fillRect(cbLabelX, cbLabelY, cbLabelW, cbLabelH);
                                g2d.setColor(Color.WHITE);
                                g2d.drawString(code, cbLabelX + 4,
                                        cbLabelY + (int) cbBounds.getHeight() - 2);
                            }

                            // PVP junto al checkbox
                            if (overlay.getPrice() > 0) {
                                g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
                                String priceStr = String.format("%.2f \u20AC", overlay.getPrice());
                                java.awt.font.FontRenderContext frc = g2d.getFontRenderContext();
                                java.awt.geom.Rectangle2D pBounds = g2d.getFont().getStringBounds(priceStr, frc);
                                int pW = (int) pBounds.getWidth() + 10;
                                int pH = (int) pBounds.getHeight() + 4;
                                int pX = sqX + size + 4;
                                int pY = cy - pH / 2;
                                g2d.setColor(Color.BLACK);
                                g2d.fillRect(pX, pY, pW, pH);
                                g2d.setColor(Color.WHITE);
                                g2d.drawString(priceStr, pX + 5,
                                        pY + (int) pBounds.getHeight() - 2);
                            }
                        }
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    }

                    // --- DIBUJAR COMENTARIOS (CommentOverlay) ---
                    var commentOverlay = pi.getCommentOverlay();
                    if (commentOverlay != null && commentOverlay.getText() != null
                            && !commentOverlay.getText().isEmpty()) {
                        java.awt.geom.Point2D src = new java.awt.geom.Point2D.Double(
                                commentOverlay.getImageX(), commentOverlay.getImageY());
                        java.awt.geom.Point2D dst = new java.awt.geom.Point2D.Double();
                        at.transform(src, dst);
                        int cx = (int) Math.round(dst.getX());
                        int cy = (int) Math.round(dst.getY());

                        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
                        String commentText = commentOverlay.getText();
                        java.util.List<String> lines = wrapText(commentText, g2d, 40);
                        java.awt.font.FontRenderContext frc = g2d.getFontRenderContext();
                        double lineH = 0;
                        double maxW = 0;
                        java.util.List<java.awt.geom.Rectangle2D> bounds = new java.util.ArrayList<>();
                        for (String line : lines) {
                            java.awt.geom.Rectangle2D lb = g2d.getFont().getStringBounds(line, frc);
                            bounds.add(lb);
                            maxW = Math.max(maxW, lb.getWidth());
                            lineH = lb.getHeight();
                        }
                        int totalH = (int) (lineH * lines.size() + 8);
                        int totalW = (int) maxW + 12;
                        int textX = cx;
                        int textY = cy;
                        g2d.setColor(new Color(0, 0, 0, 200));
                        g2d.fillRoundRect(textX - 4, textY - 4, totalW, totalH, 6, 6);
                        g2d.setColor(Color.WHITE);
                        int drawY = textY + (int) lineH - 2;
                        for (int i = 0; i < lines.size(); i++) {
                            g2d.drawString(lines.get(i), textX + 2, drawY);
                            drawY += (int) lineH;
                        }
                    }
                }
            }
            // --- FIN CHECKBOXES CLIENTE ---

            g2d.dispose();
        }

        // --- DIBUJAR MARCO DE IMAGEN MARCADA O PLACEHOLDER ---
        if (this.isMarcada || this.isPlaceholderSinImagen) {
            Graphics2D g2dBorder = (Graphics2D) g.create();
            try {
                int thickness = 4;
                // Comentario: trigger rebuild
                Color colorBorde;
                
                if (this.isPlaceholderSinImagen) {
                    colorBorde = Color.RED; // Marco rojo para archivos sin imagen
                } else {
                    colorBorde = (themeManager != null && themeManager.getTemaActual() != null) 
                        ? themeManager.getTemaActual().colorImagenMarcada() 
                        : new Color(255, 191, 0); // Fallback Ámbar
                }

                g2dBorder.setColor(colorBorde);
                g2dBorder.fillRect(0, 0, panelAncho, thickness); // Top
                g2dBorder.fillRect(0, panelAlto - thickness, panelAncho, thickness); // Bottom
                g2dBorder.fillRect(0, 0, thickness, panelAlto); // Left
                g2dBorder.fillRect(panelAncho - thickness, 0, thickness, panelAlto); // Right
            } finally {
                g2dBorder.dispose();
            }
        }
    } // --- Fin del método paintComponent ---

    public void setSolidBackgroundColor(Color color) {
        if (color == null)
            return;
        this.colorFondoSolido = color;
        if (this.fondoACuadros) {
            this.fondoACuadros = false;
        }
        repaint();
    } // --- Fin del método setSolidBackgroundColor ---

    public void setEditorOverlayInstance(boolean editorOverlayInstance) {
        this.editorOverlayInstance = editorOverlayInstance;
    }

    public boolean isEditorOverlayInstance() {
        return editorOverlayInstance;
    }

    public void setUseEditorZoom(boolean useEditorZoom) {
        this.useEditorZoom = useEditorZoom;
    }

    public void setEditorZoomFactor(double factor) {
        this.editorZoomFactor = Math.max(0.1, Math.min(10.0, factor));
        repaint();
    }

    public double getEditorZoomFactor() {
        return editorZoomFactor;
    }

    public void setEditorOffset(double dx, double dy) {
        this.editorOffsetX += dx;
        this.editorOffsetY += dy;
        repaint();
    }

    public void resetEditorZoom() {
        this.editorZoomFactor = 1.0;
        this.editorOffsetX = 0;
        this.editorOffsetY = 0;
        repaint();
    }

    public void setProjectManager(IProjectManager projectManager) {
        this.projectManager = projectManager;
    } // --- Fin del método setProjectManager ---

    public String getCurrentImageKey() {
        if (model == null) return null;
        String selectedKey = model.getSelectedImageKey();
        if (selectedKey == null) return null;
        java.nio.file.Path fullPath = model.getRutaCompleta(selectedKey);
        if (fullPath != null) return fullPath.toString();
        return selectedKey;
    } // --- Fin del método getCurrentImageKey ---

    public AffineTransform getCurrentImageTransform() {
        return currentImageTransform;
    } // --- Fin del método getCurrentImageTransform ---

    public IProjectManager getProjectManager() {
        return projectManager;
    } // --- Fin del método getProjectManager ---

    public void setCheckeredBackground(boolean activado) {
        if (this.fondoACuadros != activado) {
            this.fondoACuadros = activado;
            repaint();
        }
    } // --- Fin del método setCheckeredBackground ---

    // --- Este método ahora solo actualiza el label. El controlador limpia el
    // modelo. ---
    public void mostrarError(String mensaje, ImageIcon iconoError) {

        this.internalLabel.setText(mensaje);
        this.internalLabel.setIcon(iconoError);
        this.internalLabel.setForeground(Color.RED);

        this.showingWelcome = false; // Desactivar bienvenida si hay un error

        repaint(); // Forzamos repintado para que se vea el error y desaparezca la imagen vieja.
    } // --- Fin del método mostrarError ---

    // --- limpiar() ahora solo limpia el label. El controlador limpia el modelo.
    // ---
    public void limpiar() {

        this.internalLabel.setText(null);
        this.internalLabel.setIcon(null);
        this.isPlaceholderSinImagen = false;
        repaint();
    } // --- Fin del método limpiar ---

    /**
     * Muestra un placeholder especial para archivos que no son imágenes (como STLs).
     * 
     * @param rutaArchivo La ruta del archivo para mostrar su nombre y ubicación.
     */
    public void mostrarPlaceholderArchivoSinImagen(java.nio.file.Path rutaArchivo) {
        if (rutaArchivo == null) return;

        this.isPlaceholderSinImagen = true;
        this.showingWelcome = false;

        String fileName = rutaArchivo.getFileName().toString();
        String location = rutaArchivo.getParent() != null ? rutaArchivo.getParent().toString() : "";

        String html = "<html><body style='text-align: center; color: white; padding: 20px;'>"
                + "<div style='font-size: 16pt; font-weight: bold;'>Este archivo no tiene imagen</div><br><br>"
                + "<div style='font-size: 14pt; color: #EEEEEE;'>" + fileName + "</div><br>"
                + "<div style='font-size: 11pt; color: #AAAAAA;'>" + location + "</div>"
                + "</body></html>";

        this.internalLabel.setText(html);
        this.internalLabel.setForeground(Color.WHITE);
        
        // Configurar posición del icono arriba del texto
        this.internalLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        this.internalLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        this.internalLabel.setIconTextGap(30);

        // Intentar cargar el icono status-warning.png
        try {
            // Intentamos varias rutas comunes por si acaso
            java.net.URL iconUrl = getClass().getResource("/iconos/comunes/status-warning.png");
            if (iconUrl == null) iconUrl = getClass().getResource("/resources/iconos/comunes/status-warning.png");
            
            if (iconUrl != null) {
                this.internalLabel.setIcon(new ImageIcon(iconUrl));
            } else {
                logger.warn("No se encontró el icono status-warning.png en el classpath.");
            }
        } catch (Exception e) {
            logger.warn("Error al cargar el icono para el placeholder: " + e.getMessage());
        }

        repaint();
    }

    /**
     * Muestra un mensaje indicando que la carpeta actual no contiene imágenes compatibles.
     * 
     * @param rutaCarpeta La ruta de la carpeta vacía.
     */
    public void mostrarMensajeCarpetaVacia(java.nio.file.Path rutaCarpeta) {
        if (rutaCarpeta == null) return;

        this.isPlaceholderSinImagen = true;
        this.showingWelcome = false;

        String folderName = rutaCarpeta.getFileName().toString();
        String fullPath = rutaCarpeta.toString();

        String html = "<html><body style='text-align: center; color: white; padding: 20px;'>"
                + "<div style='font-size: 18pt; font-weight: bold; color: #FFCC00;'>Carpeta sin imágenes</div><br><br>"
                + "<div style='font-size: 14pt; color: #EEEEEE;'>La carpeta <b>" + folderName + "</b></div>"
                + "<div style='font-size: 14pt; color: #EEEEEE;'>no contiene archivos de imagen compatibles.</div><br>"
                + "<div style='font-size: 10pt; color: #AAAAAA;'>" + fullPath + "</div>"
                + "</body></html>";

        this.internalLabel.setText(html);
        this.internalLabel.setForeground(Color.WHITE);
        
        this.internalLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        this.internalLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        this.internalLabel.setIconTextGap(30);

        try {
            java.net.URL iconUrl = getClass().getResource("/iconos/comunes/status-warning.png");
            if (iconUrl == null) iconUrl = getClass().getResource("/resources/iconos/comunes/status-warning.png");
            
            if (iconUrl != null) {
                this.internalLabel.setIcon(new ImageIcon(iconUrl));
            }
        } catch (Exception e) {
            logger.warn("Error al cargar el icono para el mensaje de carpeta vacía: " + e.getMessage());
        }

        repaint();
    }

    /**
     * Indica si el panel está mostrando actualmente el placeholder de archivo sin imagen.
     * @return true si el placeholder está activo.
     */
    public boolean isPlaceholderSinImagen() {
        return this.isPlaceholderSinImagen;
    }

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
     * Devuelve si el panel está configurado actualmente para mostrar el fondo a
     * cuadros.
     * 
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

    public boolean isShowingWelcome() {
        return this.showingWelcome;
    } // ---FIN de metodo---

    private static java.util.List<String> wrapText(String text, java.awt.Graphics2D g2d, int maxCharsPerLine) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine.setLength(0);
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        return lines;
    }

    /**
     * Configura botones de navegación (anterior/siguiente) superpuestos en los
     * laterales.
     */
    public void setNavigationActions(javax.swing.Action prevAction, javax.swing.Action nextAction,
            javax.swing.Icon prevIcon, javax.swing.Icon nextIcon) {
        if (prevAction != null && prevIcon != null) {
            javax.swing.JButton prevButton = new javax.swing.JButton(prevAction);
            prevButton.setIcon(prevIcon);
            prevButton.setText("");
            prevButton.setContentAreaFilled(false);
            prevButton.setBorderPainted(false);
            prevButton.setFocusable(false);
            prevButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            navArrowsLeftPanel = new JPanel(new java.awt.GridBagLayout());
            navArrowsLeftPanel.setOpaque(false);
            navArrowsLeftPanel.add(prevButton);
            this.add(navArrowsLeftPanel, BorderLayout.WEST);
        }

        if (nextAction != null && nextIcon != null) {
            javax.swing.JButton nextButton = new javax.swing.JButton(nextAction);
            nextButton.setIcon(nextIcon);
            nextButton.setText("");
            nextButton.setContentAreaFilled(false);
            nextButton.setBorderPainted(false);
            nextButton.setFocusable(false);
            nextButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

            navArrowsRightPanel = new JPanel(new java.awt.GridBagLayout());
            navArrowsRightPanel.setOpaque(false);
            navArrowsRightPanel.add(nextButton);
            this.add(navArrowsRightPanel, BorderLayout.EAST);
        }

        this.revalidate();
        this.repaint();
    } // --- FIN de metodo setNavigationActions ---


    public void setNavigationArrowsVisible(boolean visible) {
        if (navArrowsLeftPanel != null) navArrowsLeftPanel.setVisible(visible);
        if (navArrowsRightPanel != null) navArrowsRightPanel.setVisible(visible);
    } // --- FIN de metodo setNavigationArrowsVisible ---

} // --- FIN DE LA CLASE ImageDisplayPanel ---
