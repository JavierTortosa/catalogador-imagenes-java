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
        // Desactivamos la función de unmapping de PDFBox para evitar errores de acceso a memoria en Java 17+
        System.setProperty("org.apache.pdfbox.io.IOUtils.unmapSupported", "false");

        version = "V2.67.30";

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
    
    // FIXME añadir F1 para que muestre el panel de informacion
    // TODO añadir ctrl + 1-5 para activar el modo visor, proyecto, cliente, datos, carrousel
    
    
    // VISUALIZADOR
    
    // TODO HACER SALTO DE CARPETA. Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
    // TODO CTRL + SHIFT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
    
    
    
    // PROYECTO
    
    // TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
    // TODO hacer que la lista de imagenes cargada pueda copiar las imagenes al portapapeles para enviar por whatsapp por ej....
    // TODO añadir el tornado en seleccion
    // TODO en el panel de pdf, poder ordenar las imagenes mediante drag & drop o algun otro sistema
    // FIXME el panel de detalles de pdf no tiene el split para ajustar la altura
    
    
    // CLIENTE
    
    
    // TOOLBAR
    
    // FIXME el boton que muestra los botones ocultos por el tamaño de la ventana de la aplicacion no aparece al inicio de la aplicacion en ventana
    
    
    // ZOOM
    
    
    //DATOS
    
    
    // CONFIGURACION
    // FIXME en apariencia|personalizar tema: si customizamos un tema y vamos a salir nos tiene que avisar que el tema no se ha guardado y que la proxima vez que iniciemos la aplicacion lo hara con el tema actual
    
    
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
