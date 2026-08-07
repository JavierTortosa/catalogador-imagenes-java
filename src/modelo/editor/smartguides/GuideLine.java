package modelo.editor.smartguides;

/**
 * Línea guía temporal que se dibuja durante el arrastre cuando se produce un
 * ajuste. Las guías se representan en coordenadas de canvas.
 *
 * @param vertical true si es una guía vertical (alineación en X), false si es horizontal
 * @param pos posición de la guía en el eje alineado (coordenada X o Y)
 * @param start inicio del segmento a lo largo del otro eje
 * @param end fin del segmento a lo largo del otro eje
 */
public record GuideLine(boolean vertical, int pos, int start, int end) {
}
