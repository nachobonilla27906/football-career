# Squad data sources

Snapshot date: 2026-08-27.

## Premier League

`players_premier_league.csv` contains the available first-team and registered
players for Arsenal, Liverpool, Manchester City and Manchester United from the
open `vaastav/Fantasy-Premier-League` 2026/27 dataset:

https://github.com/vaastav/Fantasy-Premier-League/tree/master/data/2026-27

Names, dates of birth and club registrations come from that snapshot. The
source does not provide nationality, preferred foot or Football Career's custom
attributes. Nationality is therefore temporarily `Unknown`, preferred foot is
a neutral placeholder, and ratings/value/salary are generated specifically for
this learning project. They are not copied from EA Sports FC or Football
Manager.

Players without a date of birth in the source are excluded until that fact can
be verified.

## Other top-five leagues

`players_top5_2025_26.csv` contains the 2025/26 squads for the eleven Spanish,
Italian, German and French clubs currently represented by the project. The
source is Hubert Sidorowicz's MIT-licensed Football Players Stats dataset,
derived from FBref:

https://www.kaggle.com/datasets/hubertsidorowicz/football-players-stats-2025-2026

This source only contains the birth year. `07-01` is used as an explicitly
provisional technical date until exact birth dates are enriched. Names,
nationality, broad position and 2025/26 squad come from the source. Ratings,
potential, preferred foot, value and salary are Football Career estimates.
