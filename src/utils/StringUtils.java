package utils; // O package servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clase de utilidad para operaciones comunes con Strings.
 * Contiene solo métodos estáticos y no puede ser instanciada.
 */
public final class StringUtils { // 'final' para evitar herencia

    /**
     * Constructor privado para prevenir la instanciación de la clase de utilidad.
     */
    private StringUtils() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada.");
    }

    /**
     * Formatea una cadena de entrada (típicamente una clave de configuración o identificador)
     * a un formato legible "Title Case", eliminando separadores comunes y
     * capitalizando la primera letra de cada palabra resultante.
     *
     * Ejemplo: "interfaz.menu.reset_zoom" se convierte en "Interfaz Menu Reset Zoom"
     * Ejemplo: "Archivo" se convierte en "Archivo"
     * Ejemplo: "__inicio..carpeta_" se convierte en "Inicio Carpeta"
     *
     * @param input La cadena de entrada a formatear. Puede contener '.', '_', espacios múltiples.
     * @return La cadena formateada en Title Case, o una cadena vacía si la entrada es nula o está vacía después de limpiar.
     */
    public static String formatTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ""; // Devolver vacío para null o entradas vacías/solo espacios
        }

        //    Reemplazar separadores comunes (., _) por un espacio.
        //    Usamos expresiones regulares para manejar múltiples ocurrencias o combinaciones.
        //    [._\\s]+ significa "uno o más puntos, guiones bajos o espacios en blanco".
        String processed = input.trim().replaceAll("[._\\s]+", " ");

        //    Dividir la cadena procesada en palabras usando el espacio como delimitador.
        //    Stream.of(...).filter(...) asegura que no procesemos palabras vacías si había
        //    separadores al inicio/final o múltiples juntos.
        return Stream.of(processed.split(" "))
                     .map(String::trim) // Quitar espacios extra por si acaso
                     .filter(word -> !word.isEmpty()) // Ignorar palabras vacías
                     .map(word -> {
                         // Capitalizar la primera letra y poner el resto en minúsculas
                         if (word.length() == 1) {
                             return word.toUpperCase();
                         } else {
                             return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
                         }
                     })
                     // Unir las palabras capitalizadas de nuevo con un solo espacio.
                     .collect(Collectors.joining(" "));
    }

    
    //********************************************************************************** dynamicLOG
    
    /**
     * Registra dinámicamente pares de nombre-valor en la consola.
     * Se espera que los argumentos se pasen en pares: "nombreVariable", valorVariable, "otroNombre", otroValor, ...
     *
     * @param argsPares Una secuencia de objetos donde cada par representa
     *                  un nombre (String) y su valor correspondiente (Object).
     */
    public static void dynamicLog(String titulo, Object... argsPares) {
        // Marca de tiempo para saber cuándo se hizo el log
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
    	
    	System.out.println("\n************************************** DYNAMIC LOG **************************************");
    	System.out.println(timestamp + ": " + titulo != null ? titulo : "Sin Título" + "\n");
    	
//    	System.out.println("----- [" + timestamp + "] Log: " + (titulo != null ? titulo : "Sin Título") + " -----");
    	
//        System.out.println("----- D Y N A M I C   L O G   [" + timestamp + "] -----");

        if (argsPares == null || argsPares.length == 0) {
            System.out.println("  (No se proporcionaron argumentos)");
        } else if (argsPares.length % 2 != 0) {
            // Advertencia si el número de argumentos no es par
            System.out.println("  ADVERTENCIA: Número impar de argumentos. Se esperan pares nombre-valor.");
            // Intenta mostrar los pares posibles
            logPairs(argsPares, argsPares.length - 1);
            // Muestra el último argumento que quedó sin par
            System.out.println("  Argumento sin par: " + formatValue(argsPares[argsPares.length - 1]));
        } else {
            // Procesa los pares nombre-valor
            logPairs(argsPares, argsPares.length);
        }

        System.out.println("*****************************************************************************************\n");
    }

    // Método auxiliar para procesar y mostrar los pares
    private static void logPairs(Object[] args, int limit) {
        for (int i = 0; i < limit; i += 2) {
            Object nameObj = args[i];
            Object valueObj = args[i + 1];

            // Asegurarse de que el nombre sea una cadena (o representarlo como tal)
            String name = (nameObj instanceof String) ? (String) nameObj : String.valueOf(nameObj);

            System.out.println("  " + name + ": " + formatValue(valueObj));
        }
    }

    // Método auxiliar para formatear el valor (maneja null y cadenas explícitamente)
    private static String formatValue(Object value) {
        if (value == null) {
            return "[null]";
        } else if (value instanceof String) {
            return "\"" + value + "\""; // Poner comillas a las cadenas
        } else {
            // Usa Objects.toString para manejar arrays de forma más legible que el toString por defecto
            return Objects.toString(value);
        }
    }
    // --- Fin del método dynamicLog ---
    
  //********************************************************************************** FIN dynamicLOG    
    

} //fin StringUtils

