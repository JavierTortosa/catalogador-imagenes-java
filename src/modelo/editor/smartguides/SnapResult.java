package modelo.editor.smartguides;

import java.util.List;

/**
 * Resultado de un cálculo de ajuste del motor de Smart Guides.
 *
 * @param dx desplazamiento X corregido (ya aplicado el snap) que debe usarse para recolocar las capas
 * @param dy desplazamiento Y corregido (ya aplicado el snap)
 * @param guides líneas guía activas para dibujar durante este evento
 */
public record SnapResult(int dx, int dy, List<GuideLine> guides) {
}
