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
import modelo.proyecto.ExportItem;

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
			logger.warn("Error cargando imagen para PDF: {}", item.getRutaImagen(), e);
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

