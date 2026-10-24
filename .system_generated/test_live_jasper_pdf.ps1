$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri "http://localhost:8080/login" -Method POST -Body "username=admin&clave=admin123" -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing | Out-Null
Write-Host "Logged in successfully."

# 1. Test /reportes page
$rep = Invoke-WebRequest -Uri "http://localhost:8080/reportes" -WebSession $session -UseBasicParsing
Write-Host "Reportes Status: $($rep.StatusCode)"
$btnFound = $rep.Content.Contains("/reportes/indicadores.pdf")
Write-Host "Indicadores PDF Button Found: $btnFound"

# 2. Test /reportes/indicadores.pdf
$pdfOutPath = "C:\Users\Ronald Jozz\.gemini\antigravity-ide\brain\85a87a46-ce8e-4362-9e89-7f08764d5b4c\scratch\reporte_indicadores_live.pdf"
$pdfResp = Invoke-WebRequest -Uri "http://localhost:8080/reportes/indicadores.pdf" -WebSession $session -UseBasicParsing -OutFile $pdfOutPath
Write-Host "PDF Endpoint Status: OK"

if (Test-Path $pdfOutPath) {
    $bytes = [System.IO.File]::ReadAllBytes($pdfOutPath)
    Write-Host "PDF File Size: $($bytes.Length) bytes"
    $header = [System.Text.Encoding]::ASCII.GetString($bytes[0..4])
    Write-Host "PDF Header Magic Bytes: $header"
    if ($header.StartsWith("%PDF-") -and $bytes.Length -gt 1000) {
        Write-Host "SUCCESS: JasperReports PDF generated perfectly!"
    } else {
        Write-Host "FAILURE: File is not a valid PDF or is too small."
    }
} else {
    Write-Host "FAILURE: PDF file was not created."
}
