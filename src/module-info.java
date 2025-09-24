	/**
 * Módulo principal de la aplicación Visor de Imágenes.
 * Define las dependencias y la encapsulación de los paquetes.
 */
module VisorImagenes {
    
    // --- DEPENDENCIAS EXTERNAS ---
    // El módulo necesita el kit de herramientas de escritorio de Java (para Swing/AWT).
    requires java.desktop;
    
    // El módulo necesita la librería FlatLaf para el Look and Feel.
    requires com.formdev.flatlaf;
    requires com.formdev.flatlaf.extras;
    requires com.formdev.flatlaf.swingx;
    requires com.formdev.flatlaf.intellijthemes;
    
    // El módulo necesita la API de logging SLF4J.
    requires transitive org.slf4j;
    
    // El módulo de manejo de JSON
    requires com.google.gson;
    
    // El módulo necesita la implementación de Logback (tanto classic como core).
    requires transitive ch.qos.logback.classic;
    requires ch.qos.logback.core;
	requires metadata.extractor;
	requires com.github.benmanes.caffeine;
	requires net.coobird.thumbnailator;
    // NO HAY NADA MÁS AQUÍ. SE ACABARON LOS REQUIRES.

    
    // --- PAQUETES EXPUESTOS ---
    // Exportamos el paquete 'principal' para que el lanzador de Java
    // pueda encontrar y ejecutar la clase main (VisorV2).
    exports principal;

    
    // --- PAQUETES ABIERTOS PARA REFLEXIÓN ---
    // Abrimos los paquetes que contienen clases de "modelo" o "datos"
    // para que librerías como FlatLaf o Logback puedan acceder a ellas
    // mediante reflexión si lo necesitan (por ejemplo, para leer propiedades).
    
    // Paquetes de Modelo de Datos
    opens modelo to com.formdev.flatlaf, ch.qos.logback.core;
    opens modelo.proyecto to com.formdev.flatlaf, ch.qos.logback.core, com.google.gson;
    
    // Paquetes de Configuración de la Vista (muy probable que FlatLaf los necesite)
    opens vista.config to com.formdev.flatlaf;
    
    // Por seguridad, también abrimos los paquetes principales.
    // Es una buena práctica para evitar problemas de reflexión inesperados.
    opens controlador to com.formdev.flatlaf, ch.qos.logback.core;
    opens servicios to com.formdev.flatlaf, ch.qos.logback.core;
    opens vista to com.formdev.flatlaf, ch.qos.logback.core;
    
}