# Auditoría UI/UX — estado real de la Beta 0.1

Actualizada: 31 de agosto de 2026

## Veredicto ejecutivo

La interfaz ya tiene una dirección reconocible: grafito, burdeos, coral, oro, fondos
cinematográficos y escudos reales. Central, Mercado, Clasificación, Plantilla, Alineación,
Resultados, Oficina y Calendario han recibido cambios estructurales. Aun así, todavía no alcanza
el estándar de un juego de gestión deportivo serio.

**Valoración global honesta: 5,5/10.**

Parece una beta funcional con componentes prometedores, no un producto comercial cohesionado.
La debilidad principal ya no es la ausencia de estilo, sino la falta de una dirección de
interacción única: diferentes pantallas emplean modelos mentales, densidades y patrones distintos.
Se perciben varias iteraciones superpuestas.

## Qué merece conservarse

- Identidad grafito/burdeos/coral/oro.
- Navegación superior por áreas y barra lateral contextual.
- Escudos locales y fondos cinematográficos.
- Calendario mensual como calendario físico.
- Clasificación deportiva con escudos y zonas cromáticas.
- Mercado separado entre catálogo, selección y negociación.
- Ficha contextual de Plantilla y campo oscuro de Alineación.
- Revelado progresivo en Oficina, Ajustes, filtros y roles del once.
- Overlays internos en lugar de diálogos nativos.

Son la base del producto. Deben normalizarse y pulirse, no descartarse.

## Problemas sistémicos

### P0 — impiden sensación de producto terminado

1. **La nueva arquitectura visual necesita extenderse a los componentes específicos.** El antiguo
   `app.css` ya fue sustituido por tokens, reglas consolidadas de pantalla y una base común de
   controles. Falta reducir la cantidad de selectores específicos y migrarlos progresivamente a
   componentes reutilizables.

2. **No hay una cuadrícula común.** Conviven tres columnas, anchos completos, `SplitPane`, tabs,
   drawers y paneles anidados sin medidas compartidas. Faltan columnas, alturas y anchos estándar.

3. **JavaFX genérico sigue siendo visible.** Scrollbars, combos, tabs, fechas, popups y listas
   conservan aspecto de aplicación de escritorio. Necesitan geometría y estados propios.

4. **Hay demasiadas cajas.** Agrupar no siempre exige borde, fondo y sombra. Deben utilizarse
   espacio, alineación, tipografía y divisores antes de crear otra tarjeta.

5. **No se empaqueta una tipografía propia.** Se solicitan fuentes del sistema que pueden variar
   entre equipos. Además existen demasiadas combinaciones de tamaño y peso.

6. **La iconografía es provisional.** Los símbolos Unicode cambian según la fuente y tienen
   métricas irregulares. Hace falta un set SVG homogéneo.

7. **Falta respuesta emocional.** Guardar un once, cerrar un fichaje, recibir una oferta o ganar
   produce principalmente cambios de texto. Faltan microanimación, transición y confirmación.

8. **El responsive es reactivo.** Reducir padding no basta. A 1280×720 algunas columnas deben
   apilarse, ciertos datos abreviarse y las acciones importantes permanecer visibles.

## Evaluación por pantalla

| Área | Nota | Diagnóstico | Cambio necesario |
|---|---:|---|---|
| Portada/carga | 6/10 | Buena identidad, composición estática. | Jerarquizar partidas y mostrar progreso de carga real. |
| Shell | 6/10 | Arquitectura válida; ocupa altura y usa iconos provisionales. | SVG, cabecera más limpia y sidebar adaptable. |
| Central | 6/10 | Mejor jerarquía, pero hero, acciones y actividad aún carecen de narrativa. | Próximo evento → decisiones → actualidad; menos texto auxiliar. |
| Plantilla | 6/10 | La ficha contextual ayuda; el catálogo sigue siendo denso. | Agrupar por posición, foto/silueta, cabecera fija y mejor estado visual. |
| Alineación | 6/10 | Campo y tarjetas tienen dirección; editor lateral cargado. | Banquillo horizontal, huecos explícitos y drag preview. |
| Entrenamiento | 4/10 | Tres tarjetas funcionales pero superficiales. | Semana visual, grupos, carga e impacto previsto. |
| Centro médico | 4/10 | Lista y botones de aplicación administrativa. | Casos visuales, recuperación y riesgo de recaída. |
| Calendario | 7/10 | Modelo sólido; escudos y colores ayudan. | Leyenda compacta y detalle lateral en pantalla ancha. |
| Clasificación | 7/10 | Clara, deportiva y con escudos. | Chips V/E/D, cabecera fija y explicación exacta de cupos. |
| Resultados | 6/10 | Las tarjetas superan la tabla. | Agrupar por competición/jornada y priorizar marcador. |
| Mercado | 5/10 | Modelo correcto, ejecución densa; scroll permanente es funcional, no elegante. | Filtros sticky, dossier fijo y flujo responsive. |
| Negociación | 4/10 | Profunda pero todavía parece un formulario por fases. | Escena, progreso, postura del club/agente y campos contextuales. |
| Ventas/ofertas | 5/10 | Mejor separadas, sin visión de pipeline. | Timeline de estados y comparación con precio pedido. |
| Oficina | 5/10 | Tabs evitan columna infinita; interior aún son paneles/formularios. | Resumen ejecutivo, tendencias y finanzas visuales. |
| Ajustes | 5/10 | Ordenados, pero técnicos. | Switches, textos breves y diagnóstico fuera del menú normal. |
| Previa | 5/10 | Exceso de información antes de jugar. | Duelo, claves, bajas y once resumido; detalle bajo demanda. |
| Partido directo | 5/10 | Sigue siendo el punto de mayor carga cognitiva. | Campo como figura, relato secundario y táctica como drawer. |
| Informe | 5/10 | Completo pero demasiado estadístico. | Historia, xG visual, timeline y protagonistas primero. |

