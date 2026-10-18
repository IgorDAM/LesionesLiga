# Publicar en Azure gratis

Combinación: **Azure App Service (plan Free F1, Linux, Java SE)** para la web + **Azure SQL Database (oferta gratuita)** para los datos.

## Estado real del despliegue (18-10-2026)

Desplegado y funcionando con la cuenta de Educastur (pendiente de migrar a una cuenta personal).

| Recurso | Valor |
|---|---|
| **URL pública** | `https://lesiones-liga-b0d6hpatcxfbcpd7.francecentral-01.azurewebsites.net/` |
| Grupo de recursos | `rg-lesiones` (France Central) |
| Web App | `lesiones-liga` · Linux · Java 17 · Java SE |
| Plan de App Service | `plan-lesiones-f1` (Gratis F1, propio: no comparte cuota de CPU con otros proyectos) |
| Servidor SQL | `lesiones-sql-fr` (France Central) · base `lesiones` (oferta gratuita, auto-pausa al agotar el cupo) |
| Despliegue | GitHub Actions desde `main` (`.github/workflows/main_lesiones-liga.yml`, generado por Azure) con identidad administrada `oidc-msi-aa17` |
| Comprobación | `/api/resumen` devolvió 69 lesionados activos tras el primer arranque (47 Primera, 22 Segunda) |

**Importante sobre la URL:** el nombre de host por defecto es «único y seguro» y lleva un **sufijo aleatorio**.
`https://lesiones-liga.azurewebsites.net` **no existe**; usa siempre la dirección larga de arriba (también visible en Azure → Web App → Información general → *Dominio predeterminado*).

Notas del despliegue real:
- Cada `git push` a `main` dispara compilación (`mvn clean install`, con los tests) y despliegue. Antes de editar en local, haz `git pull`: Azure añadió el workflow al repositorio.
- `LesionesLoader` inserta al arrancar las lesiones de `seed/lesiones.csv` sin duplicar; no hace falta importarlas a mano.
- Si el primer intento de guardar el Deployment Center falla con errores de OIDC o «la asignación de rol ya existe», recarga la página y, al volver a elegir repositorio y rama, selecciona la identidad que ya se creó en lugar de «(Nuevo)».
- El presupuesto `realprint-zero-cost-alert` solo filtra `realprint-rg`; `rg-lesiones` necesita el suyo (o uno sobre toda la suscripción).

> Los límites y precios salen de la documentación oficial consultada el 10-10-2026; compruébalos antes de depender de ellos, porque cambian.
> Los pasos del portal pueden tener pequeñas diferencias de nombre. Si algo no coincide, dímelo y lo ajusto.

## Qué es gratis y con qué límites

| Servicio | Oferta | Límites relevantes |
|---|---|---|
| **App Service F1** | Plan gratuito | 60 minutos de CPU al día, 1 GB de memoria, 1 GB de almacenamiento, 5 websockets, **sin dominio propio** (tu URL será `https://NOMBRE-SUFIJO.REGION-01.azurewebsites.net`, con HTTPS). Sin «Always On»: la app se duerme tras un rato sin visitas y la primera carga es lenta. |
| **Azure SQL Database (oferta gratuita)** | 100.000 vCore-segundos y 32 GB al mes, gratis «durante toda la vida de la suscripción» | Hasta 10 bases por suscripción. Si se agota el cupo mensual puedes elegir **auto-pausa hasta el mes siguiente** (sin coste) o seguir pagando. **Elige auto-pausa.** |

Con un uso de una persona actualizando una vez por semana y poca gente consultando, ambos límites sobran. Si superas la cuota diaria de CPU de F1, la app se detiene temporalmente hasta que se renueva.

**Por qué Azure SQL y no Azure Database for PostgreSQL:** la oferta gratuita de PostgreSQL es de **12 meses desde la creación de la cuenta** (después se paga). La de Azure SQL no caduca, y el proyecto ya trae el driver y funciona con él.
Las ofertas de «12 meses» dependen de la fecha de alta de tu cuenta de Azure, así que no cuentes con ellas si tu cuenta ya tiene tiempo.

