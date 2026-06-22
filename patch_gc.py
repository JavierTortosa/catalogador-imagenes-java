import re

with open(r"D:\Programacion\Eclipse\Workspace 2024-12R\VisorImagenes\src\controlador\GeneralController.java", "r", encoding="utf-8") as f:
    content = f.read()

# Add field and setter
content = content.replace("private DataController dataController;", "private DataController dataController;\n    private ClientController clientController;")
content = content.replace("public void setDataController(DataController dataController) {", 
"""public void setClientController(ClientController clientController) {
        this.clientController = java.util.Objects.requireNonNull(clientController, "ClientController no puede ser null");
    }

    public ClientController getClientController() {
        return clientController;
    }

    public void setDataController(DataController dataController) {""")

# Update IModoController delegation
# find: 
# } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
#     dataController.XXXX;
# } else {
# replace with:
# } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.DATOS) {
#     dataController.XXXX;
# } else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CLIENTE) {
#     if (clientController != null) clientController.XXXX;
# } else {

pattern = r"(\} else if \(model\.getCurrentWorkMode\(\) == VisorModel\.WorkMode\.DATOS\) \{\s+dataController\.([^;]+;)\s+)(\} else \{)"
replacement = r"\1} else if (model.getCurrentWorkMode() == VisorModel.WorkMode.CLIENTE) {\n            if (clientController != null) clientController.\2\n        \3"

content = re.sub(pattern, replacement, content)

with open(r"D:\Programacion\Eclipse\Workspace 2024-12R\VisorImagenes\src\controlador\GeneralController.java", "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
