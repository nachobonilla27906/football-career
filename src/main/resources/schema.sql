PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS seasons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    start_year INTEGER NOT NULL,
    end_year INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    finished INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    birth_date TEXT NOT NULL,
    nationality TEXT NOT NULL,
    position TEXT NOT NULL,
    preferred_foot TEXT NOT NULL,
    overall INTEGER NOT NULL,
    potential INTEGER NOT NULL,
    pace INTEGER NOT NULL,
    shooting INTEGER NOT NULL,
    passing INTEGER NOT NULL,
    dribbling INTEGER NOT NULL,
    defending INTEGER NOT NULL,
    physical INTEGER NOT NULL,
    market_value REAL NOT NULL,
    salary REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    country TEXT NOT NULL,
    stadium_name TEXT NOT NULL,
    stadium_capacity INTEGER NOT NULL,
    reputation INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS leagues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    tier INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS competitions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    tier INTEGER NOT NULL,
    season_id INTEGER NOT NULL,
    league_id INTEGER,

    FOREIGN KEY (season_id)
        REFERENCES seasons(id),

    FOREIGN KEY (league_id)
        REFERENCES leagues(id)
);

CREATE TABLE IF NOT EXISTS competition_teams (
    competition_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,

    PRIMARY KEY (competition_id, team_id),

    FOREIGN KEY (competition_id)
        REFERENCES competitions(id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS player_team (
    player_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT,

    PRIMARY KEY (player_id, team_id, start_date),

    FOREIGN KEY (player_id)
        REFERENCES players(id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    competition_id INTEGER NOT NULL,
    home_team_id INTEGER NOT NULL,
    away_team_id INTEGER NOT NULL,

    date TEXT NOT NULL,

    home_goals INTEGER NOT NULL DEFAULT 0,
    away_goals INTEGER NOT NULL DEFAULT 0,

    played INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY (competition_id)
        REFERENCES competitions(id),

    FOREIGN KEY (home_team_id)
        REFERENCES teams(id),

    FOREIGN KEY (away_team_id)
        REFERENCES teams(id),

    CHECK (home_team_id <> away_team_id),
    CHECK (home_goals >= 0),
    CHECK (away_goals >= 0),
    UNIQUE (competition_id, home_team_id, away_team_id)
);

CREATE TABLE IF NOT EXISTS player_season_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    player_id INTEGER NOT NULL,
    season_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,

    appearances INTEGER NOT NULL DEFAULT 0,
    starts INTEGER NOT NULL DEFAULT 0,
    minutes INTEGER NOT NULL DEFAULT 0,

    goals INTEGER NOT NULL DEFAULT 0,
    assists INTEGER NOT NULL DEFAULT 0,

    yellow_cards INTEGER NOT NULL DEFAULT 0,
    red_cards INTEGER NOT NULL DEFAULT 0,

    average_rating REAL NOT NULL DEFAULT 0.0,

    FOREIGN KEY (player_id)
        REFERENCES players(id),

    FOREIGN KEY (season_id)
        REFERENCES seasons(id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id),

    CHECK (appearances >= 0),
    CHECK (starts >= 0),
    CHECK (minutes >= 0),
    CHECK (goals >= 0),
    CHECK (assists >= 0),
    CHECK (yellow_cards >= 0),
    CHECK (red_cards >= 0),
    CHECK (average_rating >= 0 AND average_rating <= 10)
);

CREATE TABLE IF NOT EXISTS league_standings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    competition_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,

    played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,

    goals_for INTEGER NOT NULL DEFAULT 0,
    goals_against INTEGER NOT NULL DEFAULT 0,

    points INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY (competition_id)
        REFERENCES competitions(id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id),

    UNIQUE (competition_id, team_id),

    CHECK (played >= 0),
    CHECK (wins >= 0),
    CHECK (draws >= 0),
    CHECK (losses >= 0),
    CHECK (goals_for >= 0),
    CHECK (goals_against >= 0),
    CHECK (points >= 0)
);

CREATE TABLE IF NOT EXISTS careers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    manager_name TEXT NOT NULL,

    controlled_team_id INTEGER NOT NULL,
    current_season_id INTEGER NOT NULL,

    current_date TEXT NOT NULL,

    FOREIGN KEY (controlled_team_id)
        REFERENCES teams(id),

    FOREIGN KEY (current_season_id)
        REFERENCES seasons(id)
);
