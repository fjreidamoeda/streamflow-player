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
if %ERRORLEVEL% EQU 0 (
    echo [OK] GitHub CLI (gh) encontrado
    echo.
    echo Deseja fazer login agora? (s/n)
    set /p resp=
    if /i "!resp!"=="s" (
        gh auth login
        if %ERRORLEVEL% EQU 0 (
            echo.
            echo Criando repositorio...
            gh repo create streamflow-player --public --source ..\player_app --remote origin --push
            echo.
            echo Repositorio criado! Acesse:
            echo https://github.com/%USERNAME%/streamflow-player/actions
            echo.
            echo Va em Actions, clique no workflow "Build StreamFlow Player APK"
            echo e depois em "Run workflow" para compilar o APK.
            pause
            exit /b 0
        )
    )
)

echo.
echo ============= GUIA MANUAL =============
echo.
echo 1. Acesse https://github.com/login e crie/conecte sua conta
echo.
echo 2. Clique no botao "+" no canto superior direito,
echo    depois em "New repository"
echo.
echo 3. Nome: streamflow-player
echo    Deixar PUBLICO
echo    Nao marcar nada (README, .gitignore, license)
echo    Clique em "Create repository"
echo.
echo 4. Faca upload da pasta player_app:
echo    - No repositorio, clique em "Add file" > "Upload files"
echo    - Arraste a pasta player_app inteira
echo    - Clique em "Commit changes"
echo.
echo 5. Va na aba "Actions" do repositorio
echo    - Clique no workflow "Build StreamFlow Player APK"
echo    - Clique em "Run workflow" (botao verde)
echo    - Aguarde 2-3 minutos
echo.
echo 6. Quando terminar, clique no workflow concluido,
echo    desca ate "Artifacts" e baixe o APK.
echo.
echo ============================================
pause
