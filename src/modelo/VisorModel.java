package modelo;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon; // Necesario para el caché de miniaturas si lo ponemos aquí

/**
 * Modelo de datos para el Visor de Imágenes. Contiene el estado de la
 * aplicación.
 */
public class VisorModel
{

	// --- Estado Principal ---
	private DefaultListModel<String> modeloLista; // Modelo de la JList
	private Map<String, Path> rutaCompletaMap; // Mapa de claves a rutas completas
	private BufferedImage currentImage; // Imagen original actualmente cargada
	private String selectedImageKey; // Clave (ruta relativa) de la imagen seleccionada

	// --- Estado de Visualización/Interacción ---
	private int imageOffsetX;
	private int imageOffsetY;
	private boolean zoomHabilitado;
	private double zoomFactor;
	
	
	// --- Estado de Configuración/Comportamiento (que afecta la lógica de
	// carga/visualización) ---
	private boolean mostrarSoloCarpetaActual;
    private int miniaturasAntes;
    private int miniaturasDespues;
    private int miniaturaSelAncho;
    private int miniaturaSelAlto; // ¡Sin valor inicial aquí!
    private int miniaturaNormAncho;
    private int miniaturaNormAlto;
    private boolean mantenerProporcion;

	// FIXME --- Caché de Miniaturas (Considerar si va aquí o en Controller/Servicio) ---
	// Por simplicidad inicial, la ponemos aquí. Si crece mucho, se puede extraer.
	private Map<String, ImageIcon> miniaturasMap;

	// --- Constructor ---
	public VisorModel()
	{

		// Inicializar colecciones y valores por defecto
		modeloLista = new DefaultListModel<>();
		rutaCompletaMap = new HashMap<>();
		miniaturasMap = new HashMap<>(); // Inicializar caché

		// Valores iniciales por defecto (podrían ser sobrescritos por config)
		currentImage = null;
		selectedImageKey = null;
		zoomHabilitado = false;// Estado inicial funcional razonable
        zoomFactor = 1.0;     // Estado inicial funcional razonable
        imageOffsetX = 0;
        imageOffsetY = 0;
        
     // --- Valor mantener proporciones ---
        mantenerProporcion = true; // Valor inicial por defecto (coincide con config default)

   	}

	// --- Getters y Setters (o métodos de modificación) ---

	public DefaultListModel<String> getModeloLista ()
	{

		return modeloLista;

	}

	public void setModeloLista (DefaultListModel<String> nuevoModelo)
	{

		if (nuevoModelo != null)
		{ // Añadir chequeo null

			this.modeloLista = nuevoModelo;
			
			//LOG [Model] Referencia de modeloLista actualizada. Nuevo tamaño: 
			//System.out.println(
			//		"[Model] Referencia de modeloLista actualizada. Nuevo tamaño: " + this.modeloLista.getSize());

		} else
		{

			System.err.println("[Model] Intento de asignar un modelo nulo!");
			this.modeloLista = new DefaultListModel<>(); // Poner uno vacío para evitar NullPointer

		}
	}

	public void limpiarModeloLista ()
	{

		this.modeloLista.clear();

	}

	public Map<String, Path> getRutaCompletaMap ()
	{

		return rutaCompletaMap;

	}

	public void setRutaCompletaMap (Map<String, Path> nuevoMapa)
	{

		this.rutaCompletaMap = nuevoMapa;

	}

	public void limpiarRutaCompletaMap ()
	{

		this.rutaCompletaMap.clear();

	}

	public Path getRutaCompleta (String key)
	{

		return rutaCompletaMap.get(key);

	}

	public void putRutaCompleta (String key, Path path)
	{

		rutaCompletaMap.put(key, path);

	}

	public BufferedImage getCurrentImage ()
	{

		return currentImage;

	}

	public void setCurrentImage (BufferedImage currentImage)
	{

		this.currentImage = currentImage;

	}

	public String getSelectedImageKey ()
	{

		return selectedImageKey;

	}

	public void setSelectedImageKey (String selectedImageKey)
	{

		this.selectedImageKey = selectedImageKey;

	}

	public boolean isZoomHabilitado ()
	{

		return zoomHabilitado;

	}

