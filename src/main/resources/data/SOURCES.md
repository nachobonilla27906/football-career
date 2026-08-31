# Fuentes de población deportiva

## Recursos visuales

- Los escudos se usan sin modificar para identificar equipos. El lote principal procede de URLs
  públicas de ESPN mediante el manifiesto reproducible `frertommy/team-logos` de 2026-06-15.
  Los clubes ausentes se consultan mediante endpoints oficiales de TheSportsDB y requieren su
  atribución. Todos los escudos siguen siendo marcas de sus respectivos titulares.
- `assets/backgrounds/stadium-tunnel-beta.png` es un recurso original generado para la interfaz
  cinematográfica Beta 0.1; no contiene marcas de terceros ni texto incrustado.
- Barlow y Barlow Condensed se empaquetan desde el repositorio oficial de Google Fonts.
  Copyright 2017 The Barlow Project Authors; licencia SIL Open Font License 1.1 incluida en
  `assets/fonts/OFL-Barlow.txt`.

Instantánea generada: 2026-08-30. Temporada jugable: 2026/27.

## Clubes participantes

Los 96 participantes se contrastaron con las publicaciones oficiales de Premier
League, LALIGA, Lega Serie A, Bundesliga/DFL y Ligue 1/LFP para 2026/27. El
catálogo incluye 20 clubes ingleses, 20 españoles, 20 italianos, 18 alemanes y
18 franceses.

## Jugadores y atributos

La base de atributos parte del dataset completo FC 26 publicado por EAFC26
DataHub, que documenta como origen `rovnez/fc-26-fifa-26-player-data`:

https://github.com/ismailoksuz/EAFC26-DataHub

Se utilizan nombre real, fecha de nacimiento, nacionalidad, club, posiciones,
pie preferido, altura, media, potencial y atributos técnicos. Las inscripciones
de clubes y fechas de nacimiento se refrescan cuando existe coincidencia con las
listas oficiales del Mundial 2026 de OpenFootball:

https://github.com/openfootball/worldcup.json

El archivo distribuido por el juego es una transformación compacta de 2.673
jugadores, no una copia completa de la fuente.

## Valor y salario

El valor de mercado no se copia de una web comercial. Se deriva de forma
determinista a partir de media, potencial y edad, dando prioridad a la media para
evitar jerarquías absurdas. El salario anual parte del salario semanal del
dataset y se normaliza a la unidad usada por Football Career.

`scripts/generate-top5-dataset.ps1` reproduce la transformación y valida que
cada club tenga al menos 18 jugadores. Los tests añaden controles sobre el total
de clubes, profundidad de plantilla, porteros, nacionalidades y jerarquía de
Courtois dentro del Real Madrid.
