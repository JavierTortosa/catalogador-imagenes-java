package vista.renderers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;
import java.util.Objects;

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
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

@SuppressWarnings("serial")
public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {

    // --- Dependencias y estado interno (inmutables) ---
    private final JLabel etiquetaIcono;
    private final JLabel etiquetaNombre;
    private final JPanel panelContenedorIcono;
    private final ThumbnailService servicioMiniaturas;
    private final VisorModel modeloVisor;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;
    private final boolean mostrarNombresConfigurado;
    private final int anchoMiniaturaObjetivo;
    private final int altoMiniaturaObjetivo;
    private final int alturaTotalCeldaFija;
    private final int anchoTotalCeldaFijo;

    public MiniaturaListCellRenderer(
            ThumbnailService servicioMiniaturas,
            VisorModel modeloVisor,
            ThemeManager themeManager,
            IconUtils iconUtils,
            int anchoMiniaturaDeseado,
            int altoMiniaturaDeseado,
            boolean mostrarNombresConfig
    ) {
        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas);
        this.modeloVisor = Objects.requireNonNull(modeloVisor);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.mostrarNombresConfigurado = mostrarNombresConfig;

        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;

        setLayout(new BorderLayout(0, 2));
        setOpaque(true);

        this.etiquetaIcono = new JLabel();
        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));

        this.panelContenedorIcono = new JPanel(new GridBagLayout());
        this.panelContenedorIcono.setOpaque(false);
        this.panelContenedorIcono.add(this.etiquetaIcono, new GridBagConstraints());

        this.etiquetaNombre = new JLabel();
        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));

        add(this.panelContenedorIcono, BorderLayout.CENTER);
        add(this.etiquetaNombre, BorderLayout.SOUTH);

        // --- CÁLCULO DE DIMENSIONES DE CELDA (CON LA CORRECCIÓN) ---
        final int paddingHorizontalTotalCelda = 10;
        final int paddingVerticalTotalCelda = 6;
        int alturaEstimadaParaNombre = this.mostrarNombresConfigurado ? 18 : 0;
        int gapVerticalEntreIconoYNombre = this.mostrarNombresConfigurado ? 2 : 0;
        
        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotalCelda;
        
        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo + 
                                    alturaEstimadaParaNombre + 
                                    paddingVerticalTotalCelda + // <-- La corrección clave
                                    gapVerticalEntreIconoYNombre;

        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));
    } // --- Fin del Constructor ---

    public int getAlturaCalculadaDeCelda() { return this.alturaTotalCeldaFija; }
    public int getAnchoCalculadaDeCelda() { return this.anchoTotalCeldaFijo; }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        
        Tema temaActual = this.themeManager.getTemaActual();
        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
        
        ImageIcon miniaturaCargada = null;
        if (rutaCompleta != null) {
            miniaturaCargada = this.servicioMiniaturas.obtenerOCrearMiniatura(
                rutaCompleta, value, this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo, true);
        }
        
        if (miniaturaCargada != null) {
            this.etiquetaIcono.setIcon(miniaturaCargada);
            this.etiquetaIcono.setText(null);
        } else {
            ImageIcon iconoErrorParaCelda = this.iconUtils.getScaledCommonIcon(
                "imagen-rota.png", this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo);
            this.etiquetaIcono.setIcon(iconoErrorParaCelda);
            this.etiquetaIcono.setText(iconoErrorParaCelda == null ? "X" : null);
        }

        if (this.mostrarNombresConfigurado) {
            String nombreParaMostrar = "N/A";
            if (rutaCompleta != null) {
                nombreParaMostrar = rutaCompleta.getFileName().toString();
            } else if (value != null) {
                nombreParaMostrar = value;
            }
            this.etiquetaNombre.setText(nombreParaMostrar);
            this.etiquetaNombre.setVisible(true);
        } else {
            this.etiquetaNombre.setText(" ");
            this.etiquetaNombre.setVisible(false);
        }

        Border bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
        Border bordeSeleccionado = BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2),
                                    BorderFactory.createEmptyBorder(1, 3, 1, 3));
        
        if (isSelected) {
            setBackground(temaActual.colorSeleccionFondo());
            this.etiquetaNombre.setForeground(temaActual.colorSeleccionTexto());
            setBorder(bordeSeleccionado);
        } else {
            setBackground(temaActual.colorFondoPrincipal());
            this.etiquetaNombre.setForeground(temaActual.colorTextoPrimario());
            setBorder(bordeNormal);
        }
        
        if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
            this.etiquetaIcono.setForeground(Color.RED);
        }

        return this;
    } // --- Fin del método getListCellRendererComponent ---
} // --- Fin de la clase MiniaturaListCellRenderer ---


