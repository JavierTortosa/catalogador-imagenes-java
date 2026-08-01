package controlador.tools.editors;

import controlador.tools.ToolContext;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;

/**
 * Editor de capas de forma.
 * <p>
 * El doble clic sobre una capa SHAPE (desde EditTool) la activa y muestra el
 * panel de formas en la barra de opciones, con sus propiedades cargadas, para
 * poder editar tipo, relleno, borde y grosor.
 */
public class ShapeLayerEditor implements LayerEditor {

    @Override
    public boolean supports(Layer layer) {
        return layer instanceof ImageLayer il && il.getType() == ImageLayer.LayerType.SHAPE;
    } // --- Fin del metodo supports ---

    @Override
    public void onDoubleClick(Layer layer, ToolContext ctx) {
        ctx.layerModel().setActiveLayer(layer);
        ctx.componentBar().updateEditLayerFields(layer);
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo onDoubleClick ---

} // --- Fin de la clase ShapeLayerEditor ---
