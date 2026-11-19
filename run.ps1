$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not $env:JAVA_HOME) { throw "Укажите JAVA_HOME для установленного JDK 21." }
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$javaVersion = (& java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "21\.') { throw "Для КР2 требуется JDK 21." }
if (-not $env:EXAM_API_URL -and (Test-Path 'config/client.properties')) {
    $url = Get-Content 'config/client.properties' | Where-Object { $_ -match '^api.url=' } | Select-Object -First 1
    if ($url) { $env:EXAM_API_URL = $url.Substring(8) }
}
& mvn -B -ntp -DskipTests -pl common,desktop -am install
if ($LASTEXITCODE -ne 0) { throw 'Ошибка сборки клиента.' }
& mvn -f desktop/pom.xml javafx:run @args
exit $LASTEXITCODE
