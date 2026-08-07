package controlador.actions.editoravanzado.autolayout;

/**
 * Contrato de un algoritmo Auto Layout.
 * <p>
 * Cada herramienta (Grid, Pack, Hero, Espiral, ...) implementa esta interface.
 * La única responsabilidad de un algoritmo es calcular la nueva composición de
 * las capas a partir del contexto. No debe acceder a singletons, a clases del
 * editor ajenas al contexto ni a la interfaz gráfica, y no debe realizar
 * operaciones de Undo/Redo ni invocar {@code repaint()}.
 */
public interface AutoLayoutAlgorithm {

    /**
     * Calcula la nueva composición de las capas según la herramienta.
     *
     * @param ctx contexto con modelo de capas, canvas y configuración
     */
    void layout(AutoLayoutContext ctx);

} // --- Fin de la interfaz AutoLayoutAlgorithm ---
