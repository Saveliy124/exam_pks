$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$utf8 = [System.Text.UTF8Encoding]::new($false)
& docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Запустите Docker Desktop и повторите команду.' }
if (-not (Test-Path '.env')) {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $password = ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
    [IO.File]::WriteAllLines((Join-Path $PWD '.env'), @('EXAM_DB_NAME=exam_center', 'EXAM_DB_USER=exam_center', "EXAM_DB_PASSWORD=$password", 'EXAM_DB_PORT=55433'), $utf8)
}
$settings = @{}
foreach ($line in (Get-Content '.env' -Encoding UTF8)) {
    if ($line -match '^\s*([A-Z_]+)=(.*)$') { $settings[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
foreach ($key in @('EXAM_DB_NAME','EXAM_DB_USER','EXAM_DB_PASSWORD','EXAM_DB_PORT')) {
    if (-not $settings[$key]) { throw "В .env отсутствует $key" }
}
New-Item -ItemType Directory -Force 'config' | Out-Null
[IO.File]::WriteAllLines((Join-Path $PWD 'config/database.properties'), @(
    "db.url=jdbc:postgresql://127.0.0.1:$($settings.EXAM_DB_PORT)/$($settings.EXAM_DB_NAME)",
    "db.user=$($settings.EXAM_DB_USER)", "db.password=$($settings.EXAM_DB_PASSWORD)"
), $utf8)
& docker compose --env-file .env up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose не запустил PostgreSQL.' }
Get-Content 'sql/schema.sql' -Raw -Encoding UTF8 | & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U $settings.EXAM_DB_USER -d $settings.EXAM_DB_NAME
if ($LASTEXITCODE -ne 0) { throw 'Не удалось применить схему БД.' }
$count = & docker compose exec -T postgres psql -At -U $settings.EXAM_DB_USER -d $settings.EXAM_DB_NAME -c 'SELECT (SELECT count(*) FROM applicants)+(SELECT count(*) FROM examiners)+(SELECT count(*) FROM exam_applications)'
if ($LASTEXITCODE -ne 0) { throw 'Не удалось проверить данные.' }
if ($count.Trim() -eq '0') {
    Get-Content 'sql/seed.sql' -Raw -Encoding UTF8 | & docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U $settings.EXAM_DB_USER -d $settings.EXAM_DB_NAME
    if ($LASTEXITCODE -ne 0) { throw 'Не удалось загрузить учебные данные.' }
}
if (-not $env:JAVA_HOME) { throw 'Укажите JAVA_HOME для JDK 21.' }
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& mvn -B -ntp -DskipTests install
if ($LASTEXITCODE -ne 0) { throw 'Ошибка сборки Maven.' }
& docker compose up -d --build --wait backend
if ($LASTEXITCODE -ne 0) { throw 'Backend не готов. Проверьте docker compose logs backend.' }
$apiPort = if ($settings['EXAM_API_PORT']) { $settings['EXAM_API_PORT'] } else { '8081' }
[IO.File]::WriteAllText((Join-Path $PWD 'config/client.properties'), "api.url=http://127.0.0.1:$apiPort`n", $utf8)
Write-Host 'PostgreSQL и Spring Boot готовы. Запуск клиента: .\run.ps1'
