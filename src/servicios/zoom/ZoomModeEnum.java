package servicios.zoom;

import controlador.commands.AppActionCommands;

public enum ZoomModeEnum {
//	MANUAL ("Modo Manual", AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE),
    FIT_TO_SCREEN("Ajustar a Pantalla", AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR),
    DISPLAY_ORIGINAL("Tamaño Real (100%)", AppActionCommands.CMD_ZOOM_TIPO_AUTO),
    FIT_TO_HEIGHT("Ajustar a Alto", AppActionCommands.CMD_ZOOM_TIPO_ALTO),
    FIT_TO_WIDTH("Ajustar a Ancho", AppActionCommands.CMD_ZOOM_TIPO_ANCHO),
    FILL("Rellenar Pantalla", AppActionCommands.CMD_ZOOM_TIPO_RELLENAR),
    MAINTAIN_CURRENT_ZOOM("Mantener Zoom Actual", AppActionCommands.CMD_ZOOM_TIPO_FIJO),
    USER_SPECIFIED_PERCENTAGE("Porcentaje Personalizado", AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO),
	SMART_FIT("Ajuste Inteligente", AppActionCommands.CMD_ZOOM_TIPO_SMART_FIT);
	
    private final String nombreLegible;
    private final String actionCommand;

    ZoomModeEnum(String nombreLegible, String actionCommand) {
        this.nombreLegible = nombreLegible;
        this.actionCommand = actionCommand;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }
    
    public String getAssociatedActionCommand() {
        return actionCommand;
    }

    @Override 
    public String toString() { 
        return nombreLegible; 
    }
    
    
    
    
}
