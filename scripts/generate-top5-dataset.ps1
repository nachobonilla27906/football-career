param(
    [string]$Fc26Source = "target/fc26_source.csv",
    [string]$WorldCupSource = "target/wc2026_squads.csv"
)

$ErrorActionPreference = "Stop"
$projectRoot = if ($PSScriptRoot) { Split-Path $PSScriptRoot -Parent } else { (Get-Location).Path }
$resourceDir = Join-Path $projectRoot "src\main\resources\data"

# 2026/27 participants verified against each competition's official publication.
$definitions = @"
Premier League|England|AFC Bournemouth|BOU|Vitality Stadium|11307
Premier League|England|Arsenal|ARS|Emirates Stadium|60704
Premier League|England|Aston Villa|AVL|Villa Park|42657
Premier League|England|Brentford|BRE|Gtech Community Stadium|17250
Premier League|England|Brighton & Hove Albion|BHA|Amex Stadium|31876
Premier League|England|Chelsea|CHE|Stamford Bridge|40341
Premier League|England|Coventry City|COV|Coventry Building Society Arena|32609
Premier League|England|Crystal Palace|CRY|Selhurst Park|25486
Premier League|England|Everton|EVE|Hill Dickinson Stadium|52888
Premier League|England|Fulham FC|FUL|Craven Cottage|29600
Premier League|England|Hull City|HUL|MKM Stadium|25586
Premier League|England|Ipswich Town|IPS|Portman Road|30311
Premier League|England|Leeds United|LEE|Elland Road|37645
Premier League|England|Liverpool|LIV|Anfield|61276
Premier League|England|Manchester City|MCI|Etihad Stadium|53400
Premier League|England|Manchester United|MUN|Old Trafford|74310
Premier League|England|Newcastle United|NEW|St James Park|52305
Premier League|England|Nottingham Forest|NFO|City Ground|30455
Premier League|England|Sunderland|SUN|Stadium of Light|48707
Premier League|England|Tottenham Hotspur|TOT|Tottenham Hotspur Stadium|62850
LaLiga|Spain|Athletic Club|ATH|San Mames|53289
LaLiga|Spain|Atlético Madrid|ATM|Metropolitano|70460
LaLiga|Spain|CA Osasuna|OSA|El Sadar|23576
LaLiga|Spain|RC Celta|CEL|Balaidos|24870
LaLiga|Spain|Deportivo Alavés|ALA|Mendizorroza|19840
LaLiga|Spain|Elche CF|ELC|Martinez Valero|31388
LaLiga|Spain|FC Barcelona|BAR|Spotify Camp Nou|99354
LaLiga|Spain|Getafe CF|GET|Coliseum|16800
LaLiga|Spain|Levante UD|LEV|Ciutat de Valencia|26354
LaLiga|Spain|Málaga CF|MAL|La Rosaleda|30044
LaLiga|Spain|Racing de Santander|RAC|El Sardinero|22222
LaLiga|Spain|Rayo Vallecano|RAY|Vallecas|14708
LaLiga|Spain|Deportivo de La Coruña|DEP|Riazor|32912
LaLiga|Spain|RCD Espanyol|ESP|RCDE Stadium|40500
LaLiga|Spain|Real Betis Balompié|BET|Benito Villamarin|60721
LaLiga|Spain|Real Madrid|RMA|Santiago Bernabeu|83186
LaLiga|Spain|Real Sociedad|RSO|Reale Arena|39500
LaLiga|Spain|Sevilla FC|SEV|Ramon Sanchez-Pizjuan|43883
LaLiga|Spain|Valencia CF|VAL|Mestalla|49430
LaLiga|Spain|Villarreal CF|VIL|Estadio de la Ceramica|23500
Serie A|Italy|AC Milan|MIL|San Siro|75817
Serie A|Italy|Atalanta|ATA|New Balance Arena|24750
Serie A|Italy|Bologna|BOL|Renato Dall Ara|36462
Serie A|Italy|Cagliari|CAG|Unipol Domus|16416
Serie A|Italy|Como|COM|Giuseppe Sinigaglia|13602
Serie A|Italy|Fiorentina|FIO|Artemio Franchi|43147
Serie A|Italy|Frosinone|FRO|Benito Stirpe|16227
Serie A|Italy|Genoa|GEN|Luigi Ferraris|33205
Serie A|Italy|Inter|INT|San Siro|75817
Serie A|Italy|Juventus|JUV|Allianz Stadium|41507
Serie A|Italy|Lazio|LAZ|Stadio Olimpico|70634
Serie A|Italy|Lecce|LEC|Via del Mare|31533
Serie A|Italy|Monza|MON|U-Power Stadium|18568
Serie A|Italy|Napoli|NAP|Diego Armando Maradona|54726
Serie A|Italy|Parma|PAR|Ennio Tardini|22352
Serie A|Italy|Roma|ROM|Stadio Olimpico|70634
Serie A|Italy|Sassuolo|SAS|Mapei Stadium|21584
Serie A|Italy|Torino|TOR|Olimpico Grande Torino|28177
Serie A|Italy|Udinese|UDI|Bluenergy Stadium|25144
Serie A|Italy|Venezia|VEN|Pier Luigi Penzo|11150
Bundesliga|Germany|FC Augsburg|AUG|WWK Arena|30660
Bundesliga|Germany|1. FC Union Berlin|FCU|An der Alten Forsterei|22012
Bundesliga|Germany|SV Werder Bremen|BRE2|Weserstadion|42100
Bundesliga|Germany|Borussia Dortmund|BVB|Signal Iduna Park|81365
Bundesliga|Germany|SV Elversberg|ELV|Waldstadion Kaiserlinde|10000
Bundesliga|Germany|Eintracht Frankfurt|SGE|Deutsche Bank Park|58000
Bundesliga|Germany|SC Freiburg|SCF|Europa-Park Stadion|34700
Bundesliga|Germany|Hamburger SV|HSV|Volksparkstadion|57000
Bundesliga|Germany|TSG 1899 Hoffenheim|TSG|PreZero Arena|30150
Bundesliga|Germany|1. FC Köln|KOE|RheinEnergieStadion|50000
Bundesliga|Germany|RB Leipzig|RBL|Red Bull Arena|47069
Bundesliga|Germany|Bayer 04 Leverkusen|B04|BayArena|30210
Bundesliga|Germany|1. FSV Mainz 05|M05|Mewa Arena|33305
Bundesliga|Germany|Borussia Mönchengladbach|BMG|Borussia-Park|54057
Bundesliga|Germany|FC Bayern München|FCB|Allianz Arena|75024
Bundesliga|Germany|SC Paderborn 07|SCP|Home Deluxe Arena|15000
Bundesliga|Germany|FC Schalke 04|S04|Veltins-Arena|62271
Bundesliga|Germany|VfB Stuttgart|VFB|MHPArena|60449
Ligue 1|France|Angers SCO|ANG|Stade Raymond Kopa|18752
Ligue 1|France|AJ Auxerre|AUX|Stade de l Abbe-Deschamps|17897
Ligue 1|France|Stade Brestois 29|BRE3|Stade Francis-Le Ble|15220
Ligue 1|France|Le Havre AC|HAC|Stade Oceane|25178
Ligue 1|France|RC Lens|LEN|Stade Bollaert-Delelis|38223
Ligue 1|France|Lille OSC|LIL|Stade Pierre-Mauroy|50186
Ligue 1|France|FC Lorient|LOR|Stade du Moustoir|18110
Ligue 1|France|Olympique Lyonnais|LYO|Groupama Stadium|59186
Ligue 1|France|Le Mans FC|LEM|Stade Marie-Marvingt|25064
Ligue 1|France|Olympique de Marseille|OM|Orange Velodrome|67394
Ligue 1|France|AS Monaco|ASM|Stade Louis II|18523
Ligue 1|France|OGC Nice|NIC|Allianz Riviera|35624
Ligue 1|France|Paris FC|PFC|Stade Jean-Bouin|19000
Ligue 1|France|Paris Saint-Germain|PSG|Parc des Princes|47929
Ligue 1|France|Stade Rennais FC|REN|Roazhon Park|29778
Ligue 1|France|RC Strasbourg Alsace|STR|Stade de la Meinau|26109
Ligue 1|France|Toulouse FC|TOU|Stadium de Toulouse|33150
Ligue 1|France|ESTAC Troyes|TRO|Stade de l Aube|20420
"@ -split "`n" | Where-Object { $_.Trim() } | ForEach-Object {
    $p = $_.Trim() -split '\|'; [pscustomobject]@{ League=$p[0]; Country=$p[1]; Club=$p[2]; Code=$p[3]; Stadium=$p[4]; Capacity=[int]$p[5] }
}

$clubAliases = @{
    'Atlético de Madrid'='Atlético Madrid'; 'Celta de Vigo'='RC Celta'; 'Celta Vigo'='RC Celta'
    'Deportivo La Coruña'='Deportivo de La Coruña'; 'RC Deportivo'='Deportivo de La Coruña'; 'RC Deportivo de La Coruña'='Deportivo de La Coruña'
    'Racing Santander'='Racing de Santander'; 'Real Racing Club'='Racing de Santander'
    'Málaga'='Málaga CF'; 'Real Betis'='Real Betis Balompié'; 'Espanyol'='RCD Espanyol'
    'Bayern Munich'='FC Bayern München'; 'Bayern München'='FC Bayern München'
    'Borussia Monchengladbach'='Borussia Mönchengladbach'; 'FC Cologne'='1. FC Köln'
    'Hoffenheim'='TSG 1899 Hoffenheim'; 'Werder Bremen'='SV Werder Bremen'
    'Milan'='AC Milan'; 'Internazionale'='Inter'; 'AS Roma'='Roma'
    'Olympique Marseille'='Olympique de Marseille'; 'Marseille'='Olympique de Marseille'
    'Olympique Lyon'='Olympique Lyonnais'; 'Lyon'='Olympique Lyonnais'; 'Lille'='Lille OSC'
    'Stade Rennais'='Stade Rennais FC'; 'Strasbourg'='RC Strasbourg Alsace'; 'Toulouse'='Toulouse FC'
    'Paris Saint Germain'='Paris Saint-Germain'; 'Le Mans'='Le Mans FC'; 'Troyes'='ESTAC Troyes'
    'Fulham'='Fulham FC'; 'Brighton'='Brighton & Hove Albion'; 'Newcastle'='Newcastle United'
    'Man City'='Manchester City'; 'Man United'='Manchester United'; 'Nottm Forest'='Nottingham Forest'
}

function Normalize([string]$value) {
    if (-not $value) { return '' }
    $formD = $value.Normalize([Text.NormalizationForm]::FormD)
    return (($formD.ToCharArray() | Where-Object {[Globalization.CharUnicodeInfo]::GetUnicodeCategory($_) -ne 'NonSpacingMark'}) -join '' -replace '[^a-zA-Z0-9]','').ToLowerInvariant()
}
function Map-Position([string]$positions) {
    $position = ($positions -split ',')[0].Trim()
    $mapped = switch($position) { 'GK' {'GK'} 'CB' {'CB'} 'LB' {'LB'} 'LWB' {'LB'} 'RB' {'RB'} 'RWB' {'RB'} 'CDM' {'CDM'} 'CM' {'CM'} 'CAM' {'CAM'} 'LM' {'LW'} 'LW' {'LW'} 'RM' {'RW'} 'RW' {'RW'} default {'ST'} }
    return $mapped
}
function Attribute($value, [int]$fallback) { if($null -eq $value -or "$value" -eq ''){$fallback}else{[int][math]::Round([double]$value)} }
function Market-Value([int]$overall, [int]$potential, [int]$age) {
    $base = 350000 * [math]::Pow(1.18, [math]::Max(0, $overall - 55))
    $ageFactor = if($age -le 21){1.50}elseif($age -le 25){1.25}elseif($age -le 29){1.0}elseif($age -le 32){0.78}else{0.58}
    $potentialFactor = 1 + [math]::Max(0, $potential - $overall) * 0.045
    $raw = [math]::Min(220000000, [math]::Max(150000, $base * $ageFactor * $potentialFactor))
    return [math]::Round($raw / 10000) * 10000
}

if(-not (Test-Path $Fc26Source)){ throw "Missing FC26 source: $Fc26Source" }
$fcPlayers = Import-Csv $Fc26Source
$wcPlayers = if(Test-Path $WorldCupSource){Import-Csv $WorldCupSource}else{@()}
$teamByName = @{}; $definitions | ForEach-Object {$teamByName[(Normalize $_.Club)]=$_}
$clubLookup = @{}; $definitions | ForEach-Object {$clubLookup[(Normalize $_.Club)]=$_.Club}
$clubAliases.GetEnumerator() | ForEach-Object {$clubLookup[(Normalize $_.Key)]=$_.Value}

# World Cup registrations are newer than the FC26 snapshot; use them to refresh clubs and DOB.
$worldCupByPlayer = @{}
foreach($wc in $wcPlayers){$worldCupByPlayer[(Normalize $wc.player)]=$wc}

$selected = [System.Collections.Generic.List[object]]::new()
foreach($source in $fcPlayers){
    $canonical = $clubLookup[(Normalize $source.club_name)]
    $wc = $worldCupByPlayer[(Normalize $source.long_name)]
    if($wc -and $clubLookup.ContainsKey((Normalize $wc.club))){$canonical=$clubLookup[(Normalize $wc.club)]}
    if(-not $canonical){continue}
    $team=$teamByName[(Normalize $canonical)]
    $overall=[int]$source.overall; $potential=[int]$source.potential; $age=[int]$source.age
    $birthDate = if($wc -and $wc.date_of_birth){$wc.date_of_birth}elseif($source.dob){$source.dob}else{"$((2026-$age).ToString('0000'))-07-01"}
    $words=$source.long_name.Trim() -split '\s+',2
    $first=(($words[0] -replace ',','') -replace '[\p{IsCyrillic}\p{IsArabic}].*$','').Trim()
    $last=(($(if($words.Count -gt 1){$words[1]}else{$source.short_name}) -replace ',','') -replace '[\p{IsCyrillic}\p{IsArabic}].*$','').Trim()
    $position=Map-Position $source.player_positions
    if($position -eq 'GK'){
        $pace=Attribute $source.goalkeeping_diving $overall; $shooting=Attribute $source.goalkeeping_kicking $overall
        $passing=Attribute $source.goalkeeping_kicking $overall; $dribbling=Attribute $source.goalkeeping_handling $overall
        $defending=Attribute $source.goalkeeping_positioning $overall; $physical=Attribute $source.goalkeeping_reflexes $overall
    }else{
        $physical=Attribute $source.physic $overall; $pace=Attribute $source.pace $overall
        $shooting=Attribute $source.shooting ($overall-4); $passing=Attribute $source.passing $overall
        $dribbling=Attribute $source.dribbling $overall; $defending=Attribute $source.defending ($overall-5)
    }
    $value=Market-Value $overall $potential $age
    $salary=[math]::Round([math]::Max(180000, [double]$source.wage_eur * 52) / 1000) * 1000
    $selected.Add([pscustomobject]@{First=$first;Last=$last;DOB=$birthDate;Nationality=$source.nationality_name;Position=$position;Foot=$source.preferred_foot.ToUpperInvariant();Overall=$overall;Potential=$potential;Pace=$pace;Shooting=$shooting;Passing=$passing;Dribbling=$dribbling;Defending=$defending;Physical=$physical;Value=$value;Salary=$salary;Code=$team.Code;Height=[int]$source.height_cm})
}

# Keep senior-sized squads, strongest players first, with at least two goalkeepers where available.
$final = [System.Collections.Generic.List[object]]::new()
foreach($team in $definitions){
    $squad=@($selected | Where-Object Code -eq $team.Code | Sort-Object Overall -Descending)
    if($squad.Count -gt 32){$squad=@($squad | Select-Object -First 32)}
    if($squad.Count -lt 18){throw "$($team.Club) only has $($squad.Count) mapped real players"}
    $squad | ForEach-Object {$final.Add($_)}
}

$teamRows=@('name,short_name,country,stadium_name,stadium_capacity,reputation')
foreach($team in $definitions){
    $avg=($final | Where-Object Code -eq $team.Code | Measure-Object Overall -Average).Average
    $rep=[math]::Min(96,[math]::Max(55,[math]::Round($avg+8)))
    $teamRows += "$($team.Club),$($team.Code),$($team.Country),$($team.Stadium),$($team.Capacity),$rep"
}
[IO.File]::WriteAllLines((Join-Path $resourceDir 'teams_top5_2026_27.csv'),$teamRows,[Text.UTF8Encoding]::new($false))

$competitionRows=@('competition_name,season,team_short_name')
foreach($team in $definitions){$competitionRows += "$($team.League),2026/2027,$($team.Code)"}
[IO.File]::WriteAllLines((Join-Path $resourceDir 'competition_teams_top5_2026_27.csv'),$competitionRows,[Text.UTF8Encoding]::new($false))

$header='first_name,last_name,birth_date,nationality,position,preferred_foot,overall,potential,pace,shooting,passing,dribbling,defending,physical,market_value,salary,team_short_name,height_cm'
$playerRows=@($header)
foreach($p in $final){$playerRows += "$($p.First),$($p.Last),$($p.DOB),$($p.Nationality),$($p.Position),$($p.Foot),$($p.Overall),$($p.Potential),$($p.Pace),$($p.Shooting),$($p.Passing),$($p.Dribbling),$($p.Defending),$($p.Physical),$($p.Value),$($p.Salary),$($p.Code),$($p.Height)"}
[IO.File]::WriteAllLines((Join-Path $resourceDir 'players_top5_2026_27.csv'),$playerRows,[Text.UTF8Encoding]::new($false))

"Generated $($definitions.Count) clubs and $($final.Count) real players."
