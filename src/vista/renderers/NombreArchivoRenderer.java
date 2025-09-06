package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.nio.file.Paths;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.UIManager;

import modelo.VisorModel;
import vista.theme.Tema;
import vista.theme.ThemeManager;

/**
 * Un ListCellRenderer personalizado para las listas de nombres de archivo del modo proyecto.
 * Es sensible al tema y al "foco lógico" del modo proyecto, permitiendo que la lista
 * inactiva se muestre con un aspecto "apagado" sin deshabilitarla.
 */
public class NombreArchivoRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    private final ThemeManager themeManager;
    private final boolean isForDiscardsList;
    
    private static final javax.swing.border.Border noFocusBorder = new javax.swing.border.EmptyBorder(1, 1, 1, 1);
    
    private final VisorModel model; // Referencia al modelo para consultar el foco lógico

    /**
     * Constructor principal.
     * @param themeManager El gestor de temas para obtener los colores.
     * @param model El modelo principal de la aplicación.
     * @param isForDiscardsList true si este renderer es para la lista de descartes.
     */
    public NombreArchivoRenderer(ThemeManager themeManager, VisorModel model, boolean isForDiscardsList) {
        if (themeManager == null || model == null) {
            throw new IllegalArgumentException("ThemeManager y VisorModel no pueden ser nulos en NombreArchivoRenderer");
        }
        this.themeManager = themeManager;
        this.model = model; 
        this.isForDiscardsList = isForDiscardsList;
        setOpaque(true); 
        
    } // --- Fin del método NombreArchivoRenderer (constructor) ---

    
    /**
     * Constructor de conveniencia para la lista de "Selección Actual".
     * @param themeManager El gestor de temas.
     * @param model El modelo principal.
     */
    public NombreArchivoRenderer(ThemeManager themeManager, VisorModel model) {
        this(themeManager, model, false);
        setOpaque(true); 
    } // --- Fin del método NombreArchivoRenderer (constructor de conveniencia) ---

    
//    @Override
//    public Component getListCellRendererComponent(
//            JList<?> list, 
//            Object value, 
//            int index, 
//            boolean isSelected, 
//            boolean cellHasFocus) 
//    {
//        // 1. Establecer el texto de la celda
//        if (value != null) {
//            try {
//                setText(Paths.get(value.toString()).getFileName().toString());
//            } catch (Exception e) {
//                setText(value.toString());
//            }
//        } else {
//            setText("");
//        }
//        
//        Tema temaActual = this.themeManager.getTemaActual();
//
//        // --- INICIO DE LA LÓGICA DE FOCO ---
//        boolean hasListFocus = list.hasFocus();
//        // --- FIN DE LA LÓGICA DE FOCO ---
//
//        if (isSelected) {
//            if (hasListFocus) {
//                // --- SELECCIÓN ACTIVA (la lista tiene el foco) ---
//                // Usamos los colores de acento vibrantes del tema.
//                setBackground(temaActual.colorSeleccionFondo());
//                setForeground(temaActual.colorSeleccionTexto());
//            } else {
//                // --- SELECCIÓN INACTIVA (la lista NO tiene el foco) ---
//                // Usamos un color gris neutro para indicar que la selección existe, pero no está activa.
//                // UIManager.getColor("List.selectionInactiveBackground") es el estándar en muchos LookAndFeels.
//                Color inactiveBg = UIManager.getColor("List.selectionInactiveBackground");
//                Color inactiveFg = UIManager.getColor("List.selectionInactiveForeground");
//                
//                setBackground(inactiveBg != null ? inactiveBg : Color.DARK_GRAY);
//                setForeground(inactiveFg != null ? inactiveFg : Color.LIGHT_GRAY);
//            }
//        } else {
//            // --- CELDA NO SELECCIONADA ---
//            // La lógica para las celdas no seleccionadas se queda igual,
//            // pero ahora distinguimos el fondo del panel activo/inactivo del proyecto.
//            String listaActivaEnModelo = model.getProyectoListContext().getNombreListaActiva();
//            boolean esEstaLaListaActiva = (isForDiscardsList && "descartes".equals(listaActivaEnModelo)) ||
//                                         (!isForDiscardsList && "seleccion".equals(listaActivaEnModelo));
//
//            if (esEstaLaListaActiva) {
//                setBackground(list.getBackground()); // Usar el color de fondo que le puso el ProjectController
//                setForeground(temaActual.colorTextoPrimario());
//            } else {
//                // El color de fondo de la lista inactiva
//                setBackground(list.getBackground()); 
//                setForeground(temaActual.colorTextoSecundario());
//            }
//        }
//
//        // El borde del foco de la celda individual se mantiene como estaba.
//        setBorder(cellHasFocus ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
//        
//        return this;
//        
//    } // --- Fin del método getListCellRendererComponent ---
    
    
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, 
            Object value, 
            int index, 
            boolean isSelected, 
            boolean cellHasFocus) 
    {
        // 1. Establecer el texto de la celda (tu lógica original)
        if (value != null) {
            try {
                setText(Paths.get(value.toString()).getFileName().toString());
            } catch (Exception e) {
                setText(value.toString());
            }
        } else {
            setText("");
        }
        
        // 2. Obtener el tema actual desde tu ThemeManager
        Tema temaActual = this.themeManager.getTemaActual();

        // 3. Aplicar colores basándose en si la celda está seleccionada o no
        if (isSelected) {
            // --- CELDA SELECCIONADA ---
            // Usa los colores de selección de tu tema. ¡Esto arreglará el selector invisible!
        	
            setBackground(temaActual.colorSeleccionFondo());
            setForeground(temaActual.colorSeleccionTexto());
        } else {
            // --- CELDA NO SELECCIONADA ---
            // Usa los colores normales de tu tema
            setBackground(temaActual.colorFondoPrincipal());
            setForeground(temaActual.colorTextoPrimario());
        }

        // 4. Gestionar el borde del foco para accesibilidad
        if (cellHasFocus) {
            // Usa el borde estándar de Swing para indicar el foco
            setBorder(javax.swing.UIManager.getBorder("List.focusCellHighlightBorder"));
        } else {
            // Usa el borde vacío que ya tenías definido para celdas sin foco
            setBorder(noFocusBorder); 
        }
        
        return this;
    
    } // --- Fin del método getListCellRendererComponent ---

} // --- Fin de la clase NombreArchivoRenderer ---

