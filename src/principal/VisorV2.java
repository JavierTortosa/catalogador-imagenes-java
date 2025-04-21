package principal;

import controlador.VisorController; // Importar el Controller

/**
 * Punto de entrada principal de la aplicación Visor de Imágenes V2.
 */
public class VisorV2
{

	/**
	 * The main method. Crea e inicia el controlador de la aplicación.
	 * 
	 * @param args los argumentos de línea de comandos (actualmente no usados para
	 *             iniciar).
	 */
	public static void main (String[] args)
	{
		// TODO: Procesar args si se desea abrir una imagen específica al inicio

		System.out.println("Iniciando Visor de Imágenes V2...");
		// Crea el controlador. El constructor del controlador se encargará
		// de inicializar el modelo, la vista y mostrar la ventana en el EDT.
		new VisorController();
		System.out.println("VisorController instanciado. La inicialización de la UI ocurrirá en el EDT.");

	}

} //fin VisorV2