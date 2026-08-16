package vista.panels.render;

import java.nio.file.Path;

import javax.swing.Icon;

import servicios.renderer.Zip2PngScanner.RenderCandidate;

/**
 * Elemento de modelo de los grids de miniaturas del modo Render (imágenes y
 * renders 3D). Encapsula la ruta del thumbnail, el candidato asociado, la
 * etiqueta para el tooltip y el icono ya escalado, de modo que el cell
 * renderer no tenga que volver a leer ni escalar la imagen en cada repintado.
 */
public record RenderThumbnailItem(Path pngPath, RenderCandidate candidate, String label, Icon icon) {
} // --- Fin de la clase RenderThumbnailItem ---