@echo off
set DIR=%~dp0target
java --module-path "%DIR%lib" --class-path "%DIR%VisorV2.jar" --add-modules javafx.graphics,javafx.swing,java.sql --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED principal.VisorV2 %*