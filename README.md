# IntimoCoffee App 🍫☕

Una aplicación moderna de **Point of Sale (POS)** desarrollada en Android con **Jetpack Compose** y **Material Design 3**, inspirada en el análisis de BISTRO POS.

## 🎯 Características Principales

- **Sistema de Autenticación Local**: Login seguro con usuarios predefinidos
- **Interfaz Monocromática**: Paleta elegante en negro/gris con acentos
- **Arquitectura Limpia**: Clean Architecture + MVVM + Hilt
- **Base de Datos Local**: Room Database para almacenamiento offline
- **Material Design 3**: UI moderna y accesible
- **Modo Landscape**: Optimizado para tablets y uso POS

## 🏗️ Arquitectura

```
app/
├── core/
│   ├── database/         # Room entities, DAOs, Database
│   ├── di/              # Hilt dependency injection
│   └── navigation/      # Navigation destinations
├── feature/
│   ├── auth/            # Autenticación (login/logout)
│   ├── products/        # Gestión de productos
│   ├── orders/          # Sistema de órdenes
│   ├── tables/          # Gestión de mesas
│   └── reports/         # Reportes y analytics
└── ui/theme/            # Material Design 3 theming
```

## 🛠️ Stack Tecnológico

### Frontend
- **Kotlin 100%**
- **Jetpack Compose** - UI moderna y declarativa
- **Material Design 3** - Sistema de diseño
- **Hilt** - Dependency Injection
- **Room** - Base de datos local
- **Navigation Compose** - Navegación
- **ViewModel & StateFlow** - Gestión de estado
- **Coroutines** - Programación asíncrona

### Patrones
- **Clean Architecture**
- **MVVM (Model-View-ViewModel)**
- **Repository Pattern**
- **Use Case Pattern**

## 🚀 Configuración e Instalación

### Prerequisitos
- **Android Studio Hedgehog** (2023.1.1) o superior
- **JDK 17** o superior
- **Android SDK 34**
- **Gradle 8.0** o superior

### Instalación

1. **Clonar el proyecto**:
   ```bash
   git clone <repository-url>
   cd IntimoCoffeeApp
   ```

2. **Abrir en Android Studio**:
   - Abrir Android Studio
   - File > Open > Seleccionar la carpeta del proyecto
   - Esperar a que Gradle sincronice

3. **Ejecutar la aplicación**:
   - Conectar un dispositivo Android o iniciar un emulador
   - Hacer clic en el botón "Run" (▶️)

## 👤 Usuarios Predefinidos

La aplicación viene con usuarios de prueba preconfigurados:

| Usuario    | Contraseña    | Rol            |
|------------|---------------|----------------|
| `admin`    | `admin123`    | Administrador  |
| `manager`  | `manager123`  | Gerente        |
| `empleado` | `empleado123` | Empleado       |

## 🎨 Sistema de Colores

### Paleta Monocromática
```kotlin
// Primarios
BlackPrimary   = #000000
GrayDark      = #1A1A1A  
GrayMedium    = #333333
GrayLight     = #666666
GrayLighter   = #999999
GrayLightest  = #CCCCCC
WhiteOff      = #F5F5F5
WhitePure     = #FFFFFF

// Acentos (Coffee themed)
CoffeeAccent  = #8D6E63
CoffeeLight   = #D7CCC8
```

## 📱 Funcionalidades Implementadas

### ✅ Versión Actual (v1.0)
- [x] Sistema de autenticación local
- [x] Base de datos Room configurada
- [x] Interfaz principal con navegación
- [x] Theme monocromático personalizado
- [x] Arquitectura base implementada

### 🔄 En Desarrollo
- [ ] CRUD de Productos
- [ ] Gestión de Órdenes
- [ ] Sistema de Mesas
- [ ] Carrito de Compras
- [ ] Múltiples métodos de pago
- [ ] Reportes básicos

### 🎯 Próximas Funcionalidades
- [ ] Integración con impresoras térmicas
- [ ] Scanner de código de barras  
- [ ] Sincronización cloud
- [ ] Analytics avanzados
- [ ] Multi-restaurant support

## 📂 Estructura de Base de Datos

### Entidades Principales
- **UserEntity**: Usuarios del sistema
- **CategoryEntity**: Categorías de productos
- **ProductEntity**: Productos del inventario
- **OrderEntity**: Órdenes de venta
- **OrderItemEntity**: Items individuales de órdenes
- **TableEntity**: Mesas del restaurante

## 🔧 Configuración de Desarrollo

### Build Variants
- **Debug**: Para desarrollo local
- **Release**: Para producción (configurar signing)

### Proguard
El proyecto incluye reglas de ProGuard para:
- Hilt/Dagger2
- Room Database
- Serialization

## 🧪 Testing

### Estructura de Tests
```
src/
├── test/           # Unit tests
├── androidTest/    # Integration tests
└── main/
```

### Ejecutar Tests
```bash
# Unit Tests
./gradlew test

# Android Tests (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
```

## 📄 Licencia

Este proyecto está desarrollado para fines educativos y de demostración.

## 🤝 Contribuir

1. Fork del proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit de cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Crear Pull Request

## 📞 Soporte

Para preguntas o reportar issues:
- Crear un issue en GitHub
- Contactar al equipo de desarrollo

---

**IntimoCoffee App** - Sistema POS moderno para cafeterías y restaurantes 🍫☕