## Paso a paso

### 1. Ponte un freno de gasto (hazlo primero)
Portal de Azure → **Cost Management + Billing → Budgets → Add**: presupuesto mensual de **1 €** con alerta por correo al 50 %.
Así, si algo se sale de lo gratuito, te enteras el primer día.
(Si filtras por grupo de recursos, ten en cuenta que Azure solo lista grupos que ya tienen gasto registrado; un grupo recién creado no aparece. En ese caso crea el presupuesto sobre toda la suscripción.)

### 2. Grupo de recursos
**Resource groups → Create** → nombre `rg-lesiones`, región **West Europe** (en la cuenta de Educastur no estaba permitido crear servidores SQL en East US; los recursos reales están en **France Central**).
Tenerlo todo en un grupo permite borrarlo entero con un clic si quieres parar.

### 3. Base de datos (Azure SQL, oferta gratuita)
1. **Create a resource → SQL Database → Create**.
2. Grupo `rg-lesiones`, nombre de base `lesiones`.
3. Servidor: **Create new** → nombre único, región West Europe (o la que permita tu suscripción), autenticación **SQL authentication**, usuario y contraseña de administrador (guárdalos).
4. Si aparece el banner **«Apply offer»** (oferta gratuita), pulsa **Apply offer**. Debe quedar la opción serverless gratuita con **Behavior when free limit reached = Auto-pause the database until next month**. Comprueba el coste estimado antes de crear: el formulario puede proponer Hyperscale (de pago) por defecto.
5. **Networking**: *Allow Azure services and resources to access this server* = **Yes**. (Opcional: *Add current client IP address* para conectarte tú desde tu PC.)
6. Create. Anota el servidor (`algo.database.windows.net`), la base, el usuario y la contraseña.

### 4. La web (App Service)
1. **Create a resource → Web App**.
2. Grupo `rg-lesiones`, nombre único (será parte de la URL), **Publish: Code**, **Runtime stack: Java 17**, **Java web server stack: Java SE (Embedded Web Server)**, **Operating System: Linux**, región cercana (la del servidor SQL).
3. **Plan de App Service**: crea uno propio con **Crear nuevo** y elige **Gratis F1** en *Plan de precios*. Al crear un plan nuevo, Azure propone Premium V3 (de pago) por defecto: cámbialo.
4. Create.

### 5. Variables de entorno
Web App → **Settings → Environment variables → App settings** → añade:

| Nombre | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:sqlserver://SERVIDOR.database.windows.net:1433;database=lesiones;encrypt=true;trustServerCertificate=false;loginTimeout=90` |
| `DATABASE_USER` | tu usuario administrador |
| `DATABASE_PASSWORD` | tu contraseña |
| `ADMIN_TOKEN` | una contraseña larga y aleatoria (es la del panel de administración) |
| `SERVER_PORT` | `8080` |
| `WEBSITES_PORT` | `8080` |
| `JAVA_OPTS` | `-Xmx400m -XX:+UseSerialGC` |

Guarda (Apply). `SERVER_PORT` y `WEBSITES_PORT` se fijan a propósito: así la app y Azure están de acuerdo en el puerto.
`JAVA_OPTS` limita la memoria para que no choque con el 1 GB del plan F1.

Las aplicaciones web nuevas ya traen HTTPS Only activado; compruébalo en **Settings → Configuration → General settings**.

### 6. Desplegar

**Opción A (recomendada): Deployment Center**
1. Sube el proyecto a un repositorio de GitHub.
2. Web App → **Deployment → Deployment Center** → Source: **GitHub** → autoriza → elige organización, repositorio y rama.
3. Azure genera por ti el workflow de GitHub Actions y una identidad administrada para autenticarse (OIDC). Cada `git push` despliega.

**Opción B: desde tu PC con la CLI de Azure**
```bash
mvn -DskipTests package
az login
az webapp deploy --resource-group rg-lesiones --name NOMBRE_DE_TU_APP \
   --src-path target/lesiones-liga-0.1.0.jar --type jar
```

