@echo off
set DIR=%~dp0target
java --module-path "%DIR%\lib;%DIR%\VisorV2.jar" -m VisorImagenes/principal.VisorV2 %*
