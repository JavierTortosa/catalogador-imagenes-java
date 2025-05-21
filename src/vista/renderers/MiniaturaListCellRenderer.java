package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout; // Usaremos BorderLayout
import java.nio.file.Path;
import java.util.Objects; // Para Objects.requireNonNull
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import modelo.VisorModel;
import servicios.image.ThumbnailService;

@SuppressWarnings("serial")
public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {

    // --- 1. COMPONENTES INTERNOS DE LA CELDA ---
    private final JLabel etiquetaIcono;
    private final JLabel etiquetaNombre;
    private final JPanel panelContenedorIcono;

    // --- 2. DEPENDENCIAS Y CONFIGURACIÓN INMUTABLE ---
    private final ThumbnailService servicioMiniaturas;
    private final VisorModel modeloVisor;
    private final boolean mostrarNombresConfigurado;

    // --- 3. ESTILOS VISUALES (COLORES Y BORDES) ---
    private final Color colorTextoNormal;
    private final Color colorFondoNormal;
    private final Color colorTextoSeleccionado;
    private final Color colorFondoSeleccionado;
    private final Border bordeNormal;
    private final Border bordeSeleccionado;

    // --- 4. DIMENSIONES PARA LA MINIATURA Y LA CELDA ---
    private final int anchoMiniaturaObjetivo;
    private final int altoMiniaturaObjetivo;
    private final int alturaTotalCeldaFija;
    private final int anchoTotalCeldaFijo;

    /**
     * Constructor.
     * @param servicioMiniaturas El servicio para obtener/crear miniaturas.
     * @param modeloVisor El modelo principal para obtener rutas completas.
     * @param anchoMiniaturaDeseado Ancho deseado para las imágenes en miniatura.
     * @param altoMiniaturaDeseado Alto deseado para las imágenes en miniatura.
     * @param mostrarNombresConfig Si los nombres de archivo deben ser visibles.
     * @param colorFondoDefault Color de fondo por defecto para la celda.
     * @param colorFondoSeleccionDefault Color de fondo para la celda cuando está seleccionada.
     * @param colorTextoDefault Color de texto por defecto.
     * @param colorTextoSeleccionDefault Color de texto para la celda cuando está seleccionada.
     * @param colorBordeSeleccion Color para el borde cuando la celda está seleccionada.
     */
    public MiniaturaListCellRenderer(
            ThumbnailService servicioMiniaturas,
            VisorModel modeloVisor,
            int anchoMiniaturaDeseado,
            int altoMiniaturaDeseado,
            boolean mostrarNombresConfig,
            Color colorFondoDefault,
            Color colorFondoSeleccionDefault,
            Color colorTextoDefault,
            Color colorTextoSeleccionDefault,
            Color colorBordeSeleccion) {

        // --- 5. INICIALIZACIÓN DE CAMPOS DE DEPENDENCIA Y CONFIGURACIÓN ---
        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas, "ThumbnailService no puede ser null");
        this.modeloVisor = Objects.requireNonNull(modeloVisor, "VisorModel no puede ser null");
        this.mostrarNombresConfigurado = mostrarNombresConfig;

        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;

        // --- 6. INICIALIZACIÓN DE ESTILOS VISUALES ---
        this.colorFondoNormal = colorFondoDefault != null ? colorFondoDefault : Color.WHITE;
        this.colorFondoSeleccionado = colorFondoSeleccionDefault != null ? colorFondoSeleccionDefault : new Color(57, 105, 138);
        this.colorTextoNormal = colorTextoDefault != null ? colorTextoDefault : Color.BLACK;
        this.colorTextoSeleccionado = colorTextoSeleccionDefault != null ? colorTextoSeleccionDefault : Color.WHITE;
        Color bordeSelColor = colorBordeSeleccion != null ? colorBordeSeleccion : Color.ORANGE;

        this.bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
        this.bordeSeleccionado = BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(bordeSelColor, 2),
                                    BorderFactory.createEmptyBorder(1, 3, 1, 3)
                                );

        // --- 7. CONFIGURACIÓN DEL LAYOUT DEL PANEL PRINCIPAL (ESTA CELDA) ---
        setLayout(new BorderLayout(0, 2)); // Gap vertical de 2px entre CENTER y SOUTH
        setOpaque(true);

        // --- 8. CREACIÓN Y CONFIGURACIÓN DE COMPONENTES INTERNOS ---
        // 8.1. Etiqueta para el icono de la miniatura
        this.etiquetaIcono = new JLabel(); // Asignar a campo final
        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));

        // 8.2. Panel contenedor para el icono
        this.panelContenedorIcono = new JPanel(new GridBagLayout()); // Asignar a campo final
        this.panelContenedorIcono.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        this.panelContenedorIcono.add(this.etiquetaIcono, gbc);

        // 8.3. Etiqueta para el nombre del archivo
        this.etiquetaNombre = new JLabel(); // Asignar a campo final
        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));

        // --- 9. CÁLCULO Y ASIGNACIÓN DE DIMENSIONES FIJAS DE LA CELDA ---
        // Estos campos final deben asignarse aquí.
        int paddingHorizontalTotal = 10; // 5px a cada lado
        int paddingVerticalTotal = 6;    // 3px arriba, 3px abajo
        int alturaNombreEstimada = 0;
        int gapVerticalLayout = 0;

        if (this.mostrarNombresConfigurado) {
            alturaNombreEstimada = 18; // Altura estimada para el texto del nombre
            gapVerticalLayout = 2;     // El gap del BorderLayout si el nombre se muestra
        }

        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotal;
        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo + alturaNombreEstimada + paddingVerticalTotal + gapVerticalLayout;

        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));

        // --- 10. AÑADIR COMPONENTES AL PANEL PRINCIPAL ---
        add(this.panelContenedorIcono, BorderLayout.CENTER);
        if (this.mostrarNombresConfigurado) {
            add(this.etiquetaNombre, BorderLayout.SOUTH);
        }
    } // Fin del constructor

    /**
     * Método para que VisorView pueda saber qué altura fija establecer en la JList.
     * @return La altura calculada para esta celda.
     */
    public int getAlturaCalculadaDeCelda() {
        return this.alturaTotalCeldaFija;
    }

    /**
     * Método para que VisorView pueda saber qué anchura fija establecer en la JList.
     * @return La anchura calculada para esta celda.
     */
    public int getAnchoCalculadaDeCelda() {
        return this.anchoTotalCeldaFijo;
    }

    // --- 11. MÉTODO PRINCIPAL DEL RENDERER: getListCellRendererComponent ---
    @Override
    public Component getListCellRendererComponent(JList<? extends String> list,
                                                   String value, // la clave de la imagen (ruta relativa)
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus) 
    {

        // 11.1. Manejar valor nulo
        if (value == null) {
            this.etiquetaNombre.setText("N/A");
            this.etiquetaIcono.setIcon(null);
            this.etiquetaIcono.setText("?");
            this.etiquetaNombre.setVisible(this.mostrarNombresConfigurado);
            setBackground(this.colorFondoNormal);
            // setForeground(this.colorTextoNormal); // No es necesario para el panel, sí para las etiquetas
            this.etiquetaNombre.setForeground(this.colorTextoNormal);
            this.etiquetaIcono.setForeground(Color.DARK_GRAY); // Color para el "?"
            setBorder(this.bordeNormal);
            return this;
        }

        // 11.2. Obtener ruta completa y miniatura
        Path rutaCompleta = this.modeloVisor.getRutaCompleta(value);
        ImageIcon miniatura = null;
        String nombreArchivoParaDisplay = value;

        if (rutaCompleta != null) {
            try {
                nombreArchivoParaDisplay = rutaCompleta.getFileName().toString();
            } catch (Exception e) {
                // Silencioso, nombreArchivoParaDisplay ya tiene la clave como fallback
            }
            miniatura = this.servicioMiniaturas.obtenerOCrearMiniatura(
                rutaCompleta, value,
                this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo,
                true
            );
        } else {
            nombreArchivoParaDisplay = value + " (Ruta INV)"; // Indicar problema
        }

        // 11.3. Configurar la etiqueta del icono
        this.etiquetaIcono.setIcon(miniatura);
        if (miniatura == null) {
            this.etiquetaIcono.setText("X");
            // El color se establecerá en la sección 11.5
        } else {
            this.etiquetaIcono.setText(null);
        }

        // 11.4. Configurar la etiqueta del nombre (visibilidad y texto)
        if (this.mostrarNombresConfigurado) {
            this.etiquetaNombre.setText(nombreArchivoParaDisplay);
            this.etiquetaNombre.setVisible(true);
        } else {
            this.etiquetaNombre.setText("");
            this.etiquetaNombre.setVisible(false);
        }

        // 11.5. Configurar colores y borde según el estado de selección
        if (isSelected) {
            setBackground(this.colorFondoSeleccionado);
            this.etiquetaNombre.setForeground(this.colorTextoSeleccionado);
            this.etiquetaIcono.setForeground(miniatura == null ? this.colorTextoSeleccionado : this.colorTextoNormal); // Color para 'X' si está seleccionada
            setBorder(this.bordeSeleccionado);
        } else {
            setBackground(this.colorFondoNormal);
            this.etiquetaNombre.setForeground(this.colorTextoNormal);
            this.etiquetaIcono.setForeground(miniatura == null ? Color.RED : this.colorTextoNormal); // Rojo si error, normal si no
            setBorder(this.bordeNormal);
        }

        return this;
    } // --- FIN metodo getListCellRendererComponent 
    
} // --- FIN clase MiniaturaListCellRenderer
