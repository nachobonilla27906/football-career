$ErrorActionPreference = "Stop"
$destination = "src/main/resources/assets/crests"
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$badges = [ordered]@{
    COV="https://r2.thesportsdb.com/images/media/team/badge/uxyqys1424033798.png"
    HUL="https://r2.thesportsdb.com/images/media/team/badge/fbqqda1601726113.png"
    IPS="https://r2.thesportsdb.com/images/media/team/badge/mdj1ey1634670785.png"
    MAL="https://r2.thesportsdb.com/images/media/team/badge/upqyvr1473502952.png"
    RAC="https://r2.thesportsdb.com/images/media/team/badge/97kkiq1536575158.png"
    FRO="https://r2.thesportsdb.com/images/media/team/badge/a7xa151603170120.png"
    MON="https://r2.thesportsdb.com/images/media/team/badge/bxearg1603170113.png"
    VEN="https://r2.thesportsdb.com/images/media/team/badge/vbiget1781026964.png"
    ELV="https://r2.thesportsdb.com/images/media/team/badge/z079go1677573926.png"
    SCP="https://r2.thesportsdb.com/images/media/team/badge/kddvva1566048058.png"
    S04="https://r2.thesportsdb.com/images/media/team/badge/hnci291621593978.png"
    LEM="https://r2.thesportsdb.com/images/media/team/badge/wjhziv1700145026.png"
    TRO="https://r2.thesportsdb.com/images/media/team/badge/sl5kzg1766617559.png"
}
$csv = [System.Collections.Generic.List[string]]::new()
$csv.AddRange([string[]](Get-Content "src/main/resources/data/team_crests.csv"))
foreach ($entry in $badges.GetEnumerator()) {
    $file = "$destination/$($entry.Key)_128.png"
    Invoke-WebRequest -Uri $entry.Value -OutFile $file
    Copy-Item -LiteralPath $file -Destination "$destination/$($entry.Key)_512.png" -Force
    if (-not ($csv -match "^$($entry.Key),")) {
        $csv.Add("$($entry.Key),assets/crests/$($entry.Key)_128.png,$($entry.Value)")
    }
}
$csv | Set-Content -Encoding utf8 "src/main/resources/data/team_crests.csv"
Write-Output "Imported $($badges.Count) additional team crests from TheSportsDB."
