package vista.config;

public enum MenuItemType {
    MAIN_MENU,
    SUB_MENU,
    ITEM,           // JMenuItem estándar
    CHECKBOX_ITEM,  // JCheckBoxMenuItem
    RADIO_BUTTON_ITEM, // JRadioButtonMenuItem
    SEPARATOR,
    RADIO_GROUP_START, // Marcador lógico, no es un componente Swing
    RADIO_GROUP_END    // Marcador lógico
}
