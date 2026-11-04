# API de Tipo de Cambio

Microservicio en Java que consume el servicio público [ExchangeRate-API](https://www.exchangerate-api.com/) y expone un endpoint REST para convertir montos entre monedas. Cada conversión se guarda en una base de datos H2 en memoria.

## Requisitos

- Java 17 o superior
- Conexión a internet (para consultar el tipo de cambio real)

## Cómo ejecutar

```bash
./mvnw spring-boot:run
```

La aplicación levanta en `http://localhost:8080`.

La API key de ExchangeRate-API está configurada en `application.yaml` (`exchange-rate.api.key`), por lo que no hace falta definir variables de entorno.

## Cómo ejecutar los tests

```bash
./mvnw test
```

Los tests no usan la red: el servicio externo se simula con mocks.

## Base de datos H2

| Parámetro | Valor |
|---|---|
| Consola | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:mem:testdb` |
| Usuario | `sa` |
| Password | `password` |

Las conversiones se guardan en la tabla `CONVERSIONS`:

```sql
SELECT * FROM CONVERSIONS;
```

## Endpoint

### `POST /api/conversion`

Request:

```bash
curl -X POST http://localhost:8080/api/conversion \
  -H "Content-Type: application/json" \
  -d '{"monto":100,"monedaOrigen":"USD","monedaDestino":"PEN"}'
```

Response `200 OK` (respuesta real obtenida con el servicio externo):

```json
{
  "monto": 100,
  "montoConvertido": 346.48,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.4648
}
```

### Errores

Todos los errores usan la misma estructura:

```json
{
  "codigo": "MONEDA_NO_SOPORTADA",
  "mensaje": "La moneda de destino XXX no es soportada",
  "fecha": "2026-11-04T00:16:04.701653600Z"
}
```

| HTTP | `codigo` | Cuándo ocurre |
|---|---|---|
| 400 | `SOLICITUD_INVALIDA` | Falla la validación del cuerpo (bean validation): monto nulo o menor/igual a cero, moneda vacía |
| 400 | `MONTO_INVALIDO` | Un monto menor/igual a cero llega hasta el dominio (`InvalidAmountException`); es la red de seguridad del dominio, distinta de `SOLICITUD_INVALIDA` que se detecta antes, en la validación del request |
| 400 | `MONEDA_INVALIDA` | El código de moneda no tiene 3 letras (ejemplo: `US`) |
| 400 | `MONEDA_NO_SOPORTADA` | La moneda de origen o destino no existe en el servicio externo |
| 400 | `CUERPO_INVALIDO` | JSON mal formado |
| 404 | `RECURSO_NO_ENCONTRADO` | La URL no existe |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no permitido (ejemplo: `GET /api/conversion`); incluye el header `Allow` |
| 415 | `TIPO_CONTENIDO_NO_SOPORTADO` | `Content-Type` distinto de `application/json` |
| 502 | `SERVICIO_EXTERNO_NO_DISPONIBLE` | El servicio externo falla, expira o responde algo distinto de `success` |
| 500 | `ERROR_INTERNO` | Error inesperado (mensaje genérico, sin detalles internos) |

Ejemplo `400` (monto negativo):

```json
{
  "codigo": "SOLICITUD_INVALIDA",
  "mensaje": "El monto debe ser mayor a cero",
  "fecha": "2026-11-04T00:16:04.792860300Z"
}
```

Ejemplo `502` (servicio externo caído):

```json
{
  "codigo": "SERVICIO_EXTERNO_NO_DISPONIBLE",
  "mensaje": "El servicio de tipo de cambio no está disponible",
  "fecha": "2026-11-04T00:20:11.120000000Z"
}
```

## Arquitectura

Tres capas con DDD. Las dependencias apuntan hacia el dominio.

```
pe.test.exchange
├── domain
│   ├── model           CurrencyCode, ExchangeRate (value objects) y Conversion (entidad)
│   ├── exception       Excepciones de dominio
│   ├── service         ExchangeRateProvider (puerto hacia el servicio externo)
│   └── repository      ConversionRepository (puerto de persistencia)
├── application
│   ├── controller      ConversionController y GlobalExceptionHandler
│   ├── service         ConvertCurrencyService (caso de uso)
│   ├── dto             Request, Response y ErrorResponse
│   └── mapper          ConversionMapper
└── infrastructure
    ├── external        FeignExchangeRateProvider (adaptador sobre el cliente Feign)
    ├── persistence     ConversionRepositoryAdapter sobre Spring Data JPA
    └── config          ClockConfig
```

## Decisiones de diseño

- **Puerto y adaptador:** el dominio solo conoce `ExchangeRateProvider` y `ConversionRepository`. Feign y JPA viven en infraestructura, así que se pueden cambiar sin tocar el dominio y el caso de uso se prueba sin red ni base de datos.
- **Value objects:** `CurrencyCode` normaliza (trim y mayúsculas) y valida el formato `^[A-Z]{3}$`. `ExchangeRate` exige una tasa mayor a cero. Un valor inválido no puede existir.
- **Dinero con `BigDecimal`:** nunca se usa `double`. El `tipoCambio` se devuelve tal como lo entrega el servicio y el `montoConvertido` se redondea a 2 decimales con `RoundingMode.HALF_UP`.
- **Entidad con fábrica:** `Conversion.create(...)` valida el monto y calcula el monto convertido; la regla de negocio queda en el dominio.
- **Un solo formato de error:** `GlobalExceptionHandler` traduce cada excepción de dominio a `ErrorResponse` con el estado HTTP correcto. Los errores inesperados devuelven un mensaje genérico y el detalle solo va al log.
- **Sin filtrar la API key:** el adaptador no propaga la excepción de Feign, porque su mensaje incluye la URL con la clave.
- **`Clock` inyectado:** la fecha de la operación sale de un `Clock` (UTC), lo que permite probar con una fecha fija.
- **Timeouts:** Feign usa 3 s de conexión y 5 s de lectura para no colgar la petición si el servicio externo se demora.
- **Transacciones:** el caso de uso no es `@Transactional` a propósito. Solo hay un `save` (atómico en Spring Data) y así no se mantiene una conexión de base de datos abierta mientras se espera la respuesta HTTP externa.
- **Persistencia solo si todo sale bien:** si el monto es inválido o el servicio externo falla, no se guarda nada.
