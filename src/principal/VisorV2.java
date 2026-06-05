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

        version = "V2.12.40";

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
    // FIXME cuando el programa arranca y no encuentra la carpeta de inicio que tiene en el config debe mostrar la pantalla de presentacion
    
    // VISUALIZADOR
    // TODO HACER SALTO DE CARPETA. Cuando estamos viendo una carpeta que vemos que no nos cuadra, poder saltar todas las imagenes de esa carpeta
    // TODO CTRL + SHIFT DEBE ACTIVAR EL MODO PANEO Y DESACTIVARLO CUANDO SE DEJA DE PULSAR
    // TODO mejorar el comportamiento de las miniaturas. siempre debe mostrar el maximo de miniaturas posible. si podemos ver 12 imagenes y la imagen que tenemos seleccionada es la primera solo se muestran 6 imagenes. hasta que no marcamos la 7 no muestra las 12  
    
    // PROYECTO
    // TODO añadir en el menu del click derecho poder poner o quitar etiqueta a una imagen en grid
    // TODO hacer que el salto de pagina avance o retroceda los items que se estipulan en el config, ahora lo hace pero de la lista general, no en la lista especifica, si movemos items a descartes pasa del item 0 al 10 pero si en medio no hay items porque estan en descartes va al item 10 igual
    
    // TODO añadir una imagen al proyecto arrastrando desde la carpeta de windows
    // TODO importar un txt que contenga ruta/nombre y poder añadir ese txt o crear nuevo proyecto con base a ese txt
    // TODO hacer que la lista de imagenes cargada pueda copiar las imagenes al portapapeles para enviar por whatsapp por ej....
    // TODO antes de exportar, que muestre una pantalla de confirmacion (se van a exportar estas imagenes con estos archivos asociados a esta carpeta

    
    // TOOLBAR

    
    // ZOOM
    
    
    //DATOS
    // FIXME cuando hacemos doble click se abre la ventana de zoom, pero cuando la cerramos se pierde el foco, deberia reactivarse el foco donde lo teniamos, si no hay que hacer un click para reestablecer el foco y luego se puede seguir actuando

    // FIXME Estamos arreglando: cuando tenemos una imagen en el visor y pasamos al modo datos, seria bueno que se seleccionara esa imagen en el modo datos.... la fuente maestra del programa es el visor y cuando cambiamos de modo es porque queremos hacer algo con esa imagen. luego en el modo datos si no nos interesa esa imagen ya nos iremos donde haga falta.... otra cosa. el selector de carpetas que hay en el modo datos nos debe permitir abrir una carpeta, en este caso me interesa entrar en la carpeta F:\ARCHIVOS 3D\JUEGOS\Blood Bowl\TorchLight\ pero cuando selecciono la carpeta en el modo datos me aparece esta pantalla.... quiero poder entrar en esta carpeta, seleccionar los skaven que haya ahi (que supongo que habra varios) y ponerles a todas las imagenes la etiqueta "skaven".... por ej.... 
    // FIXME el orden del menu de click derecho de una imagen deberia ser, añadir etiqueta, -, añadir a proyecto, expandir imagen (el zoom de doble click), -, localizar archivo
    // FIXME la "biblioteca" de la parte de la lista en arbol de tags tendria que decir cuantos elementos tiene (toda la coleccion)
    
    // TODO en la vista de imagen unica o polaroid (la que mas convenga) deberiamos activar un menu de click derecho para añadir un tag o abrir la carpeta con windows
    // FIXME cuando abrimos el dialogo para establecer un tag manual, el foco deberia aparecer directamente en el textbox
    // FIXME cuando añado una etiqueta se cierran todas las tags del arbol. si estoy etiquetando una categoria (bloodbowl) y añado un tag a una imagen, bloodbow se cierra, se actualiza la lista de etiquetas... no se tiene que cerrar ni cambiar el foco ni nada que me haga perder el punto donde me encuetro en este momento
    // TODO cuando añadimos texto en el textbox de añadir tag, seria bueno que en la parde de debajo nos saliera una lista de los tags que ya tenemos que coinciden con lo que vamos escribiendo y poder elegir de esa lista
    // TODO en la status bar deberia aparecer que se ha añadido el tag correctamente o que se ha creado el tag "tal" nuevo...
    
    
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
