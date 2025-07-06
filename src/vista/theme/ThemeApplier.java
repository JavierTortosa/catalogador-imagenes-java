package vista.theme;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import controlador.utils.ComponentRegistry; // ¡La dependencia clave!

/**
 * Clase responsable de aplicar un objeto Tema a todos los componentes
 * de la UI registrados en un ComponentRegistry.
 */
public class ThemeApplier {

    private final ComponentRegistry registry;

    public ThemeApplier(ComponentRegistry registry) {
        this.registry = registry;
    }

    /**
     * Aplica un tema específico a la interfaz de usuario.
     * Este es el método principal que orquesta toda la actualización visual.
     * @param tema El objeto Tema que contiene todos los colores a aplicar.
     */
    public void applyTheme(Tema tema) {
        if (tema == null) {
            System.err.println("ThemeApplier: Se intentó aplicar un tema nulo. Operación cancelada.");
            return;
        }

        System.out.println(">>> Aplicando tema: " + tema.nombreDisplay() + " <<<");

        // Paso 1: Notificar a Swing que debe actualizar sus componentes por defecto.
        // Esto es crucial para que los JComboBox, JScrollBar, etc., se actualicen.
        // Es mejor llamar a esto en el frame principal.
        JFrame mainFrame = registry.get("frame.main");
        if (mainFrame != null) {
            SwingUtilities.updateComponentTreeUI(mainFrame);
        }

        // Paso 2: Iterar sobre TODOS los componentes registrados y aplicar colores personalizados.
//        for (Component component : registry.getAllComponents()) {
//            if (component instanceof JComponent) {
//                applyColorToComponent((JComponent) component, tema);
//            }
//        }
//        
        for (JComponent component : registry.getAllJComponents()) {
            applyColorToComponent(component, tema);
        }
        
        // Paso 3: Aplicar colores a componentes específicos que necesitan un trato especial.
        // Aquí usamos los nombres canónicos que definimos en el registro.
        applySpecializedColors(tema);

        System.out.println(">>> Aplicación del tema completada. <<<");
    }

    /**
     * Aplica colores a un componente individual basándose en su tipo.
     * @param component El componente a colorear.
     * @param tema El tema a aplicar.
     */
    private void applyColorToComponent(JComponent component, Tema tema) {
        // Colores generales
        component.setForeground(tema.colorTextoPrimario());

        if (component instanceof JPanel || component instanceof JToolBar) {
            component.setBackground(tema.colorFondoPrincipal());
        } else if (component instanceof JButton) {
            component.setBackground(tema.colorBotonFondo());
            component.setForeground(tema.colorBotonTexto());
        } else if (component instanceof JList) {
            component.setBackground(tema.colorFondoSecundario());
            component.setForeground(tema.colorTextoPrimario());
            ((JList<?>) component).setSelectionBackground(tema.colorSeleccionFondo());
            ((JList<?>) component).setSelectionForeground(tema.colorSeleccionTexto());
        } else if (component instanceof JScrollPane) {
            component.setBackground(tema.colorFondoPrincipal());
            ((JScrollPane) component).getViewport().setBackground(tema.colorFondoPrincipal());
            component.setBorder(UIManager.getBorder("ScrollPane.border")); // Reset al borde del L&F
        } else if (component instanceof JCheckBoxMenuItem || component instanceof JRadioButtonMenuItem) {
             component.setBackground(tema.colorFondoSecundario());
             component.setForeground(tema.colorTextoPrimario());
        }
        
        // ... aquí se pueden añadir más reglas generales para otros tipos de componentes.
    }

    /**
     * Aplica colores a componentes específicos identificados por su nombre en el registro.
     * Esto es para casos donde el color depende del ROL del componente, no solo de su TIPO.
     * @param tema El tema a aplicar.
     */
    private void applySpecializedColors(Tema tema) {
        // Frame principal
    	JFrame mainFrame = registry.get("frame.main");
        if (mainFrame != null) {
            mainFrame.getContentPane().setBackground(tema.colorFondoPrincipal());
        }

        // Paneles con fondo secundario
        JPanel panelInfoSuperior = registry.get("panel.info.superior");
        if (panelInfoSuperior != null) panelInfoSuperior.setBackground(tema.colorFondoSecundario());
        
        JPanel bottomStatusBar = registry.get("panel.estado.inferior");
        if (bottomStatusBar != null) bottomStatusBar.setBackground(tema.colorFondoSecundario());

        // Labels con texto secundario
        JLabel modoZoomLabel = registry.get("label.info.modoZoom");
        if (modoZoomLabel != null) modoZoomLabel.setForeground(tema.colorTextoSecundario());
        // ... aplicar a otros JLabels de la barra de info que usen color secundario

        // Paneles con bordes titulados
        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
        if (panelIzquierdo != null && panelIzquierdo.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panelIzquierdo.getBorder();
            border.setTitleColor(tema.colorBordeTitulo());
            panelIzquierdo.repaint();
        }
        
        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
        if (scrollMiniaturas != null && scrollMiniaturas.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) scrollMiniaturas.getBorder();
            border.setTitleColor(tema.colorBordeTitulo());
            scrollMiniaturas.repaint();
        }
    }
    
} // --- FIN de la clase ThemeApplier ---