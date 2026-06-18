package controlador.actions.archivo;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

/**
 * Acción para imprimir la imagen actual.
 */
public class PrintAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;


    /**
     * Constructor para PrintAction.
     *
     * @param name  El nombre de la acción.
     * @param icon  El icono de la acción.
     * @param model El modelo del visor.
     */
    public PrintAction(String name, ImageIcon icon, VisorModel model) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Imprimir la imagen actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ARCHIVO_IMPRIMIR);
    } // --- Fin del método PrintAction ---


    /**
     * Ejecuta la acción de imprimir la imagen actual.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        BufferedImage img = model.getCurrentImage();
        if (img == null) return;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Visor de Imágenes - Imprimir");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics g = graphics.create();
            double pageW = pageFormat.getImageableWidth();
            double pageH = pageFormat.getImageableHeight();
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            double scale = Math.min(pageW / imgW, pageH / imgH);
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int drawX = (int) (pageFormat.getImageableX() + (pageW - drawW) / 2);
            int drawY = (int) (pageFormat.getImageableY() + (pageH - drawH) / 2);

            g.drawImage(img, drawX, drawY, drawW, drawH, null);
            g.dispose();
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            new Thread(() -> {
                try {
                    job.print();
                } catch (Exception ex) {
                    javax.swing.JOptionPane.showMessageDialog(null,
                        "Error al imprimir: " + ex.getMessage(),
                        "Error de impresión", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }).start();
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en la imagen actual y el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        setEnabled(model.getCurrentImage() != null && model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase PrintAction ---