	public void setZoomHabilitado (boolean zoomHabilitado)
	{

		this.zoomHabilitado = zoomHabilitado;

		if (!zoomHabilitado)
		{
			// Podríamos resetear el zoom al desactivar, como opción de diseño
			// resetZoomState();
		}

	}

	public double getZoomFactor ()
	{

		return zoomFactor;

	}

	public void setZoomFactor (double zoomFactor)
	{

		// Aplicar límites aquí si se desea
		this.zoomFactor = Math.max(0.1, Math.min(zoomFactor, 10.0));

	}

	public int getImageOffsetX ()
	{

		return imageOffsetX;

	}

	public void setImageOffsetX (int imageOffsetX)
	{

		this.imageOffsetX = imageOffsetX;

	}

	public void addImageOffsetX (int deltaX)
	{

		this.imageOffsetX += deltaX;

	}

	public int getImageOffsetY ()
	{

		return imageOffsetY;

	}

	public void setImageOffsetY (int imageOffsetY)
	{

		this.imageOffsetY = imageOffsetY;

	}

	public void addImageOffsetY (int deltaY)
	{

		this.imageOffsetY += deltaY;

	}

	public void resetZoomState ()
	{

		this.zoomFactor = 1.0;
		this.imageOffsetX = 0;
		this.imageOffsetY = 0;

	}

	public boolean isMostrarSoloCarpetaActual ()
	{

		return mostrarSoloCarpetaActual;

	}

	public void setMostrarSoloCarpetaActual (boolean mostrarSoloCarpetaActual)
	{

		this.mostrarSoloCarpetaActual = mostrarSoloCarpetaActual;

	}

	// --- Getters/Setters para configuración ---
	public int getMiniaturasAntes ()
	{

		return miniaturasAntes;

	}

	public void setMiniaturasAntes (int val)
	{

		this.miniaturasAntes = val;

	}

	public int getMiniaturasDespues ()
	{

		return miniaturasDespues;

	}

	public void setMiniaturasDespues (int val)
	{

		this.miniaturasDespues = val;

	}

	public int getMiniaturaSelAncho ()
	{

		return miniaturaSelAncho;

	}

	public void setMiniaturaSelAncho (int val)
	{

		this.miniaturaSelAncho = val;

	}

	public int getMiniaturaSelAlto ()
	{

		return miniaturaSelAlto;

	}

	public void setMiniaturaSelAlto (int val)
	{

		this.miniaturaSelAlto = val;

	}

	public int getMiniaturaNormAncho ()
	{
		
		return miniaturaNormAncho;

	}

	public void setMiniaturaNormAncho (int val)
	{
		
		this.miniaturaNormAncho = val;

	}

	public int getMiniaturaNormAlto ()
	{

		return miniaturaNormAlto;

	}

	public void setMiniaturaNormAlto (int val)
	{

		this.miniaturaNormAlto = val;

	}

	// --- Métodos para caché de miniaturas ---
	public Map<String, ImageIcon> getMiniaturasMap ()
	{

		return miniaturasMap;

	}

	public void setMiniaturasMap (Map<String, ImageIcon> nuevoMapa)
	{

		this.miniaturasMap = nuevoMapa;

	}

	public void limpiarMiniaturasMap ()
	{

		this.miniaturasMap.clear();

	}

	public ImageIcon getMiniatura (String key)
	{

		return miniaturasMap.get(key);

	}

	public void putMiniatura (String key, ImageIcon icon)
	{

		miniaturasMap.put(key, icon);

	}
	
	
	public boolean isMantenerProporcion() {
        return mantenerProporcion;
    }

    public void setMantenerProporcion(boolean mantenerProporcion) {
        if (this.mantenerProporcion != mantenerProporcion) { // Solo cambiar si es diferente
            this.mantenerProporcion = mantenerProporcion;
            System.out.println("  [Model] Estado mantenerProporcion cambiado a: " + this.mantenerProporcion);
            // Opcional: Disparar un PropertyChangeEvent si tienes listeners observando el modelo
            // firePropertyChange("mantenerProporcion", !this.mantenerProporcion, this.mantenerProporcion);
        }
    }

} //fin VisorModel

