package vista.builders;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import modelo.ayuda.HelpTopic;
import vista.config.MenuItemDefinition;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

public class HelpBuilder {

    // El builder ahora no tiene estado; genera el HTML cuando se le pide.
    public HelpBuilder() {
    } // --- Fin del método HelpBuilder (constructor) ---

    /**
     * Genera el contenido HTML para la ayuda de las Barras de Herramientas (Toolbars).
     */
    public String generateToolbarsHelpHtml() {
        UIDefinitionService uiDefService = new UIDefinitionService();
        Map<String, HelpTopic> topics = parseToolbarDefinitions(uiDefService);
        return buildHtmlForTopics("Referencia de Barras de Herramientas", 
            "Esta sección detalla los botones disponibles en las diferentes barras de herramientas.", topics);
    } // --- Fin del método generateToolbarsHelpHtml ---

    /**
     * Genera el contenido HTML para la ayuda de la Barra de Menús.
     */
    public String generateMenusHelpHtml() {
        UIDefinitionService uiDefService = new UIDefinitionService();
        Map<String, HelpTopic> topics = parseMenuDefinitions(uiDefService);
        return buildHtmlForTopics("Referencia de la Barra de Menús", 
            "Esta sección detalla las opciones disponibles en la barra de menús superior.", topics);
    } // --- Fin del método generateMenusHelpHtml ---

    /**
     * Método genérico para construir el HTML a partir de una lista de tópicos.
     */
    private String buildHtmlForTopics(String title, String intro, Map<String, HelpTopic> topics) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><link rel='stylesheet' href='styles.css'>");
        html.append("<style>");
        html.append(".icon-table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        html.append(".icon-table td { padding: 8px; border-bottom: 1px solid #B0BEC5; vertical-align: middle; }");
        html.append(".icon-table td:first-child { width: 40px; text-align: center; }");
        html.append(".icon-table img { width: 24px; height: 24px; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<h1>").append(title).append("</h1>");
        html.append("<p>").append(intro).append("</p>");

        Map<String, List<HelpTopic>> groupedByCategory = topics.values().stream()
                .collect(Collectors.groupingBy(HelpTopic::category));

        for (Map.Entry<String, List<HelpTopic>> entry : groupedByCategory.entrySet()) {
            buildCategorySection(html, entry.getKey(), entry.getValue());
        }

        html.append("</body></html>");
        return html.toString();
    } // --- Fin del método buildHtmlForTopics ---
    
    private void buildCategorySection(StringBuilder html, String category, List<HelpTopic> topicList) {
        html.append("<h2>").append(category.replace("->", "&rarr;")).append("</h2>");
        html.append("<table class='icon-table'>");
        for (HelpTopic topic : topicList) {
            html.append("<tr>");
            if (topic.iconName() != null && !topic.iconName().isBlank()) {
                html.append("<td><img src='ICON_PLACEHOLDER_").append(topic.iconName()).append("'></td>");
            } else {
                html.append("<td></td>");
            }
            html.append("<td><b>").append(topic.description()).append("</b></td>");
            html.append("</tr>");
        }
        html.append("</table>");
    } // --- Fin del método buildCategorySection ---

    private Map<String, HelpTopic> parseToolbarDefinitions(UIDefinitionService service) {
        Map<String, HelpTopic> tempTopics = new java.util.LinkedHashMap<>();
        for (ToolbarDefinition toolbarDef : service.generateModularToolbarStructure()) {
            for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                if (compDef instanceof ToolbarButtonDefinition buttonDef && isCommandRelevant(buttonDef.comandoCanonico())) {
                    HelpTopic topic = new HelpTopic(
                        buttonDef.comandoCanonico(), buttonDef.textoTooltip(), buttonDef.claveIcono(), toolbarDef.titulo()
                    );
                    tempTopics.put(topic.command(), topic);
                }
            }
        }
        return tempTopics;
    } // --- Fin del método parseToolbarDefinitions ---

    private Map<String, HelpTopic> parseMenuDefinitions(UIDefinitionService service) {
        Map<String, HelpTopic> tempTopics = new java.util.LinkedHashMap<>();
        parseMenuItemsRecursive(service.generateMenuStructure(), "", tempTopics);
        return tempTopics;
    } // --- Fin del método parseMenuDefinitions ---
    
    private void parseMenuItemsRecursive(List<MenuItemDefinition> items, String category, Map<String, HelpTopic> tempTopics) {
        for (MenuItemDefinition itemDef : items) {
            String currentMenuName = (itemDef.textoMostrado() != null) ? itemDef.textoMostrado() : "";
            
            if (itemDef.actionCommand() != null && isCommandRelevant(itemDef.actionCommand())) {
                tempTopics.computeIfAbsent(itemDef.actionCommand(), cmd -> 
                    new HelpTopic(cmd, currentMenuName, null, category)
                );
            }
            if (itemDef.subItems() != null && !itemDef.subItems().isEmpty()) {
                parseMenuItemsRecursive(itemDef.subItems(), category.isEmpty() ? currentMenuName : category + " -> " + currentMenuName, tempTopics);
            }
        }
    } // --- Fin del método parseMenuItemsRecursive ---
    
    private boolean isCommandRelevant(String command) {
        return command != null && !command.isBlank() && !command.equals("cmd.todo.funcionalidad_pendiente");
    } // --- Fin del método isCommandRelevant ---
    
} // --- FIN de la clase HelpBuilder ---