package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import controlador.managers.filter.FilterCriterion;

/**
 * Diálogo modal para crear o editar un FilterCriterion.
 */
public class FilterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    // Componentes de la UI
    private javax.swing.JToggleButton btnTypeTexto, btnTypeCarpeta, btnTypeTag;
    private JTextField txtValor;
    private JButton btnBrowse;
    private JRadioButton rbLogicAdd, rbLogicNot;
    
    // Estado interno
    private FilterCriterion resultCriterion = null;

    public FilterDialog(JFrame owner, Map<FilterCriterion.SourceType, Icon> typeIcons) {
        super(owner, "Añadir Nuevo Filtro", true); // Título y modalidad
        
        // --- Panel Principal con GridBagLayout para control preciso ---
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Fila 1: TIPO de Filtro ---
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.setBorder(new TitledBorder("Tipo de Filtro"));
        ButtonGroup typeGroup = new ButtonGroup();
        
        btnTypeTexto = new javax.swing.JToggleButton("Texto", typeIcons.get(FilterCriterion.SourceType.TEXT), true);
        btnTypeTexto.setToolTipText("Filtrar por nombre de archivo");
        
        btnTypeCarpeta = new javax.swing.JToggleButton("Carpeta", typeIcons.get(FilterCriterion.SourceType.FOLDER));
        btnTypeCarpeta.setToolTipText("Filtrar por ruta de carpeta");
        
        btnTypeTag = new javax.swing.JToggleButton("Etiqueta", typeIcons.get(FilterCriterion.SourceType.TAG));
        btnTypeTag.setToolTipText("Filtrar por etiqueta (no implementado)");
        btnTypeTag.setEnabled(false);

        typeGroup.add(btnTypeTexto);
        typeGroup.add(btnTypeCarpeta);
        typeGroup.add(btnTypeTag);
        typePanel.add(btnTypeTexto);
        typePanel.add(btnTypeCarpeta);
        typePanel.add(btnTypeTag);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(typePanel, gbc);

        // --- Fila 2: VALOR del Filtro ---
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Valor:"), gbc);

        txtValor = new JTextField(30);
        gbc.gridx = 1;
        mainPanel.add(txtValor, gbc);
        
        btnBrowse = new JButton("...");
        btnBrowse.setVisible(false); // Inicialmente oculto
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(btnBrowse, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; // Restaurar fill

        // --- Fila 3: LÓGICA del Filtro ---
        JPanel logicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logicPanel.setBorder(new TitledBorder("Lógica"));
        ButtonGroup logicGroup = new ButtonGroup();

        rbLogicAdd = new JRadioButton("Incluir (contiene)", true);
        rbLogicNot = new JRadioButton("Excluir (no contiene)");

        logicGroup.add(rbLogicAdd);
        logicGroup.add(rbLogicNot);
        logicPanel.add(rbLogicAdd);
        logicPanel.add(rbLogicNot);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        mainPanel.add(logicPanel, gbc);

        // --- Panel de Botones (Aceptar/Cancelar) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAceptar = new JButton("Aceptar");
        JButton btnCancelar = new JButton("Cancelar");
        buttonPanel.add(btnAceptar);
        buttonPanel.add(btnCancelar);

        // --- Añadir paneles al diálogo ---
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // --- LÓGICA DE EVENTOS ---

        // Mostrar/ocultar el botón "..." si se selecciona tipo Carpeta
        ActionListener typeListener = e -> btnBrowse.setVisible(btnTypeCarpeta.isSelected());
        btnTypeTexto.addActionListener(typeListener);
        btnTypeCarpeta.addActionListener(typeListener);
        btnTypeTag.addActionListener(typeListener);

        // Acción del botón "..." (JFileChooser)
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtValor.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Acción del botón Aceptar
        btnAceptar.addActionListener(e -> {
            String valor = txtValor.getText();

            // --- INICIO DE LA VALIDACIÓN ---

            // Regla 1: El campo de valor no puede estar vacío.
            if (valor == null || valor.isBlank()) {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "El campo 'Valor' no puede estar vacío.", 
                    "Entrada Inválida", 
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return; // Detiene la ejecución y NO cierra el diálogo.
            }

            // Regla 2: Si el tipo es "Carpeta", el valor debe ser una carpeta válida.
            if (btnTypeCarpeta.isSelected()) {
                try {
                    Path path = Paths.get(valor);
                    if (!Files.isDirectory(path)) { // Comprueba si existe Y es un directorio
                        javax.swing.JOptionPane.showMessageDialog(
                            this, 
                            "La ruta especificada no es una carpeta válida.", 
                            "Ruta Inválida", 
                            javax.swing.JOptionPane.WARNING_MESSAGE
                        );
                        return; // Detiene la ejecución y NO cierra el diálogo.
                    }
                } catch (java.nio.file.InvalidPathException ipe) {
                    // Captura de error si el texto ni siquiera es una ruta válida (ej. "batman")
                    javax.swing.JOptionPane.showMessageDialog(
                        this, 
                        "El texto introducido no tiene un formato de ruta válido.", 
                        "Ruta Inválida", 
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            }
            
            // --- FIN DE LA VALIDACIÓN ---

            // Si todas las validaciones pasan, procedemos a crear el criterio.
            FilterCriterion.SourceType type;
            if (btnTypeCarpeta.isSelected()) {
                type = FilterCriterion.SourceType.FOLDER;
            } else if (btnTypeTag.isSelected()) {
                type = FilterCriterion.SourceType.TAG;
            } else {
                type = FilterCriterion.SourceType.TEXT;
            }

            FilterCriterion.Logic logic = rbLogicAdd.isSelected() ? FilterCriterion.Logic.ADD : FilterCriterion.Logic.NOT;
            
            resultCriterion = new FilterCriterion();
            resultCriterion.setValue(valor);
            resultCriterion.setSourceType(type);
            resultCriterion.setLogic(logic);
            
            dispose(); // Cierra el diálogo
        });

        // Acción del botón Cancelar
        btnCancelar.addActionListener(e -> {
            resultCriterion = null;
            dispose(); // Cierra el diálogo
        });

        pack(); // Ajusta el tamaño del diálogo a sus componentes
        setLocationRelativeTo(owner); // Centra el diálogo
    } // ---FIN de metodo FilterDialog---

    /**
     * Muestra el diálogo y devuelve el FilterCriterion creado.
     * @return El FilterCriterion si el usuario pulsó Aceptar, o null si canceló.
     */
    public FilterCriterion showDialog() {
        setVisible(true);
        return resultCriterion;
    } // ---FIN de metodo showDialog---

} // --- FIN de la clase FilterDialog ---