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
import modelo.datos.Tag;
import vista.components.TagIntelliSenseField;

/**
 * Diálogo modal para crear o editar un FilterCriterion.
 */
public class FilterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    // Componentes de la UI
    private javax.swing.JToggleButton btnTypeTexto, btnTypeCarpeta, btnTypeTag;
    private JPanel txtValorPanel;
    private JTextField txtValor;
    private TagIntelliSenseField tagField;
    private javax.swing.JCheckBox chkRutaCompleta;
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
//        btnTypeTag.setEnabled(false);

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

        txtValorPanel = new JPanel(new java.awt.BorderLayout());
        txtValor = new JTextField(30);
        tagField = new TagIntelliSenseField();
        tagField.setColumns(30);
        // Inicialmente mostrar el JTextField plano (modo TEXTO)
        txtValorPanel.add(txtValor, java.awt.BorderLayout.CENTER);
        gbc.gridx = 1;
        mainPanel.add(txtValorPanel, gbc);
        
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

        // --- Fila 4: Checkbox de ruta completa (solo visible en modo etiqueta) ---
        chkRutaCompleta = new javax.swing.JCheckBox("Incluir ruta completa (todos los ancestros)", false);
        chkRutaCompleta.setToolTipText("Añade un filtro por cada tag de la ruta, no solo el último.");
        chkRutaCompleta.setVisible(false);
        gbc.gridy = 3;
        mainPanel.add(chkRutaCompleta, gbc);

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

        // Mostrar/ocultar el botón "..." si se selecciona tipo Carpeta o Etiqueta,
        // y cambiar el campo de valor segun el tipo
        ActionListener typeListener = e -> {
            btnBrowse.setVisible(btnTypeCarpeta.isSelected() || btnTypeTag.isSelected());
            txtValorPanel.removeAll();
            if (btnTypeTag.isSelected()) {
                tagField.refreshTags(new servicios.db.TagDAO().getAllTags());
                txtValorPanel.add(tagField, java.awt.BorderLayout.CENTER);
            } else {
                txtValorPanel.add(txtValor, java.awt.BorderLayout.CENTER);
            }
            txtValorPanel.revalidate();
            txtValorPanel.repaint();
            chkRutaCompleta.setVisible(btnTypeTag.isSelected());
        };
        btnTypeTexto.addActionListener(typeListener);
        btnTypeCarpeta.addActionListener(typeListener);
        btnTypeTag.addActionListener(typeListener);

        // Acción del botón "..." (JFileChooser o TagSelectionDialog)
        btnBrowse.addActionListener(e -> {
            if (btnTypeCarpeta.isSelected()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    txtValor.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            } else if (btnTypeTag.isSelected()) {
                vista.VisorView mainView = (vista.VisorView) getOwner();
                vista.util.IconUtils iconUtils = (mainView != null && mainView.getController() != null) ? mainView.getController().getIconUtils() : null;
                TagSelectionDialog dialog = new TagSelectionDialog(this, iconUtils);
                String selectedTag = dialog.showDialog();
                if (selectedTag != null) {
                    tagField.setText("." + selectedTag);
                }
            }
        });

        // Acción del botón Aceptar
        btnAceptar.addActionListener(e -> {
            String valor = btnTypeTag.isSelected() ? tagField.getText() : txtValor.getText();

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

            // Regla 1.5: Si el tipo es "Etiqueta", el valor debe ser un tag valido.
            if (btnTypeTag.isSelected()) {
                // Resolver la ruta a un tag final
                Tag resolved = tagField.resolveCurrentTag();
                if (resolved == null) {
                    javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "La ruta de etiqueta '" + valor + "' no es válida.\nUsa el autocompletado con '.' para navegar o selecciona con '...'.",
                        "Etiqueta Inexistente",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                if (chkRutaCompleta.isSelected()) {
                    // Incluir toda la ruta: "trofeos,blood bowl"
                    String raw = tagField.getText().trim();
                    if (raw.startsWith(".")) raw = raw.substring(1);
                    String[] segments = raw.split("\\.");
                    java.util.ArrayList<String> names = new java.util.ArrayList<>();
                    for (String seg : segments) {
                        String s = seg.trim();
                        if (!s.isEmpty()) names.add(s);
                    }
                    valor = String.join(",", names);
                } else {
                    // Solo el tag final
                    valor = resolved.getNombre();
                }
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
            FilterCriterion.SourceType sourceType;
            FilterCriterion.FilterSource source; // El enum antiguo (Nombre corregido)
            FilterCriterion.FilterType filterType; 

            if (btnTypeCarpeta.isSelected()) {
                sourceType = FilterCriterion.SourceType.FOLDER;
                source = FilterCriterion.FilterSource.FOLDER_PATH;
            } else if (btnTypeTag.isSelected()) {
                sourceType = FilterCriterion.SourceType.TAG;
                source = FilterCriterion.FilterSource.DATABASE_TAGS;
            } else {
                sourceType = FilterCriterion.SourceType.TEXT;
                source = FilterCriterion.FilterSource.FILENAME;
            }

            FilterCriterion.Logic logic = rbLogicAdd.isSelected() ? FilterCriterion.Logic.ADD : FilterCriterion.Logic.NOT;
            filterType = (logic == FilterCriterion.Logic.ADD) ? FilterCriterion.FilterType.CONTAINS : FilterCriterion.FilterType.DOES_NOT_CONTAIN;

            // Usamos el constructor original que inicializa todos los campos para compatibilidad
            resultCriterion = new FilterCriterion(valor, source, filterType);
            
            // Y luego ajustamos los campos más nuevos si es necesario (aunque el constructor ya lo hace)
            resultCriterion.setSourceType(sourceType);
            resultCriterion.setLogic(logic);
            
//            FilterCriterion.SourceType type;
//            if (btnTypeCarpeta.isSelected()) {
//                type = FilterCriterion.SourceType.FOLDER;
//            } else if (btnTypeTag.isSelected()) {
//                type = FilterCriterion.SourceType.TAG;
//            } else {
//                type = FilterCriterion.SourceType.TEXT;
//            }
//
//            FilterCriterion.Logic logic = rbLogicAdd.isSelected() ? FilterCriterion.Logic.ADD : FilterCriterion.Logic.NOT;
//            
//            resultCriterion = new FilterCriterion();
//            resultCriterion.setValue(valor);
//            resultCriterion.setSourceType(type);
//            resultCriterion.setLogic(logic);
            
            
            
            
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