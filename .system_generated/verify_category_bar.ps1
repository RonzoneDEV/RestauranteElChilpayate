$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{ username = 'admin'; clave = 'admin123' }
$res = Invoke-WebRequest -Uri 'http://localhost:8080/login' -Method Post -Body $loginBody -WebSession $session -UseBasicParsing

# 1. Verificar POS Salon (Mesa 3)
$htmlPos = (Invoke-WebRequest -Uri 'http://localhost:8080/pedidos/mesa/3' -WebSession $session -UseBasicParsing).Content
Write-Host "=== TEST POS SALON (Mesa 3) ==="
$hasChipsBarPos = $htmlPos -match 'category-chips-bar'
$hasPrevBtnPos = $htmlPos -match 'cat-nav-prev'
$hasNextBtnPos = $htmlPos -match 'cat-nav-next'
$hasChipsContainerPos = $htmlPos -match 'id="catChipsContainer"'
$catCountPos = ([regex]::Matches($htmlPos, 'cat-chip')).Count

Write-Host "Has category-chips-bar: $hasChipsBarPos"
Write-Host "Has cat-nav-prev button: $hasPrevBtnPos"
Write-Host "Has cat-nav-next button: $hasNextBtnPos"
Write-Host "Has id catChipsContainer: $hasChipsContainerPos"
Write-Host "Total category chips matches: $catCountPos"

# 2. Verificar Delivery (create)
$htmlDel = (Invoke-WebRequest -Uri 'http://localhost:8080/delivery/create' -WebSession $session -UseBasicParsing).Content
Write-Host "=== TEST DELIVERY CREATE ==="
$hasChipsBarDel = $htmlDel -match 'category-chips-bar'
$hasPrevBtnDel = $htmlDel -match 'cat-nav-prev'
$hasNextBtnDel = $htmlDel -match 'cat-nav-next'
$hasChipsContainerDel = $htmlDel -match 'id="catChipsContainer"'
$catCountDel = ([regex]::Matches($htmlDel, 'cat-chip')).Count

Write-Host "Has category-chips-bar: $hasChipsBarDel"
Write-Host "Has cat-nav-prev button: $hasPrevBtnDel"
Write-Host "Has cat-nav-next button: $hasNextBtnDel"
Write-Host "Has id catChipsContainer: $hasChipsContainerDel"
Write-Host "Total category chips matches: $catCountDel"
