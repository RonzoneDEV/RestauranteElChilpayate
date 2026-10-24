$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-WebRequest -Uri "http://localhost:8080/login" -Method POST -Body "username=admin&clave=admin123" -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing

$rep = Invoke-WebRequest -Uri "http://localhost:8080/reportes" -WebSession $session -UseBasicParsing
Write-Host "StatusCode: $($rep.StatusCode)"

$checks = @(
    "Alita Más Vendida",
    "Salsa Favorita",
    "Mozo Más Productivo",
    "Hora Punta",
    "Especial de Alitas",
    "Rendimiento y Productividad de Mozos",
    "Top 10 Platos Más Vendidos",
    "Curva de Demanda por Franja Horaria"
)

foreach ($item in $checks) {
    $found = $rep.Content.Contains($item)
    Write-Host "$item : $found"
}

[System.IO.File]::WriteAllText("C:\Users\Ronald Jozz\.gemini\antigravity-ide\brain\85a87a46-ce8e-4362-9e89-7f08764d5b4c\scratch\live_reportes.html", $rep.Content, [System.Text.Encoding]::UTF8)
Write-Host "Saved live reportes HTML."
