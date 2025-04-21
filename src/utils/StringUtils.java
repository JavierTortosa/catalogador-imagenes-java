package utils; // O package servicios;

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

    

} //fin StringUtils

