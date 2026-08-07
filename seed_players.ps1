# seed_players.ps1
# Registers 10 test players, sets varied MMR directly in Postgres, then joins them all to the queue.

$gatewayUrl = "http://localhost:8080"
$dbContainer = "matchmaking-postgres"
$dbUser = "gameuser"
$dbName = "matchmaking"

$players = @{
    "seed_p1"  = 950
    "seed_p2"  = 980
    "seed_p3"  = 1000
    "seed_p4"  = 1010
    "seed_p5"  = 1025
    "seed_p6"  = 1040
    "seed_p7"  = 1060
    "seed_p8"  = 1075
    "seed_p9"  = 1090
    "seed_p10" = 1100
}

foreach ($username in $players.Keys) {
    $mmr = $players[$username]
    $password = "test1234"

    Write-Host "Registering $username..." -ForegroundColor Cyan

    $registerBody = @{ username = $username; password = $password } | ConvertTo-Json

    try {
        $registerResponse = Invoke-RestMethod -Uri "$gatewayUrl/auth/register" `
            -Method Post -Body $registerBody -ContentType "application/json"
        Write-Host "  Registered: playerId=$($registerResponse.playerId)" -ForegroundColor Green
    } catch {
        Write-Host "  Register failed (maybe already exists) - continuing to login" -ForegroundColor Yellow
    }

    $sql = "UPDATE players SET mmr = $mmr WHERE username = '$username';"
    docker exec $dbContainer psql -U $dbUser -d $dbName -c "$sql" | Out-Null
    Write-Host "  Set MMR to $mmr" -ForegroundColor Green

    $loginBody = @{ username = $username; password = $password } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Uri "$gatewayUrl/auth/login" `
        -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.token

    $headers = @{ Authorization = "Bearer $token" }
    $joinResponse = Invoke-RestMethod -Uri "$gatewayUrl/queue/join" `
        -Method Post -Headers $headers
    Write-Host "  Joined queue: $joinResponse" -ForegroundColor Magenta
    Write-Host ""
}

Write-Host "All 10 players registered and queued. Watch Matchmaking Service's console." -ForegroundColor Cyan