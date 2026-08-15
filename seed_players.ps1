# seed_players.ps1
# Registers 10 test players, sets varied MMR directly in Postgres, joins them to the queue,
# waits for a match to form, then marks all 10 as ready.

$gatewayUrl = "http://localhost:8080"
$dbContainer = "matchmaking-postgres"
$dbUser = "gameuser"
$dbName = "matchmaking"
$uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

$players = @{
    "seed_p1"  = 830
    "seed_p2"  = 860
    "seed_p3"  = 880
    "seed_p4"  = 900
    "seed_p5"  = 915
    "seed_p6"  = 930
    "seed_p7"  = 950
    "seed_p8"  = 965
    "seed_p9"  = 980
    "seed_p10" = 1000
}

# Track each player's token so we can call /ready later
$tokens = @{}

# Capture the latest lobby id BEFORE this run, so we can tell which one is new
$beforeRaw = docker exec $dbContainer psql -U $dbUser -d $dbName -t -c `
    "SELECT id FROM lobbies ORDER BY created_at DESC LIMIT 1;"
$beforeMatch = [regex]::Match($beforeRaw, $uuidPattern)
$beforeLobbyId = if ($beforeMatch.Success) { $beforeMatch.Value } else { "" }

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
    $tokens[$username] = $token   # save for the ready-check step later

    $headers = @{ Authorization = "Bearer $token" }
    $joinResponse = Invoke-RestMethod -Uri "$gatewayUrl/queue/join" `
        -Method Post -Headers $headers
    Write-Host "  Joined queue: $joinResponse" -ForegroundColor Magenta
    Write-Host ""
}

Write-Host "All 10 players registered and queued." -ForegroundColor Cyan
Write-Host "Waiting for MatchmakingScheduler to form the match (polling every 2s)..." -ForegroundColor Cyan

# Poll Postgres until a NEW lobby row shows up (scheduler runs every 5s)
$lobbyId = $null
$maxAttempts = 15
$attempt = 0

while (-not $lobbyId -and $attempt -lt $maxAttempts) {
    Start-Sleep -Seconds 2
    $attempt++

    $rawOutput = docker exec $dbContainer psql -U $dbUser -d $dbName -t -c `
        "SELECT id FROM lobbies ORDER BY created_at DESC LIMIT 1;"
    $match = [regex]::Match($rawOutput, $uuidPattern)
    $latest = if ($match.Success) { $match.Value } else { $null }

    if ($latest -and $latest -ne $beforeLobbyId) {
        $lobbyId = $latest
    } else {
        Write-Host "  Still waiting... (attempt $attempt/$maxAttempts)" -ForegroundColor DarkGray
    }
}

if (-not $lobbyId) {
    Write-Host "No new lobby appeared after $($maxAttempts * 2)s - check Matchmaking Service console." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Lobby formed: $lobbyId" -ForegroundColor Green
Write-Host "Marking all 10 players as ready..." -ForegroundColor Cyan
Write-Host ""

foreach ($username in $players.Keys) {
    $token = $tokens[$username]
    $headers = @{ Authorization = "Bearer $token" }

    try {
        $readyResponse = Invoke-RestMethod -Uri "$gatewayUrl/lobby/$lobbyId/ready" `
            -Method Post -Headers $headers
        Write-Host "  $username ready -> lobby status: $($readyResponse.lobbyStatus)" -ForegroundColor Green
    } catch {
        Write-Host "  $username ready call FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Done. Lobby $lobbyId should now be IN_PROGRESS." -ForegroundColor Cyan
Write-Host "Check the match-started Kafka topic to confirm the event published:" -ForegroundColor DarkGray
Write-Host "  docker exec -it matchmaking-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic match-started --from-beginning" -ForegroundColor DarkGray