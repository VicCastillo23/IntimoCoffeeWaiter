#!/bin/bash

echo "🔍 VERIFICACIÓN COMPLETA DEL PROYECTO INTIMOCOFFEEAPP"
echo "=================================================="

PROJECT_ROOT="/Users/vicente_castillo/Desktop/IntimoCafe/IntimoCoffeeApp"
cd "$PROJECT_ROOT"

echo -e "\n1. 📁 Verificando estructura de directorios..."
echo "   ✓ Proyecto raíz: $(pwd)"

# Verificar archivos principales
echo -e "\n2. 📋 Verificando archivos principales de configuración..."

files_to_check=(
    "build.gradle"
    "settings.gradle"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "gradle.properties"
    "local.properties"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (FALTA)"
    fi
done

echo -e "\n3. 🎨 Verificando archivos de tema y UI..."
theme_files=(
    "app/src/main/java/com/intimocoffee/app/ui/theme/Color.kt"
    "app/src/main/java/com/intimocoffee/app/ui/theme/Theme.kt"
    "app/src/main/java/com/intimocoffee/app/ui/theme/Type.kt"
)

for file in "${theme_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (FALTA)"
    fi
done

echo -e "\n4. 🏗️ Verificando arquitectura principal..."
main_files=(
    "app/src/main/java/com/intimocoffee/app/IntimoCoffeeApplication.kt"
    "app/src/main/java/com/intimocoffee/app/MainActivity.kt"
    "app/src/main/java/com/intimocoffee/app/MainScreen.kt"
    "app/src/main/java/com/intimocoffee/app/MainViewModel.kt"
)

for file in "${main_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (FALTA)"
    fi
done

echo -e "\n5. 🔐 Verificando módulos de autenticación..."
auth_files=(
    "app/src/main/java/com/intimocoffee/app/feature/auth/presentation/LoginScreen.kt"
    "app/src/main/java/com/intimocoffee/app/feature/auth/presentation/LoginViewModel.kt"
    "app/src/main/java/com/intimocoffee/app/feature/auth/domain/usecase/LoginUseCase.kt"
)

for file in "${auth_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (FALTA)"
    fi
done

echo -e "\n6. 🗄️ Verificando base de datos y modelos..."
db_files=(
    "app/src/main/java/com/intimocoffee/app/core/database/IntimoCoffeeDatabase.kt"
    "app/src/main/java/com/intimocoffee/app/core/di/DatabaseModule.kt"
    "app/src/main/java/com/intimocoffee/app/core/di/DataStoreModule.kt"
)

for file in "${db_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file (FALTA)"
    fi
done

echo -e "\n7. 🧭 Verificando navegación..."
if [ -f "app/src/main/java/com/intimocoffee/app/core/navigation/Destinations.kt" ]; then
    echo "   ✅ Destinations.kt"
else
    echo "   ❌ Destinations.kt (FALTA)"
fi

echo -e "\n8. 📊 Conteo de archivos por tipo..."
kotlin_count=$(find . -name "*.kt" | wc -l | xargs)
xml_count=$(find . -name "*.xml" | wc -l | xargs)
gradle_count=$(find . -name "*.gradle*" | wc -l | xargs)

echo "   📱 Archivos Kotlin: $kotlin_count"
echo "   📄 Archivos XML: $xml_count" 
echo "   🔧 Archivos Gradle: $gradle_count"

echo -e "\n9. 🔍 Verificando configuraciones críticas..."

# Verificar namespace en build.gradle
if grep -q "namespace.*com.intimocoffee.app" app/build.gradle; then
    echo "   ✅ Namespace configurado correctamente"
else
    echo "   ⚠️  Namespace podría necesitar verificación"
fi

# Verificar compileSdk
if grep -q "compileSdk 34" app/build.gradle; then
    echo "   ✅ compileSdk configurado a 34"
else
    echo "   ⚠️  compileSdk podría necesitar verificación"
fi

# Verificar buildTools
if grep -q "buildToolsVersion.*30.0.3" app/build.gradle; then
    echo "   ✅ buildToolsVersion configurado a 30.0.3"
else
    echo "   ⚠️  buildToolsVersion podría necesitar verificación"
fi

echo -e "\n10. ✅ RESUMEN FINAL"
echo "====================="

# Contar archivos críticos
critical_files=0
total_critical=15

critical_check=(
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/intimocoffee/app/IntimoCoffeeApplication.kt"
    "app/src/main/java/com/intimocoffee/app/MainActivity.kt"
    "app/src/main/java/com/intimocoffee/app/MainScreen.kt"
    "app/src/main/java/com/intimocoffee/app/ui/theme/Color.kt"
    "app/src/main/java/com/intimocoffee/app/ui/theme/Theme.kt"
    "app/src/main/java/com/intimocoffee/app/ui/theme/Type.kt"
    "app/src/main/java/com/intimocoffee/app/feature/auth/presentation/LoginScreen.kt"
    "app/src/main/java/com/intimocoffee/app/feature/auth/presentation/LoginViewModel.kt"
    "app/src/main/java/com/intimocoffee/app/core/database/IntimoCoffeeDatabase.kt"
    "app/src/main/java/com/intimocoffee/app/core/di/DatabaseModule.kt"
    "app/src/main/java/com/intimocoffee/app/core/di/DataStoreModule.kt"
    "app/src/main/java/com/intimocoffee/app/core/navigation/Destinations.kt"
    "gradle.properties"
)

for file in "${critical_check[@]}"; do
    if [ -f "$file" ]; then
        ((critical_files++))
    fi
done

percentage=$((critical_files * 100 / total_critical))

echo "   📊 Archivos críticos presentes: $critical_files/$total_critical ($percentage%)"

if [ $percentage -ge 90 ]; then
    echo "   🎉 PROYECTO LISTO PARA COMPILACIÓN"
    echo "   ▶️  Puedes abrir el proyecto en Android Studio"
    echo "   🔧 Ejecuta: ./gradlew assembleDebug para compilar"
elif [ $percentage -ge 70 ]; then
    echo "   ⚠️  PROYECTO CASI LISTO - Revisar archivos faltantes"
else
    echo "   ❌ PROYECTO NECESITA MÁS TRABAJO - Muchos archivos críticos faltan"
fi

echo -e "\n📝 SIGUIENTES PASOS RECOMENDADOS:"
echo "   1. Abrir Android Studio"
echo "   2. File -> Open -> Seleccionar la carpeta IntimoCoffeeApp"
echo "   3. Esperar sincronización de Gradle"
echo "   4. Build -> Make Project"
echo "   5. Run -> Run 'app'"

echo -e "\n🎯 CARACTERÍSTICAS DEL PROYECTO:"
echo "   • Tema monocromático negro/gris ⚫"
echo "   • Autenticación local 🔐"
echo "   • Base de datos Room 🗄️"
echo "   • Navegación Jetpack Compose 🧭"
echo "   • Inyección de dependencias Hilt 💉"
echo "   • Orientación landscape para POS 📱"

echo -e "\n=================================================="
echo "🚀 ¡INTIMOCOFFEEAPP VERIFICACIÓN COMPLETADA!"
echo "=================================================="