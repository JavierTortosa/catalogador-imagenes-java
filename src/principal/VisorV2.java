package principal;

import controlador.VisorController; 

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2
{

	// VISUALIZADOR
		//TODO no se guarda la imagen con la que cerramos la aplicacion
	
		//TODO HACER SALTO DE CARPETA
	    //Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
	    
	    //TODO CTRL + ALT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
	    
	    //TODO Pasar las imagenes rapido con la rueda del raton no debe mostrar las imagenes, 
	    //solo un placebo que parezca que estan pasando imagenes pero en realidad no se ve nada
	    
	    //TODO conectar a internet en busca de una imgen para un zip que no la tiene
	
		//TODO añadir botones de agrandar o encoger las miniaturas y que se ajuste a la pantalla
	
		//TODO añadir el popup menu
			// -marcar imagen, mostrar ubicacion de imagen, modo paneo, 
	
		//TODO mostrar arbol de carpetas
    
	// PROYECTO
	
		//TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista
		//especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
	
		//TODO hacer que la mochila guarde el archivo y el foco activo en el cofig para la proxima ejecucion o para el cambio de foco
	
		//TODO cambiar formato de las pestañas de exportar, descartes... para que no parezcan labels, que sean algo mas
	
		//TODO cuando busca los archivos asociados deberia aparecer un progressbar indicando que esta buscando los archivos asociados.... 
		//si hay 50 imagenes son 50 archivos que tiene que localizar identificar y asimilar, esto lleva un tiempo que la UI esta parada y no se ve que haga nada
	
    	//TODO hacer que el boton de marcar imagen en modo proyecto mueva la imagen a descartes o viceversa
    
    	//TODO permitir la seleccion multiple en modo proyecto
    
    	//TODO añadir una opcion al panel de descartes para borrar seleccion o todo
	
		//TODO hacer que las imagenes solo muestren el nombre o el path con la configuracion del config
	
		//TODO forzar a que el splitPanel de modo proyecto se ajuste a un espacio del 20%
	
		//TODO hacer que la lista de archivos no muestre la ruta o por lo menos que no la muestre desplegada (ocupa mucho sitio en pantalla)
	
		//TODO permitir seleccionar varios archivos con el raton + shift por ej. para enviarlos a descartes o a exportar
	
		//TODO ajustar los botones que se tienen que ver o modificar el comportamiento entre visualizador y proyecto
			//-Marcar Imagen: debe desmarcar la imagen en modo proyecto y por lo tanto eliminarla del proyecto o debe mandar la imagen a descartes
			//-Selector de Carpeta: en modo proyecto se deberia usar para abrir un proyecto, no para iniciar la exploracion de imagenes como en el visualizador
			//-
		
		//TODO ajustar opciones del menu para que se activen o desactiven las que son o no relevantes en modo proyecto
	
	
	
	
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