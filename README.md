# Quizly

Aplicación web de cuestionarios organizada por asignaturas y temas. Permite a los usuarios realizar cuestionarios, seguir su progreso y crear los suyos propios. Los administradores pueden además generarlos con IA a partir de un PDF. Incluye un panel de administración.

## Funcionalidades

**Usuarios**
- Registro e inicio de sesión con formulario o con Google (OAuth2); recuperación de contraseña por correo.
- Navegación por asignaturas → temas → cuestionarios, y listados de cuestionarios recientes y favoritos.
- Realización de cuestionarios con estado de intento persistido, reanudación, navegación entre preguntas y cierre por tiempo o por inactividad.
- Página de progreso con historial de intentos.
- Creación, edición y borrado de cuestionarios propios.
- Compartición de cuestionarios entre usuarios, con solicitudes aceptables/rechazables desde las notificaciones.
- Tarjetas inteligentes (`/mazos`): mazos de tarjetas anverso/reverso, propios y privados, que se estudian con repetición espaciada (algoritmo SM-2 con granularidad de días: cada tarjeta se califica como "Otra vez", "Difícil", "Bien" o "Fácil" y vuelve cuando toca). Se crean a mano o importando un cuestionario ("Crear tarjetas" en su ficha y en el resumen del intento). Las preguntas falladas al terminar un intento alimentan un mazo automático, "Repaso de fallos": reprograman su tarjeta si ya existe o la crean. Los intentos que cierra el job de caducidad no alimentan tarjetas.
- Mapas mentales (`/mapas`): mapas propios y privados con un editor visual (añadir, mover y plegar ideas; clic derecho para más opciones), que se pueden vincular a un tema, descargar como PNG, imprimir y convertir en un mazo de tarjetas ("Crear mazo de tarjetas": una tarjeta por cada idea con ramas, máximo 50). Se crean a mano o con IA desde un PDF. El contenido se valida siempre en el servidor (máximo 500 nodos, 10 niveles y 200 caracteres por nodo).
- Edición del perfil, cambio de contraseña y eliminación de cuenta.

**Administración** (rol `ADMIN`)
- Panel con estadísticas.
- CRUD de asignaturas, temas, cuestionarios y usuarios.
- Importación de cuestionarios desde JSON.
- Generación de cuestionarios con IA a partir de un PDF (desde "Mis cuestionarios").
- Generación de mazos de tarjetas con IA a partir de un PDF (desde "Tarjetas", hasta 50 tarjetas). Una sola llamada al modelo: si el PDF no cabe en la ventana de contexto se rechaza como demasiado largo. Comparte con la generación de cuestionarios la cuota de uso por usuario (10 por hora y 40 al día).
- Generación de mapas mentales con IA a partir de un PDF (desde "Mapas mentales", hasta unos 80 nodos y 4 niveles; lo que excede se descarta). Una sola llamada al modelo, con el mismo rechazo por PDF demasiado largo y la misma cuota de uso por usuario que cuestionarios y tarjetas. El mapa generado se edita después como cualquier otro.

**Seguridad**
- Limitación de frecuencia en login, recuperación de contraseña y generación con IA (cuestionarios, tarjetas y mapas mentales).

## Tecnologías

- Java 21, Spring Boot 4.0.6, empaquetado **WAR** (Maven)
- Spring MVC, Spring Data JPA, Spring Security (formulario + OAuth2 Google), Spring Mail, Validation, AOP/AspectJ
- Thymeleaf + htmx
- Tailwind CSS 4 + DaisyUI, Font Awesome
- PostgreSQL
- Apache PDFBox 3.0.4 (extracción de texto de PDF)
- Generación con IA mediante la API de Gemini, con rotación de modelos, reintentos y control de ventana de contexto

## Requisitos previos

- JDK 21
- PostgreSQL con una base de datos vacía
- Node.js y npm (solo para recompilar el CSS)
- Credenciales de Google OAuth2, de un proveedor SMTP y de la API de IA

Maven no hace falta instalarlo: se usa el wrapper (`mvnw` / `mvnw.cmd`).

## Puesta en marcha

1. **Base de datos.** El proyecto no usa Flyway. Ejecuta manualmente sobre la base de datos, en orden, los scripts de [src/main/resources/db/init/](src/main/resources/db/init/):
   - `V1__create_schema.sql` (esquema)
   - `V2__seed_reference_data.sql` (datos de referencia)

   Si ya tienes una base de datos de una versión anterior, aplica en su lugar los scripts de [src/main/resources/db/update/](src/main/resources/db/update/) (por ejemplo, `1.0.0_to_1.1.0.sql` añade las tablas `mazos`, `tarjetas` y `mapas_mentales`).

   En producción `ddl-auto` es `validate`, por lo que cualquier cambio de esquema debe aplicarse a mano y reflejarse en estos scripts.

2. **Variables de entorno** (ver tabla siguiente).

3. **CSS** (solo si cambias clases de Tailwind o plantillas; el CSS generado ya está versionado):
   ```bash
   npm install
   npm run build:css     # o npm run dev:css para modo watch
   ```

4. **Arrancar en desarrollo:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   En Windows (PowerShell): `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`.
   La aplicación queda en `http://localhost:8080`.

## Configuración

