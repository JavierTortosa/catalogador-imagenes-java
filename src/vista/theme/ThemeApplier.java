// Contenido de ThemeApplier.java
package vista.theme;

import java.awt.Color;
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
    } // ---FIN de metodo [Constructor ThemeApplier]---

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

        
        for (JComponent component : registry.getAllJComponents()) {
            applyColorToComponent(component, tema);
        }
        
        // Paso 3: Aplicar colores a componentes específicos que necesitan un trato especial.
        // Aquí usamos los nombres canónicos que definimos en el registro.
        applySpecializedColors(tema);

        System.out.println(">>> Aplicación del tema completada. <<<");
    } // ---FIN de metodo [applyTheme]---

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
    } // ---FIN de metodo [applyColorToComponent]---

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
        
        // Paneles de las barras de estado
        Color statusBarBackgroundColor = tema.colorBarraEstadoFondo();
        Color statusBarForegroundColor = tema.colorBarraEstadoTexto();

        JPanel panelInfoSuperior = registry.get("panel.info.superior");
        if (panelInfoSuperior != null) {
            panelInfoSuperior.setBackground(statusBarBackgroundColor);
            // --- INICIO DE LA CORRECCIÓN ---
            // Aplicar el color de texto a todos los JLabels hijos
            for (Component child : panelInfoSuperior.getComponents()) {
                if (child instanceof JLabel) {
                    child.setForeground(statusBarForegroundColor);
                }
            }
            // --- FIN DE LA CORRECCIÓN ---
        }
        
        JPanel bottomStatusBar = registry.get("panel.estado.inferior");
        if (bottomStatusBar != null) {
            bottomStatusBar.setBackground(statusBarBackgroundColor);
            // --- INICIO DE LA CORRECCIÓN ---
            // Aplicar el color de texto a todos los JLabels hijos
            for (Component child : bottomStatusBar.getComponents()) {
                if (child instanceof JLabel) {
                    child.setForeground(statusBarForegroundColor);
                }
            }
             // --- FIN DE LA CORRECCIÓN ---
        }
        
        JToolBar statusBarToolbar = registry.get("toolbar.barra_estado_controles");
        if (statusBarToolbar != null) {
            statusBarToolbar.setOpaque(true);
            statusBarToolbar.setBackground(statusBarBackgroundColor);
            statusBarToolbar.setBorder(null);

            // --- INICIO DE LA CORRECCIÓN ---
            // Aplicar el color de texto a los JLabels que pueda contener
            for (Component child : statusBarToolbar.getComponents()) {
                if (child instanceof JLabel) {
                    child.setForeground(statusBarForegroundColor);
                }
            }
            // --- FIN DE LA CORRECCIÓN ---
        }

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
    } // ---FIN de metodo [applySpecializedColors]---
    
} // --- FIN de la clase ThemeApplier ---


