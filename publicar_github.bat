@echo off
title StreamFlow Player - Publicar no GitHub
echo ============================================
echo  StreamFlow Player - Publicar no GitHub
echo ============================================
echo.
echo Este script ajuda a publicar o codigo fonte
echo do StreamFlow Player no GitHub para compilar
echo o APK automaticamente.
echo.

where gh >nul 2>nul
if %ERRORLEVEL% NEQ 0 goto manual

echo [OK] GitHub CLI (gh) encontrado
echo.
echo Deseja fazer login agora? (s/n)
set /p resp=
if /i "%resp%"=="s" (
    gh auth login
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo Criando repositorio...
        gh repo create streamflow-player --public --source . --remote origin --push
        echo.
        echo Repositorio criado!
        echo Va em https://github.com/settings/repos e encontre streamflow-player
        echo Depois va na aba Actions e execute o workflow.
        pause
        exit /b 0
    )
)

:manual
echo.
echo ============= GUIA MANUAL =============
echo.
echo Seu codigo JA ESTA no GitHub em:
echo https://github.com/fjreidamoeda/streamflow-player
echo.
echo Va em:
echo https://github.com/fjreidamoeda/streamflow-player/actions
echo.
echo Clique em "Build StreamFlow Player APK"
echo Depois em "Run workflow" para compilar o APK.
echo.
echo ============================================
pause
