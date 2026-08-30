# Auditoría de calidad del día 10

Actualizada: 2026-08-30

La lista original contiene 134 problemas. El progreso solo aumenta cuando un
problema original queda materialmente resuelto; las mejoras auxiliares y los
tests no suman por separado.

| Área | Cerrados | Parciales | Pendientes | Total |
|---|---:|---:|---:|---:|
| Problemas críticos | 7 | 0 | 0 | 7 |
| Inicio y partidas | 6 | 0 | 0 | 6 |
| Dashboard | 10 | 0 | 0 | 10 |
| Navegación e interfaz | 12 | 0 | 0 | 12 |
| Plantilla y jugadores | 11 | 0 | 0 | 11 |
| Alineación | 12 | 0 | 0 | 12 |
| Partido | 14 | 0 | 0 | 14 |
| Calendario, resultados y clasificación | 13 | 0 | 0 | 13 |
| Mercado | 23 | 0 | 0 | 23 |
| Lógica y profundidad | 15 | 0 | 0 | 15 |
| Calidad técnica y rendimiento | 11 | 0 | 0 | 11 |
| **Total** | **134** | **0** | **0** | **134** |

## Cerrado o materialmente resuelto

- Interfaz desplazable, carga asíncrona, estados de carga y selector de club
  buscable con país, liga, reputación y presupuesto.
- Validación del entrenador, gestión básica de partidas, confirmaciones
  integradas y comunicación del autoguardado.
- Reparación profunda de guardados: fechas ilegibles/fuera de temporada y
  registros parciales de calendario, plantilla, contratos, estado y finanzas.
- Dashboard con posición, forma reciente, estado físico, noticias, bandeja y
  avance semanal con resumen.
- Navegación activa, overlays consistentes y eliminación de ventanas nativas.
- Iconografía consistente en navegación principal/secundaria, conservando
  contadores, estado activo, tooltips y jerarquía visual.
- Los informes de partido regresan a la pantalla desde la que fueron abiertos,
  en vez de enviar siempre al calendario.
- Scroll, pestañas, filtros y jugador seleccionado sobreviven a la navegación y
  al reconstruido de Plantilla/Mercado, restaurándose por identificador estable.
- Plantilla tabular con búsqueda, filtros, ordenación y fichas renovables.
- La ficha traduce cada atributo a su efecto jugable, señala los tres atributos
  clave según la posición y calcula un nivel efectivo comprensible usando forma
  y fitness, con fortalezas y aspectos prioritarios visibles.
- Evolución visual automática con gráficas de media y valor, tendencia acumulada
  y diferencias desde el inicio de la carrera; sus hitos no dependen de abrir la ficha.
- Perfil completo con pierna, altura persistente, posición natural y secundaria,
  moral, rol y minutos esperados; las carreras antiguas migran estos datos al abrirse.
- Campo táctico, tres formaciones, validación posicional, once recomendado y
  comparación de GRL, forma y fitness.
- Capitán y lanzadores de penaltis/córners persistentes y validados contra el
  once; afectan liderazgo, probabilidad de gol y generación de asistencias.
- Once base reutilizable por carrera: al guardar una alineación quedan registrados
  titulares, banquillo, formación, instrucciones, capitán y lanzadores, y el
  siguiente partido parte automáticamente de esa hoja de equipo.
- Guardar la alineación ofrece confirmación persistente con hora, resumen del
  contenido guardado, estado visual y accesible, y bloqueo breve contra dobles clics.
- Previa, pausa, salto al final e informe posterior del partido.
- Partido en directo con narración contextual variable, centro táctico persistente
  y lectura dinámica de impulso, riesgo, marcador y fase del encuentro.
- El directo ya no reproduce un resultado precalculado: minuto, ocasiones, goles,
  tarjetas y estadísticas nacen incrementalmente; el resultado solo se confirma y
  persiste al finalizar, y los cambios tácticos alteran las probabilidades siguientes.
- Campo en directo con dirección de ataque, zonas contextuales y balón animado;
  goles, disciplina y sustituciones desplazan el foco a ubicaciones coherentes.
- Los partidos ajenos conservan el simulador rápido, pero ya producen goleadores,
  asistentes, tarjetas y estadísticas completas coherentes con el marcador; sus
  informes tienen la misma estructura útil sin construir alineaciones pesadas.
- Informes enriquecidos con goles esperados, pases, precisión y entradas, además
  de posesión, tiros, córneres, faltas y tarjetas; se generan también para la IA.
- El informe de partido elimina su N+1 de jugadores: goleadores, asistentes y
  jugador destacado se hidratan con una sola lectura indexada para toda la cronología.
