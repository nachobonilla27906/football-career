param([string]$Source = "target/team-logos-source")

$ErrorActionPreference = "Stop"
$destination = "src/main/resources/assets/crests"
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$aliases = @{
    "AFC Bournemouth"="Bournemouth"; "Brighton & Hove Albion"="Brighton"
    "Leeds United"="Leeds"; "Newcastle United"="Newcastle"
    "Tottenham Hotspur"="Tottenham"; "Atlético Madrid"="Atletico Madrid"
    "CA Osasuna"="Osasuna"; "RC Celta"="Celta Vigo"; "Deportivo Alavés"="Alaves"
    "Elche CF"="Elche"; "FC Barcelona"="Barcelona"; "Getafe CF"="Getafe"
    "Levante UD"="Levante"; "RCD Espanyol"="Espanyol"
    "Real Betis Balompié"="Real Betis"; "Valencia CF"="Valencia"
    "Villarreal CF"="Villarreal"; "Roma"="AS Roma"
    "1. FC Union Berlin"="Union Berlin"; "SV Werder Bremen"="Werder Bremen"
    "TSG 1899 Hoffenheim"="1899 Hoffenheim"; "Bayer 04 Leverkusen"="Bayer Leverkusen"
    "1. FSV Mainz 05"="FSV Mainz 05"; "FC Bayern München"="Bayern München"
    "Angers SCO"="Angers"; "AJ Auxerre"="Auxerre"; "Le Havre AC"="Le Havre"
    "RC Lens"="Lens"; "Lille OSC"="Lille"; "FC Lorient"="Lorient"
    "Olympique Lyonnais"="Lyon"; "Olympique de Marseille"="Marseille"
    "AS Monaco"="Monaco"; "OGC Nice"="Nice"; "Stade Rennais FC"="Rennes"
    "RC Strasbourg Alsace"="Strasbourg"
}
$slugAliases = @{
    "Fulham FC"="fulham"; "Deportivo Alavés"="alaves"
    "Real Betis Balompié"="real-betis"; "Sevilla FC"="sevilla"
    "1. FC Köln"="1-fc-koln"; "Borussia Mönchengladbach"="borussia-monchengladbach"
    "FC Bayern München"="bayern-munchen"; "Toulouse FC"="toulouse"
}
$shortSlugAliases = @{ "ALA"="alaves"; "BET"="real-betis"; "FCB"="bayern-munchen" }
function Normalize-Team([string]$value) {
    $decomposed = $value.Normalize([Text.NormalizationForm]::FormD)
    $plain = -join ($decomposed.ToCharArray() | Where-Object {
        [Globalization.CharUnicodeInfo]::GetUnicodeCategory($_) -ne
            [Globalization.UnicodeCategory]::NonSpacingMark
    })
    return ($plain.ToLowerInvariant() -replace '[^a-z0-9]', '')
}
$manifest = (Get-Content -Raw -Encoding utf8 "$Source/manifest.json" | ConvertFrom-Json).teams |
    Where-Object competition -eq "MSI2026"
$byName = @{}
$manifest | ForEach-Object { $byName[(Normalize-Team $_.team)] = $_ }
$rows = [System.Collections.Generic.List[string]]::new()
$rows.Add("short_name,resource,source")
foreach ($team in Import-Csv "src/main/resources/data/teams_top5_2026_27.csv") {
    $catalogName = if ($aliases.ContainsKey($team.name)) { $aliases[$team.name] } else { $team.name }
    $entry = if ($shortSlugAliases.ContainsKey($team.short_name)) {
        $manifest | Where-Object slug -eq $shortSlugAliases[$team.short_name] | Select-Object -First 1
    } elseif ($slugAliases.ContainsKey($team.name)) {
        $manifest | Where-Object slug -eq $slugAliases[$team.name] | Select-Object -First 1
    } else { $byName[(Normalize-Team $catalogName)] }
    if ($null -eq $entry) { continue }
    $big = $entry.files | Where-Object { $_ -like '*_big_square.png' } | Select-Object -First 1
    $small = $entry.files | Where-Object { $_ -like '*_small_square.png' } | Select-Object -First 1
    Copy-Item -LiteralPath (Join-Path $Source $big) -Destination "$destination/$($team.short_name)_512.png" -Force
    Copy-Item -LiteralPath (Join-Path $Source $small) -Destination "$destination/$($team.short_name)_128.png" -Force
    $rows.Add("$($team.short_name),assets/crests/$($team.short_name)_128.png,$($entry.source_logo)")
}
$rows | Set-Content -Encoding utf8 "src/main/resources/data/team_crests.csv"
Write-Output "Imported $($rows.Count - 1) team crests."
