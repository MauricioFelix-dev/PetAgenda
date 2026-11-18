@echo off
REM PetAgenda - roda os testes automaticos CT-01 a CT-19.
REM Antes, deixe o sistema ligado em outro terminal: mvnw.cmd spring-boot:run
REM Uso: testes\rodar-testes.bat   (a partir da pasta do projeto)
chcp 65001 > nul
cd /d "%~dp0.."
java testes\TestesCT.java %*
echo.
pause
