# Mesero (Waiter) ↔ Tablet (IntimoCoffeeApp)

El mesero localiza el **HTTP server** de la app principal en el puerto **8080** (`GET /discover` con `serviceType: INTIMO_COFFEE_MAIN`).

## Requisitos de red

1. **Misma WiFi** que la tablet (no datos móviles en el teléfono si la tablet está en WiFi).
2. **Aislamiento de clientes (AP isolation)** desactivado en el router, si no, los equipos no se ven entre sí.
3. En la tablet, **IntimoCoffeeApp abierta** (el servidor arranca al iniciar la app).

## Si no conecta (NSD / escaneo fallan)

1. En la tablet, anota la **IP WiFi** (ajustes de red o pantalla de depuración si la tienes).
2. En el proyecto **IntimoCoffeeWaiter**, edita **`gradle.properties`**:

   ```properties
   INTIMO_MAIN_SERVER_URL=http://LA_IP_DE_LA_TABLET:8080/
   ```

   Ejemplo: `http://192.168.0.112:8080/`

3. **Sync Gradle** y vuelve a **compilar/instalar** el APK del mesero (BuildConfig se genera en build).

Esa URL se prueba **antes** que NSD y el escaneo por subred.

## Comprobar la tablet desde un PC o el teléfono

En el navegador: `http://IP_TABLET:8080/discover`  
Debe devolver JSON con `"serviceType":"INTIMO_COFFEE_MAIN"`.

## Emulador Android Studio

Solo en emulador se usa por defecto `http://10.0.2.2:8080/` si falla el descubrimiento. En **teléfono físico** no sirve; usa `INTIMO_MAIN_SERVER_URL` o arregla la red.

## Logcat (qué mirar)

Si ves peticiones a `http://10.0.2.2:8080/` en un **teléfono real**, el cliente aún no había ejecutado el descubrimiento (o falló). La app ahora lanza descubrimiento al **inicio**; igual conviene fijar `INTIMO_MAIN_SERVER_URL` para redes difíciles (NSD bloqueado, etc.).

```bash
adb logcat -d --pid=$(adb shell pidof com.intimocoffee.waiter) | grep -E "okhttp|Servidor principal|ServerDiscovery|DynamicRetrofit"
```
