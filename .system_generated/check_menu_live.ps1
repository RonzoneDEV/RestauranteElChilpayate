$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{ username = 'admin'; clave = 'admin123' }
$res = Invoke-WebRequest -Uri 'http://localhost:8080/login' -Method Post -Body $loginBody -WebSession $session -UseBasicParsing
$html = (Invoke-WebRequest -Uri 'http://localhost:8080/pedidos/mesa/3/carrito' -WebSession $session -UseBasicParsing).Content

Write-Host "Length of HTML:" $html.Length
$matchesBrand = [regex]::Matches($html, 'brand-text')
Write-Host "Occurrences of brand-text:" $matchesBrand.Count
$matchClose = [regex]::Match($html, '<button[^>]*sidebar-close-btn[^>]*>')
Write-Host "Sidebar close button tag:" $matchClose.Value
$matchMobile = [regex]::Match($html, '<button[^>]*mobile-menu-btn[^>]*>')
Write-Host "Mobile menu button tag:" $matchMobile.Value

# Check if target/classes has updated files or if devtools reloaded
