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

    private static String version;

    /**
     * The main method. Crea e inicia el controlador de la aplicación.
     * 
     * @param args los argumentos de línea de comandos (actualmente no usados).
     */
    public static final void main(String[] args) {

        version = "V2.8.00";
// tab-add_48x48.png tab-substr_48x48.png
        System.out.println("Iniciando Visor de Imágenes " + version);

        UIManager.put("MenuBar.windowBindings", new Object[] {});

        // La única responsabilidad del método main es programar la creación de la
        // aplicación en el Event Dispatch Thread (EDT) de Swing.
        // Toda la lógica de inicialización, incluyendo la configuración del tema,
        // será manejada por AppInitializer y ThemeManager.
        javax.swing.SwingUtilities.invokeLater(() -> {
            new VisorController(version);

            logger.debug("VisorController instanciado. La inicialización de la UI ha sido programada en el EDT.");
        });

    }

    // GENERAL
    // TODO la rueda del mouse en modo grid tiene que desplazarse por filas, no a la siguiente imagen
    // TODO con ctrl+ + y ctrl + - tiene que aumentar o dismiuir el tamaño de la imgen del grid, este hotkey debe aparecer en el resumen y en el manual de instrucciones

    
    // VISUALIZADOR
    // TODO HACER SALTO DE CARPETA. Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
    // TODO CTRL + SHIFT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
    
    
    // PROYECTO
    // TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
    // TODO permitir la seleccion multiple en modo proyecto
    // TODO permitir seleccionar varios archivos con el raton + shift por ej. para enviarlos a descartes o a exportar
    // TODO añadir una imagen al proyecto arrastrando desde la carpeta de windows
    // TODO importar un txt que contenga ruta/nombre y poder añadir ese txt o crear nuevo proyecto con base a ese txt
    // TODO hacer que la lista de imagenes cargada pueda copiar las imagenes al portapapeles para enviar por whatsapp por ej....
    // TODO antes de exportar, que muestre una pantalla de confirmacion (se van a exportar estas imagenes con estos archivos asociados a esta carpeta
    // FIXME falta un boton para MOVER los archivos en lugar de copiarlos 

    
    // TOOLBAR

    
    // ZOOM

    
    //DATOS
    // FIXME cada carpeta de la ruta del archivo es un tag y se debe reflejar en la base de datos
    
    
    // CARROUSEL

    
    
    /*
    OPINIONES DE ANTIGRAVITY
    -------------------------
    
    Opinión sobre tus propuestas:
	Añadir desde Windows (Drag & Drop): Es la evolución natural. Implementar Arrastrar y Soltar directamente sobre las listas o el visor sería lo más intuitivo. Podríamos hacer que, al soltar un archivo, el sistema escanee la carpeta de origen buscando automáticamente archivos con el mismo nombre (ej. si sueltas miniatura.jpg, que busque si existe miniatura.stl al lado).
	Importación desde .txt: Muy útil para flujos de trabajo masivos o para usuarios que ya tienen listas de inventario. Sería como un "procesado por lotes" de rutas.
	Selección Múltiple: Es fundamental. Una vez que el proyecto crece, mover de 1 en 1 a descartes es tedioso. Implementar Ctrl+Click y Shift+Click en las listas JList es el siguiente paso lógico.
	
	Sugerencias de mejora extra:
	Detección inteligente de "Parejas": Cuando añades un archivo manualmente, el programa podría preguntarte: "He encontrado un archivo STL con el mismo nombre, ¿quieres asociarlo como archivo del proyecto?".
	Placeholders específicos: Ahora usamos el mismo icono de advertencia para todo. Podríamos diferenciar visualmente si el archivo es un .zip, un .stl o un .obj usando iconos distintos en el marco rojo.
	Buscador en el Proyecto: Si acabas teniendo proyectos con cientos de archivos añadidos de distintas carpetas, un pequeño cuadro de búsqueda que filtre las listas de "Selección" y "Descartes" sería de gran ayuda.
	Miniaturas para STLs (Futuro Lejano): Aunque es complejo, existen librerías para generar una captura 2D básica de un archivo 3D. Sería la "guinda del pastel" para no tener placeholders.
	
	A est ultima propuesta, (stl->2D) habria que añadir la opcion de guardar la imagen. si he añadido el stl es porque no tengo la imagen y puede ser muuuy potente esta opcion.
    */
}
