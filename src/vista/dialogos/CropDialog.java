package vista.dialogos;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import servicios.image.ImageEdition;

public class CropDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int HANDLE_SIZE = 8;
    private static final int HANDLE_HIT = 8;

    private final BufferedImage sourceImage;
    private Rectangle cropRect;
    private boolean accepted = false;

    private double zoom = 1.0;
    private double fitScale = 1.0;
    private int panX = 0, panY = 0;
    private int imgX, imgY, imgW, imgH;
    private final CropOverlayPanel overlayPanel;

    private enum Handle { NONE, MOVE, CREATE, PAN, NW, NE, SW, SE, N, S, E, W }
    private Handle currentHandle = Handle.NONE;
    private Point dragStart;
    private Rectangle dragStartRect;
    private Rectangle savedCropRect;

    public CropDialog(JFrame owner, BufferedImage image) {
        super(owner, "Recortar imagen", true);
        this.sourceImage = image;
        this.cropRect = new Rectangle(0, 0, image.getWidth(), image.getHeight());

        overlayPanel = new CropOverlayPanel();
        add(overlayPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setSize(900, 700);
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() { return accepted; }
    public BufferedImage getCroppedImage() {
        return ImageEdition.crop(sourceImage, cropRect.x, cropRect.y, cropRect.width, cropRect.height);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));

        JButton btnAccept = new JButton("Aceptar");
        btnAccept.addActionListener(e -> {
            if (cropRect.width > 0 && cropRect.height > 0) {
                accepted = true;
                dispose();
            }
        });

        JButton btnSaveAs = new JButton("Guardar como...");
        btnSaveAs.addActionListener(e -> guardarComo());

        JButton btnCopy = new JButton("Copiar al portapapeles");
        btnCopy.addActionListener(e -> copiarAlPortapapeles());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnAccept);
        panel.add(btnSaveAs);
        panel.add(btnCopy);
        panel.add(btnCancel);
        return panel;
    }

    private void guardarComo() {
        if (cropRect.width <= 0 || cropRect.height <= 0) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar recorte como");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            BufferedImage cropped = getCroppedImage();
            if (cropped != null) {
                try {
                    ImageIO.write(cropped, "png", file);
                    JOptionPane.showMessageDialog(this, "Imagen guardada en:\n" + file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void copiarAlPortapapeles() {
        if (cropRect.width <= 0 || cropRect.height <= 0) return;
        BufferedImage cropped = getCroppedImage();
        if (cropped != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new Transferable() {
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.imageFlavor};
                    }
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.imageFlavor.equals(flavor);
                    }
                    public Object getTransferData(DataFlavor flavor) {
                        return cropped;
                    }
                }, null
            );
        }
    }

    private double getScale() { return fitScale * zoom; }

    private void updateFitScale() {
        int pw = overlayPanel.getWidth() - 40;
        int ph = overlayPanel.getHeight() - 40;
        if (pw <= 0 || ph <= 0) return;
        double sx = (double) pw / sourceImage.getWidth();
        double sy = (double) ph / sourceImage.getHeight();
        fitScale = Math.min(sx, sy);
    }

    private void updateImgPosition() {
        double s = getScale();
        int cw = overlayPanel.getWidth();
        int ch = overlayPanel.getHeight();
        imgW = (int) (sourceImage.getWidth() * s);
        imgH = (int) (sourceImage.getHeight() * s);
        imgX = (cw - imgW) / 2 + panX;
        imgY = (ch - imgH) / 2 + panY;
    }

    private Rectangle imageToDisplay(Rectangle r) {
        return new Rectangle(
            imgX + (int) (r.x * getScale()),
            imgY + (int) (r.y * getScale()),
            Math.max(1, (int) (r.width * getScale())),
            Math.max(1, (int) (r.height * getScale()))
        );
    }

    private Rectangle displayToImage(Rectangle r) {
        double s = getScale();
        int x = (int) ((r.x - imgX) / s);
        int y = (int) ((r.y - imgY) / s);
        int w = (int) (r.width / s);
        int h = (int) (r.height / s);
        x = Math.max(0, Math.min(x, sourceImage.getWidth() - 1));
        y = Math.max(0, Math.min(y, sourceImage.getHeight() - 1));
        w = Math.max(1, Math.min(w, sourceImage.getWidth() - x));
        h = Math.max(1, Math.min(h, sourceImage.getHeight() - y));
        return new Rectangle(x, y, w, h);
    }

    private Handle getHandleAt(Point p) {
        Rectangle dr = imageToDisplay(cropRect);
        int x = p.x, y = p.y;
        int hs = HANDLE_HIT;

        if (Math.abs(x - dr.x) <= hs && Math.abs(y - dr.y) <= hs) return Handle.NW;
        if (Math.abs(x - (dr.x + dr.width)) <= hs && Math.abs(y - dr.y) <= hs) return Handle.NE;
        if (Math.abs(x - dr.x) <= hs && Math.abs(y - (dr.y + dr.height)) <= hs) return Handle.SW;
        if (Math.abs(x - (dr.x + dr.width)) <= hs && Math.abs(y - (dr.y + dr.height)) <= hs) return Handle.SE;
        if (Math.abs(x - (dr.x + dr.width / 2)) <= hs && Math.abs(y - dr.y) <= hs) return Handle.N;
        if (Math.abs(x - (dr.x + dr.width / 2)) <= hs && Math.abs(y - (dr.y + dr.height)) <= hs) return Handle.S;
        if (Math.abs(y - (dr.y + dr.height / 2)) <= hs && Math.abs(x - dr.x) <= hs) return Handle.W;
        if (Math.abs(y - (dr.y + dr.height / 2)) <= hs && Math.abs(x - (dr.x + dr.width)) <= hs) return Handle.E;
        if (dr.contains(p)) return Handle.MOVE;
        return Handle.NONE;
    }

    private Cursor getCursorForHandle(Handle h) {
        switch (h) {
            case NW: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case NE: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case SW: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case SE: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            case N:  return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case S:  return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case W:  return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case E:  return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case CREATE: return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            case MOVE: return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            default: return Cursor.getDefaultCursor();
        }
    }

    /**
     * Panel central que dibuja la imagen, el overlay oscuro y el rectángulo de selección.
     */
    private class CropOverlayPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        CropOverlayPanel() {
            setBackground(Color.DARK_GRAY);
            setBorder(new LineBorder(new Color(100, 100, 100), 2, true));

            MouseAdapter ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        currentHandle = Handle.PAN;
                        dragStart = e.getPoint();
                        dragStartRect = new Rectangle(imageToDisplay(cropRect));
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        return;
                    }
                    Handle h = getHandleAt(e.getPoint());
                    if (h == Handle.NONE) {
                        currentHandle = Handle.CREATE;
                        savedCropRect = new Rectangle(cropRect);
                        dragStartRect = new Rectangle(imageToDisplay(cropRect));
                        dragStart = e.getPoint();
                    } else {
                        currentHandle = h;
                        dragStart = e.getPoint();
                        dragStartRect = new Rectangle(imageToDisplay(cropRect));
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (currentHandle == Handle.CREATE && dragStart != null) {
                        int w = Math.abs(e.getX() - dragStart.x);
                        int h = Math.abs(e.getY() - dragStart.y);
                        if (w <= 5 || h <= 5) {
                            cropRect = savedCropRect;
                        }
                    } else if (currentHandle == Handle.PAN) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    if (cropRect.width < 1) cropRect.width = 1;
                    if (cropRect.height < 1) cropRect.height = 1;
                    currentHandle = Handle.NONE;
                    dragStart = null;
                    dragStartRect = null;
                    repaint();
                }
            };

            MouseMotionAdapter mml = new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart == null || dragStartRect == null) return;
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    Rectangle dr = new Rectangle(dragStartRect);

                    switch (currentHandle) {
                        case CREATE: {
                            int x = Math.min(dragStart.x, e.getX());
                            int y = Math.min(dragStart.y, e.getY());
                            int w = Math.abs(e.getX() - dragStart.x);
                            int h = Math.abs(e.getY() - dragStart.y);
                            cropRect = displayToImage(new Rectangle(x, y, w, h));
                            repaint();
                            return;
                        }
                        case PAN: {
                            panX += e.getX() - dragStart.x;
                            panY += e.getY() - dragStart.y;
                            dragStart = e.getPoint();
                            repaint();
                            return;
                        }
                        case MOVE: {
                            dr.x += dx; dr.y += dy;
                            break;
                        }
                        case NW: dr.x += dx; dr.y += dy; dr.width -= dx; dr.height -= dy; break;
                        case NE: dr.y += dy; dr.width += dx; dr.height -= dy; break;
                        case SW: dr.x += dx; dr.width -= dx; dr.height += dy; break;
                        case SE: dr.width += dx; dr.height += dy; break;
                        case N:  dr.y += dy; dr.height -= dy; break;
                        case S:  dr.height += dy; break;
                        case W:  dr.x += dx; dr.width -= dx; break;
                        case E:  dr.width += dx; break;
                        default: return;
                    }

                    if (dr.width < 5) dr.width = 5;
                    if (dr.height < 5) dr.height = 5;
                    cropRect = displayToImage(dr);
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(getCursorForHandle(getHandleAt(e.getPoint())));
                }
            };

            addMouseListener(ml);
            addMouseMotionListener(mml);
            addMouseWheelListener(e -> {
                updateFitScale();
                double oldS = fitScale * zoom;
                int cw = getWidth();
                int ch = getHeight();
                int curImgX = (cw - (int)(sourceImage.getWidth() * oldS)) / 2 + panX;
                int curImgY = (ch - (int)(sourceImage.getHeight() * oldS)) / 2 + panY;
                double imgRelX = (e.getX() - curImgX) / oldS;
                double imgRelY = (e.getY() - curImgY) / oldS;
                zoom *= Math.pow(1.1, -e.getWheelRotation());
                zoom = Math.max(0.1, Math.min(20.0, zoom));
                double newS = fitScale * zoom;
                int centeredX = (cw - (int)(sourceImage.getWidth() * newS)) / 2;
                int centeredY = (ch - (int)(sourceImage.getHeight() * newS)) / 2;
                panX = (int)(e.getX() - imgRelX * newS) - centeredX;
                panY = (int)(e.getY() - imgRelY * newS) - centeredY;
                repaint();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (sourceImage == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            updateFitScale();
            updateImgPosition();

            // Dibujar imagen
            g2.drawImage(sourceImage, imgX, imgY, imgW, imgH, null);

            // Overlay oscuro fuera del rectángulo de selección
            Rectangle dr = imageToDisplay(cropRect);
            g2.setColor(new Color(0, 0, 0, 160));
            Area overlay = new Area(new Rectangle(imgX, imgY, imgW, imgH));
            overlay.subtract(new Area(dr));
            g2.fill(overlay);

            // Borde del rectángulo de selección
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(dr);

            // Controles de esquina
            g2.setColor(Color.WHITE);
            drawHandle(g2, dr.x, dr.y);
            drawHandle(g2, dr.x + dr.width, dr.y);
            drawHandle(g2, dr.x, dr.y + dr.height);
            drawHandle(g2, dr.x + dr.width, dr.y + dr.height);

            // Controles de punto medio
            drawHandle(g2, dr.x + dr.width / 2, dr.y);
            drawHandle(g2, dr.x + dr.width / 2, dr.y + dr.height);
            drawHandle(g2, dr.x, dr.y + dr.height / 2);
            drawHandle(g2, dr.x + dr.width, dr.y + dr.height / 2);

            // Información
            String info = String.format("%d x %d px", cropRect.width, cropRect.height);
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(dr.x, dr.y - 24, g2.getFontMetrics().stringWidth(info) + 10, 20);
            g2.setColor(Color.WHITE);
            g2.drawString(info, dr.x + 5, dr.y - 8);

            g2.dispose();
        }

        private void drawHandle(Graphics2D g2, int x, int y) {
            int hs = HANDLE_SIZE;
            g2.fillRect(x - hs / 2, y - hs / 2, hs, hs);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawRect(x - hs / 2, y - hs / 2, hs, hs);
            g2.setColor(Color.WHITE);
        }
    }

}