//package vista.renderers;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.nio.file.Path;
//import java.util.Objects;
//
//import javax.swing.BorderFactory;
//import javax.swing.ImageIcon;
//import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JPanel;
//import javax.swing.ListCellRenderer;
//import javax.swing.SwingConstants;
//import javax.swing.border.Border;
//
//import modelo.VisorModel;
//import servicios.image.ThumbnailService;
//import vista.theme.Tema;
//import vista.theme.ThemeManager;
//import vista.util.IconUtils;
//
//@SuppressWarnings("serial")
//public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {
//
//    // --- Componentes Internos de la Celda ---
//    private final JLabel etiquetaIcono;
//    private final JLabel etiquetaNombre;
//    private final JPanel panelContenedorIcono;
//
//    // --- Dependencias Clave ---
//    private final ThumbnailService servicioMiniaturas;
//    private final VisorModel modeloVisor;
//    private final ThemeManager themeManager;
//    private final IconUtils iconUtils;
//
//    // --- Configuración y Dimensiones (establecidas en el constructor) ---
//    private final boolean mostrarNombresConfigurado;
//    private final int anchoMiniaturaObjetivo;
//    private final int altoMiniaturaObjetivo;
//    private final int alturaTotalCeldaFija;
//    private final int anchoTotalCeldaFijo;
//
//    public MiniaturaListCellRenderer(
//            ThumbnailService servicioMiniaturas,
//            VisorModel modeloVisor,
//            ThemeManager themeManager,
//            IconUtils iconUtils,
//            int anchoMiniaturaDeseado,
//            int altoMiniaturaDeseado,
//            boolean mostrarNombresConfig
//    ) {
//        // --- 1. Asignación de dependencias ---
//        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas);
//        this.modeloVisor = Objects.requireNonNull(modeloVisor);
//        this.themeManager = Objects.requireNonNull(themeManager);
//        this.iconUtils = Objects.requireNonNull(iconUtils);
//        this.mostrarNombresConfigurado = mostrarNombresConfig;
//
//        // --- 2. Asignación de dimensiones ---
//        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
//        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;
//
//        // --- 3. Configuración del Layout y Componentes Internos ---
//        setLayout(new BorderLayout(0, 2));
//        setOpaque(true);
//
//        this.etiquetaIcono = new JLabel();
//        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
//        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
//        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));
//
//        this.panelContenedorIcono = new JPanel(new GridBagLayout());
//        this.panelContenedorIcono.setOpaque(false);
//        this.panelContenedorIcono.add(this.etiquetaIcono, new GridBagConstraints());
//
//        this.etiquetaNombre = new JLabel();
//        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
//        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));
//
//        // --- 4. Añadir SIEMPRE los componentes al layout ---
//        // La visibilidad se controlará en getListCellRendererComponent.
//        add(this.panelContenedorIcono, BorderLayout.CENTER);
//        add(this.etiquetaNombre, BorderLayout.SOUTH);
//
//        // --- 5. Cálculo CORRECTO y DEFINITIVO de las dimensiones de la celda ---
//        final int paddingHorizontalTotalCelda = 10;
//        final int paddingVerticalTotalCelda = 6;
//        int alturaEstimadaParaNombre = this.mostrarNombresConfigurado ? 18 : 0;
//        int gapVerticalEntreIconoYNombre = this.mostrarNombresConfigurado ? 2 : 0;
//        
//        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotalCelda;
//        
//        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo + 
//                                    alturaEstimadaParaNombre + 
//                                    paddingVerticalTotalCelda + // <-- La corrección clave
//                                    gapVerticalEntreIconoYNombre;
//
//        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));
//    } // --- Fin del método (Constructor) ---
//
//    // --- Métodos para que la Vista configure la JList ---
//    public int getAlturaCalculadaDeCelda() { return this.alturaTotalCeldaFija; }
//    public int getAnchoCalculadaDeCelda() { return this.anchoTotalCeldaFijo; }
//
//    @Override
//    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
//        
//        // --- 1. OBTENER DATOS Y RECURSOS ---
//        Tema temaActual = this.themeManager.getTemaActual();
//        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
//        
//        // --- 2. CONFIGURAR ICONO ---
//        ImageIcon miniaturaCargada = null;
//        if (rutaCompleta != null) {
//            miniaturaCargada = this.servicioMiniaturas.obtenerOCrearMiniatura(
//                rutaCompleta, value, this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo, true);
//        }
//        
//        if (miniaturaCargada != null) {
//            this.etiquetaIcono.setIcon(miniaturaCargada);
//            this.etiquetaIcono.setText(null);
//        } else {
//            ImageIcon iconoErrorParaCelda = this.iconUtils.getScaledCommonIcon(
//                "imagen-rota.png", this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo);
//            if (iconoErrorParaCelda != null) {
//                this.etiquetaIcono.setIcon(iconoErrorParaCelda);
//                this.etiquetaIcono.setText(null);
//            } else {
//                this.etiquetaIcono.setIcon(null);
//                this.etiquetaIcono.setText("X");
//            }
//        }
//
//        // --- 3. CONFIGURAR NOMBRE (VISIBILIDAD Y CONTENIDO) ---
//        if (this.mostrarNombresConfigurado) {
//            String nombreParaMostrar = "N/A";
//            if (rutaCompleta != null) {
//                nombreParaMostrar = rutaCompleta.getFileName().toString();
//            } else if (value != null) {
//                nombreParaMostrar = value;
//            }
//            this.etiquetaNombre.setText(nombreParaMostrar);
//            this.etiquetaNombre.setVisible(true);
//        } else {
//            this.etiquetaNombre.setText(" "); // Espacio para mantener el layout
//            this.etiquetaNombre.setVisible(false);
//        }
//
//        // --- 4. APLICAR ESTILOS VISUALES ---
//        Border bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
//        Border bordeSeleccionado = BorderFactory.createCompoundBorder(
//                                    BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2),
//                                    BorderFactory.createEmptyBorder(1, 3, 1, 3));
//        
//        if (isSelected) {
//            setBackground(temaActual.colorSeleccionFondo());
//            this.etiquetaNombre.setForeground(temaActual.colorSeleccionTexto());
//            setBorder(bordeSeleccionado);
//        } else {
//            setBackground(temaActual.colorFondoPrincipal());
//            this.etiquetaNombre.setForeground(temaActual.colorTextoPrimario());
//            setBorder(bordeNormal);
//        }
//        
//        // Colorear el texto "X" de error si es necesario
//        if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
//            this.etiquetaIcono.setForeground(Color.RED);
//        }
//
//        return this;
//    } // --- Fin del método getListCellRendererComponent ---
//
//} // --- Fin de la clase MiniaturaListCellRenderer ---
//
////package vista.renderers;
////
////import java.awt.BorderLayout;
////import java.awt.Color;
////import java.awt.Component;
////import java.awt.Dimension;
////import java.awt.GridBagConstraints;
////import java.awt.GridBagLayout;
////import java.nio.file.Path;
////import java.util.Objects;
////
////import javax.swing.BorderFactory;
////import javax.swing.ImageIcon;
////import javax.swing.JLabel;
////import javax.swing.JList;
////import javax.swing.JPanel;
////import javax.swing.ListCellRenderer;
////import javax.swing.SwingConstants;
////import javax.swing.border.Border;
////
////import modelo.VisorModel;
////import servicios.image.ThumbnailService;
////import vista.theme.Tema;
////import vista.theme.ThemeManager;
////import vista.util.IconUtils;
////
////@SuppressWarnings("serial")
////public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {
////
////    // --- Componentes Internos de la Celda ---
////    private final JLabel etiquetaIcono;
////    private final JLabel etiquetaNombre;
////    private final JPanel panelContenedorIcono;
////
////    // --- Dependencias Clave ---
////    private final ThumbnailService servicioMiniaturas;
////    private final VisorModel modeloVisor;
////    private final ThemeManager themeManager;
////    private final IconUtils iconUtils;
////
////    // --- Configuración y Dimensiones ---
////    private boolean mostrarNombresConfigurado; // <<< CAMBIO: Ya no es final
////    private final int anchoMiniaturaObjetivo;
////    private final int altoMiniaturaObjetivo;
////    private final int alturaEstimadaParaNombre; // <<< CAMBIO: Guardamos la altura del nombre
////    private int alturaTotalCeldaFija; // <<< CAMBIO: Ya no es final
////    private final int anchoTotalCeldaFijo;
////    
////
////    public MiniaturaListCellRenderer(
////            ThumbnailService servicioMiniaturas,
////            VisorModel modeloVisor,
////            ThemeManager themeManager,
////            IconUtils iconUtils,
////            int anchoMiniaturaDeseado,
////            int altoMiniaturaDeseado,
////            boolean mostrarNombresConfig
////    ) {
////        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas);
////        this.modeloVisor = Objects.requireNonNull(modeloVisor);
////        this.themeManager = Objects.requireNonNull(themeManager);
////        this.iconUtils = Objects.requireNonNull(iconUtils);
////        this.mostrarNombresConfigurado = mostrarNombresConfig;
////
////        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
////        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;
////
////        // --- Configuración del Layout y Componentes Internos ---
////        setLayout(new BorderLayout(0, 2));
////        setOpaque(true);
////
////        this.etiquetaIcono = new JLabel();
////        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
////        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
////        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));
////
////        this.panelContenedorIcono = new JPanel(new GridBagLayout());
////        this.panelContenedorIcono.setOpaque(false);
////        this.panelContenedorIcono.add(this.etiquetaIcono, new GridBagConstraints());
////
////        this.etiquetaNombre = new JLabel();
////        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
////        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));
////
////        // <<< CAMBIO: Añadir SIEMPRE ambos componentes al layout
////        add(this.panelContenedorIcono, BorderLayout.CENTER);
////        add(this.etiquetaNombre, BorderLayout.SOUTH);
////
////        // --- Cálculo de Dimensiones Fijas ---
////        final int paddingHorizontalTotalCelda = 10;
////        final int paddingVerticalTotalCelda = 6;
////        this.alturaEstimadaParaNombre = 18; // Altura fija para el nombre
////        
////        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotalCelda;
////        
////        // <<< CAMBIO: La altura total ahora se calcula en un método separado
////        recalcularAlturaCelda();
////    }
////
////    // <<< CAMBIO: Nuevo método para recalcular la altura
////    private void recalcularAlturaCelda() {
////        final int paddingVerticalTotalCelda = 6;
////        int alturaNombreActual = this.mostrarNombresConfigurado ? this.alturaEstimadaParaNombre : 0;
////        int gapVerticalActual = this.mostrarNombresConfigurado ? 2 : 0;
////        
////        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo +
////        							alturaEstimadaParaNombre + 
//////        							alturaNombreActual + 
////        							paddingVerticalTotalCelda + 
////        							gapVerticalActual;
////        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));
////    }
////
////    public int getAlturaCalculadaDeCelda() { return this.alturaTotalCeldaFija; }
////    public int getAnchoCalculadaDeCelda() { return this.anchoTotalCeldaFijo; }
////
////    @Override
////    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
////        
////        // <<< CAMBIO: Comprobar si la configuración de mostrar nombres ha cambiado
////        boolean configActual = this.mostrarNombresConfigurado;
////        if (configActual != this.mostrarNombresConfigurado) {
////            this.mostrarNombresConfigurado = configActual;
////            recalcularAlturaCelda(); // Recalcula la altura si la config cambió
////        }
////        
////        Tema temaActual = this.themeManager.getTemaActual();
////        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
////        ImageIcon miniaturaCargada = null;
////        if (rutaCompleta != null) {
////            miniaturaCargada = this.servicioMiniaturas.obtenerOCrearMiniatura(
////                rutaCompleta, value, this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo, true);
////        }
////        
////        if (miniaturaCargada != null) {
////            this.etiquetaIcono.setIcon(miniaturaCargada);
////            this.etiquetaIcono.setText(null);
////        } else {
////            ImageIcon iconoErrorParaCelda = this.iconUtils.getScaledCommonIcon(
////                "imagen-rota.png", this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo);
////
////            if (iconoErrorParaCelda != null) {
////                this.etiquetaIcono.setIcon(iconoErrorParaCelda);
////            } else {
////                this.etiquetaIcono.setIcon(null);
////                this.etiquetaIcono.setText("X");
////            }
////        }
////        
////        // <<< CAMBIO: Manejo de la etiqueta de nombre simplificado
////        if (this.mostrarNombresConfigurado) {
////            this.etiquetaNombre.setText((value != null) ? rutaCompleta.getFileName().toString() : "N/A");
////            this.etiquetaNombre.setVisible(true);
////        } else {
////            this.etiquetaNombre.setText(" "); // Poner un espacio para que mantenga su altura
////            this.etiquetaNombre.setVisible(false);
////        }
////
////        Border bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
////        Border bordeSeleccionado = BorderFactory.createCompoundBorder(
////                                    BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2),
////                                    BorderFactory.createEmptyBorder(1, 3, 1, 3));
////        
////        if (isSelected) {
////            setBackground(temaActual.colorSeleccionFondo());
////            this.etiquetaNombre.setForeground(temaActual.colorSeleccionTexto());
////            setBorder(bordeSeleccionado);
////        } else {
////            setBackground(temaActual.colorFondoPrincipal());
////            this.etiquetaNombre.setForeground(temaActual.colorTextoPrimario());
////            setBorder(bordeNormal);
////        }
////        
////        if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
////            this.etiquetaIcono.setForeground(Color.RED);
////        }
////
////        return this;
////    }
////}
////
////// *********************************************************************************************************
////// *********************************************************************************************************
////
//////package vista.renderers;
//////
//////import java.awt.BorderLayout;
//////import java.awt.Color;
//////import java.awt.Component;
//////import java.awt.Dimension;
//////import java.awt.GridBagConstraints;
//////import java.awt.GridBagLayout;
//////import java.nio.file.Path;
//////import java.util.Objects;
//////
//////import javax.swing.BorderFactory;
//////import javax.swing.ImageIcon;
//////import javax.swing.JLabel;
//////import javax.swing.JList;
//////import javax.swing.JPanel;
//////import javax.swing.ListCellRenderer;
//////import javax.swing.SwingConstants;
//////import javax.swing.border.Border;
//////
//////import modelo.VisorModel;
//////import servicios.image.ThumbnailService;
//////import vista.theme.Tema;
//////import vista.theme.ThemeManager;
//////import vista.util.IconUtils;
//////
//////@SuppressWarnings("serial")
//////public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {
//////
//////    // --- Componentes Internos de la Celda ---
//////    private final JLabel etiquetaIcono;
//////    private final JLabel etiquetaNombre;
//////    private final JPanel panelContenedorIcono;
//////
//////    // --- Dependencias Clave ---
//////    private final ThumbnailService servicioMiniaturas;
//////    private final VisorModel modeloVisor;
//////    private final ThemeManager themeManager; // <--- ¡MODIFICACIÓN CLAVE!
//////    private final IconUtils iconUtils;       // <--- Recibimos la utilidad de iconos
//////
//////    // --- Configuración y Dimensiones ---
//////    private final boolean mostrarNombresConfigurado;
//////    private final int anchoMiniaturaObjetivo;
//////    private final int altoMiniaturaObjetivo;
//////    private final int alturaTotalCeldaFija;
//////    private final int anchoTotalCeldaFijo;
//////
//////    /**
//////     * Constructor del renderer "inteligente".
//////     * Ya no almacena colores, sino una referencia al ThemeManager.
//////     */
//////    public MiniaturaListCellRenderer(
//////            ThumbnailService servicioMiniaturas,
//////            VisorModel modeloVisor,
//////            ThemeManager themeManager, // <--- NUEVO
//////            IconUtils iconUtils,       // <--- NUEVO
//////            int anchoMiniaturaDeseado,
//////            int altoMiniaturaDeseado,
//////            boolean mostrarNombresConfig
//////    ) {
//////        // --- Asignación de Dependencias ---
//////        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas);
//////        this.modeloVisor = Objects.requireNonNull(modeloVisor);
//////        this.themeManager = Objects.requireNonNull(themeManager);
//////        this.iconUtils = Objects.requireNonNull(iconUtils);
//////        this.mostrarNombresConfigurado = mostrarNombresConfig;
//////
//////        // --- Configuración de Dimensiones ---
//////        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
//////        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;
//////
//////        // --- Configuración del Layout y Componentes Internos (sin cambios de lógica) ---
//////        setLayout(new BorderLayout(0, 2));
//////        setOpaque(true);
//////
//////        this.etiquetaIcono = new JLabel();
//////        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
//////        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
//////        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));
//////
//////        this.panelContenedorIcono = new JPanel(new GridBagLayout());
//////        this.panelContenedorIcono.setOpaque(false);
//////        this.panelContenedorIcono.add(this.etiquetaIcono, new GridBagConstraints());
//////
//////        this.etiquetaNombre = new JLabel();
//////        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
//////        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));
//////        this.etiquetaNombre.setVisible(this.mostrarNombresConfigurado);
//////        
//////        add(this.panelContenedorIcono, BorderLayout.CENTER);
//////        if (this.mostrarNombresConfigurado) {
//////            add(this.etiquetaNombre, BorderLayout.SOUTH);
//////        }
//////
//////        // --- Cálculo de Dimensiones Fijas (sin cambios de lógica) ---
//////        final int paddingHorizontalTotalCelda = 10;
//////        final int paddingVerticalTotalCelda = 6;
//////        int alturaEstimadaParaNombre = this.mostrarNombresConfigurado ? 18 : 0;
//////        int gapVerticalEntreIconoYNombre = this.mostrarNombresConfigurado ? 2 : 0;
//////        
//////        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotalCelda;
//////        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo + alturaEstimadaParaNombre + paddingVerticalTotalCelda + gapVerticalEntreIconoYNombre;
//////        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));
//////
//////    } // --- Fin del Constructor ---
//////
//////    public int getAlturaCalculadaDeCelda() { return this.alturaTotalCeldaFija; }
//////    public int getAnchoCalculadaDeCelda() { return this.anchoTotalCeldaFijo; }
//////
//////    @Override
//////    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
//////        
//////        // --- 1. OBTENER EL TEMA Y LOS RECURSOS VISUALES ACTUALES ---
//////        //    ¡LA MAGIA! Se obtienen los datos frescos en cada llamada a pintar.
//////        Tema temaActual = this.themeManager.getTemaActual();
//////
//////        // --- 2. Lógica de carga de miniatura (sin cambios) ---
//////        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
//////        ImageIcon miniaturaCargada = null;
//////        if (rutaCompleta != null) {
//////            miniaturaCargada = this.servicioMiniaturas.obtenerOCrearMiniatura(
//////                rutaCompleta, value, this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo, true);
//////        }
//////        
//////        // --- 3. Configurar la etiqueta del icono ---
//////        if (miniaturaCargada != null) {
//////            this.etiquetaIcono.setIcon(miniaturaCargada);
//////            this.etiquetaIcono.setText(null);
//////        } else {
//////            // Cargar el icono de error con el TAMAÑO correcto CADA VEZ,
//////            // porque la carpeta de iconos del tema podría haber cambiado.
//////            ImageIcon iconoErrorParaCelda = this.iconUtils.getScaledCommonIcon(
//////                "imagen-rota.png", this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo);
//////
//////            if (iconoErrorParaCelda != null) {
//////                this.etiquetaIcono.setIcon(iconoErrorParaCelda);
//////                this.etiquetaIcono.setText(null);
//////            } else {
//////                this.etiquetaIcono.setIcon(null);
//////                this.etiquetaIcono.setText("X");
//////            }
//////        }
//////
//////        // --- 4. Configurar la etiqueta del nombre ---
//////        if (this.mostrarNombresConfigurado) {
//////            this.etiquetaNombre.setText((value != null) ? value : "N/A");
//////            this.etiquetaNombre.setVisible(true);
//////        } else {
//////            this.etiquetaNombre.setVisible(false);
//////        }
//////
//////        // --- 5. APLICAR ESTILOS VISUALES DEL TEMA ACTUAL ---
//////        Border bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
//////        Border bordeSeleccionado = BorderFactory.createCompoundBorder(
//////                                    BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2),
//////                                    BorderFactory.createEmptyBorder(1, 3, 1, 3));
//////        
//////        if (isSelected) {
//////            setBackground(temaActual.colorSeleccionFondo());
//////            this.etiquetaNombre.setForeground(temaActual.colorSeleccionTexto());
//////            setBorder(bordeSeleccionado);
//////        } else {
//////            setBackground(temaActual.colorFondoPrincipal()); // Usar fondo principal del tema
//////            this.etiquetaNombre.setForeground(temaActual.colorTextoPrimario());
//////            setBorder(bordeNormal);
//////        }
//////        
//////        // Colorear el texto de error "X"
//////        if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
//////            this.etiquetaIcono.setForeground(Color.RED);
//////        }
//////
//////        return this;
//////    } // --- Fin del método getListCellRendererComponent ---
//////
//////} // --- Fin de la clase MiniaturaListCellRenderer ---
////
////// *********************************************************************************************************
////// *********************************************************************************************************
////
////
//////package vista.renderers;
//////
//////import java.awt.BorderLayout; // Usaremos BorderLayout
//////import java.awt.Color;
//////import java.awt.Component;
//////import java.awt.Dimension;
//////import java.awt.GridBagConstraints;
//////import java.awt.GridBagLayout;
//////import java.nio.file.Path;
//////import java.util.Objects; // Para Objects.requireNonNull
//////
//////import javax.swing.BorderFactory;
//////import javax.swing.ImageIcon;
//////import javax.swing.JLabel;
//////import javax.swing.JList;
//////import javax.swing.JPanel;
//////import javax.swing.ListCellRenderer;
//////import javax.swing.SwingConstants;
//////import javax.swing.border.Border;
//////
//////import modelo.VisorModel;
//////import servicios.image.ThumbnailService;
//////import vista.util.IconUtils;
//////
//////@SuppressWarnings("serial")
//////public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {
//////
//////    // --- 1. COMPONENTES INTERNOS DE LA CELDA ---
//////    private final JLabel etiquetaIcono;
//////    private final JLabel etiquetaNombre;
//////    private final JPanel panelContenedorIcono;
//////
//////    // --- 2. DEPENDENCIAS Y CONFIGURACIÓN INMUTABLE ---
//////    private final ThumbnailService servicioMiniaturas;
//////    private final VisorModel modeloVisor;
//////    private final boolean mostrarNombresConfigurado;
//////
//////    // --- 3. ESTILOS VISUALES (COLORES Y BORDES) ---
//////    private final Color colorTextoNormal;
//////    private final Color colorFondoNormal;
//////    private final Color colorTextoSeleccionado;
//////    private final Color colorFondoSeleccionado;
//////    private final Border bordeNormal;
//////    private final Border bordeSeleccionado;
//////
//////    // --- 4. DIMENSIONES PARA LA MINIATURA Y LA CELDA ---
//////    private final int anchoMiniaturaObjetivo;
//////    private final int altoMiniaturaObjetivo;
//////    private final int alturaTotalCeldaFija;
//////    private final int anchoTotalCeldaFijo;
//////    
//////    // --- 5. CAMPO para el icono de error escalado para la celda ---
//////    private ImageIcon iconoErrorParaCelda;
//////    private final IconUtils iconUtilsRef;
//////
//////    /**
//////     * Constructor del renderer para celdas de miniaturas.
//////     * Inicializa los componentes de la celda, configura estilos y calcula dimensiones.
//////     *
//////     * @param servicioMiniaturas    El servicio para obtener/crear miniaturas de imágenes.
//////     * @param modeloVisor           El modelo principal para obtener rutas completas de las imágenes.
//////     * @param anchoMiniaturaDeseado Ancho deseado para las imágenes en miniatura (ej. 40px).
//////     * @param altoMiniaturaDeseado  Alto deseado para las imágenes en miniatura (ej. 40px).
//////     * @param mostrarNombresConfig  Si true, se mostrará el nombre del archivo debajo de la miniatura.
//////     * @param colorFondoDefault     Color de fondo para celdas no seleccionadas.
//////     * @param colorFondoSeleccionDefault Color de fondo para celdas seleccionadas.
//////     * @param colorTextoDefault     Color de texto para celdas no seleccionadas.
//////     * @param colorTextoSeleccionDefault Color de texto para celdas seleccionadas.
//////     * @param colorBordeSeleccion   Color del borde para celdas seleccionadas.
//////     * @param iconUtils             La instancia de IconUtils para cargar/escalar el icono de error común.
//////     */
//////    public MiniaturaListCellRenderer(
//////            ThumbnailService servicioMiniaturas,
//////            VisorModel modeloVisor,
//////            int anchoMiniaturaDeseado,
//////            int altoMiniaturaDeseado,
//////            boolean mostrarNombresConfig,
//////            Color colorFondoDefault,
//////            Color colorFondoSeleccionDefault,
//////            Color colorTextoDefault,
//////            Color colorTextoSeleccionDefault,
//////            Color colorBordeSeleccion,
//////            IconUtils iconUtils // Parámetro para la instancia de IconUtils
//////    ) {
//////        // --- 1. VALIDACIÓN Y ASIGNACIÓN DE DEPENDENCIAS PRINCIPALES ---
//////        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas, "ThumbnailService no puede ser null en MiniaturaListCellRenderer");
//////        this.modeloVisor = Objects.requireNonNull(modeloVisor, "VisorModel no puede ser null en MiniaturaListCellRenderer");
//////        this.mostrarNombresConfigurado = mostrarNombresConfig;
//////        this.iconUtilsRef = Objects.requireNonNull(iconUtils, "IconUtils (iconUtils) no puede ser null en MiniaturaListCellRenderer");
//////
//////        // --- 2. ESTABLECER DIMENSIONES OBJETIVO PARA LA IMAGEN DE LA MINIATURA ---
//////        //    Se asegura de que haya un valor positivo por defecto si los parámetros son inválidos.
//////        this.anchoMiniaturaObjetivo = anchoMiniaturaDeseado > 0 ? anchoMiniaturaDeseado : 40;
//////        this.altoMiniaturaObjetivo = altoMiniaturaDeseado > 0 ? altoMiniaturaDeseado : 40;
//////
//////        // --- 3. CARGAR Y ESCALAR EL ICONO DE ERROR PARA LA CELDA ---
//////        //    Este icono se usará si una miniatura no se puede generar.
//////        if (this.iconUtilsRef != null) { // Doble chequeo, aunque Objects.requireNonNull ya lo hizo.
//////            this.iconoErrorParaCelda = this.iconUtilsRef.getScaledCommonIcon(
//////                "imagen-rota.png", // Nombre del archivo en "resources/iconos/comunes/"
//////                this.anchoMiniaturaObjetivo,
//////                this.altoMiniaturaObjetivo
//////            );
//////            if (this.iconoErrorParaCelda == null) {
//////                System.err.println("WARN [MiniaturaListCellRenderer Constructor]: No se pudo cargar/escalar 'imagen-rota.png' para la celda. Se usará 'X'.");
//////            } else {
//////                // System.out.println("  [MiniaturaListCellRenderer Constructor] iconoErrorParaCelda cargado y escalado exitosamente.");
//////            }
//////        } else {
//////            this.iconoErrorParaCelda = null; // Si iconUtils es null, no podemos cargar el icono.
//////        }
//////
//////        // --- 4. CONFIGURACIÓN DE ESTILOS VISUALES (COLORES Y BORDES) ---
//////        //    Se usan los colores pasados o defaults si son null.
//////        this.colorFondoNormal = colorFondoDefault != null ? colorFondoDefault : Color.WHITE;
//////        this.colorFondoSeleccionado = colorFondoSeleccionDefault != null ? colorFondoSeleccionDefault : new Color(57, 105, 138);
//////        this.colorTextoNormal = colorTextoDefault != null ? colorTextoDefault : Color.BLACK;
//////        this.colorTextoSeleccionado = colorTextoSeleccionDefault != null ? colorTextoSeleccionDefault : Color.WHITE;
//////        Color bordeColorParaSeleccion = colorBordeSeleccion != null ? colorBordeSeleccion : Color.ORANGE;
//////
//////        // Definición de bordes para estado normal y seleccionado (incluye padding)
//////        this.bordeNormal = BorderFactory.createEmptyBorder(3, 5, 3, 5); // Padding: Top, Left, Bottom, Right
//////        this.bordeSeleccionado = BorderFactory.createCompoundBorder(
//////                                    BorderFactory.createLineBorder(bordeColorParaSeleccion, 2), // Borde externo coloreado
//////                                    BorderFactory.createEmptyBorder(1, 3, 1, 3) // Padding interno (ajustado por el grosor del borde)
//////                                );
//////
//////        // --- 5. CONFIGURACIÓN DEL LAYOUT DEL PANEL PRINCIPAL (ESTE JPANEL ES LA CELDA) ---
//////        setLayout(new BorderLayout(0, 2)); // Gap vertical de 2px entre el área del icono y el área del nombre
//////        setOpaque(true); // Esencial para que setBackground() tenga efecto.
//////
//////        // --- 6. CREACIÓN Y CONFIGURACIÓN DE LOS COMPONENTES INTERNOS DE LA CELDA ---
//////
//////        // 6.1. JLabel para mostrar el icono de la miniatura (o el icono de error, o texto "X")
//////        this.etiquetaIcono = new JLabel();
//////        this.etiquetaIcono.setHorizontalAlignment(SwingConstants.CENTER);
//////        this.etiquetaIcono.setVerticalAlignment(SwingConstants.CENTER);
//////        // El preferredSize asegura que el JLabel tenga el tamaño de la imagen de la miniatura,
//////        // lo que ayuda al panelContenedorIcono a centrarla correctamente.
//////        this.etiquetaIcono.setPreferredSize(new Dimension(this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo));
//////
//////        // 6.2. JPanel intermedio para centrar la etiquetaIcono.
//////        //      GridBagLayout sin constraints específicos centra su contenido.
//////        this.panelContenedorIcono = new JPanel(new GridBagLayout());
//////        this.panelContenedorIcono.setOpaque(false); // Para que tome el color de fondo del panel principal
//////        GridBagConstraints gbcIcono = new GridBagConstraints(); // Constraints por defecto (CENTER)
//////        this.panelContenedorIcono.add(this.etiquetaIcono, gbcIcono);
//////
//////        // 6.3. JLabel para mostrar el nombre del archivo (si está configurado)
//////        this.etiquetaNombre = new JLabel();
//////        this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
//////        // Usar una fuente un poco más pequeña para los nombres de archivo.
//////        this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));
//////        this.etiquetaNombre.setVisible(this.mostrarNombresConfigurado); // Visibilidad inicial
//////
//////        // --- 7. CÁLCULO DE LAS DIMENSIONES FIJAS TOTALES DE LA CELDA ---
//////        //    Estos valores se usarán para JList.setFixedCellWidth/Height.
//////        final int paddingHorizontalTotalCelda = 10; // Suma de bordeNormal.getBorderInsets().left + right (5+5)
//////        final int paddingVerticalTotalCelda = 6;    // Suma de bordeNormal.getBorderInsets().top + bottom (3+3)
//////        int alturaEstimadaParaNombre = 0;
//////        int gapVerticalEntreIconoYNombre = 0;
//////
//////        if (this.mostrarNombresConfigurado) {
//////            // Si se muestran nombres, estimar la altura que ocupará la etiqueta del nombre.
//////            // Esto es una aproximación; para mayor precisión se podría usar FontMetrics.
//////            alturaEstimadaParaNombre = 18; // Altura típica para una fuente de 10pt + algo de espacio.
//////            gapVerticalEntreIconoYNombre = 2; // El Vgap del BorderLayout.
//////        }
//////
//////        // Ancho total = ancho de la imagen de miniatura + padding horizontal de la celda.
//////        this.anchoTotalCeldaFijo = this.anchoMiniaturaObjetivo + paddingHorizontalTotalCelda;
//////
//////        // Alto total = alto de la imagen de miniatura + altura del nombre (si visible) +
//////        //              padding vertical de la celda + gap del layout (si nombre visible).
//////        this.alturaTotalCeldaFija = this.altoMiniaturaObjetivo +
//////                                    alturaEstimadaParaNombre +
//////                                    paddingVerticalTotalCelda +
//////                                    gapVerticalEntreIconoYNombre;
//////
//////        // Establecer el tamaño preferido de este JPanel (la celda) para ayudar a la JList.
//////        setPreferredSize(new Dimension(this.anchoTotalCeldaFijo, this.alturaTotalCeldaFija));
//////
//////        // --- 8. AÑADIR COMPONENTES AL PANEL PRINCIPAL (ESTA CELDA) ---
//////        add(this.panelContenedorIcono, BorderLayout.CENTER); // El icono (o su contenedor) en el centro.
//////        if (this.mostrarNombresConfigurado) {
//////            add(this.etiquetaNombre, BorderLayout.SOUTH); // El nombre debajo.
//////        }
//////
//////        // System.out.println("  [MiniaturaListCellRenderer Constructor] Renderer de celda inicializado. " +
//////        //                    "Dimensiones calculadas de celda: " + this.anchoTotalCeldaFijo + "x" + this.alturaTotalCeldaFija);
//////    } // --- FIN del Constructor ---
//////
//////
//////    /**
//////     * Método para que VisorView pueda saber qué altura fija establecer en la JList.
//////     * @return La altura calculada para esta celda.
//////     */
//////    public int getAlturaCalculadaDeCelda() {
//////        return this.alturaTotalCeldaFija;
//////    }
//////
//////    /**
//////     * Método para que VisorView pueda saber qué anchura fija establecer en la JList.
//////     * @return La anchura calculada para esta celda.
//////     */
//////    public int getAnchoCalculadaDeCelda() {
//////        return this.anchoTotalCeldaFijo;
//////    }
//////
//////    // --- 11. MÉTODO PRINCIPAL DEL RENDERER: getListCellRendererComponent ---
//////    
//////    /**
//////     * Configura y devuelve el componente que Swing usará para dibujar esta celda en la JList.
//////     * Este método es llamado por la JList para cada ítem que necesita ser dibujado.
//////     *
//////     * @param list La JList en la que estamos pintando.
//////     * @param value El valor a visualizar (en tu caso, la clave de la imagen, ej. ruta relativa).
//////     * @param index El índice de la celda.
//////     * @param isSelected true si la celda está actualmente seleccionada.
//////     * @param cellHasFocus true si la celda actualmente tiene el foco.
//////     * @return El componente JPanel configurado para representar esta celda.
//////     */
//////    @Override
//////    public Component getListCellRendererComponent(JList<? extends String> list,
//////                                                   String value, // la clave de la imagen (ruta relativa)
//////                                                   int index,
//////                                                   boolean isSelected,
//////                                                   boolean cellHasFocus) {
//////
//////        // --- 1. MANEJAR VALOR NULO O INVÁLIDO ---
//////        //    Si el 'value' (clave de imagen) es null, mostramos un estado por defecto.
//////        if (value == null) {
//////            // System.out.println("  [Renderer Miniatura] Valor nulo para índice: " + index);
//////            this.etiquetaNombre.setText("N/A");
//////            this.etiquetaIcono.setIcon(null); // No hay icono de error específico para "valor nulo" aquí, solo texto
//////            this.etiquetaIcono.setText("?");  // Indicador textual de valor nulo
//////            this.etiquetaNombre.setVisible(this.mostrarNombresConfigurado); // Respetar config de mostrar nombres
//////
//////            // Aplicar estilos de no seleccionado
//////            setBackground(this.colorFondoNormal);
//////            this.etiquetaNombre.setForeground(this.colorTextoNormal);
//////            this.etiquetaIcono.setForeground(Color.DARK_GRAY); // Color para el "?"
//////            setBorder(this.bordeNormal);
//////            return this; // Devolver el panel configurado
//////        }
//////
//////        // --- 2. OBTENER RUTA COMPLETA Y MINIATURA ---
//////        //    'value' es la clave única de la imagen (ej. ruta relativa).
//////        Path rutaCompleta = this.modeloVisor.getRutaCompleta(value);
//////        ImageIcon miniaturaCargada = null; // La miniatura que se obtendrá del servicio
//////        String nombreArchivoParaDisplay = value; // Fallback al 'value' si no se puede obtener nombre de archivo
//////
//////        if (rutaCompleta != null) {
//////            // Intentar obtener un nombre de archivo más amigable si la ruta es válida
//////            try {
//////                nombreArchivoParaDisplay = rutaCompleta.getFileName().toString();
//////            } catch (Exception e) {
//////                // En caso de error al obtener el nombre, se mantiene 'value' como nombre.
//////                // System.err.println("WARN [Renderer Miniatura] No se pudo obtener nombre de archivo de: " + rutaCompleta);
//////            }
//////
//////            // Solicitar la miniatura al ThumbnailService.
//////            // Se asume que el tamaño normal de caché se usa aquí (esTamanoNormal = true).
//////            miniaturaCargada = this.servicioMiniaturas.obtenerOCrearMiniatura(
//////                rutaCompleta,
//////                value, // claveUnica
//////                this.anchoMiniaturaObjetivo,
//////                this.altoMiniaturaObjetivo,
//////                true   // esTamanoNormal (para que se guarde/recupere del caché principal)
//////            );
//////        } else {
//////            // Si la rutaCompleta es null, es un problema con el modelo o la clave.
//////            nombreArchivoParaDisplay = value + " (Ruta Inválida)"; // Indicar problema en el nombre
//////             System.err.println("WARN [Renderer Miniatura] Ruta completa es null para clave: " + value);
//////            // miniaturaCargada permanecerá null.
//////        }
//////
//////        // --- 3. CONFIGURAR LA ETIQUETA DEL ICONO (etiquetaIcono) ---
//////        if (miniaturaCargada != null) {
//////            // 3a. Caso Éxito: Se obtuvo una miniatura.
//////            this.etiquetaIcono.setIcon(miniaturaCargada);
//////            this.etiquetaIcono.setText(null); // No mostrar texto si hay icono
//////        } else {
//////            // 3b. Caso Error: No se pudo obtener/crear la miniatura.
//////            //     Intentar mostrar el icono de error general escalado para la celda.
//////            if (this.iconoErrorParaCelda != null) {
//////                this.etiquetaIcono.setIcon(this.iconoErrorParaCelda);
//////                this.etiquetaIcono.setText(null);
//////            } else {
//////                // Fallback final si incluso el icono de error para celda no está disponible: mostrar "X".
//////                this.etiquetaIcono.setIcon(null);
//////                this.etiquetaIcono.setText("X");
//////                // El color de la "X" se establecerá en la sección de estilos.
//////            }
//////        }
//////
//////        // --- 4. CONFIGURAR LA ETIQUETA DEL NOMBRE (etiquetaNombre) ---
//////        if (this.mostrarNombresConfigurado) {
//////            this.etiquetaNombre.setText(nombreArchivoParaDisplay);
//////            this.etiquetaNombre.setVisible(true);
//////        } else {
//////            this.etiquetaNombre.setText(""); // Limpiar texto si no se deben mostrar
//////            this.etiquetaNombre.setVisible(false);
//////        }
//////
//////        // --- 5. APLICAR ESTILOS VISUALES (COLORES Y BORDE) SEGÚN SELECCIÓN ---
//////        if (isSelected) {
//////            setBackground(this.colorFondoSeleccionado);
//////            this.etiquetaNombre.setForeground(this.colorTextoSeleccionado);
//////            setBorder(this.bordeSeleccionado);
//////
//////            // Establecer color para el texto de la etiquetaIcono si está mostrando "X" o "?"
//////            if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
//////                this.etiquetaIcono.setForeground(this.colorTextoSeleccionado);
//////            } else {
//////                this.etiquetaIcono.setForeground(null); // Restablecer para que el icono use sus propios colores
//////            }
//////        } else { // No seleccionada
//////            setBackground(this.colorFondoNormal);
//////            this.etiquetaNombre.setForeground(this.colorTextoNormal);
//////            setBorder(this.bordeNormal);
//////
//////            // Establecer color para el texto de la etiquetaIcono si está mostrando "X" o "?"
//////            if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
//////                if ("?".equals(this.etiquetaIcono.getText())) { // Si es el '?' de valor nulo
//////                    this.etiquetaIcono.setForeground(Color.DARK_GRAY);
//////                } else { // Si es la 'X' de error de carga
//////                    this.etiquetaIcono.setForeground(Color.RED);
//////                }
//////            } else {
//////                this.etiquetaIcono.setForeground(null); // Restablecer
//////            }
//////        }
//////        
//////        boolean huboErrorEnMiniatura = (miniaturaCargada == null && rutaCompleta != null); // Indica un error de carga real, no solo valor nulo
//////
//////        if (isSelected) {
//////            setBackground(this.colorFondoSeleccionado);
//////            this.etiquetaNombre.setForeground(this.colorTextoSeleccionado);
//////            setBorder(this.bordeSeleccionado);
//////
//////            if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
//////                this.etiquetaIcono.setForeground(this.colorTextoSeleccionado);
//////            } else {
//////                this.etiquetaIcono.setForeground(null);
//////            }
//////        } else { // No seleccionada
//////            if (huboErrorEnMiniatura) {
//////                setBackground(new Color(255, 200, 200)); // Un rojo pálido para el fondo
//////                this.etiquetaNombre.setForeground(Color.DARK_GRAY); // Un color de texto que contraste con el rojo pálido
//////                this.etiquetaIcono.setForeground(Color.RED); // Para la "X" si se muestra
//////            } else {
//////                setBackground(this.colorFondoNormal);
//////                this.etiquetaNombre.setForeground(this.colorTextoNormal);
//////                if (this.etiquetaIcono.getIcon() == null && "?".equals(this.etiquetaIcono.getText())) {
//////                    this.etiquetaIcono.setForeground(Color.DARK_GRAY);
//////                } else {
//////                    this.etiquetaIcono.setForeground(null); // El icono se encarga, o el color por defecto de la "X" si hubo error de iconoErrorParaCelda
//////                }
//////            }
//////            setBorder(this.bordeNormal);
//////        }
//////
//////        // Devolver este panel (la celda) para ser dibujado por la JList.
//////        return this;
//////    } // --- FIN del método getListCellRendererComponent ---
//////    
//////} // --- FIN clase MiniaturaListCellRenderer
