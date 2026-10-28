<#
 Moovie Flix pairing bridge for people who run their OWN Supabase backend (Docker / self-hosted)
 instead of the desktop app's built-in sync server.

 The TV app pairs by LAN broadcast (UDP 47630) + POST /moovieflix/v1/pair with a 6-digit code.
 A Supabase backend does not speak that protocol, so this script does it for you: it answers
 discovery, shows a code, and on success hands the TV your backend URL, anon key and the
 account to sign in with. Nothing is hardcoded; everything comes from a local properties file.

 Usage (PowerShell):
   powershell -ExecutionPolicy Bypass -File pair-bridge.ps1 -Config C:\path\to\local.properties
 The file needs:
   MOOVIEFLIX_SUPABASE_URL=http://<your-pc-lan-ip>:<port>      (must be reachable from the TV)
   MOOVIEFLIX_SUPABASE_ANON_KEY=<anon key>
   MOOVIEFLIX_LOCAL_AUTO_AUTH_EMAIL=<account email>
   MOOVIEFLIX_LOCAL_AUTO_AUTH_PASSWORD=<account password>
 Allow PowerShell on Private networks if Windows Firewall asks. Stop with Ctrl+C after pairing.
#>
param([Parameter(Mandatory=$true)][string]$Config, [int]$Port = 47630, [int]$Minutes = 15)
$ErrorActionPreference = 'Stop'
$p = @{}
Get-Content $Config | ? { $_ -match '^\s*([A-Z0-9_.]+)\s*=(.*)$' } | % { $p[$Matches[1]] = $Matches[2].Trim().Replace('\:', ':') }
foreach ($k in 'MOOVIEFLIX_SUPABASE_URL','MOOVIEFLIX_SUPABASE_ANON_KEY','MOOVIEFLIX_LOCAL_AUTO_AUTH_EMAIL','MOOVIEFLIX_LOCAL_AUTO_AUTH_PASSWORD') {
  if (-not $p[$k]) { throw "Missing $k in $Config" }
}
$backend = $p['MOOVIEFLIX_SUPABASE_URL'].TrimEnd('/')
if ($backend -match '//(localhost|127\.)') { throw "MOOVIEFLIX_SUPABASE_URL must be a LAN address the TV can reach, not localhost" }
# The TV signs in to this backend right after pairing, so it has to be up now (and whenever the TV is used).
function Test-Backend {
  try { return (Invoke-WebRequest "$backend/.well-known/nuvio" -UseBasicParsing -TimeoutSec 5).StatusCode -lt 500 }
  catch { if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode -lt 500 }; return $false }
}
if (-not (Test-Backend)) {
  $dd = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
  if ((Test-Path $dd) -and -not (Get-Process 'Docker Desktop' -ErrorAction SilentlyContinue)) {
    Write-Host "  Backend not answering; starting Docker Desktop..." -ForegroundColor Yellow; Start-Process $dd
  }
  Write-Host "  Waiting for the backend at $backend (up to 4 min)..."
  $until = (Get-Date).AddMinutes(4)
  while (-not (Test-Backend) -and (Get-Date) -lt $until) { Start-Sleep -Seconds 5 }
  if (-not (Test-Backend)) { throw "The backend at $backend is not reachable. Start it (Docker Desktop / docker compose up) and run this again." }
}
$code = '{0:D6}' -f (Get-Random -Minimum 100000 -Maximum 999999)
$expires = (Get-Date).AddMinutes($Minutes); $fails = 0
$name = $env:COMPUTERNAME -replace '"',''
$udp = New-Object System.Net.Sockets.UdpClient
$udp.Client.SetSocketOption('Socket','ReuseAddress',$true)
$udp.Client.Bind((New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, $Port)))
$tcp = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $Port); $tcp.Start()
Write-Host ""; Write-Host "  Moovie Flix pairing code:  $code" -ForegroundColor Green
Write-Host "  Backend handed to the TV:  $backend"
Write-Host "  On the TV pick '$name' (or type this PC's IP, port $Port). Valid $Minutes min. Ctrl+C to quit."; Write-Host ""
function Send($c, [int]$status, [string]$json) {
  $b = [Text.Encoding]::UTF8.GetBytes($json); $s = $c.GetStream()
  $h = [Text.Encoding]::ASCII.GetBytes("HTTP/1.1 $status X`r`nContent-Type: application/json`r`nContent-Length: $($b.Length)`r`nConnection: close`r`n`r`n")
  $s.Write($h,0,$h.Length); $s.Write($b,0,$b.Length); $s.Flush(); $c.Close()
}
function J($o) { $o | ConvertTo-Json -Compress }
$done = $false
while (-not $done -and (Get-Date) -lt $expires) {
  if ($udp.Available -gt 0) {
    $ep = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any,0)
    $msg = [Text.Encoding]::UTF8.GetString($udp.Receive([ref]$ep)).Trim()
    if ($msg.StartsWith('MOOVIEFLIX_DISCOVER_V1')) {
      $r = [Text.Encoding]::UTF8.GetBytes((J @{service='moovieflix';version=1;port=$Port;name=$name}))
      [void]$udp.Send($r,$r.Length,$ep)
    }
  }
  if ($tcp.Pending()) {
    $c = $tcp.AcceptTcpClient(); $c.ReceiveTimeout = 5000
    try {
      $rd = New-Object IO.StreamReader($c.GetStream(), [Text.Encoding]::UTF8, $false, 4096, $true)
      $line = $rd.ReadLine(); $len = 0
      while (($h = $rd.ReadLine()) -ne '') { if ($h -match '^Content-Length:\s*(\d+)') { $len = [int]$Matches[1] } }
      $buf = New-Object char[] $len; $n = 0; while ($n -lt $len) { $n += $rd.Read($buf,$n,$len-$n) }
      $body = -join $buf
      if ($line -like 'POST /moovieflix/v1/pair*') {
        $got = ''; try { $got = (($body | ConvertFrom-Json).code -replace '\D','') } catch {}
        if ($got -ne $code) {
          $fails++; if ($fails -ge 8) { $expires = Get-Date }
          Send $c 400 (J @{code='MF002';message='Wrong pairing code'})
        } else {
          Send $c 200 (J @{backend_url=$backend; publishable_key=$p['MOOVIEFLIX_SUPABASE_ANON_KEY']; email=$p['MOOVIEFLIX_LOCAL_AUTO_AUTH_EMAIL']; password=$p['MOOVIEFLIX_LOCAL_AUTO_AUTH_PASSWORD']; device_id=[guid]::NewGuid().ToString()})
          Write-Host "  TV paired. It now talks to $backend directly; you can close this window." -ForegroundColor Green
          Write-Host "  Keep this backend running whenever you use the TV (the TV signs in again by itself when it is back)."
          $done = $true
        }
      } else { Send $c 404 (J @{message='not found'}) }
    } catch { try { $c.Close() } catch {} }
  }
  Start-Sleep -Milliseconds 50
}
$tcp.Stop(); $udp.Close()
if (-not $done) { Write-Host "  Code expired. Run the script again for a new one." }
