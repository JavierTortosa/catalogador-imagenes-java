package vista.renderers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import servicios.image.ThumbnailService;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class MiniaturaListCellRenderer extends JPanel implements ListCellRenderer<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
    // --- Dependencias y estado interno (inmutables) ---
    private final JLabel etiquetaIcono;
    private final JLabel etiquetaNombre;
    private final JPanel panelContenedorIcono;
    private final ThumbnailService servicioMiniaturas;
    private final VisorModel modeloVisor;
    private final IProjectManager projectManager;
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
            IProjectManager projectManager,
            ThemeManager themeManager,
            IconUtils iconUtils,
            int anchoMiniaturaDeseado,
            int altoMiniaturaDeseado,
            boolean mostrarNombresConfig
    ) {
    	
    	
        this.servicioMiniaturas = Objects.requireNonNull(servicioMiniaturas);
        this.modeloVisor = Objects.requireNonNull(modeloVisor);
        this.projectManager = projectManager;
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

        if (this.mostrarNombresConfigurado) {
            this.etiquetaNombre = new JLabel();
            this.etiquetaNombre.setHorizontalAlignment(SwingConstants.CENTER);
            this.etiquetaNombre.setFont(this.etiquetaNombre.getFont().deriveFont(10.0f));
            add(this.panelContenedorIcono, BorderLayout.CENTER);
            add(this.etiquetaNombre, BorderLayout.SOUTH);
        } else {
            this.etiquetaNombre = null;
            add(this.panelContenedorIcono, BorderLayout.CENTER);
        }

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
                rutaCompleta, value, this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo, true,
                (generatedKey) -> {
                    if (list != null && list.isShowing()) {
                        ListModel<? extends String> model = list.getModel();
                        for (int i = 0; i < model.getSize(); i++) {
                            if (generatedKey.equals(model.getElementAt(i))) {
                                Rectangle cellBounds = list.getCellBounds(i, i);
                                if (cellBounds != null) list.repaint(cellBounds);
                                break;
                            }
                        }
                    }
                }
            );
        }
        
        if (miniaturaCargada != null) {
            this.etiquetaIcono.setIcon(miniaturaCargada);
            this.etiquetaIcono.setText(null);
        } else {
            ImageIcon iconoPlaceholder = this.iconUtils.getScaledCommonIcon(
                "placeholder-grid.png", this.anchoMiniaturaObjetivo, this.altoMiniaturaObjetivo);
            this.etiquetaIcono.setIcon(iconoPlaceholder);
            this.etiquetaIcono.setText(null);
        }

        if (this.etiquetaNombre != null) {
            String nombreParaMostrar = "N/A";
            if (rutaCompleta != null) {
                Path fileNamePath = rutaCompleta.getFileName();
                nombreParaMostrar = (fileNamePath != null) ? fileNamePath.toString() : rutaCompleta.toString();
            } else if (value != null) {
                nombreParaMostrar = value;
            }
            this.etiquetaNombre.setText(nombreParaMostrar);
            this.etiquetaNombre.setVisible(true);
        }

        boolean isMarked = false;
        if (rutaCompleta != null && projectManager != null) {
            isMarked = projectManager.estaMarcada(rutaCompleta);
        }

        Border bordeFinal;
        Border bordeVacioInterno = BorderFactory.createEmptyBorder(1, 3, 1, 3);
        
        if (isSelected) {
            setBackground(temaActual.colorSeleccionFondo());
            if (this.etiquetaNombre != null) this.etiquetaNombre.setForeground(temaActual.colorSeleccionTexto());
            
            Border bordeFoco = BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2);
            
            if (isMarked) {
                // DOBRE MARCO: Azul (Foco) + Verde (Marcada)
                Border bordeMarcado = BorderFactory.createLineBorder(temaActual.colorImagenMarcada(), 2);
                bordeFinal = BorderFactory.createCompoundBorder(bordeFoco, bordeMarcado);
            } else {
                bordeFinal = BorderFactory.createCompoundBorder(bordeFoco, bordeVacioInterno);
            }
        } else {
            setBackground(temaActual.colorFondoPrincipal());
            if (this.etiquetaNombre != null) this.etiquetaNombre.setForeground(temaActual.colorTextoPrimario());
            
            if (isMarked) {
                // Solo borde de Marcada (Verde)
                bordeFinal = BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(temaActual.colorImagenMarcada(), 2),
                                bordeVacioInterno);
            } else {
                bordeFinal = BorderFactory.createEmptyBorder(3, 5, 3, 5);
            }
        }
        
        setBorder(bordeFinal);
        
        if (this.etiquetaIcono.getIcon() == null && this.etiquetaIcono.getText() != null) {
            this.etiquetaIcono.setForeground(Color.RED);
        }

        return this;
    } // --- Fin del método getListCellRendererComponent ---
    
} // --- Fin de la clase MiniaturaListCellRenderer ---