Se define en [src/main/resources/application.yaml](src/main/resources/application.yaml) y en los perfiles `application-dev.yaml` / `application-prod.yaml`.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Sí | Conexión JDBC a PostgreSQL |
| `AI_API_KEY`, `AI_BASE_URL`, `AI_MODELS` | Sí | Clave, URL base y lista de modelos para la generación con IA |
| `EMAIL_USERNAME`, `EMAIL_PASSWORD` | Sí | Credenciales SMTP |
| `EMAIL_HOST`, `EMAIL_PORT` | Solo en `prod` | Servidor SMTP (en `dev` tienen valor por defecto) |
| `GMAIL_REFRESH_TOKEN` | No (solo `prod`) | Si está definido, los correos se envían por la API de Gmail (usando `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` y `EMAIL_USERNAME` como remitente); si está vacío, se usa SMTP |
| `APP_BASE_URL` | Solo en `prod` | URL pública, (en `dev` por defecto `http://localhost:8080`) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Para login con Google y envío por la API de Gmail | Por defecto `disabled` |
| `PORT` | No | Puerto del servidor (por defecto 8080) |
| `AI_MODEL_COOLDOWN_SECONDS`, `AI_TIMEOUT_SECONDS`, `AI_CONTEXT_WINDOW_TOKENS`, `AI_OUTPUT_TOKEN_RESERVE`, `AI_CONTEXT_SAFETY_MARGIN`, `AI_RETRY_MAX_ATTEMPTS`, `AI_RETRY_INITIAL_BACKOFF_MS` | No | Ajustes del cliente de IA, con valores por defecto |

Propiedades `quiz.*`: `inactivity-max-duration`, `activity-update-interval` e `inactivity-check-cron` (cierre de intentos caducados, zona horaria Europe/Madrid).

Perfiles:
- `dev`: log SQL, cookies de sesión no seguras, SMTP por defecto de pruebas (Mailtrap).
- `prod`: `ddl-auto: validate`, cookies seguras, caché de Thymeleaf, pool Hikari acotado.

## Compilación y despliegue

```bash
./mvnw clean package -DskipTests   # genera target/*.war
```

El [Dockerfile](Dockerfile) compila el WAR en una etapa con Maven/Temurin 21 y lo ejecuta con `java -jar` sobre un JRE 21; expone el puerto 10000 (configurable con `PORT`). Para ese entorno hay que activar el perfil `prod` (por ejemplo con `SPRING_PROFILES_ACTIVE=prod`) y definir las variables anteriores. Existe un endpoint `/ping` para comprobaciones de disponibilidad.

## Arquitectura

Paquete raíz `com.quizly.app`, en capas `controller` → `service` → `repository` sobre entidades JPA.

```
src/main/java/com/quizly/app/
├── controller/   # MVC y fragmentos htmx
├── service/      # lógica de negocio, IA (PDF → planificador de contexto → Gemini → parser)
├── repository/   # Spring Data JPA
├── model/        # entity, enums, session (intento en curso), view
├── security/     # UserDetails, OIDC, limitador de frecuencia
├── config/       # SecurityConfig, QuizProperties
├── advice/       # atributos globales, migas de pan, manejo de excepciones
├── job/          # cierre programado de intentos caducados
├── event/        # evento de intento finalizado → alimenta "Repaso de fallos" tras el commit
└── ...           # dto, mapper, logging (AOP), web/notification (toasts), util
src/main/resources/
├── templates/    # Thymeleaf (layouts, fragments, components, admin…)
├── static/       # JS plano y CSS (Tailwind generado desde static/css/input.css)
└── db/           # init/: esquema y datos de referencia; update/: migraciones entre versiones
```

## Pruebas

```bash
./mvnw test                                    # todas
./mvnw test -Dtest=CuestionarioServiceTest     # una clase
```

Hay pruebas unitarias con JUnit/Mockito de servicios (IA, parser, importación JSON, cuestionarios, favoritos, compartición, perfil, tarjetas, algoritmo SM-2 y mapas mentales, etc.), de seguridad (limitador de frecuencia, filtro de login) y de varios controladores. `AppApplicationTests` es una prueba de contexto que puede requerir la configuración y la base de datos descritas arriba.

## Licencia

Distribuido bajo la licencia [PolyForm Noncommercial 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0): el código es de acceso público y puede usarse con fines no comerciales, pero no para uso comercial. Consulta [LICENSE.md](LICENSE.md).

### Componentes de terceros

[Font Awesome Free](https://fontawesome.com) 7.3.0, © 2026 Fonticons, Inc. Sus fuentes (`src/main/resources/static/webfonts/`) y su CSS (incluido en `tailwind.css`) se redistribuyen en este repositorio y conservan su propia licencia, no la de Quizly:

- Fuentes: [SIL OFL 1.1](https://openfontlicense.org/) (nombre reservado "Font Awesome").
- Iconos: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- Código: [MIT](https://opensource.org/license/mit).

Licencia completa: <https://fontawesome.com/license/free>.

El CSS generado (`tailwind.css`) incluye también, bajo licencia [MIT](https://opensource.org/license/mit):

- [Tailwind CSS](https://tailwindcss.com) 4.3.0, © Tailwind Labs, Inc.
- [daisyUI](https://daisyui.com) 5.5.20, © 2020 Pouya Saadeghi ([licencia](https://github.com/saadeghi/daisyui/blob/master/LICENSE)).
