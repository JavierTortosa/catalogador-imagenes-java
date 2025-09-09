package principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
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

        UIManager.put("MenuBar.windowBindings", new Object[] {});
        
        // La única responsabilidad del método main es programar la creación de la 
        // aplicación en el Event Dispatch Thread (EDT) de Swing.
        // Toda la lógica de inicialización, incluyendo la configuración del tema,
        // será manejada por AppInitializer y ThemeManager.
        javax.swing.SwingUtilities.invokeLater(() -> {
            new VisorController();
            logger.debug("VisorController instanciado. La inicialización de la UI ha sido programada en el EDT.");
        });
        
    }



	// VISUALIZADOR
		//TODO HACER SALTO DE CARPETA
			//Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
	    //TODO CTRL + SHIFT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
	    //TODO Pasar las imagenes rapido con la rueda del raton no debe mostrar las imagenes, 
			//solo un placebo que parezca que estan pasando imagenes pero en realidad no se ve nada
		//TODO buscar por nombre de archivo en la carpeta actual y si le doy la ruta que cambie la lista en funcion de esa nueva imagen (si existe)

	// PROYECTO
		//TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista
			//especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
		//TODO cuando busca los archivos asociados deberia aparecer un progressbar indicando que esta buscando los archivos asociados.... 
			//si hay 50 imagenes son 50 archivos que tiene que localizar identificar y asimilar, esto lleva un tiempo que la UI esta parada y no se ve que haga nada
    	//TODO permitir la seleccion multiple en modo proyecto
		//TODO permitir seleccionar varios archivos con el raton + shift por ej. para enviarlos a descartes o a exportar
		//TODO ajustar opciones del menu para que se activen o desactiven las que son o no relevantes en modo proyecto
		//TODO añadir la opcion de poder agregar al proyecto un archivo sin imagen (se puede crear una imagen que tenga el texto que le introduzca el usuario)
		//TODO añadir una imagen al proyecto arrastrando desde la carpeta de windows
		//TODO importar un txt que contenga ruta/nombre y poder añadir ese txt o crear nuevo proyecto con base a ese txt
		//TODO hacer que la lista de imagenes cargada pueda copiar las imagenes al portapapeles para enviar por whatsapp por ej....
		//FIXME la lista de imagenes cargadas no oculta la ruta y solo copia al portapapeles los archivos con la ruta
    	//TODO Que pasa cuando 2 archivos con el mismo nombre se exportan a una carpeta?
    	//TODO poner un mensaje en la status bar cuando se guarda el proyecto


	//TOOLBAR
		
	
	// ZOOM
		
//29/08/2025
    // GENERAL
    
    // FIXME INTERCAMBIO ENTRE VISOR Y PROYECTO NO GUARDA EL VISORMODE... Y SE DESINCRONIZA
    
	//VISUALIZADOR 
    // FIXME cuando la carpeta que estamos viendo cambia, que no cambie donde tengo el selector.
    // FIXME al salir de una carpeta con el boton de subir un nivel, no se guarda la posicion que teniamos al cambiar de carpeta 
    // TODO AÑADIR UN MARCO ALREDEDOR DEL PANEL DEL DISPLAY (EN SINGLE) EN ALGUN COLOR QUE QUERE COHERENTE CON EL TEMA PERO QUE NOS INDIQUE QUE LA IMAGEN ESTA MARCADA

    
    //PROYECTO
    
    // TODO CUANDO GUARDAMOS EL PROYECTO QUE SE GUARDE LAS CORRECCIONES QUE HEMOS HECHO A MANO
    // FIXME EL BOTON DE CAMBIAR DE CARPETA NO DEBERIA PODERSE UTILIZAR
    
    // FIXME si no hay carpeta de destino no se puede activar el boton de exportar
    // TODO antes de exportar, que muestre una pantalla de confirmacion (se van a exportar estas imagenes con estos archivos asociados a esta carpeta
    // TODO no se como asignar varios archivos a una imagen (part1.zip, part2.zip) 
    // FIXME permitir buscar a mano una imagen que ha cambiado de nombre 
    // FIXME error entre imagen en carga y el icono de imagen erronea. si una imagen cambia de nombre no se puede cargar
    
    // FIXME IDEAS de como actuar entre sesiones con respecto a los proyectos (mayor robustez)
    //
    //	tendremos 3 tipos de archivos de proyecto:
    //		actual(temporal): en donde hacemos las modificaciones de esta sesion
    //		seguridad: seria una copia de seguridad de las acciones que hacemos en esta sesion
    //		guardado: el archivo que guarda el usuario con su proyecto
    //
    //
    //	Actuar como un Notepad o similares
    //	-Al Salir
    //  	- Preguntar antes de cerrar el programa
    //			- si tenemos un proyecto abierto avisar de si se quiere guardar el proyecto o se pierden los cambios
    //			- si no tenemos un proyecto abierto solo tenemos el proyecto actual y hay que avisar de que si no se guarda se perderan los cambios
    //			- si guardamos el proyecto actual se añade al proyecto guardado y se convierte en proyecto seguridad
    //
    //		- como medida de seguridad, el proyecto actual se convierte en proyecto seguridad
    //
    //	-Al Entrar
    //		- Empezar con un proyecto actual vacio
    //		- Si pulsamos en ir al modo proyecto sin ninguna imagen seleccionada, abrir el selector de proyecto para abrir un proyecto o recuperar sesion anterior
    //		- En el selector de abrir proyecto añadir un boton de abrir proyecto anterior
    //
    //		

    
}    

