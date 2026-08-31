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
    format TEXT NOT NULL DEFAULT 'DOMESTIC_LEAGUE',

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
    stage TEXT NOT NULL DEFAULT 'LEAGUE',
    career_id INTEGER,

    FOREIGN KEY (competition_id)
        REFERENCES competitions(id),

    FOREIGN KEY (home_team_id)
        REFERENCES teams(id),

    FOREIGN KEY (away_team_id)
        REFERENCES teams(id),

    CHECK (home_team_id <> away_team_id),
    CHECK (home_goals >= 0),
    CHECK (away_goals >= 0),
    FOREIGN KEY (career_id) REFERENCES careers(id)
);

CREATE TABLE IF NOT EXISTS initial_player_team (
    player_id INTEGER PRIMARY KEY,
    team_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS career_player_team (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT,
    PRIMARY KEY (career_id, player_id, team_id, start_date),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS career_match_states (
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    home_goals INTEGER NOT NULL DEFAULT 0,
    away_goals INTEGER NOT NULL DEFAULT 0,
    played INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (career_id, match_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    CHECK (home_goals >= 0),
    CHECK (away_goals >= 0),
    CHECK (played IN (0, 1))
);

CREATE TABLE IF NOT EXISTS european_ties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    competition_id INTEGER NOT NULL,
    stage TEXT NOT NULL,
    bracket_order INTEGER NOT NULL,
    home_team_id INTEGER NOT NULL,
    away_team_id INTEGER NOT NULL,
    winner_team_id INTEGER,
    UNIQUE(career_id, competition_id, stage, bracket_order),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (competition_id) REFERENCES competitions(id),
    FOREIGN KEY (home_team_id) REFERENCES teams(id),
    FOREIGN KEY (away_team_id) REFERENCES teams(id),
    FOREIGN KEY (winner_team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS match_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    secondary_player_id INTEGER,
    minute INTEGER NOT NULL,
    type TEXT NOT NULL,
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (secondary_player_id) REFERENCES players(id),
    CHECK (minute BETWEEN 1 AND 120),
    CHECK (type IN ('GOAL', 'YELLOW_CARD', 'RED_CARD', 'SUBSTITUTION'))
);

CREATE TABLE IF NOT EXISTS match_team_stats (
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    possession INTEGER NOT NULL,
    shots INTEGER NOT NULL,
    shots_on_target INTEGER NOT NULL,
    corners INTEGER NOT NULL,
    fouls INTEGER NOT NULL,
    yellow_cards INTEGER NOT NULL,
    red_cards INTEGER NOT NULL,
    expected_goals REAL NOT NULL DEFAULT 0,
    passes INTEGER NOT NULL DEFAULT 0,
    pass_accuracy INTEGER NOT NULL DEFAULT 0,
    tackles INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (match_id, team_id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    CHECK (possession BETWEEN 0 AND 100),
    CHECK (shots >= 0),
    CHECK (shots_on_target BETWEEN 0 AND shots),
    CHECK (corners >= 0),
    CHECK (fouls >= 0),
    CHECK (yellow_cards >= 0),
    CHECK (red_cards >= 0)
);

CREATE TABLE IF NOT EXISTS match_lineups (
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    position_order INTEGER NOT NULL,
    PRIMARY KEY (match_id, team_id, player_id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (role IN ('STARTER', 'SUBSTITUTE')),
    CHECK (position_order >= 0),
    UNIQUE (match_id, team_id, role, position_order)
);

CREATE TABLE IF NOT EXISTS match_tactics (
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    formation TEXT NOT NULL DEFAULT '4-3-3',
    mentality TEXT NOT NULL DEFAULT 'BALANCED',
    pressing TEXT NOT NULL DEFAULT 'MEDIUM',
    tempo TEXT NOT NULL DEFAULT 'NORMAL',
    PRIMARY KEY (match_id, team_id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    CHECK (formation IN ('4-3-3', '4-2-3-1', '4-4-2'))
);

CREATE TABLE IF NOT EXISTS career_match_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    secondary_player_id INTEGER,
    minute INTEGER NOT NULL,
    type TEXT NOT NULL,
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    CHECK (minute BETWEEN 1 AND 120)
);

CREATE TABLE IF NOT EXISTS career_match_team_stats (
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    possession INTEGER NOT NULL,
    shots INTEGER NOT NULL,
    shots_on_target INTEGER NOT NULL,
    corners INTEGER NOT NULL,
    fouls INTEGER NOT NULL,
    yellow_cards INTEGER NOT NULL,
    red_cards INTEGER NOT NULL,
    expected_goals REAL NOT NULL DEFAULT 0,
    passes INTEGER NOT NULL DEFAULT 0,
    pass_accuracy INTEGER NOT NULL DEFAULT 0,
    tackles INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (career_id, match_id, team_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE IF NOT EXISTS career_match_lineups (
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    position_order INTEGER NOT NULL,
    PRIMARY KEY (career_id, match_id, team_id, player_id),
    UNIQUE (career_id, match_id, team_id, role, position_order),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE IF NOT EXISTS career_match_tactics (
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    formation TEXT NOT NULL DEFAULT '4-3-3',
    mentality TEXT NOT NULL DEFAULT 'BALANCED',
    pressing TEXT NOT NULL DEFAULT 'MEDIUM',
    tempo TEXT NOT NULL DEFAULT 'NORMAL',
    PRIMARY KEY (career_id, match_id, team_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE IF NOT EXISTS match_roles (
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    captain_id INTEGER NOT NULL,
    penalty_taker_id INTEGER NOT NULL,
    corner_taker_id INTEGER NOT NULL,
    PRIMARY KEY (match_id, team_id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE IF NOT EXISTS career_match_roles (
    career_id INTEGER NOT NULL,
    match_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    captain_id INTEGER NOT NULL,
    penalty_taker_id INTEGER NOT NULL,
    corner_taker_id INTEGER NOT NULL,
    PRIMARY KEY (career_id, match_id, team_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE IF NOT EXISTS career_team_sheets (
    career_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    formation TEXT NOT NULL,
    mentality TEXT NOT NULL,
    pressing TEXT NOT NULL,
    tempo TEXT NOT NULL,
    captain_id INTEGER NOT NULL,
    penalty_taker_id INTEGER NOT NULL,
    corner_taker_id INTEGER NOT NULL,
    PRIMARY KEY (career_id, team_id),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS career_team_sheet_players (
    career_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    position_order INTEGER NOT NULL,
    PRIMARY KEY (career_id, team_id, player_id),
    UNIQUE (career_id, team_id, role, position_order),
    FOREIGN KEY (career_id, team_id) REFERENCES career_team_sheets(career_id, team_id)
        ON DELETE CASCADE,
    CHECK (role IN ('STARTER', 'SUBSTITUTE'))
);

CREATE TABLE IF NOT EXISTS career_shortlist (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    added_date TEXT NOT NULL,
    PRIMARY KEY (career_id, player_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE TABLE IF NOT EXISTS training_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    session_date TEXT NOT NULL,
    training_type TEXT NOT NULL,
    UNIQUE (career_id, session_date),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    CHECK (training_type IN ('RECOVERY', 'BALANCED', 'INTENSIVE'))
);

CREATE TABLE IF NOT EXISTS contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    salary REAL NOT NULL,
    signing_bonus REAL NOT NULL DEFAULT 0,
    release_clause REAL,
    squad_role TEXT NOT NULL DEFAULT 'ROTATION',
    active INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    CHECK (salary >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS one_active_contract_per_player
ON contracts(player_id) WHERE active = 1;

CREATE TABLE IF NOT EXISTS career_contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    salary REAL NOT NULL,
    signing_bonus REAL NOT NULL DEFAULT 0,
    release_clause REAL,
    squad_role TEXT NOT NULL DEFAULT 'ROTATION',
    active INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS one_active_career_contract_per_player
ON career_contracts(career_id, player_id) WHERE active = 1;

CREATE TABLE IF NOT EXISTS player_state (
    player_id INTEGER PRIMARY KEY,
    form INTEGER NOT NULL DEFAULT 50,
    morale INTEGER NOT NULL DEFAULT 50,
    fitness INTEGER NOT NULL DEFAULT 100,
    unavailable_until TEXT,
    unavailable_reason TEXT,
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (form BETWEEN 0 AND 100),
    CHECK (morale BETWEEN 0 AND 100),
    CHECK (fitness BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS career_player_state (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    form INTEGER NOT NULL DEFAULT 50,
    morale INTEGER NOT NULL DEFAULT 50,
    fitness INTEGER NOT NULL DEFAULT 100,
    unavailable_until TEXT,
    unavailable_reason TEXT,
    PRIMARY KEY (career_id, player_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (form BETWEEN 0 AND 100),
    CHECK (morale BETWEEN 0 AND 100),
    CHECK (fitness BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS career_player_development (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    overall INTEGER NOT NULL,
    pace INTEGER NOT NULL,
    shooting INTEGER NOT NULL,
    passing INTEGER NOT NULL,
    dribbling INTEGER NOT NULL,
    defending INTEGER NOT NULL,
    physical INTEGER NOT NULL,
    PRIMARY KEY (career_id, player_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE TABLE IF NOT EXISTS career_player_progress_history (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    snapshot_date TEXT NOT NULL,
    overall INTEGER NOT NULL,
    market_value REAL NOT NULL,
    PRIMARY KEY (career_id, player_id, snapshot_date),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE TABLE IF NOT EXISTS club_finances (
    team_id INTEGER PRIMARY KEY,
    transfer_budget REAL NOT NULL,
    wage_budget REAL NOT NULL,
    current_wage_spend REAL NOT NULL DEFAULT 0,
    balance REAL NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams(id),
    CHECK (transfer_budget >= 0),
    CHECK (wage_budget >= 0),
    CHECK (current_wage_spend >= 0)
);

CREATE TABLE IF NOT EXISTS career_club_finances (
    career_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    transfer_budget REAL NOT NULL,
    wage_budget REAL NOT NULL,
    current_wage_spend REAL NOT NULL DEFAULT 0,
    balance REAL NOT NULL,
    PRIMARY KEY (career_id, team_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS player_market_status (
    player_id INTEGER PRIMARY KEY,
    status TEXT NOT NULL DEFAULT 'NOT_LISTED',
    asking_price REAL,
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (status IN ('NOT_LISTED', 'TRANSFER_LISTED')),
    CHECK (asking_price IS NULL OR asking_price > 0)
);

CREATE TABLE IF NOT EXISTS career_player_market_status (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'NOT_LISTED',
    asking_price REAL,
    PRIMARY KEY (career_id, player_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (status IN ('NOT_LISTED', 'TRANSFER_LISTED'))
);

CREATE TABLE IF NOT EXISTS transfer_offers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER,
    player_id INTEGER NOT NULL,
    buying_team_id INTEGER NOT NULL,
    selling_team_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    offer_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    counter_amount REAL,
    resolution_reason TEXT,
    upfront_percent INTEGER NOT NULL DEFAULT 100,
    appearance_bonus REAL NOT NULL DEFAULT 0,
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (buying_team_id) REFERENCES teams(id),
    FOREIGN KEY (selling_team_id) REFERENCES teams(id),
    CHECK (buying_team_id <> selling_team_id),
    CHECK (amount > 0),
    CHECK (counter_amount IS NULL OR counter_amount > 0),
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'COMPLETED'))
);

CREATE TABLE IF NOT EXISTS transfer_obligations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER,
    offer_id INTEGER NOT NULL,
    debtor_team_id INTEGER NOT NULL,
    creditor_team_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    due_date TEXT,
    condition_type TEXT NOT NULL,
    condition_value INTEGER NOT NULL DEFAULT 0,
    paid INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (offer_id) REFERENCES transfer_offers(id) ON DELETE CASCADE,
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (condition_type IN ('DATE', 'APPEARANCES'))
);

CREATE TABLE IF NOT EXISTS transfer_negotiation_rounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER,
    offer_id INTEGER NOT NULL,
    round_number INTEGER NOT NULL,
    proposed_by TEXT NOT NULL,
    amount REAL NOT NULL,
    created_date TEXT NOT NULL,
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (offer_id) REFERENCES transfer_offers(id) ON DELETE CASCADE,
    UNIQUE (offer_id, round_number, proposed_by),
    CHECK (proposed_by IN ('BUYER', 'SELLER')),
    CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS transfers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER,
    player_id INTEGER NOT NULL,
    from_team_id INTEGER NOT NULL,
    to_team_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    transfer_date TEXT NOT NULL,
    season_id INTEGER NOT NULL,
    offer_id INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (from_team_id) REFERENCES teams(id),
    FOREIGN KEY (to_team_id) REFERENCES teams(id),
    FOREIGN KEY (season_id) REFERENCES seasons(id),
    FOREIGN KEY (offer_id) REFERENCES transfer_offers(id),
    CHECK (from_team_id <> to_team_id),
    CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS career_loans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    parent_team_id INTEGER NOT NULL,
    borrowing_team_id INTEGER NOT NULL,
    fee REAL NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (parent_team_id) REFERENCES teams(id),
    FOREIGN KEY (borrowing_team_id) REFERENCES teams(id),
    CHECK (parent_team_id <> borrowing_team_id),
    CHECK (fee >= 0),
    CHECK (status IN ('ACTIVE', 'RETURNED'))
);

CREATE TABLE IF NOT EXISTS career_scouts (
    career_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    quality INTEGER NOT NULL,
    hired_date TEXT NOT NULL,
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (quality BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS career_staff (
    career_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    name TEXT NOT NULL,
    level INTEGER NOT NULL,
    hired_date TEXT NOT NULL,
    PRIMARY KEY (career_id, role),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (role IN ('COACH', 'PHYSIO', 'ANALYST')),
    CHECK (level BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS career_manager_reputation (
    career_id INTEGER NOT NULL,
    snapshot_date TEXT NOT NULL,
    score INTEGER NOT NULL,
    PRIMARY KEY (career_id, snapshot_date),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (score BETWEEN 1 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_career_match_states_activity
ON career_match_states(career_id, played, match_id);

CREATE INDEX IF NOT EXISTS idx_transfers_career_date
ON transfers(career_id, transfer_date);

CREATE INDEX IF NOT EXISTS idx_career_loans_activity
ON career_loans(career_id, start_date);

CREATE INDEX IF NOT EXISTS idx_training_sessions_activity
ON training_sessions(career_id, session_date);

CREATE TABLE IF NOT EXISTS career_youth_candidates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    nationality TEXT NOT NULL,
    position TEXT NOT NULL,
    preferred_foot TEXT NOT NULL,
    height_cm INTEGER NOT NULL DEFAULT 180,
    secondary_position TEXT,
    birth_date TEXT NOT NULL,
    overall INTEGER NOT NULL,
    potential INTEGER NOT NULL,
    report_date TEXT NOT NULL,
    promoted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (promoted IN (0, 1))
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

CREATE TABLE IF NOT EXISTS career_preferences (
    career_id INTEGER PRIMARY KEY,
    stop_at_match INTEGER NOT NULL DEFAULT 1,
    stop_on_offer INTEGER NOT NULL DEFAULT 1,
    stop_on_fatigue INTEGER NOT NULL DEFAULT 1,
    assistance_level TEXT NOT NULL DEFAULT 'GUIDED',
    difficulty TEXT NOT NULL DEFAULT 'NORMAL',
    manager_identity TEXT NOT NULL DEFAULT 'GENERALIST',
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    CHECK (assistance_level IN ('GUIDED', 'STANDARD', 'EXPERT')),
    CHECK (difficulty IN ('CASUAL', 'NORMAL', 'HARD', 'LEGENDARY')),
    CHECK (manager_identity IN ('GENERALIST', 'TACTICIAN', 'DEVELOPER', 'MOTIVATOR'))
);

CREATE TABLE IF NOT EXISTS medical_treatments (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    treatment_date TEXT NOT NULL,
    treatment_type TEXT NOT NULL,
    PRIMARY KEY (career_id, player_id, treatment_date),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (treatment_type IN ('REHAB', 'SPECIALIST'))
);

CREATE TABLE IF NOT EXISTS player_conversations (
    career_id INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    conversation_date TEXT NOT NULL,
    approach TEXT NOT NULL,
    morale_change INTEGER NOT NULL,
    form_change INTEGER NOT NULL,
    fitness_change INTEGER NOT NULL,
    PRIMARY KEY (career_id, player_id, conversation_date),
    FOREIGN KEY (career_id) REFERENCES careers(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players(id),
    CHECK (approach IN ('SUPPORT', 'CHALLENGE', 'REST'))
);

CREATE TABLE IF NOT EXISTS career_player_season_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    career_id INTEGER NOT NULL,
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
    UNIQUE (career_id, player_id, season_id),
    FOREIGN KEY (career_id) REFERENCES careers(id),
    FOREIGN KEY (player_id) REFERENCES players(id),
    FOREIGN KEY (season_id) REFERENCES seasons(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);
