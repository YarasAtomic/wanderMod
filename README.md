# Wander Mod

Un mod de Minecraft que se comunica con una aplicacion Java por un socket para simular un agente en un mundo 2D

Solo funciona en la version 1.20.2 de Minecraft

## Ejecutar desde un IDE

Para desarrolladores
Clonar el repositorio y ejecutar la tarea de Gradle runClient usando la extension de Gradle para abrir el juego.
Si se usa vscode, esta tarea puede no aparecer y se tiene que reiniciar el editor.

## Instalación

Para usuarios finales
Se requiere el mod loader Fabric, se requiere Fabric API
https://fabricmc.net/

Instalar el loader

Dentro de la carpeta de instalación de Minecraft (.minecraft) colocar el jar del mod y de la API

## Probar

Ejecutar la versión del juego con el loader instalado.

Crear un mundo nuevo o acceder a un mundo, asegurarse de activar los trucos al crear el mundo.

Usar el comando para obtener el controlador del agente

    /give @s wandermod:agent_player

Colocar el bloque del controlador y hacer click derecho sobre él

Si la aplicación del agente se está ejecutando, se creará en el juego un mapa y se empezará a ejecutar el agente




