package principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.VisorController; 

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2 {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
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
		//TODO no se guarda la imagen con la que cerramos la aplicacion
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
		//TODO ajustar los botones que se tienen que ver o modificar el comportamiento entre visualizador y proyecto
			//-Marcar Imagen: debe desmarcar la imagen en modo proyecto y por lo tanto eliminarla del proyecto o debe mandar la imagen a descartes
			//-Selector de Carpeta: en modo proyecto se deberia usar para abrir un proyecto, no para iniciar la exploracion de imagenes como en el visualizador
		//TODO ajustar opciones del menu para que se activen o desactiven las que son o no relevantes en modo proyecto
		//TODO implementar la barra de multiproyectos
		//TODO hacer un boton que ponga en amarillo las imagenes que no tienen su correspondiente zip
		//TODO añadir la opcion de poder agregar al proyecto un archivo sin imagen (se puede crear una imagen que tenga el texto que le introduzca el usuario)
		//TODO hacer que las listas esten ordenadas alfabeticamente (luego ya pondremos la toolbar de sort)
		//TODO agregar el displaymode grid independiente para seleccionados, descartes y exportar

//TODO hacer que el boton de marcar imagen en modo proyecto mueva la imagen a descartes o viceversa
//TODO añadir opcion en el popup de localizar archivo tanto en selecionados, descartes y exportar
//TODO añadir la opcion de vaciar en descartes y seleccion
//TODO añadir una opcion al panel de descartes para borrar seleccion o todo
//TODO permitir ir a la carpeta del proyecto cuando se termina de exportar
		

	//TOOLBAR
		
	
	// ZOOM
		
