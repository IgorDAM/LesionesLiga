# LesionesLiga

Web personal con las lesiones de Primera y Segunda División: qué lesión tiene ahora un jugador y cuánto tardan en recuperarse los que sufren la misma.

- **Web en producción:** <https://lesiones-liga-b0d6hpatcxfbcpd7.francecentral-01.azurewebsites.net/> (plan gratuito: la primera carga puede tardar hasta un minuto)
- Documentación completa: [docs/README.md](docs/README.md)
- Publicar gratis en Azure: [docs/AZURE.md](docs/AZURE.md)
- Seguimiento semanal de lesiones: [docs/SEGUIMIENTO.md](docs/SEGUIMIENTO.md)

Arranque rápido (Java 17 + Maven): `mvn spring-boot:run -Dspring-boot.run.profiles=demo` y abre http://localhost:8080.
Los datos de lesiones recogen hechos publicados por prensa y webs oficiales (cada una con su enlace). Sin relación con LaLiga ni con los clubes.