### 7. Comprobar
- Abre la **URL pública completa** (con sufijo) que aparece en Azure → Web App → Información general → *Dominio predeterminado*.
- Si no responde, mira **Monitoring → Log stream**.
- **La primera carga puede tardar**, por dos motivos: F1 no tiene «Always On» (la app arranca de cero) y la base serverless se despierta si estaba en pausa.
- Al primer arranque, Hibernate crea las tablas solo (`ddl-auto=update`) y se cargan los datos semilla (zonas, tipos de lesión, clubes y lesiones activas).
- Comprueba `/api/resumen`: debe mostrar los lesionados activos cargados.
- Entra en la pestaña **Administración** con tu `ADMIN_TOKEN`.

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| La app no arranca y el log habla de conexión o `Login timeout` | La base estaba pausada y tardó en despertar; reinicia la app. Revisa también que *Allow Azure services* esté en Yes y que el usuario y la contraseña sean correctos. |
| Error de tablas o de tipos al crear el esquema | Con SQL Server el esquema se creó bien en el despliegue real (18-10-2026). Si vuelve a pasar, pasa el error del Log stream. |
| «502/503» o el sitio nunca responde | Revisa `SERVER_PORT` y `WEBSITES_PORT` = `8080` y que el runtime sea Java 17 (comprueba los disponibles con `az webapp list-runtimes --os linux`). |
| `NOMBRE.azurewebsites.net` no abre | Usa la URL larga con sufijo aleatorio (ver «Estado real del despliegue»). |
| La app se detiene a media tarde | Se superó la cuota diaria de CPU de F1 (60 min). Se renueva solo; si te pasa a menudo, avísame y miramos cómo reducir el consumo. |
| Error al guardar el Deployment Center (OIDC / «role assignment already exists») | Recarga la página y vuelve a elegir repositorio y rama; selecciona la identidad ya creada en lugar de «(Nuevo)». |

## ¿Y MySQL?

El proyecto también trae el driver de MySQL: basta con cambiar `DATABASE_URL`
(`jdbc:mysql://HOST:3306/BASE?sslMode=REQUIRED&serverTimezone=Europe/Madrid`), `DATABASE_USER` y `DATABASE_PASSWORD`.
Pero **en Azure, MySQL no es gratis para siempre**: la oferta gratuita de Azure Database for MySQL Flexible Server es de
**12 meses**, con recursos limitados y para usuarios nuevos ([precios de MySQL en Azure](https://azure.microsoft.com/en-us/pricing/details/mysql/)).
Pasado ese año se paga. Si quieres MySQL y gratis de verdad, las opciones son:

- **MySQL en tu propio PC** (gratis, sin límite de tiempo), válido si la web solo la usas tú en local.
- **Un proveedor externo de MySQL gratuito**: hay varios, pero sus planes gratuitos cambian a menudo y no he verificado hoy cuáles siguen vigentes.
  Si te interesa, los busco y comparo.

Como Hibernate crea las tablas solo, no hay scripts SQL que adaptar entre motores.

## Alternativas

- **Base de datos PostgreSQL gratuita fuera de Azure (Neon):** pon `DATABASE_URL=jdbc:postgresql://HOST/BASE?sslmode=require` (más usuario y contraseña) y el resto igual. Ya está soportado.
- **Parar de pagar y de usar recursos:** borra el grupo `rg-lesiones` desde el portal.

## Fuentes (documentación oficial de Microsoft)
- [Oferta gratuita de Azure SQL Database](https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer)
- [Límites de Azure App Service (tabla Free F1)](https://learn.microsoft.com/en-us/azure/azure-resource-manager/management/azure-subscription-service-limits)
- [Configurar una app Java en App Service (JAVA_OPTS, despliegue de JAR)](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run)
- [Servicios gratuitos de Azure](https://azure.microsoft.com/en-us/pricing/free-services/)
