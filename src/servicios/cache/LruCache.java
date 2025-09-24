//package servicios.cache;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
///**
// * Una implementación de un caché de tipo LRU (Least Recently Used).
// * Este caché tiene un tamaño máximo fijo. Cuando se intenta añadir un nuevo
// * elemento y el caché está lleno, el elemento que fue accedido hace más tiempo
// * (el menos usado recientemente) es automáticamente eliminado para hacer espacio.
// * 
// * Esta clase extiende {@link LinkedHashMap} y utiliza su constructor especial
// * que ordena los elementos por orden de acceso.
// *
// * @param <K> el tipo de las claves mantenidas por este caché.
// * @param <V> el tipo de los valores mapeados.
// */
//public class LruCache<K, V> extends LinkedHashMap<K, V> {
//    private static final long serialVersionUID = 1L; // Requerido para clases serializables como LinkedHashMap
//    
//    private final int maxSize;
//
//    /**
//     * Construye un nuevo LruCache con el tamaño máximo especificado.
//     *
//     * @param maxSize el número máximo de entradas que el caché puede contener.
//     *                Debe ser un entero positivo.
//     */
//    public LruCache(int maxSize) {
//        // Llama al constructor de LinkedHashMap con los siguientes parámetros:
//        // - initialCapacity: Un valor inicial razonable, no crítico.
//        // - loadFactor: El factor de carga por defecto (0.75f).
//        // - accessOrder: true. ¡Este es el parámetro clave! Le dice a LinkedHashMap
//        //   que ordene las entradas según el orden de acceso (del menos accedido
//        //   al más accedido recientemente), en lugar del orden de inserción.
//        super(maxSize > 0 ? maxSize : 16, 0.75f, true);
//        
//        if (maxSize <= 0) {
//            throw new IllegalArgumentException("El tamaño máximo del caché (maxSize) debe ser positivo.");
//        }
//        this.maxSize = maxSize;
//    } // --- Fin del método LruCache (constructor) ---
//
//    /**
//     * Este método es invocado por los métodos put y putAll de LinkedHashMap
//     * después de que una nueva entrada ha sido añadida al mapa.
//     * Devuelve true si el mapa debe eliminar su entrada más antigua (la menos
//     * usada recientemente), lo cual hacemos si el tamaño actual supera el máximo.
//     *
//     * @param eldest La entrada menos recientemente accedida en el mapa.
//     * @return true si la entrada 'eldest' debe ser eliminada del mapa,
//     *         false si debe ser retenida.
//     */
//    @Override
//    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
//        // Devuelve true para iniciar la eliminación si el tamaño del caché
//        // ha superado el tamaño máximo permitido.
//        return size() > this.maxSize;
//    } // --- Fin del método removeEldestEntry ---
//    
//} // --- FIN DE LA CLASE LruCache ---