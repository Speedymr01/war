# Build script for TeamDeathmatch plugin
# Use JAVA_HOME provided by GitHub Actions

mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Plugin JAR is at:" -ForegroundColor Green
    Write-Host "target\TeamDeathmatch-1.0.0.jar" -ForegroundColor Cyan
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
}
