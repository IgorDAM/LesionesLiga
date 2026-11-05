# Parte de lesiones · Primera y Segunda División

Web personal para ir recopilando las lesiones de los jugadores de Primera y Segunda División,
consultar qué lesión tiene ahora mismo un jugador y comparar cuánto tardan en recuperarse
los jugadores con **la misma lesión**.

- **Web en producción**: <https://lesiones-liga-b0d6hpatcxfbcpd7.francecentral-01.azurewebsites.net/> (la primera carga puede tardar hasta un minuto: el plan gratuito duerme la app).
- **Gratis de punta a punta**: Spring Boot + H2 en local; al publicar, Azure App Service (plan gratuito F1) y Azure SQL Database (oferta gratuita).
- **Fuentes**: prensa, LaLiga y webs oficiales de los clubes, introducidas por ti una vez por semana.
  Cada lesión guarda el enlace a su fuente.
- **Infografía**: mapa corporal (frente y espalda) en SVG que se colorea según la zona lesionada.

## ⚠️ Estado de verificación (léelo primero)

| Parte | Estado |
|---|---|
| Frontend (HTML/JS/CSS + mapa SVG) | Probado con jsdom contra una API simulada (pestañas con teclado, carga con reintentos, comparativa con cuartiles y filtro de fechas verificadas, conservación del foco). |
| Backend (Spring Boot) | Desplegado en Azure. El CI (`.github/workflows/ci.yml`) ejecuta `mvn verify` en cada push; los tests de integración (`ApiTest`) cubren catálogo, token, límite de intentos, cabeceras y caché, búsqueda sin tildes, estadísticas, validaciones y exportación CSV. |
| Despliegue en Azure (`AZURE.md`) | Funcionando con SQL Server (Azure SQL). El estado real está en `AZURE.md`. |

## Cómo ejecutarlo

Requisitos: **Java 17** y **Maven**.

```bash
# Probar con datos FICTICIOS (base de datos en memoria, se borra al parar)
mvn spring-boot:run -Dspring-boot.run.profiles=demo
# → http://localhost:8080   (token de administración por defecto: cambia-esto)

# Uso real (los datos se guardan en ./data/lesiones.mv.db)
ADMIN_TOKEN="una-clave-larga" mvn spring-boot:run
```

En Windows (PowerShell): `$env:ADMIN_TOKEN="una-clave-larga"; mvn spring-boot:run`

El perfil `demo` crea un club inventado («Club Demo (datos ficticios)») con jugadores y lesiones
falsos, para ver cómo se comportan las comparativas sin mezclar nada con jugadores reales.
Incluye adrede un caso con pocos datos (LCA, 3 casos) para ver el aviso de baja fiabilidad.

## Carga inicial de lesiones (10-10-2026)

