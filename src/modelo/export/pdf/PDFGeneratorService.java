package modelo.export.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import modelo.proyecto.CommentThread;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectImage;
import modelo.proyecto.SelectionState;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PDFGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(PDFGeneratorService.class);

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN = 25;
    private static final float TOP = 35;
    private static final float BOTTOM = 25;

    private static final int COLS = 2;
    private static final int ROWS = 2;
    private static final int ITEMS_PER_PAGE = COLS * ROWS;
    private static final float GAP = 10;

    private static final float BOX_W = (PAGE_W - 2 * MARGIN - (COLS - 1) * GAP) / COLS;
    private static final float BOX_H = (PAGE_H - TOP - BOTTOM - (ROWS - 1) * GAP) / ROWS;
    private static final float PADDING = 6;

    // Genera el PDF con los items de exportación en una cuadrícula 2x2, más las notas del proyecto al final
    public void crearPresupuesto(List<ExportItem> items, File destino, String notasProyecto) throws Exception {
        System.setProperty("org.apache.pdfbox.io.IOUtils.unmapSupported", "false");

        try (PDDocument doc = new PDDocument()) {
            int index = 0;
            while (index < items.size()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.setFont(new PDType1Font(FontName.HELVETICA_BOLD), 14);
                    cs.beginText();
                    cs.newLineAtOffset(PAGE_W / 2 - 50, PAGE_H - 15);
                    showText(cs, "Catálogo");
                    cs.endText();

                    for (int cell = 0; cell < ITEMS_PER_PAGE && index < items.size(); cell++, index++) {
                        ExportItem item = items.get(index);
                        int col = cell % COLS;
                        int row = cell / COLS;

                        float bx = MARGIN + col * (BOX_W + GAP);
                        float by = PAGE_H - TOP - (row + 1) * BOX_H - row * GAP;

                        drawBox(cs, bx, by, BOX_W, BOX_H, item, doc);
                    }
                }
            }

            if (notasProyecto != null && !notasProyecto.isBlank()) {
                drawProjectCommentPages(doc, notasProyecto);
            }

            doc.save(destino);
        }
    } // --- Fin del metodo: crearPresupuesto ---


    /**
     * Genera un PDF simple cuadrícula 2x2 (4 imágenes por página) con
     * código, precio, imagen y comentarios auto-contenidos por celda.
     */
    public void crearPresupuestoCliente(List<ProjectImage> imagenes, File destino, String notasProyecto) throws Exception {
        System.setProperty("org.apache.pdfbox.io.IOUtils.unmapSupported", "false");

        PDFont bold = new PDType1Font(FontName.HELVETICA_BOLD);
        PDFont normal = new PDType1Font(FontName.HELVETICA);
        PDFont italic = new PDType1Font(FontName.HELVETICA_OBLIQUE);

        try (PDDocument doc = new PDDocument()) {
            int index = 0;
            while (index < imagenes.size()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    for (int cell = 0; cell < ITEMS_PER_PAGE && index < imagenes.size(); cell++, index++) {
                        ProjectImage pi = imagenes.get(index);
                        int col = cell % COLS;
                        int row = cell / COLS;
                        float bx = MARGIN + col * (BOX_W + GAP);
                        float by = PAGE_H - TOP - (row + 1) * BOX_H - row * GAP;
                        drawClientBox(cs, bx, by, BOX_W, BOX_H, pi, doc, bold, normal, italic);
                    }
                }
            }

            // Página(s) de resumen con tabla de precios
            double total = imagenes.stream().mapToDouble(ProjectImage::getPrice).sum();
            drawResumenPage(doc, imagenes, total, notasProyecto, bold, normal, italic);

            doc.save(destino);
        }
    }

    private void drawResumenPage(PDDocument doc, List<ProjectImage> imagenes, double total,
            String notasProyecto, PDFont bold, PDFont normal, PDFont italic) throws Exception {
        float[] cols = { MARGIN, 100, MARGIN + 320 };
        float colW = PAGE_W - 2 * MARGIN;
        float rowH = 14;
        int itemsPerPage = 18;
        int start = 0;

        // Cargar logo (solo para primera página)
        BufferedImage logo = null;
        try {
            java.net.URL logoUrl = getClass().getResource("/iconos/comunes/application/Parabellum Print & Paint black.png");
            if (logoUrl != null) {
                logo = ImageIO.read(logoUrl);
            }
        } catch (Exception e) {
            logger.debug("No se pudo cargar el logo para el PDF");
        }

        String fecha = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());

        while (start < imagenes.size()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            int end = Math.min(start + itemsPerPage, imagenes.size());
            boolean isFirstPage = start == 0;
            boolean isLastPage = end >= imagenes.size();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H - MARGIN;

                if (isFirstPage && logo != null) {
                    // Logo a la izquierda
                    float logoMaxW = 160;
                    float logoScale = Math.min(logoMaxW / logo.getWidth(), 50f / logo.getHeight());
                    float lw = logo.getWidth() * logoScale;
                    float lh = logo.getHeight() * logoScale;
                    PDImageXObject pdLogo = LosslessFactory.createFromImage(doc, logo);
                    cs.drawImage(pdLogo, MARGIN, y - lh, lw, lh);

                    // Título a la derecha del logo
                    cs.setFont(bold, 18);
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN + lw + 15, y - 14);
                    showText(cs, "Presupuesto");
                    cs.endText();

                    // Fecha
                    cs.setFont(normal, 10);
                    cs.setNonStrokingColor(new Color(100, 100, 100));
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN + lw + 15, y - 30);
                    showText(cs, fecha);
                    cs.endText();

                    // Teléfono debajo de la fecha
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN + lw + 15, y - 44);
                    showText(cs, "Tel.: 681.81.82.40");
                    cs.endText();
                    

                    y -= Math.max(lh, 40) + 5;
                } else {
                    // Título + fecha centrados en páginas siguientes
                    cs.setFont(bold, 16);
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y - 10);
                    showText(cs, "Presupuesto (cont.)");
                    cs.endText();
                    y -= 20;
                }

                // Línea decorativa
                y -= 6;
                cs.setStrokingColor(0.7f, 0.7f, 0.7f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_W - MARGIN, y);
                cs.stroke();
                y -= 12;

                // Encabezados de columna
                cs.setFont(bold, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                showText(cs, "Código");
                cs.endText();
                cs.beginText();
                cs.newLineAtOffset(cols[1], y);
                showText(cs, "Nombre");
                cs.endText();
                cs.beginText();
                cs.newLineAtOffset(cols[2], y);
                showText(cs, "PVP");
                cs.endText();

                // Línea separadora
                y -= 4;
                cs.setStrokingColor(0.6f, 0.6f, 0.6f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_W - MARGIN, y);
                cs.stroke();

                // Filas
                y -= rowH - 2;
                cs.setFont(normal, 9);
                cs.setNonStrokingColor(0, 0, 0);

                for (int i = start; i < end; i++) {
                    ProjectImage pi = imagenes.get(i);

                    String codigo = pi.getCodigoCatalogo() != null ? pi.getCodigoCatalogo() : "-";
                    String nombre = new File(pi.getRutaImagen()).getName();
                    String pvp = String.format("%.2f   EUR", pi.getPrice());

                    if (textWidth(nombre, normal, 9) > colW - 240) {
                        nombre = ellipsizeToWidth(nombre, normal, 9, colW - 240);
                    }

                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y);
                    showText(cs, codigo);
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(cols[1], y);
                    showText(cs, nombre);
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(cols[2], y);
                    showText(cs, pvp);
                    cs.endText();

                    y -= rowH;
                }

                // Total + notas solo en última página
                if (isLastPage) {
                    y -= 6;
                    cs.setStrokingColor(0.6f, 0.6f, 0.6f);
                    cs.moveTo(MARGIN, y);
                    cs.lineTo(PAGE_W - MARGIN, y);
                    cs.stroke();

                    y -= rowH + 2;
                    cs.setFont(bold, 12);
                    cs.setNonStrokingColor(new Color(0, 100, 0));
                    cs.beginText();
                    cs.newLineAtOffset(cols[2] - 30, y);
                    showText(cs, "Total:");
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(cols[2], y);
                    showText(cs, String.format("%.2f   EUR", total));
                    cs.endText();

                    // Notas del proyecto
                    if (notasProyecto != null && !notasProyecto.isBlank()) {
                        y -= 30;
                        cs.setFont(bold, 10);
                        cs.setNonStrokingColor(0, 0, 0);
                        cs.beginText();
                        cs.newLineAtOffset(MARGIN, y);
                        showText(cs, "Comentario del proyecto:");
                        cs.endText();

                        y -= 18;
                        cs.setFont(normal, 9);
                        List<String> lines = wrapText(notasProyecto, normal, 9, colW);
                        for (String line : lines) {
                            if (y < BOTTOM + 14) break;
                            cs.beginText();
                            cs.newLineAtOffset(MARGIN, y);
                            showText(cs, line);
                            cs.endText();
                            y -= 12;
                        }
                    }
                }

                start = end;
            }
        }
    }

    private void drawClientBox(PDPageContentStream cs, float x, float y, float w, float h,
            ProjectImage pi, PDDocument doc, PDFont bold, PDFont normal, PDFont italic) throws Exception {
        cs.setStrokingColor(0.8f, 0.8f, 0.8f);
        cs.addRect(x, y, w, h);
        cs.stroke();

        float pad = 6;
        // Código + Nombre + Precio en la misma línea
        String codigo = pi.getCodigoCatalogo() != null ? pi.getCodigoCatalogo() : "-";
        String nombre = pi.getRutaImagen() != null
                ? new java.io.File(pi.getRutaImagen()).getName().replaceAll("\\.[^.]+$", "")
                : "";
        String codigoYNombre = codigo + (nombre.isEmpty() ? "" : " - " + nombre);
        String precio = String.format("%.2f   EUR", pi.getPrice());

        float precioW = bold.getStringWidth(precio) / 1000f * 10f;
        float maxNombreW = (w - pad * 2) - precioW - 10;
        if (textWidth(codigoYNombre, bold, 10) > maxNombreW) {
            codigoYNombre = ellipsizeToWidth(codigoYNombre, bold, 10, maxNombreW);
        }

        cs.setFont(bold, 10);
        cs.setNonStrokingColor(0, 0, 0);
        cs.beginText();
        cs.newLineAtOffset(x + pad, y + h - 14);
        showText(cs, codigoYNombre);
        cs.endText();

        cs.setFont(bold, 10);
        cs.setNonStrokingColor(new Color(0, 100, 0));
        cs.beginText();
        cs.newLineAtOffset(x + w - pad - precioW, y + h - 14);
        showText(cs, precio);
        cs.endText();

        // Línea separadora
        float sepY = y + h - 20;
        cs.setStrokingColor(0.85f, 0.85f, 0.85f);
        cs.moveTo(x + pad, sepY);
        cs.lineTo(x + w - pad, sepY);
        cs.stroke();

        // Obtener checkboxes y último mensaje del hilo de chat para calcular espacio necesario
        List<ImageCheckboxOverlay> cbs = pi.getCheckboxes();
        CommentThread thread = pi.getCommentThreadAccess();
        String imgComment = (thread != null) ? thread.getLastText() : (pi.getComment() != null ? pi.getComment() : "");
        int numCbs = cbs != null ? cbs.size() : 0;
        boolean hasMsg = imgComment != null && !imgComment.isBlank();
        float commentAreaH = Math.max(40, (numCbs + (hasMsg ? 1 : 0)) * 8 + 8);

        // Imagen: área entre separador superior y zona de comentarios
        float imgTop = sepY - 4;
        float imgBottom = y + commentAreaH;
        float imgH = imgTop - imgBottom;
        if (imgH <= 0) imgH = 1;
        float imgW = w - pad * 2;

        try {
            BufferedImage bi = ImageIO.read(new File(pi.getRutaImagen()));
            if (bi != null) {
                float scale = Math.min(imgW / bi.getWidth(), imgH / bi.getHeight());
                float dw = bi.getWidth() * scale;
                float dh = bi.getHeight() * scale;
                float dx = x + (w - dw) / 2f;
                float dy = imgBottom + (imgH - dh) / 2f;
                PDImageXObject pdImg = LosslessFactory.createFromImage(doc, bi);
                cs.drawImage(pdImg, dx, dy, dw, dh);
            }
        } catch (Exception e) {
            logger.debug("No se pudo incluir imagen en PDF cliente: {}", pi.getRutaImagen());
        }

        // Checkboxes + último mensaje debajo de la imagen
        float commentY = y + commentAreaH - 8;
        cs.setFont(normal, 6);
        cs.setNonStrokingColor(new Color(60, 60, 60));

        if (cbs != null && !cbs.isEmpty()) {
            float maxW = w - pad * 2;
            for (ImageCheckboxOverlay cb : cbs) {
                String cbText = (cb.getLabel() != null ? cb.getLabel() : cb.getCheckboxCode())
                        + ": " + estadoStr(cb.getState());
                if (cb.getComment() != null && !cb.getComment().isBlank()) {
                    cbText += " " + cb.getComment();
                }
                if (textWidth(cbText, normal, 6) > maxW) {
                    cbText = ellipsizeToWidth(cbText, normal, 6, maxW);
                }
                cs.beginText();
                cs.newLineAtOffset(x + pad, commentY);
                showText(cs, cbText);
                cs.endText();
                commentY -= 8;
            }
        }

        if (hasMsg) {
            float maxW = w - pad * 2;
            String commentLine = imgComment.length() > 60 ? imgComment.substring(0, 57) + "..." : imgComment;
            if (textWidth(commentLine, italic, 6) > maxW) {
                commentLine = ellipsizeToWidth(commentLine, italic, 6, maxW);
            }
            cs.setFont(italic, 6);
            cs.setNonStrokingColor(new Color(80, 80, 80));
            cs.beginText();
            cs.newLineAtOffset(x + pad, commentY);
            showText(cs, commentLine);
            cs.endText();
        }
    }

    private String estadoStr(SelectionState s) {
        if (s == null) return "?";
        return switch (s) {
            case SELECTED -> "V";
            case DISCARDED -> "X";
            case UNDEFINED -> "—";
        };
    }


    // Dibuja la caja individual de cada item: código, imagen, y datos de archivos
	private void drawBox(PDPageContentStream cs, float x, float y, float w, float h, ExportItem item, PDDocument doc)
			throws Exception
	{
		cs.setStrokingColor(0.65f, 0.65f, 0.65f);
		cs.addRect(x, y, w, h);
		cs.stroke();

		PDFont bold = new PDType1Font(FontName.HELVETICA_BOLD);
		PDFont normal = new PDType1Font(FontName.HELVETICA);
		PDFont italic = new PDType1Font(FontName.HELVETICA_OBLIQUE);

		float padding = PADDING;
		float fontSize = 8;

		float codigoH = 20f;
		float infoH = 48f;

		String codigo = "Código: " + valueOrDash(item.getCodigoCatalogo());
		float codigoFontSize = 11f;
		codigo = ellipsizeToWidth(codigo, bold, codigoFontSize, w - padding * 2);
		float codigoWidth = bold.getStringWidth(codigo) / 1000f * codigoFontSize;
		float codigoX = x + (w - codigoWidth) / 2f;
		float codigoY = y + h - 16;
		cs.beginText();
		cs.setFont(bold, codigoFontSize);
		cs.setNonStrokingColor(0, 0, 0);
		cs.newLineAtOffset(codigoX, codigoY);
		showText(cs, codigo);
		cs.endText();

		float infoY = y + 2;
		float separatorY = infoY + infoH;
		float imgAreaY = separatorY + 4;
		float imgAreaH = (y + h - codigoH - 4) - imgAreaY;
		float imgAreaX = x + padding;
		float imgAreaW = w - (padding * 2);

		try
		{
			BufferedImage bi = ImageIO.read(item.getRutaImagen().toFile());
			if (bi != null)
			{
				float scale = Math.min(imgAreaW / bi.getWidth(), imgAreaH / bi.getHeight());
				float dw = bi.getWidth() * scale;
				float dh = bi.getHeight() * scale;
				float dx = imgAreaX + (imgAreaW - dw) / 2f;
				float dy = imgAreaY + (imgAreaH - dh) / 2f;
				PDImageXObject pdImg = LosslessFactory.createFromImage(doc, bi);
				cs.drawImage(pdImg, dx, dy, dw, dh);
			}
		} catch (Exception e)
		{
			logger.debug("No se pudo incluir la imagen en el PDF (probablemente no existe): {}", item.getRutaImagen());
		}

		cs.setStrokingColor(0.85f, 0.85f, 0.85f);
		cs.moveTo(x + padding, separatorY);
		cs.lineTo(x + w - padding, separatorY);
		cs.stroke();
		cs.setFont(normal, fontSize);
		cs.setNonStrokingColor(0, 0, 0);

		// Sección de datos: Notas + 3 filas con 3 columnas cada una
		int piezasCS = item.getPiezasConSoporte();
		int piezasSS = item.getPiezasSinSoporte();
		int totalPiezas = item.getPiezas() > 0 ? item.getPiezas() : 0;
		boolean hasLychee = item.hasLychee();
		boolean hasChitubox = item.hasChitubox();
		String totalSizeStr = String.format("%.1f MB", item.getTotalSizeMb());

		float colW = (w - padding * 2) / 3f;
		float rowH = 11f;
		float baseY = infoY + 2;

		// Notas (primera línea, itálica, ancho completo)
		String notas = item.getNotas();
		if (notas != null && !notas.isBlank())
		{
			notas = ellipsizeToWidth(notas, italic, 7, w - padding * 2);
			cs.beginText();
			cs.setFont(italic, 7);
			cs.setNonStrokingColor(new Color(110, 110, 110));
			cs.newLineAtOffset(x + padding, baseY + rowH * 3 + 2);
			showText(cs, notas);
			cs.endText();
		}

		// Fila 1: Piezas  C/S | Lychee | Tamaño Total
		float row1Y = baseY + rowH * 2;
		drawInfoCell(cs, normal, "Piezas C/S: " + (piezasCS > 0 ? String.valueOf(piezasCS) : "-"), x + padding, colW, row1Y);
		String lycheeStr = "Lychee: " + (hasLychee ? "[x]" : "[ ]");
		drawInfoCell(cs, normal, lycheeStr, x + padding + colW, colW, row1Y);
		drawInfoCell(cs, normal, "Tamaño: " + totalSizeStr, x + padding + colW * 2, colW, row1Y);

		// Fila 2: Piezas  S/S | Chitubox | (vacío)
		float row2Y = baseY + rowH;
		drawInfoCell(cs, normal, "Piezas S/S: " + (piezasSS > 0 ? String.valueOf(piezasSS) : "-"), x + padding, colW, row2Y);
		String chituboxStr = "Chitubox: " + (hasChitubox ? "[x]" : "[ ]");
		drawInfoCell(cs, normal, chituboxStr, x + padding + colW, colW, row2Y);

		// Fila 3: Piezas  Totales | LVL | PVP
		float row3Y = baseY;
		drawInfoCell(cs, normal, "Piezas: " + (totalPiezas > 0 ? String.valueOf(totalPiezas) : "-"), x + padding, colW, row3Y);
		drawInfoCell(cs, normal, "LVL: " + valueOrDash(item.getLvl()), x + padding + colW, colW, row3Y);
		drawInfoCell(cs, normal, "PVP: " + valueOrDash(item.getPvp()), x + padding + colW * 2, colW, row3Y);
	} // --- Fin del metodo: drawBox ---


	private void drawInfoCell(PDPageContentStream cs, PDFont font, String text, float colX, float colW, float y) throws IOException {
		String truncated = ellipsizeToWidth(text, font, 8, colW - 2);
		cs.beginText();
		cs.setFont(font, 8);
		cs.setNonStrokingColor(0, 0, 0);
		cs.newLineAtOffset(colX + 1, y);
		showText(cs, truncated);
		cs.endText();
	}


    // Dibuja las páginas de comentarios del proyecto al final del PDF
    private void drawProjectCommentPages(PDDocument doc, String comentario) throws IOException {
        PDFont titleFont = new PDType1Font(FontName.HELVETICA_BOLD);
        PDFont bodyFont = new PDType1Font(FontName.HELVETICA);
        float bodyFontSize = 11f;
        float lineHeight = 15f;
        float contentWidth = PAGE_W - MARGIN * 2;
        List<String> lines = wrapText(comentario, bodyFont, bodyFontSize, contentWidth);

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        try {
            float textY = drawCommentPageHeader(cs, titleFont);
            cs.setFont(bodyFont, bodyFontSize);
            cs.setNonStrokingColor(0, 0, 0);

            for (String line : lines) {
                if (textY < BOTTOM + lineHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    textY = drawCommentPageHeader(cs, titleFont);
                    cs.setFont(bodyFont, bodyFontSize);
                    cs.setNonStrokingColor(0, 0, 0);
                }

                if (!line.isEmpty()) {
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, textY);
                    showText(cs, line);
                    cs.endText();
                }
                textY -= lineHeight;
            }
        } finally {
            cs.close();
        }
    } // --- Fin del metodo: drawProjectCommentPages ---


    // Dibuja el encabezado de la página de comentarios (título y línea separadora)
    private float drawCommentPageHeader(PDPageContentStream cs, PDFont titleFont) throws IOException {
        cs.setFont(titleFont, 16);
        cs.setNonStrokingColor(0, 0, 0);
        cs.beginText();
        cs.newLineAtOffset(MARGIN, PAGE_H - 40);
        showText(cs, "Comentario del proyecto");
        cs.endText();

        cs.setStrokingColor(0.75f, 0.75f, 0.75f);
        cs.moveTo(MARGIN, PAGE_H - 52);
        cs.lineTo(PAGE_W - MARGIN, PAGE_H - 52);
        cs.stroke();

        return PAGE_H - 75;
    } // --- Fin del metodo: drawCommentPageHeader ---


    // Envuelve un texto largo en líneas que encajan dentro del ancho máximo disponible
    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            String safeParagraph = safePdfText(paragraph).trim();
            if (safeParagraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            for (String word : safeParagraph.split("\\s+")) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine.setLength(0);
                    }
                    appendWrappedWord(lines, currentLine, word, font, fontSize, maxWidth);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    } // --- Fin del metodo: wrapText ---


    // Parte una palabra demasiado larga en fragmentos que quepan en el ancho disponible
    private void appendWrappedWord(List<String> lines, StringBuilder currentLine, String word, PDFont font,
            float fontSize, float maxWidth) throws IOException {
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            String candidate = chunk.toString() + word.charAt(i);
            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                chunk.append(word.charAt(i));
            } else {
                if (chunk.length() > 0) {
                    lines.add(chunk.toString());
                    chunk.setLength(0);
                }
                chunk.append(word.charAt(i));
            }
        }
        currentLine.append(chunk);
    } // --- Fin del metodo: appendWrappedWord ---


    // Retorna el valor o un guión si es nulo o vacío
    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value.trim() : "-";
    } // --- Fin del metodo: valueOrDash ---


    // Escribe texto seguro en el PDF, escapando caracteres no soportados
    private void showText(PDPageContentStream cs, String text) throws IOException {
        cs.showText(safePdfText(text));
    } // --- Fin del metodo: showText ---


    // Recorta un texto con puntos suspensivos si excede el ancho máximo
    private String ellipsizeToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String safe = safePdfText(text);
        if (textWidth(safe, font, fontSize) <= maxWidth) {
            return safe;
        }

        String suffix = "...";
        int maxLength = safe.length();
        while (maxLength > 0) {
            String candidate = safe.substring(0, maxLength).stripTrailing() + suffix;
            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                return candidate;
            }
            maxLength--;
        }
        return suffix;
    } // --- Fin del metodo: ellipsizeToWidth ---


    // Calcula el ancho en puntos de un texto con la fuente y tamaño dados
    private float textWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    } // --- Fin del metodo: textWidth ---


    // Limpia y normaliza un texto eliminando caracteres que PDFBox no puede mostrar
    private String safePdfText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace('\u2014', '-')
                .replace('\u2013', '-')
                .replace('\u2212', '-')
                .replace("\u2026", "...")
                .replace("\u20ac", " EUR")
                .replace("\u00a3", " GBP")
                .replace("\u00a5", " JPY")
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u00a0', ' ');
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if (ch >= 32 && ch <= 255) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    } // --- Fin del metodo: safePdfText ---


} // --- Fin de la Clase PDFGeneratorService ---

