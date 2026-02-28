# Build script for TeamDeathmatch plugin
# Sets JAVA_HOME to Java 21 before building

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Plugin JAR is at:" -ForegroundColor Green
    Write-Host "target\TeamDeathmatch-1.0.0.jar" -ForegroundColor Cyan
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
}