//package vista.theme;
//
//import java.awt.Color;
//
//import javax.swing.JButton;
//import javax.swing.JCheckBoxMenuItem;
//import javax.swing.JComponent;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JPanel;
//import javax.swing.JRadioButtonMenuItem;
//import javax.swing.JScrollPane;
//import javax.swing.JToolBar;
//import javax.swing.SwingUtilities;
//import javax.swing.UIManager;
//import javax.swing.border.TitledBorder;
//
//import controlador.utils.ComponentRegistry; // ¡La dependencia clave!
//
///**
// * Clase responsable de aplicar un objeto Tema a todos los componentes
// * de la UI registrados en un ComponentRegistry.
// */
//public class ThemeApplier {
//
//    private final ComponentRegistry registry;
//
//    public ThemeApplier(ComponentRegistry registry) {
//        this.registry = registry;
//    }
//
//    /**
//     * Aplica un tema específico a la interfaz de usuario.
//     * Este es el método principal que orquesta toda la actualización visual.
//     * @param tema El objeto Tema que contiene todos los colores a aplicar.
//     */
//    public void applyTheme(Tema tema) {
//        if (tema == null) {
//            System.err.println("ThemeApplier: Se intentó aplicar un tema nulo. Operación cancelada.");
//            return;
//        }
//
//        System.out.println(">>> Aplicando tema: " + tema.nombreDisplay() + " <<<");
//
//        // Paso 1: Notificar a Swing que debe actualizar sus componentes por defecto.
//        // Esto es crucial para que los JComboBox, JScrollBar, etc., se actualicen.
//        // Es mejor llamar a esto en el frame principal.
//        JFrame mainFrame = registry.get("frame.main");
//        if (mainFrame != null) {
//            SwingUtilities.updateComponentTreeUI(mainFrame);
//        }
//
//        // Paso 2: Iterar sobre TODOS los componentes registrados y aplicar colores personalizados.
//
//        
//        for (JComponent component : registry.getAllJComponents()) {
//            applyColorToComponent(component, tema);
//        }
//        
//        // Paso 3: Aplicar colores a componentes específicos que necesitan un trato especial.
//        // Aquí usamos los nombres canónicos que definimos en el registro.
//        applySpecializedColors(tema);
//
//        System.out.println(">>> Aplicación del tema completada. <<<");
//    }
//
//    /**
//     * Aplica colores a un componente individual basándose en su tipo.
//     * @param component El componente a colorear.
//     * @param tema El tema a aplicar.
//     */
//    private void applyColorToComponent(JComponent component, Tema tema) {
//        // Colores generales
//        component.setForeground(tema.colorTextoPrimario());
//
//        if (component instanceof JPanel || component instanceof JToolBar) {
//            component.setBackground(tema.colorFondoPrincipal());
//        } else if (component instanceof JButton) {
//            component.setBackground(tema.colorBotonFondo());
//            component.setForeground(tema.colorBotonTexto());
//        } else if (component instanceof JList) {
//            component.setBackground(tema.colorFondoSecundario());
//            component.setForeground(tema.colorTextoPrimario());
//            ((JList<?>) component).setSelectionBackground(tema.colorSeleccionFondo());
//            ((JList<?>) component).setSelectionForeground(tema.colorSeleccionTexto());
//        } else if (component instanceof JScrollPane) {
//            component.setBackground(tema.colorFondoPrincipal());
//            ((JScrollPane) component).getViewport().setBackground(tema.colorFondoPrincipal());
//            component.setBorder(UIManager.getBorder("ScrollPane.border")); // Reset al borde del L&F
//        } else if (component instanceof JCheckBoxMenuItem || component instanceof JRadioButtonMenuItem) {
//             component.setBackground(tema.colorFondoSecundario());
//             component.setForeground(tema.colorTextoPrimario());
//        }
//        
//        // ... aquí se pueden añadir más reglas generales para otros tipos de componentes.
//    }
//
//    /**
//     * Aplica colores a componentes específicos identificados por su nombre en el registro.
//     * Esto es para casos donde el color depende del ROL del componente, no solo de su TIPO.
//     * @param tema El tema a aplicar.
//     */
//    private void applySpecializedColors(Tema tema) {
//        // Frame principal
//    	JFrame mainFrame = registry.get("frame.main");
//        if (mainFrame != null) {
//            mainFrame.getContentPane().setBackground(tema.colorFondoPrincipal());
//        }
//        
//        
//        // --- INICIO DE LA MODIFICACIÓN ---
//        // Paneles de las barras de estado
//        // Ahora leemos nuestra clave personalizada directamente del UIManager.
//        Color statusBarColor = UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND);
//
//        JPanel panelInfoSuperior = registry.get("panel.info.superior");
//        if (panelInfoSuperior != null) {
//            panelInfoSuperior.setBackground(statusBarColor);
//        }
//        
//        JPanel bottomStatusBar = registry.get("panel.estado.inferior");
//        if (bottomStatusBar != null) {
//            bottomStatusBar.setBackground(statusBarColor);
//        }
//        // --- FIN DE LA MODIFICACIÓN ---
//        
//        
//        // 1. Buscamos la JToolBar específica en el registro.
//        JToolBar statusBarToolbar = registry.get("toolbar.barra_estado_controles");
//
//        if (statusBarToolbar != null) {
//            // 2. Le damos la orden DIRECTA de pintar su fondo con nuestro color.
//            
//            // 3. Le confirmamos que SÍ debe pintar su fondo (es opaca).
//            statusBarToolbar.setOpaque(true);
//            statusBarToolbar.setBackground(Color.GREEN);//statusBarColor);
//
//            // 4. (Opcional pero recomendado) Le quitamos el borde para una integración limpia.
//            statusBarToolbar.setBorder(null);
//        }
//
//        // Labels con texto secundario
//        JLabel modoZoomLabel = registry.get("label.info.modoZoom");
//        if (modoZoomLabel != null) modoZoomLabel.setForeground(tema.colorTextoSecundario());
//        // ... aplicar a otros JLabels de la barra de info que usen color secundario
//
//        // Paneles con bordes titulados
//        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
//        if (panelIzquierdo != null && panelIzquierdo.getBorder() instanceof TitledBorder) {
//            TitledBorder border = (TitledBorder) panelIzquierdo.getBorder();
//            border.setTitleColor(tema.colorBordeTitulo());
//            panelIzquierdo.repaint();
//        }
//        
//        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
//        if (scrollMiniaturas != null && scrollMiniaturas.getBorder() instanceof TitledBorder) {
//            TitledBorder border = (TitledBorder) scrollMiniaturas.getBorder();
//            border.setTitleColor(tema.colorBordeTitulo());
//            scrollMiniaturas.repaint();
//        }
//    }
//    
//} // --- FIN de la clase ThemeApplier ---