`src/main/resources/seed/lesiones.csv` trae **56 lesiones activas reales** (45 de Primera, 11 de Segunda, 19 con fecha verificada) recogidas de
[FútbolFantasy](https://www.futbolfantasy.com/laliga/lesionados) y Jornada Perfecta ([Primera](https://www.jornadaperfecta.com/lesionados), [Segunda](https://www.jornadaperfecta.com/segunda/lesionados)).
`LesionesLoader` las inserta al arrancar (perfil normal, no demo/test) sin duplicar.

- Fecha de inicio de Primera = «lesionado desde» de FútbolFantasy; en Segunda la fuente no da fecha, se usa la de detección. Todas se marcan como aproximadas en *notas*.
- `revisar=true` → la fuente no precisa el tipo (p. ej. «lesión en el cuádriceps») y se clasificó provisionalmente; corrígelo al dar el alta.
- El lado es «no aplica» salvo donde la fuente lo dice; se indica «Lado sin confirmar» en notas.
- **No cargadas** (tipo o zona no precisados): molestias/dudas, «lesión de rodilla» genérica, artroscopias sin diagnóstico, lesiones musculares de Segunda sin zona.
- Cada semana se amplía este CSV (o se usa Administración) y los altas se registran en la web.

## Flujo semanal (15–20 minutos)

1. Entra en **Administración → Revisión semanal**. Los clubes revisados hace más tiempo salen primero.
2. Para cada club, abre *Noticias* (búsqueda de Google Noticias ya preparada), su web o su parte médico.
3. **Lesión nueva** → *Registrar lesión*: jugador, zona, tipo, lado, fecha de inicio, plazo que da el club,
   diagnóstico del club en texto corto y **URL de la fuente** (obligatoria).
4. **Jugador que vuelve** → *Altas y borrados*: pones la fecha de alta y, si lo sabes, los partidos perdidos.
   En ese momento el caso empieza a contar en las estadísticas.
5. Pulsa *Marcar revisado* en cada club.
6. De vez en cuando, descarga el CSV desde *Copia de seguridad*.

Si un jugador no existe todavía: *Jugadores, clubes y catálogo → Nuevo jugador*. Si ficha por otro club,
usa *Fichaje*: conserva todo su historial.

## Modelo de datos

```
Club ──< Jugador ──< Episodio >── TipoLesion >── ZonaCorporal
```

- **ZonaCorporal** (22 zonas): catálogo cerrado. Su `codigo` es el mismo `data-zona` del SVG.
- **TipoLesion** (66 tipos): catálogo cerrado. *«Misma lesión» = mismo registro*, por eso las
  comparativas son un simple `GROUP BY` y no dependen de cómo lo escriba cada periódico.
- **Episodio**: jugador + tipo + lado + fecha de inicio + fecha de alta (vacía = sigue lesionado),
  partidos perdidos, recaída, cirugía, plazo estimado por el club, fuente.
- El **lado** (derecho/izquierdo) es del *jugador*, no del dibujo, y solo se pide en zonas bilaterales.

Para ampliar el catálogo: edita `src/main/resources/seed/tipos.csv` y reinicia (solo inserta lo que falta),
o usa el formulario de administración.

### Cómo se calculan las comparativas

- Solo cuentan los casos **cerrados**. Se muestran mediana, media, mínimo y máximo, y los partidos perdidos.
- Se priorizan la **mediana** y la lista de casos porque la media se distorsiona con un solo caso extremo.
- Se puede filtrar por **con/sin cirugía** y **incluir/excluir recaídas**: son dos factores que cambian mucho el plazo.
- Con menos de **5 casos cerrados** (`app.minimo-casos`) la web avisa de que los números son orientativos.
- Se compara también el **plazo anunciado por el club** con el real (días de desviación media).

## API (resumen)

Pública (solo lectura): `GET /api/zonas`, `/api/tipos?zona=`, `/api/clubes`, `/api/jugadores?q=`,
`/api/jugadores/{id}`, `/api/lesiones/activas?division=&clubId=&zona=`, `/api/resumen`,
`/api/estadisticas/zona/{codigo}`, `/api/estadisticas/tipo/{id}?cirugia=&incluirRecaidas=&soloVerificados=`.

Administración (cabecera `X-Admin-Token`): `POST /api/admin/clubes`, `PUT /api/admin/clubes/{id}`,
`PATCH /api/admin/clubes/{id}/revisado`, `POST /api/admin/jugadores`, `PATCH /api/admin/jugadores/{id}/club`,
`POST /api/admin/tipos`, `POST|PUT /api/admin/episodios`, `PATCH /api/admin/episodios/{id}/alta`,
`DELETE /api/admin/episodios/{id}`, `GET /api/admin/export/episodios.csv`.

## Publicarlo gratis (Azure)

La guía paso a paso está en **[AZURE.md](AZURE.md)**. En resumen:

1. **Base de datos:** Azure SQL Database con la oferta gratuita (100.000 vCore-segundos y 32 GB al mes, sin caducidad; elige «auto-pausa» al agotar el cupo).
2. **Web:** Azure App Service, plan **Free F1**, Linux, Java 17 (60 min de CPU al día, URL `azurewebsites.net`, se duerme sin visitas).
3. **Variables de entorno:** `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `ADMIN_TOKEN`, `SERVER_PORT=8080`, `WEBSITES_PORT=8080`.
4. **Despliegue:** Deployment Center de Azure con GitHub (cada `git push` despliega) o `az webapp deploy`.

*No uses H2 en producción:* el disco de App Service gratuito no es un sitio fiable para guardar la base de datos.
Antes de nada, crea un presupuesto de 1 €/mes con alerta en Cost Management para enterarte si algo se sale de lo gratuito.

> Los límites y condiciones de los planes gratuitos cambian: compruébalos en la documentación de Azure antes de depender de ellos.
> El `Dockerfile` incluido es opcional (por ejemplo, para Azure Container Apps u otro hosting con Docker); App Service con Java SE no lo necesita.

## Seguridad y caché

- **`ADMIN_TOKEN` es obligatorio con el perfil `prod`:** la app se niega a arrancar si falta o es `cambia-esto`. Conviene una clave de 16 caracteres o más (si es más corta, solo se avisa en el log).
- Tras 10 intentos fallidos con el token desde una misma dirección, `/api/admin/**` responde `429` durante 15 minutos (el contador vive en memoria y se reinicia al reiniciar la app).
- Todas las respuestas llevan cabeceras de seguridad (CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`).
- Los GET públicos de `/api/**` se cachean 2 minutos (`Cache-Control: public, max-age=120`); `/api/admin/**` y los errores nunca. Un cambio hecho desde el panel puede tardar hasta 2 minutos en verse para el resto de visitantes; el administrador, con sesión iniciada, siempre ve datos frescos.

## Calidad de los datos

Cada episodio guarda si su **fecha de inicio está verificada** en una fuente (`inicioVerificado`). En `seed/lesiones.csv` sale de la columna `inicio_exacto`; al arrancar, el cargador rellena el campo en episodios antiguos que aún no lo tenían, sin pisar un valor ya decidido. La comparativa muestra cuántos casos tienen fecha aproximada, marca esos casos y permite filtrar «Solo inicio verificado». Además de mediana y media se muestra el rango habitual (cuartiles 1 y 3).

## Fuentes y límites legales

- Se guardan **hechos y enlaces**, no se copian textos ni imágenes de prensa o clubes. Los hechos (quién, qué lesión,
  cuándo) no tienen derechos de autor; los artículos y las fotos sí.
- El campo «diagnóstico del club» es un texto corto tuyo, no un copia-pega.
- No se hace scraping de Transfermarkt ni de otras webs que lo prohíben en sus términos.
- La web indica que no tiene relación con LaLiga ni con los clubes y que no es consejo médico.
- Los datos de salud de personas identificables que ya han publicado sus clubes son información pública, pero si algún día
  abres el proyecto a otros usuarios conviene revisar la normativa de protección de datos (RGPD).

## Ideas para más adelante

- Radar automático: feeds RSS de clubes y prensa filtrados por palabras clave (lesión, parte médico, baja…) que solo
  *propongan* candidatos; la verificación y el registro seguirían siendo manuales.
- Pestaña «Historial» (o sección en «Lesionados ahora») con todos los jugadores ya recuperados, ordenados por fecha de alta y con filtros por división, club y zona. Hoy el histórico solo se ve en la ficha de cada jugador y en «Comparar recuperaciones».
- Filtro por temporada y por posición en las comparativas.
- Edición de episodios cerrados desde la web (la API ya la admite: `PUT /api/admin/episodios/{id}`).
- Gráfico de evolución de lesiones por mes.

## Estructura

```
src/main/java/es/lesiones/liga/
  domain/    entidades JPA (Club, Jugador, ZonaCorporal, TipoLesion, Episodio)
  repo/      repositorios Spring Data
  service/   ConsultaService (lectura y estadísticas), AdminService (escritura), SeedLoader, DemoData
  web/       controladores REST, DTOs, token de administración, manejo de errores
src/main/resources/
  seed/      zonas.csv, tipos.csv, clubes.csv (Primera y Segunda 2026/27)
  static/    index.html, css/, js/ (app.js y body.js: mapa SVG)
src/test/    ApiTest (integración con MockMvc y H2 en memoria)
```
