package vista.renderers; // O vista.list_renderers, etc.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import javax.swing.*;
import javax.swing.border.Border;

import modelo.VisorModel; // Para obtener la ruta completa
import servicios.image.ThumbnailService; // Para obtener la miniatura

/**
 * ListCellRenderer para mostrar una miniatura y opcionalmente el nombre
 * del archivo en una JList. Utiliza ThumbnailService para obtener los iconos.
 */
@SuppressWarnings ("serial")
public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {

    private JLabel etiquetaIcono;
    private JLabel etiquetaNombre;
    private ThumbnailService servicioMiniaturas;
    private VisorModel modeloVisor; // Necesario para obtener la ruta completa

    // Colores (podrían venir de ViewUIConfig si pasas la referencia)
    private Color colorTextoNormal = Color.BLACK;
    private Color colorFondoNormal = Color.WHITE;
    private Color colorTextoSeleccionado = Color.WHITE;
    private Color colorFondoSeleccionado = new Color(57, 105, 138); // Azul oscuro
    private Border bordeNormal = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    private Border bordeSeleccionado = BorderFactory.createLineBorder(Color.YELLOW, 2); // Borde amarillo para selección

    // Dimensiones deseadas para la miniatura DENTRO del renderer
    // Deben coincidir con lo que se configurará en JList.setFixedCell...
    private int anchoMiniatura = 60; // Valor por defecto, podría ser configurable
    private int altoMiniatura = 60;  // Valor por defecto

    /**
     * Constructor.
     * @param servicioMiniaturas El servicio para obtener/crear miniaturas.
     * @param modeloVisor El modelo principal para obtener rutas completas.
     * @param ancho Celda Ancho deseado para las miniaturas en esta lista.
     * @param altoCelda Alto deseado para las miniaturas en esta lista.
     */
    public MiniaturaListCellRenderer (
    		ThumbnailService servicioMiniaturas, VisorModel modeloVisor, int anchoCelda, int altoCelda) 
    {
        this.servicioMiniaturas = servicioMiniaturas;
        this.modeloVisor = modeloVisor;
        this.anchoMiniatura = anchoCelda > 0 ? anchoCelda : 60;
        this.altoMiniatura = altoCelda > 0 ? altoCelda : 60;

        // 1. Usar BoxLayout con orientación vertical (Y_AXIS)
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        //setLayout(new BorderLayout(2, 2)); // Layout para icono y texto
        setOpaque(true); // Importante para que los colores de fondo se vean

        // 2. Crear los JLabels
        etiquetaIcono = new JLabel();
        etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
        etiquetaNombre = new JLabel();
        etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
        // Tamaño fuente pequeño para el nombre
        etiquetaNombre.setFont(etiquetaNombre.getFont().deriveFont(10.0f)); 
        
        // 3. Alinear los JLabels AL CENTRO horizontalmente dentro del BoxLayout
        etiquetaIcono.setAlignmentX(Component.CENTER_ALIGNMENT);
        etiquetaNombre.setAlignmentX(Component.CENTER_ALIGNMENT);

     // 4. Añadir componentes al panel (en orden vertical)
        // Opcional: Añadir espacio vertical si se desea
        // add(Box.createVerticalGlue()); // Empuja hacia abajo (para centrar verticalmente todo)
        add(etiquetaIcono);
        // add(Box.createRigidArea(new Dimension(0, 3))); // Espacio fijo entre icono y nombre
        add(etiquetaNombre);
        // add(Box.createVerticalGlue()); // Empuja hacia arriba (para centrar verticalmente todo)
        //add(etiquetaIcono, BorderLayout.CENTER);
        //add(etiquetaNombre, BorderLayout.SOUTH); // Nombre debajo
        
        // Establecer tamaño preferido del panel basado en miniatura + texto
        setPreferredSize(new Dimension(anchoMiniatura + 10, altoMiniatura + 20)); 
    }

    @Override
    public Component getListCellRendererComponent (JList<? extends String> list,
                                                   String value, // 'value' es la clave (ruta relativa)
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus) {

        if (value == null) {
            // Manejar valor nulo (no debería ocurrir con DefaultListModel)
            etiquetaNombre.setText("Nulo");
            etiquetaIcono.setIcon(null); // O un icono de error genérico
            return this;
        }

        // --- Obtener la Miniatura ---
        Path rutaCompleta = modeloVisor.getRutaCompleta(value);
        ImageIcon miniatura = null;
        String nombreArchivo = value; // Usar clave como nombre por defecto

        if (rutaCompleta != null) {
            // Obtener nombre de archivo real
             try {
                 nombreArchivo = rutaCompleta.getFileName().toString();
             } catch (Exception e) { /* Ignorar si falla */ }

            // Pedir miniatura al servicio (indicando que es tamaño normal para caché)
            // ¡OJO! Esto podría bloquear si el servicio carga desde disco aquí.
            // Idealmente, las miniaturas ya están precargadas.
            miniatura = servicioMiniaturas.obtenerOCrearMiniatura(
                rutaCompleta, value, anchoMiniatura, altoMiniatura, true // Pedir tamaño normal para cachear
            );
        } else {
             System.err.println("[Renderer] No se encontró ruta completa para: " + value);
             nombreArchivo = value + " (Ruta perdida)";
        }


        // --- Configurar Apariencia ---
        etiquetaNombre.setText(nombreArchivo);
        etiquetaIcono.setIcon(miniatura); // Poner la miniatura (o null si falló)

        if (miniatura == null && rutaCompleta != null) {
            // Si hubo error al cargar, mostrar texto de error en lugar de icono
            etiquetaIcono.setText("X"); // O usar un icono de error cargado
            etiquetaIcono.setForeground(Color.RED);
        } else {
             etiquetaIcono.setText(null); // Quitar texto si hay icono
             etiquetaIcono.setForeground(colorTextoNormal); // Restaurar color
        }


        // --- Configurar Colores y Borde según Selección ---
        if (isSelected) {
            setBackground(colorFondoSeleccionado);
            etiquetaIcono.setForeground(colorTextoSeleccionado); // Puede no ser visible si hay icono
            etiquetaNombre.setForeground(colorTextoSeleccionado);
            setBorder(bordeSeleccionado);
        } else {
            setBackground(colorFondoNormal);
            etiquetaIcono.setForeground(colorTextoNormal);
            etiquetaNombre.setForeground(colorTextoNormal);
            setBorder(bordeNormal);
        }

        // Podríamos añadir lógica para cellHasFocus si queremos un borde diferente para el foco

        return this; // Devolver este JPanel configurado
    }
}