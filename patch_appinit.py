import re

with open(r"D:\Programacion\Eclipse\Workspace 2024-12R\VisorImagenes\src\controlador\AppInitializer.java", "r", encoding="utf-8") as f:
    content = f.read()

# Import the new classes
imports = """import controlador.ProjectController;
import controlador.ClientController;
import servicios.ValidationService;
import servicios.cliente.ClientSyncService;"""
content = content.replace("import controlador.ProjectController;", imports)

# Declare variables
vars_decl = """private ProjectController projectController;
    private ClientController clientController;
    private ValidationService validationService;
    private ClientSyncService clientSyncService;"""
content = content.replace("private ProjectController projectController;", vars_decl)

# Instantiate variables
instantiation = """this.projectController = new ProjectController(); // Crear la ÚNICA instancia aquí
        this.clientController = new ClientController();
        this.validationService = new ValidationService();
        this.clientSyncService = new ClientSyncService();
        
        this.clientController.setGeneralController(this.generalController);
        this.clientController.setValidationService(this.validationService);
        this.clientController.setClientSyncService(this.clientSyncService);
        """
content = content.replace("this.projectController = new ProjectController(); // Crear la ÚNICA instancia aquí", instantiation)

# Inject into GeneralController
injection = """this.generalController.setProjectController(this.projectController);
        this.generalController.setClientController(this.clientController);"""
content = content.replace("this.generalController.setProjectController(this.projectController);", injection)

# Inject into ActionFactory
af_injection = """this.actionFactory.setClientController(this.clientController);
                this.actionFactory.initializeLateActions();"""
content = content.replace("this.actionFactory.initializeLateActions();", af_injection)

# Set ProjectManager in ClientController
pm_injection = """this.projectController.setProjectManager(this.projectManagerService);
        this.clientController.setProjectManager(this.projectManagerService);"""
content = content.replace("this.projectController.setProjectManager(this.projectManagerService);", pm_injection)


with open(r"D:\Programacion\Eclipse\Workspace 2024-12R\VisorImagenes\src\controlador\AppInitializer.java", "w", encoding="utf-8") as f:
    f.write(content)

print("Done AppInitializer")
