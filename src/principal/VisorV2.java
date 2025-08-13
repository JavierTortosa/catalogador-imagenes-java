package principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.VisorController; 

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2 {

	private static final Logger logger = LoggerFactory.getLogger(VisorV2.class);
	
    /**
     * The main method. Crea e inicia el controlador de la aplicación.
     * 
     * @param args los argumentos de línea de comandos (actualmente no usados).
     */
    public static void main(String[] args) {
    	
        System.out.println("Iniciando Visor de Imágenes V2...");

        // La única responsabilidad del método main es programar la creación de la 
        // aplicación en el Event Dispatch Thread (EDT) de Swing.
        // Toda la lógica de inicialización, incluyendo la configuración del tema,
        // será manejada por AppInitializer y ThemeManager.
        javax.swing.SwingUtilities.invokeLater(() -> {
            new VisorController();
            logger.debug("VisorController instanciado. La inicialización de la UI ha sido programada en el EDT.");
        });
        
    }

}

	// VISUALIZADOR
		//TODO HACER SALTO DE CARPETA
			//Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
	    //TODO CTRL + SHIFT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
	    //TODO Pasar las imagenes rapido con la rueda del raton no debe mostrar las imagenes, 
			//solo un placebo que parezca que estan pasando imagenes pero en realidad no se ve nada
		//TODO añadir botones de agrandar o encoger las miniaturas y que se ajuste a la pantalla
		//TODO mostrar arbol de carpetas
		//TODO añadir los botones de siguiente y anterior al panel de la imagen
		//TODO ir a la primera imagen que empiece por la letra marcada (como el explorador de windows)
		//TODO buscar por nombre de archivo en la carpeta actual y si le doy la ruta que cambie la lista en funcion de esa nueva imagen (si existe)
		//TODO hacer un boton que ponga en amarillo las imagenes que no tienen su correspondiente zip

	// PROYECTO
		//TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista
			//especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
		//TODO cuando busca los archivos asociados deberia aparecer un progressbar indicando que esta buscando los archivos asociados.... 
			//si hay 50 imagenes son 50 archivos que tiene que localizar identificar y asimilar, esto lleva un tiempo que la UI esta parada y no se ve que haga nada
    	//TODO permitir la seleccion multiple en modo proyecto
		//TODO permitir seleccionar varios archivos con el raton + shift por ej. para enviarlos a descartes o a exportar
		//TODO ajustar opciones del menu para que se activen o desactiven las que son o no relevantes en modo proyecto
		//TODO implementar la barra de multiproyectos
		//TODO hacer un boton que ponga en amarillo las imagenes que no tienen su correspondiente zip
		//TODO añadir la opcion de poder agregar al proyecto un archivo sin imagen (se puede crear una imagen que tenga el texto que le introduzca el usuario)
		//TODO añadir una imagen al proyecto arrastrando desde la carpeta de windows
		//TODO importar un txt que contenga ruta/nombre y poder añadir ese txt o crear nuevo proyecto con base a ese txt
		//TODO hacer que la lista de imagenes cargada pueda copiar las imagenes al portapapeles para enviar por whatsapp por ej....
		//FIXME la lista de imagenes cargadas no oculta la ruta y solo copia al portapapeles los archivos con la ruta
		//FIXME cuando entramos en proyecto no esta activo el selector de imagen
		//FIXME la barra de progreso de carga de imagenes no es la misma que cuando exporta proyecto. la segunda es mejor



	//TOOLBAR
		
	
	// ZOOM
		
