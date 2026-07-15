package servicios.renderer;

import java.io.File;

/**
 * Interfaz para motores de renderizado de modelos 3D.
 * Cada implementación convierte un archivo 3D de entrada en un PNG de salida.
 */
public interface ModelRenderer {

    /**
     * Renderiza un archivo 3D generando un PNG.
     *
     * @param input  archivo 3D de entrada (STL, OBJ, 3MF…)
     * @param output archivo PNG de salida
     * @throws Exception si ocurre algún error durante el renderizado
     */
    void render(File input, File output) throws Exception;

} // --- Fin de la interfaz ModelRenderer ---
