$procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' or Name = 'javaw.exe'"
foreach ($p in $procs) {
    Write-Host $p.ProcessId $p.CommandLine
}
