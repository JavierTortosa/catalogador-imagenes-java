package controlador.interfaces;

/**
 * Interfaz marcador para identificar Actions que controlan la visibilidad de
 * componentes principales de la UI y cuyo estado debe ser aplicado al
 * iniciar la aplicación.
 * <p>
 * Una Action que implemente esta interfaz le está diciendo al sistema de
 * inicialización: "Mi estado (leído de la configuración) debe reflejarse
 * visualmente en la UI al arrancar. Por favor, ejecuta mi método
 * {@code actionPerformed} una vez si mi estado inicial no es el
 * predeterminado (visible)".
 */
public interface InitializableVisibility {
    // Esta interfaz no declara ningún método.
    // Su única función es ser utilizada con el operador 'instanceof'
    // para filtrar las acciones que requieren una sincronización inicial.
}