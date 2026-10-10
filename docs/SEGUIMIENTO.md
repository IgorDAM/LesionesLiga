# Seguimiento semanal

Cada sábado se contrastan las lesiones activas con las fuentes, se anotan las altas probables (hasta que se confirmen) y se añaden los nuevos lesionados.

## 2026-10-10 · carga inicial
- 56 lesiones activas (45 Primera, 11 Segunda), 19 con fecha de inicio verificada en `src/main/resources/seed/lesiones.csv`.
- Fuentes: [FútbolFantasy](https://www.futbolfantasy.com/laliga/lesionados), [Jornada Perfecta Primera](https://www.jornadaperfecta.com/lesionados), [Jornada Perfecta Segunda](https://www.jornadaperfecta.com/segunda/lesionados).
- Límites: fechas de inicio aproximadas; 33 con tipo por revisar; AS, Marca y prensa local no accesibles desde el entorno de trabajo.

## 2026-10-10 · verificación de fechas
- Investigación de la fecha real de inicio con prensa y partes oficiales: 19 fechas verificadas (columna `inicio_exacto`), el resto sigue como aproximada.
- Correcciones de diagnóstico: De Jong (ligamento lateral interno, no cruzado), Asencio (fractura por estrés de tibia, operada el 01-10), Adilson (rotura parcial del LCA), Foulquier retirado (rodilla operada, tipo sin verificar), Ignacio Martín retirado (lesión no confirmada).
- Probables altas a confirmar: Militão, Antañón, Vargas, Diakhaby.
- Lesiones heredadas de 2025/26 a comprobar: Tabatadze, Adilson, Guille Bueno.
