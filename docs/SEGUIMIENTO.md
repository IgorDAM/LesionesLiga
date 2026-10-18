# Seguimiento semanal

Cada sábado se contrastan las lesiones activas con las fuentes, se anotan las altas probables (hasta que se confirmen) y se añaden los nuevos lesionados.

## 2026-10-10 · carga inicial
- 69 lesiones activas (46 Primera, 23 Segunda), 19 con fecha de inicio verificada en `src/main/resources/seed/lesiones.csv`.
- Fuentes: [FútbolFantasy](https://www.futbolfantasy.com/laliga/lesionados), [Jornada Perfecta Primera](https://www.jornadaperfecta.com/lesionados), [Jornada Perfecta Segunda](https://www.jornadaperfecta.com/segunda/lesionados).
- Límites: fechas de inicio aproximadas; 33 con tipo por revisar; AS, Marca y prensa local no accesibles desde el entorno de trabajo.

## 2026-10-10 · verificación de fechas
- Investigación de la fecha real de inicio con prensa y partes oficiales: 19 fechas verificadas (columna `inicio_exacto`), el resto sigue como aproximada.
- Correcciones de diagnóstico: De Jong (ligamento lateral interno, no cruzado), Asencio (fractura por estrés de tibia, operada el 01-10), Adilson (rotura parcial del LCA), Foulquier retirado (rodilla operada, tipo sin verificar), Ignacio Martín retirado (lesión no confirmada).
- Probables altas a confirmar: Militão, Antañón, Vargas, Diakhaby.
- Lesiones heredadas de 2025/26 a comprobar: Tabatadze, Adilson, Guille Bueno.

## 2026-10-10 · contraste con convocatorias (parcial)
- Convocados (probable alta): Ville Koski (Alavés–Atlético) y Luiz Felipe (Rayo–Athletic). Pendientes de confirmar en `seed/altas.csv`.
- Mikel Rodríguez (Alavés, LCA): en la convocatoria sale un «Miguel Rodríguez», que puede ser otro jugador. No se da de alta.
- Augusto Batalla (Rayo) no está convocado: sigue de baja.
- El resto de partidos son el 11-10 y 12-10 y sus convocatorias aún no estaban publicadas. Pendiente repetirlo con todos los clubes.

## 2026-10-10 · barrido club por club
- Añadidas 12 lesiones que faltaban (69 filas en total): Puertas (Albacete), Viti, Clemente, Jesé y Ale García (Las Palmas), Roca, Morlanes y Jan Salas (Mallorca), Iosifov, Otero y Nacho Martín (Sporting), Foulquier (Valencia, ahora con aductor y rodilla operada de febrero).
- No cargadas por no encajar en el catálogo o no estar claras: Ely (artritis de tobillo), Román y El-Abdellaoui (artroscopia de rodilla sin diagnóstico), Konrad (cartílago de rodilla), Jastin García (fuentes en conflicto), Petit, Park, Sandro, Cedeño, Recoba, Loum, Ferrari, Isfan, Pedrola, y las «molestias» de Primera.
- Lesiones de 2025/26 que conviene comprobar si siguen activas: Puertas, Jan Salas, Tabatadze, Adilson, Guille Bueno.

## 2026-10-18 · actualización semanal (con investigación previa)
- Método: antes de escribir se contrastaron las listas con partidos jugados, partes oficiales de los clubes y fichas de jugador, porque las listas de lesionados llevan retraso (p. ej. Raphinha seguía como lesionado tras jugar).
- Altas verificadas (`seed/altas.csv`): Raphinha y De Jong (16-10), Antañón (14-10), Sangaré y Cajuste (16-10), Raúl Moro (17-10), Stassin (16-10), Vargas (11-10), Diakhaby (10-10), Koski y Luiz Felipe (10-10). Militão (18-10) y Jesé (16-10) sin confirmar.
- Nuevas lesiones (16 filas): Eric García (cuádriceps dcho.) y Koundé (tobillo izdo.), lesionados el 17-10 en La Cartuja ante el Betis, parte oficial del 18-10; Koke, Cristian Romero, Borja Iglesias, Pablo Durán, Guridi, Miguel Sierra, Kochorashvili, Aimar Oroz (recaída), Beitia, Luiz Felipe (LCA, episodio nuevo), Juan Cruz (recaída), Murillo, Jastin García y Dani Pérez.
- Correcciones a aplicar al cerrar: Raúl Moro era rotura grado I (19-09); Militão posible rotura completa/avulsión; Pablo Durán gemelo interno dcho.
- Pendientes de comprobar: Jan Salas, Guille Bueno, Adilson, Tabatadze, Puertas y Viti (Jornada Perfecta los da como lesión muscular y la BD tiene menisco).
- Limitación: el cargador no lee `altas.csv` ni actualiza episodios existentes; las altas y correcciones requieren la API admin (`PATCH /api/admin/episodios/{id}/alta`, `PUT /api/admin/episodios/{id}`) o ampliar `LesionesLoader`.
- Regla del cargador (corregida el 18-10): las lesiones recuperadas **no se borran**: son el histórico que da sentido a la app (comparar tiempos de baja de la misma lesión). Al confirmar un alta se añade a su fila de `lesiones.csv` la columna 13 `fecha_alta` (aaaa-mm-dd). `LesionesLoader` cierra el episodio abierto equivalente al arrancar (o crea el episodio ya cerrado en una base nueva) y nunca reabre un episodio con el mismo jugador, tipo, lado e inicio. Las 11 altas confirmadas del 18-10 (Koski, Raphinha, De Jong, Antañón, Sangaré, Cajuste, Raúl Moro, Luiz Felipe —la rotura fibrilar; el LCA nuevo sigue abierto—, Stassin, Vargas y Diakhaby) llevan ya su `fecha_alta`. Militão y Jesé siguen abiertos hasta confirmarse.
