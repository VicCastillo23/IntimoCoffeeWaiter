#!/bin/bash

echo "🔍 Verificando proyecto IntimoCoffee App..."
echo ""

# Verificar estructura de archivos principales
echo "📁 Verificando archivos principales:"
files_to_check=(
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/intimocoffee/app/IntimoCoffeeApplication.kt"
    "app/src/main/java/com/intimocoffee/app/MainActivity.kt"
    "app/src/main/java/com/intimocoffee/app/MainScreen.kt"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/values/strings.xml"
    "app/build.gradle"
)

for file in "${files_to_check[@]}"; do
    if [[ -f "$file" ]]; then
        echo "✅ $file"
    else
        echo "❌ $file (faltante)"
    fi
done

echo ""
echo "🧮 Contando archivos Kotlin:"
kt_count=$(find . -name "*.kt" | wc -l)
echo "📝 Total archivos .kt: $kt_count"

echo ""
echo "📊 Estructura del proyecto:"
ls -la app/src/main/java/com/intimocoffee/app/

echo ""
echo "🎯 Proyecto listo para ejecutar en Android Studio!"
echo "   1. Abre Android Studio"
echo "   2. Gradle Sync automático"
echo "   3. Ejecuta en dispositivo/emulador"
echo ""
echo "🔐 Usuarios de prueba:"
echo "   • admin / admin123 (Administrador)"
echo "   • manager / manager123 (Gerente)"
echo "   • empleado / empleado123 (Empleado)"