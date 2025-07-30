package principal;

import controlador.VisorController; 

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2 {


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
            System.out.println("VisorController instanciado. La inicialización de la UI ha sido programada en el EDT.");
        });
    }

}

	// VISUALIZADOR
		//TODO no se guarda la imagen con la que cerramos la aplicacion
		//TODO HACER SALTO DE CARPETA
			//Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
	    //TODO CTRL + ALT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
	    //TODO Pasar las imagenes rapido con la rueda del raton no debe mostrar las imagenes, 
			//solo un placebo que parezca que estan pasando imagenes pero en realidad no se ve nada
		//TODO añadir botones de agrandar o encoger las miniaturas y que se ajuste a la pantalla
		//TODO mostrar arbol de carpetas
    
	// PROYECTO
		//TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista
			//especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
		//TODO cuando busca los archivos asociados deberia aparecer un progressbar indicando que esta buscando los archivos asociados.... 
			//si hay 50 imagenes son 50 archivos que tiene que localizar identificar y asimilar, esto lleva un tiempo que la UI esta parada y no se ve que haga nada
    	//TODO hacer que el boton de marcar imagen en modo proyecto mueva la imagen a descartes o viceversa
    	//TODO permitir la seleccion multiple en modo proyecto
    	//TODO añadir una opcion al panel de descartes para borrar seleccion o todo
		//TODO permitir seleccionar varios archivos con el raton + shift por ej. para enviarlos a descartes o a exportar
		//TODO ajustar los botones que se tienen que ver o modificar el comportamiento entre visualizador y proyecto
			//-Marcar Imagen: debe desmarcar la imagen en modo proyecto y por lo tanto eliminarla del proyecto o debe mandar la imagen a descartes
			//-Selector de Carpeta: en modo proyecto se deberia usar para abrir un proyecto, no para iniciar la exploracion de imagenes como en el visualizador
		//TODO ajustar opciones del menu para que se activen o desactiven las que son o no relevantes en modo proyecto
		//TODO implementar la barra de multiproyectos
	
	//TOOLBAR
		//TODO hacer que ToolbarManager reciba un parametro "libre" para las toolbars que no tiene que añadir a la barra de botones general
	
	// ZOOM
		//FIXME cuando aplicamos el reset, se aplica al modo ajustar a pantalla y no cambia el boton seleccionado y luego si intentamos pasar con el teclado
			//dice que esta en modo 1 y no se hace nada
