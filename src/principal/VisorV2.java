package principal;

import controlador.VisorController; 

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2
{

	// VISUALIZADOR
		//TODO HACER SALTO DE CARPETA
	    //Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
	    
	    //TODO CTRL + ALT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
	    
	    //TODO Pasar las imagenes rapido con la rueda del raton no debe mostrar las imagenes, 
	    //solo un placebo que parezca que estan pasando imagenes pero en realidad no se ve nada
	    
	    //TODO conectar a internet en busca de una imgen para un zip que no la tiene
	
		//TODO añadir botones de agrandar o encoger las miniaturas y que se ajuste a la pantalla
    
	// PROYECTO
    	//TODO hacer que el boton de marcar imagen en modo proyecto mueva la imagen a descartes o viceversa
    
    	//TODO permitir la seleccion multiple en modo proyecto
    
    	//TODO añadir una opcion al panel de descartes para borrar seleccion o todo
	
		//TODO hacer que las imagenes solo muestren el nombre o el path con la configuracion del config
	
		//TODO forzar a que el splitPanel de modo proyecto se ajuste a un espacio del 20%
	
	
	/**
	 * The main method. Crea e inicia el controlador de la aplicación.
	 * 
	 * @param args los argumentos de línea de comandos (actualmente no usados para
	 *             iniciar).
	 */
	public static void main (String[] args) {
	    // Importaciones necesarias dentro del método si no están ya arriba
	    // import servicios.ConfigurationManager;
	    // import servicios.ConfigKeys;
	    // import javax.swing.SwingUtilities;

	    System.out.println("Iniciando Visor de Imágenes V2...");

	    // PASO 1: Leer la configuración ANTES de hacer nada con la UI.
	    final servicios.ConfigurationManager config = servicios.ConfigurationManager.getInstance();
	    String themeName = config.getString(servicios.ConfigKeys.TEMA_NOMBRE, "claro"); // "claro" es el fallback

	    // PASO 2: Configurar el Look and Feel de Swing BASADO en el tema guardado.
	    try {
	        if ("dark".equalsIgnoreCase(themeName) || "green".equalsIgnoreCase(themeName) || "orange".equalsIgnoreCase(themeName)) {
	            System.out.println("VisorV2.main: Configurando tema oscuro FlatDarkLaf.");
	            com.formdev.flatlaf.FlatDarkLaf.setup();
	        } else {
	            System.out.println("VisorV2.main: Configurando tema claro FlatLightLaf.");
	            com.formdev.flatlaf.FlatLightLaf.setup();
	        }
	    } catch (Exception ex) {
	        System.err.println("Error al inicializar el Look and Feel FlatLaf. La aplicación usará el L&F por defecto del sistema.");
	    }

	    // PASO 3: Ahora que Swing está configurado, lanzar el resto de la aplicación en el hilo de la UI.
	    javax.swing.SwingUtilities.invokeLater(() -> {
	        // El constructor de VisorController iniciará AppInitializer,
	        // que ahora creará los componentes sobre un L&F ya configurado.
	        new VisorController();
	        System.out.println("VisorController instanciado. La inicialización de la UI ha sido programada en el EDT.");
	    });
	}

} //fin VisorV2