- Simulación y preparación táctica eliminan sus N+1 de estado: cada partido y
  pantalla reutilizan un único mapa de forma, moral y fitness para toda la plantilla.
- Caché de lectura de jugadores por carrera para mercado, informes y partido,
  invalidada automáticamente al crear jugadores o modificar su desarrollo.
- Calendario mensual, filtros, navegación por fechas, tabla clasificatoria real
  y equipo controlado resaltado.
- Resultados en tabla adaptable con columnas reales, equipo controlado resaltado,
  filtros persistentes y apertura del informe mediante doble clic.
- Historial de fichajes y negociaciones en tablas adaptables con dirección,
  jugador, origen, destino, importe, fecha y estado; pantalla extraída de `Main`.
- Mercado con catálogo transferible, scouting global, filtros, shortlist,
  comparación, contraofertas, ventas, salario, duración contractual y cesiones
  de 6/12 meses con retorno automático al club propietario. Los fichajes ya
  incluyen prima, cláusula, rol prometido, reconocimiento médico y hasta tres
  rondas persistentes de negociación.
- Estructuras financieras jugables: pago inicial del 50/75/100%, dos cuotas
  semestrales y prima tras diez apariciones; comprometen presupuesto, vencen al
  avanzar fechas y actualizan la caja real de comprador y vendedor.
- Posturas de club diferenciadas —abierto, negociador, firme, protector o
  intransferible— según economía, reputación, importancia del jugador y puesta
  en venta; alteran precio, margen de contraoferta y explicación visible.
- Agente del jugador con evaluación del paquete completo: salario, prima,
  duración, rol y cláusula producen aceptación, contraoferta concreta o rechazo
  antes de registrar la operación con el club.
- Entrenamiento, objetivos básicos, economía de oficina y renovación de
  contratos desde la interfaz.
- Conversaciones individuales desde la ficha con apoyo, reto o descanso mental;
  modifican moral, forma y fitness y respetan un enfriamiento persistente de
  siete días por jugador y carrera.
- Dificultad persistente e independiente de la asistencia: casual, normal,
  difícil y legendaria modifican la fuerza efectiva del equipo controlado sin
  alterar los partidos neutrales de la IA.
- Identidad persistente del entrenador: táctico mejora la preparación de partido,
  desarrollador potencia la forma en entrenamientos, motivador amplifica las
  conversaciones positivas y generalista permanece neutral.
- Oficina con contratación de ojeadores, informes mensuales de cantera y
  promoción de juveniles con contrato y progresión aislados por carrera.
- Cuerpo técnico contratable con preparador, fisioterapeuta y analista; sus
  niveles afectan entrenamiento, recuperación médica y previa del rival.
- Modo compacto automático para 1280×720, con barras, paneles y alturas de
  mercado/alineación adaptadas al espacio real disponible.
- Prueba JavaFX real a 1280×720: renderiza el shell con CSS, captura un snapshot
  de 1280×720, comprueba límites de botones y ejecuta navegación y salida.
- Portadas, formularios y overlays limitan su tamaño al área útil de la ventana;
  los diálogos incorporan scroll interno para mantener accesibles sus acciones.
- Navegación que conserva el scroll horizontal y vertical independiente de
  cada pantalla, además de los filtros persistentes ya existentes.
- Clasificación y Resultados conservan sus nodos completos al navegar durante la
  misma fecha, evitando reconstruir tablas costosas; la revisión se invalida al
  avanzar el calendario para impedir mostrar información obsoleta.
- Reputación del mánager con historial diario y tendencia real vinculada a la
  confianza de la directiva.
- Historial reciente del Dashboard que combina partidos, traspasos, cesiones,
  entrenamientos y reputación, con índices preparados para carreras extensas.
- Shell, reglas de navegación, Dashboard y controlador de búsqueda del Mercado
  extraídos de `Main.java`, con frontera arquitectónica comprobada por test;
  la clase principal baja de 4.049 a 3.836 líneas.
- El controlador incremental encapsula alineaciones, táctica, eventos, estadísticas
  y persistencia del directo; un test arquitectónico impide que esa pantalla vuelva
  a instanciar directamente sus repositorios.

## Parciales prioritarios

Ninguno de los 134 puntos originales permanece parcial.

## Pendientes de mayor impacto

Ninguno de los 134 puntos originales permanece pendiente. Las ampliaciones futuras
(más ligas, miles de jugadores, competiciones europeas y pulido visual adicional)
formarán una fase nueva y no alteran el cierre de esta auditoría.
