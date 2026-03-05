# 🌐 Configuración de Conexión API - IntimoCoffee Waiter

## 📋 Resumen

La app IntimoCoffee Waiter ahora está configurada para **comunicarse directamente con las APIs de IntimoCoffeeApp** (servidor principal) usando HTTP/REST.

## ✅ **¿Qué está implementado?**

### 🚀 **Funcionalidades con API**
- ✅ **Crear Órdenes** → Envía directamente al servidor principal
- ✅ **Actualizar Estado de Órdenes** → Sincronización granular  
- ✅ **Obtener Productos** → Catálogo en tiempo real
- ✅ **Obtener Mesas** → Estados actualizados
- ✅ **Obtener Órdenes Activas** → Para mostrar en pantalla principal

### 🔧 **Arquitectura Implementada**
```
IntimoCoffee Waiter App
├── RemoteOrderService (API Client)
├── IntimoCoffeeApiService (Retrofit Interface)  
├── NetworkModule (Dependency Injection)
└── CreateOrderViewModel (Usa API remota)
```

## 🛠️ **Configuración de Red**

### 1️⃣ **Configurar IP del Servidor Principal**

Editar el archivo:
```
app/src/main/java/com/intimocoffee/waiter/core/network/NetworkModule.kt
```

Cambiar esta línea:
```kotlin
val baseUrl = "http://192.168.1.100:8080/" // ← Cambiar esta IP
```

Por la IP real de la tablet con IntimoCoffeeApp:
```kotlin
val baseUrl = "http://[IP_DE_LA_TABLET_PRINCIPAL]:8080/"
```

### 2️⃣ **Endpoints Implementados**

| Método | Endpoint | Funcionalidad |
|--------|----------|---------------|
| `POST` | `/api/orders` | Crear nueva orden |
| `PUT` | `/api/orders/status` | Actualizar estado de orden |
| `PUT` | `/api/orders/{orderId}/items/{itemId}/status` | Actualizar estado de producto específico |
| `GET` | `/api/orders/active` | Obtener órdenes activas |
| `GET` | `/api/products` | Obtener catálogo de productos |
| `GET` | `/api/tables` | Obtener estado de mesas |

### 3️⃣ **Formato de Datos**

#### Crear Orden
```json
POST /api/orders
{
  "tableId": 1,
  "tableName": "Mesa VIP", 
  "customerName": null,
  "items": [
    {
      "productId": 1,
      "productName": "Café Americano",
      "quantity": 2,
      "unitPrice": "2.50",
      "subtotal": "5.00", 
      "notes": "Sin azúcar",
      "categoryId": 1
    }
  ],
  "notes": null,
  "createdBy": 1
}
```

#### Actualizar Estado (Granular)
```json
PUT /api/orders/status  
{
  "orderId": 1,
  "itemId": 42,         // Para actualización granular
  "newStatus": "READY",
  "updatedBy": "WAITER",
  "timestamp": "2025-10-15T03:30:00Z"
}
```

## 🔄 **Flujo de Sincronización**

### Crear Orden desde Waiter:
1. **Mesero** crea orden en tablet waiter
2. **API** envía directamente a IntimoCoffeeApp principal
3. **Servidor principal** procesa y guarda en su base de datos
4. **Todas las apps** (Kitchen, Bar, etc.) ven la orden inmediatamente
5. **WebSocket** notifica cambios en tiempo real

### Actualizar Estado desde Waiter:
1. **Mesero** marca orden como "Entregada" 
2. **API** envía actualización granular al servidor
3. **Sistema principal** actualiza estado específico
4. **Sincronización** automática a todas las tablets

## 🧪 **Testing de la Conexión**

### Verificar conectividad:
```bash
# Desde la terminal donde está el waiter
ping [IP_DE_LA_TABLET_PRINCIPAL]
curl http://[IP_DE_LA_TABLET_PRINCIPAL]:8080/api/orders
```

### Logs de la App:
```bash
adb logcat | grep -E "(RemoteOrderService|CreateOrderViewModel)"
```

## ⚠️ **Consideraciones**

### 🔒 **Seguridad**
- ✅ HTTPS en producción (cambiar `http://` por `https://`)
- ✅ Timeout de 30 segundos configurado
- ✅ Logging habilitado para debug

### 📶 **Red**
- ✅ Tablets deben estar en la **misma red Wi-Fi**
- ✅ Puerto 8080 debe estar **abierto** en la tablet principal
- ✅ **Fallback** a base de datos local si API falla

### 🔄 **Sincronización**
- ✅ **API-First**: Siempre intenta usar servidor remoto
- ✅ **Local Backup**: Guarda localmente si API falla  
- ✅ **Real-time**: WebSocket notifications entre apps

## 🎯 **Estado Actual**

### ✅ **Funcionando**
- [x] Estructura API completa implementada
- [x] Retrofit + OkHttp configurado
- [x] DTOs y serialización configurada  
- [x] ViewModel actualizado para usar API
- [x] Manejo de errores implementado
- [x] Logs detallados para debugging

### 🔧 **Pendiente por Configurar**
- [ ] **IP del servidor principal** (cambiar en NetworkModule.kt)
- [ ] **Testing** con IntimoCoffeeApp ejecutándose
- [ ] **Verificación** de endpoints en servidor principal

---

**🎉 La app del waiter ahora está completamente preparada para comunicarse con las APIs de IntimoCoffeeApp.**

Solo falta configurar la IP correcta del servidor y verificar que IntimoCoffeeApp esté ejecutándose con el servidor HTTP habilitado.