## Revisión Gestalt

### Jerarquía y figura-fondo

El próximo partido y el campo funcionan como figura. En otras vistas, paneles con fondo, borde y
sombra compiten al mismo nivel. Cada pantalla debe tener un solo elemento dominante.

### Proximidad

Ha mejorado en Mercado y Plantilla, pero varios botones siguen agrupados por aspecto y no por el
objeto que modifican. Las acciones deben vivir junto al jugador, oferta, lesión u objetivo.

### Similitud

El mismo panel representa información pasiva, formulario, lista y alerta. Hace falta una gramática
clara: `hero`, `workspace`, `card`, `row`, `overlay` y `toast`.

### Continuidad

Fichaje, contrato, alineación y avance cambian de pantalla o bloque sin indicar siempre la fase.
Necesitan progreso visible y una acción primaria persistente.

### Cierre y región común

Se abusa de la región común: casi todo está encerrado. El espacio en blanco debe hacer parte del
trabajo. Menos bordes producirán más claridad y una apariencia más premium.

## Arquitectura visual necesaria

1. Definir tokens únicos de color, tipografía, espacio, radio, sombra, altura y animación.
2. Dividir estilos en `tokens.css`, `shell.css`, `components.css` y archivos por área.
3. Eliminar las reglas verdes y duplicadas, no neutralizarlas al final.
4. Empaquetar una fuente display condensada y una sans de interfaz con cifras tabulares.
5. Crear componentes Java para encabezado, barra contextual, fila de club/jugador, vacío, toast y
   acción sticky.
6. Sustituir Unicode por SVG monocromos con trazo uniforme.
7. Diseñar tres breakpoints reales: 1280×720, 1600×900 y ≥1920×1080.

## Hoja de ruta sin medias tintas

### Fase 1 — saneamiento visual

- Mantener `tokens.css`, `screens.css` y `foundation.css` sin reintroducir capas de parches.
- Instalar fuentes e iconos propios.
- Normalizar scrollbars, tabs, combos, campos, overlays, focus, disabled y tooltips.
- Crear un harness de capturas para las tres resoluciones.

**Salida:** ninguna vista contiene verde heredado, controles sin estilo o scroll horizontal; una
captura aislada se reconoce como parte del mismo juego.

### Fase 2 — cuatro experiencias núcleo

- Central: narrativa diaria y decisiones pendientes.
- Plantilla/Alineación: selección, ficha, once, banquillo y arrastre unificados.
- Mercado/Negociación: descubrimiento → dossier → oferta → contrato → resolución.
- Previa/Partido/Informe: flujo audiovisual continuo.

**Salida:** cada área tiene una acción primaria inequívoca y no más de dos niveles de superficie
visibles simultáneamente.

### Fase 3 — profundidad de gestión

- Oficina, finanzas, cantera y personal.
- Entrenamiento y centro médico.
- Calendario, clasificación y resultados.
- Estados vacíos, tutorial contextual y accesibilidad.

**Salida:** cada decisión explica consecuencias, ofrece feedback y puede completarse sin buscar
acciones fuera del viewport.

### Fase 4 — pulido de juego

- Transiciones de 140–220 ms, skeletons y cargas asíncronas.
- Sonido opcional y feedback de hitos.
- Pruebas visuales y sesiones reales de al menos una temporada.
- Contraste, teclado, escalado y truncado.

## Próximo bloque recomendado

No conviene seguir parcheando vistas individuales. El próximo bloque debe ser el **saneamiento del
sistema visual**: extraer tokens, borrar el tema verde y reglas duplicadas, empaquetar fuentes e
iconos y construir el harness de capturas. Después deben atacarse Mercado y Partido en directo,
las dos experiencias actualmente más alejadas de un juego comercial.
