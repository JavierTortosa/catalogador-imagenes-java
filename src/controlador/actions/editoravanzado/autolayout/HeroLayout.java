package controlador.actions.editoravanzado.autolayout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import modelo.editor.Layer;

/**
 * Layout Hero: una capa principal (la capa activa si pertenece a la selección,
 * si no la de mayor área visible) ocupa la superficie principal de la
 * composición y el resto se distribuye en una cuadrícula a su lado.
 */
public final class HeroLayout implements AutoLayoutAlgorithm {

    public void layout(AutoLayoutContext ctx) {
        List<Layer> targets = AutoLayoutSupport.getTargets(ctx);
        if (targets.isEmpty()) {
            return;
        }
        AutoLayoutSupport.apartarNoSeleccionadas(ctx);

        Layer hero = elegirHero(ctx, targets);
        if (hero == null) {
            return;
        }
        List<Layer> resto = new ArrayList<>(targets);
        resto.remove(hero);

        int cw = ctx.getCanvasModel().getWidth();
        int ch = ctx.getCanvasModel().getHeight();
        int margin = ctx.getConfig().getMargin();
        double heroScale = ctx.getConfig().getHeroScale();
        double usableW = Math.max(1, cw - 2.0 * margin);
        double usableH = Math.max(1, ch - 2.0 * margin);
        double heroW = usableW * heroScale;
        double heroH = usableH;

        Rectangle hb = hero.getBounds();
        if (hb != null) {
            double scale = Math.min(heroW / hb.width, heroH / hb.height);
            int newW = Math.max(1, (int) Math.round(hb.width * scale));
            int newH = Math.max(1, (int) Math.round(hb.height * scale));
            int newX = margin + (int) Math.round((heroW - newW) / 2.0);
            int newY = margin + (int) Math.round((heroH - newH) / 2.0);
            hero.setBounds(new Rectangle(newX, newY, newW, newH));
        }

        if (resto.isEmpty()) {
            return;
        }
        double restX0 = margin + heroW;
        double restW = Math.max(1, usableW - heroW);
        int m = resto.size();
        int cols = (int) Math.ceil(Math.sqrt(m));
        int rows = (int) Math.ceil((double) m / cols);
        double cellW = restW / cols;
        double cellH = usableH / rows;

        for (int i = 0; i < m; i++) {
            Layer layer = resto.get(i);
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            int col = i % cols;
            int row = i / cols;
            double scale = Math.min(cellW / b.width, cellH / b.height);
            int newW = Math.max(1, (int) Math.round(b.width * scale));
            int newH = Math.max(1, (int) Math.round(b.height * scale));
            int newX = (int) Math.round(restX0 + col * cellW + (cellW - newW) / 2.0);
            int newY = margin + (int) Math.round(row * cellH + (cellH - newH) / 2.0);
            layer.setBounds(new Rectangle(newX, newY, newW, newH));
        }
    } // --- Fin del metodo layout ---


    /**
     * Capa activa si está dentro de la selección; en caso contrario la de mayor área.
     */
    private Layer elegirHero(AutoLayoutContext ctx, List<Layer> targets) {
        Layer activa = ctx.getLayerModel().getActiveLayer();
        if (activa != null && targets.contains(activa)) {
            return activa;
        }
        Layer mayor = null;
        double mejorArea = -1;
        for (Layer layer : targets) {
            Rectangle b = layer.getBounds();
            if (b == null) {
                continue;
            }
            double area = b.width * (double) b.height;
            if (area > mejorArea) {
                mejorArea = area;
                mayor = layer;
            }
        }
        return mayor;
    } // --- Fin del metodo elegirHero ---

} // --- Fin de la clase HeroLayout ---
