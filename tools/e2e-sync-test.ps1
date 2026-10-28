<#
 Headless end-to-end test of the Moovie Flix embedded sync server with a simulated desktop app and
 a simulated TV app. Mirrors the request shapes used by MoovieFlix-desktop and MoovieFlix-tv.
 Usage: e2e-sync-test.ps1 -Base http://127.0.0.1:47699 -DataDir C:\temp\mf-e2e\server -Phase all|after-restart
#>
param([string]$Base = 'http://127.0.0.1:47699', [string]$DataDir = 'C:\temp\mf-e2e\server', [string]$Phase = 'all', [string]$StateFile = 'C:\temp\mf-e2e\tv-state.json')
$ErrorActionPreference = 'Stop'
$script:pass = 0; $script:fail = 0
function Check([string]$name, [bool]$ok, $detail = '') { if ($ok) { $script:pass++; Write-Host "PASS  $name" -ForegroundColor Green } else { $script:fail++; Write-Host "FAIL  $name  $detail" -ForegroundColor Red } }
function J($o) { $o | ConvertTo-Json -Depth 20 -Compress }
$secrets = Get-Content "$DataDir\server-secrets.json" -Raw | ConvertFrom-Json
$anon = $secrets.anon_key
function Api([string]$method, [string]$path, $body = $null, [string]$token = $null, [hashtable]$extra = @{}) {
  $h = @{ apikey = $anon }; if ($token) { $h.Authorization = "Bearer $token" } else { $h.Authorization = "Bearer $anon" }
  foreach ($k in $extra.Keys) { $h[$k] = $extra[$k] }
  $req = @{ Method = $method; Uri = "$Base$path"; Headers = $h; ContentType = 'application/json' }
  if ($null -ne $body) { $req.Body = [Text.Encoding]::UTF8.GetBytes((J $body)) }
  $resp = Invoke-RestMethod @req; $resp
}
function Rpc([string]$fn, $body, [string]$token) { Api POST "/rest/v1/rpc/$fn" $body $token }
function SignIn([string]$email, [string]$pw) { Api POST '/auth/v1/token?grant_type=password' @{ email = $email; password = $pw } }
function Refresh([string]$rt) { Api POST '/auth/v1/token?grant_type=refresh_token' @{ refresh_token = $rt } }

if ($Phase -eq 'all') {
  # ---------------- desktop: owner account, existing data ----------------
  $d = SignIn $secrets.owner_email $secrets.owner_password
  $DTok = $d.access_token
  Check 'desktop signs in as owner' ($d.user.id -eq $secrets.owner_id) (J $d.user)
  Rpc sync_push_profiles @{ p_profiles = @(@{ profile_index = 1; name = 'Main'; avatar_color_hex = '#0060F8'; avatar_id = 'avatar_aang' }, @{ profile_index = 2; name = 'Kids'; avatar_color_hex = '#FF0000'; uses_primary_addons = $true }) } $DTok | Out-Null
  Rpc set_profile_pin @{ p_profile_id = 2; p_pin = '1234' } $DTok | Out-Null
  $addons = @(@{ url = 'https://catalog.example/manifest.json'; name = ''; enabled = $true; sort_order = 0 }, @{ url = 'https://subs.example/manifest.json'; name = 'Subs'; enabled = $false; sort_order = 1 }, @{ url = 'https://streams.example/abc/manifest.json'; name = ''; enabled = $true; sort_order = 2 })
  Rpc sync_push_addons @{ p_profile_id = 1; p_addons = $addons; p_origin_client_id = 'desktop-test' } $DTok | Out-Null
  Rpc sync_push_plugins @{ p_profile_id = 1; p_plugins = @(@{ url = 'https://repo.example/plugins.json'; name = 'Repo'; enabled = $true; sort_order = 0; repo_type = 'nuvio' }) } $DTok | Out-Null
  Rpc sync_push_library_items @{ p_profile_id = 1; p_items = @(@{ content_id = 'tt0133093'; content_type = 'movie'; name = 'The Matrix'; poster = 'https://img.example/m.jpg'; poster_shape = 'POSTER'; genres = @('Action'); added_at = 1761400000000 }) } $DTok | Out-Null
  Rpc sync_push_watch_progress @{ p_profile_id = 1; p_entries = @(@{ content_id = 'tt0944947'; content_type = 'series'; video_id = 'tt0944947:1:2'; season = 1; episode = 2; position = 600000; duration = 3600000; last_watched = 1761500000000; progress_key = 'tt0944947_s1e2' }) } $DTok | Out-Null
  Rpc sync_push_watched_items @{ p_profile_id = 1; p_items = @(@{ content_id = 'tt0111161'; content_type = 'movie'; title = 'Shawshank'; watched_at = 1761300000000 }) } $DTok | Out-Null
  Rpc sync_push_collections @{ p_profile_id = 1; p_collections_json = @(@{ id = 'c1'; name = 'Favourites'; items = @('tt0133093') }) } $DTok | Out-Null
  Rpc sync_push_home_catalog_settings @{ p_profile_id = 1; p_platform = 'tv'; p_settings_json = @{ rows = @('popular', 'trending') } } $DTok | Out-Null
  Rpc sync_push_media_servers @{ p_profile_id = 1; p_servers = @(@{ id = 's1'; type = 'jellyfin'; url = 'http://nas.local:8096'; name = 'NAS' }) } $DTok | Out-Null
  Rpc sync_push_provider_credentials @{ p_profile_id = 1; p_credentials = @(@{ provider = 'realdebrid'; credential_json = @{ api_key = 'test-key' } }) } $DTok | Out-Null
  Api POST '/rest/v1/moovieflix_library_sources?on_conflict=user_id' @{ user_id = $secrets.owner_id; sources = @(@{ name = 'PC: TEST'; url = 'http://10.0.0.5:47631/tok/' }) } $DTok @{ Prefer = 'resolution=merge-duplicates' } | Out-Null

  # ---------------- TV: discovery + pairing ----------------
  $wk = Invoke-RestMethod "$Base/.well-known/nuvio"
  Check 'discovery doc has key + pairing flag' ($wk.publishable_key -eq $anon -and $wk.moovieflix_pairing)
  $udp = New-Object System.Net.Sockets.UdpClient; $udp.Client.ReceiveTimeout = 3000
  $probe = [Text.Encoding]::UTF8.GetBytes('MOOVIEFLIX_DISCOVER_V1'); [void]$udp.Send($probe, $probe.Length, '127.0.0.1', 47630)
  $ep = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)
  $reply = try { [Text.Encoding]::UTF8.GetString($udp.Receive([ref]$ep)) | ConvertFrom-Json } catch { $null }; $udp.Close()
  Check 'UDP discovery answers with server port' ($reply -and $reply.service -eq 'moovieflix' -and $reply.port -eq ([Uri]$Base).Port) (J $reply)
  $bad = try { Invoke-RestMethod -Method Post "$Base/moovieflix/v1/pair" -ContentType 'application/json' -Body '{"code":"000000"}'; 'accepted' } catch { 'rejected' }
  Check 'wrong pairing code rejected' ($bad -eq 'rejected')
  $code = (Get-Content "$DataDir\pair-code.txt" -Raw).Trim()
  $pair = Invoke-RestMethod -Method Post "$Base/moovieflix/v1/pair" -ContentType 'application/json' -Body (J @{ code = $code; device_name = 'onn 4K' })
  Check 'pairing returns backend, key, email, device password' ($pair.backend_url -and $pair.publishable_key -eq $anon -and $pair.email -eq $secrets.owner_email -and $pair.password -and $pair.password -ne $secrets.owner_password)
  $t = SignIn $pair.email $pair.password
  $TTok = $t.access_token
  Check 'TV signs in with its device password (same account)' ($t.user.id -eq $secrets.owner_id) (J $t.user)
  $owner = Rpc get_sync_owner @{} $TTok
  Check 'get_sync_owner = owner' ($owner -eq $secrets.owner_id)

  # ---------------- TV: initial pull (what pairing brings over) ----------------
  $tvAddons = @(Api GET "/rest/v1/addons?select=*&user_id=eq.$owner&profile_id=eq.1" $null $TTok)
  Check 'TV gets all 3 desktop addons' ($tvAddons.Count -eq 3) "got $($tvAddons.Count)"
  Check 'addon order/enabled/name preserved' ((($tvAddons | Sort-Object sort_order)[1].enabled -eq $false) -and (($tvAddons | ? url -like '*subs*').name -eq 'Subs'))
  $plugins = @(Api GET "/rest/v1/plugins?select=*&user_id=eq.$owner&profile_id=eq.1" $null $TTok)
  Check 'TV gets plugin repo' ($plugins.Count -eq 1)
  $profiles = @(Rpc sync_pull_profiles @{} $TTok)
  Check 'TV gets both profiles' ($profiles.Count -eq 2 -and ($profiles | ? profile_index -eq 2).uses_primary_addons -eq $true)
  $locks = @(Rpc sync_pull_profile_locks @{} $TTok)
  Check 'TV sees Kids PIN lock' (($locks | ? profile_index -eq 2).pin_enabled -eq $true)
  $pin = @(Rpc verify_profile_pin @{ p_profile_id = 2; p_pin = '1234' } $TTok)
  Check 'PIN verifies on TV' ($pin[0].unlocked -eq $true)
  $lib = @(Rpc sync_pull_library @{ p_profile_id = 1; p_limit = 500; p_offset = 0 } $TTok)
  Check 'TV gets library' ($lib.Count -eq 1 -and $lib[0].content_id -eq 'tt0133093')
  $libCursor = Rpc sync_get_library_delta_cursor @{ p_profile_id = 1 } $TTok
  $wp = @(Rpc sync_pull_watch_progress @{ p_profile_id = 1 } $TTok)
  Check 'TV gets watch progress / continue watching' ($wp.Count -eq 1 -and $wp[0].position -eq 600000 -and $wp[0].progress_key -eq 'tt0944947_s1e2')
  $wpCursor = Rpc sync_get_watch_progress_delta_cursor @{ p_profile_id = 1 } $TTok
  $wi = @(Rpc sync_pull_watched_items @{ p_profile_id = 1; p_page = 1; p_page_size = 500 } $TTok)
  Check 'TV gets watched items' ($wi.Count -eq 1)
  $wiCursor = Rpc sync_get_watched_items_delta_cursor @{ p_profile_id = 1 } $TTok
  $col = @(Rpc sync_pull_collections @{ p_profile_id = 1 } $TTok)
  Check 'TV gets collections' ($col.Count -eq 1 -and $col[0].collections_json[0].name -eq 'Favourites')
  $homeRows = @(Rpc sync_pull_home_catalog_settings @{ p_profile_id = 1; p_platform = 'tv' } $TTok)
  Check 'TV gets home catalog settings' ($homeRows.Count -eq 1)
  $ms = @(Rpc sync_pull_media_servers @{ p_profile_id = 1 } $TTok)
  Check 'TV gets media servers' ($ms.Count -eq 1 -and $ms[0].servers_json[0].name -eq 'NAS')
  $pc = @(Rpc sync_pull_provider_credentials @{ p_profile_id = 1 } $TTok)
  Check 'TV gets debrid/provider credentials' ($pc.Count -eq 1 -and $pc[0].credential_json.api_key -eq 'test-key')
  $src = @(Api GET "/rest/v1/moovieflix_library_sources?select=*&user_id=eq.$($t.user.id)" $null $TTok)
  Check 'TV gets shared My Movies folders' ($src.Count -eq 1 -and $src[0].sources[0].url -like 'http*47631*')

  # ---------------- TV -> desktop ----------------
  $tvPush = @($tvAddons | Sort-Object sort_order | % { $o = @{ url = $_.url; sort_order = $_.sort_order; enabled = $_.enabled }; if ($_.name) { $o.name = $_.name }; $o }) + @(@{ url = 'https://tv-added.example/manifest.json'; sort_order = 3; enabled = $true })
  Rpc sync_push_addons @{ p_addons = $tvPush; p_profile_id = 1; p_origin_client_id = 'tv-test' } $TTok | Out-Null
  Rpc sync_push_watch_progress @{ p_profile_id = 1; p_entries = @(@{ content_id = 'tt0944947'; content_type = 'series'; video_id = 'tt0944947:1:2'; season = 1; episode = 2; position = 1800000; duration = 3600000; last_watched = 1761600000000; progress_key = 'tt0944947_s1e2' }) } $TTok | Out-Null
  Rpc sync_push_library_items @{ p_profile_id = 1; p_items = @(@{ content_id = 'tt1375666'; content_type = 'movie'; name = 'Inception'; poster_shape = 'POSTER'; added_at = 1761650000000 }) } $TTok | Out-Null
  Rpc sync_push_watched_items @{ p_profile_id = 1; p_items = @(@{ content_id = 'tt0068646'; content_type = 'movie'; title = 'The Godfather'; watched_at = 1761660000000 }) } $TTok | Out-Null
  $dAddons = @(Api GET '/rest/v1/addons?select=*&profile_id=eq.1&order=sort_order.asc' $null $DTok)
  Check 'desktop sees addon added on TV' ($dAddons.Count -eq 4 -and $dAddons[3].url -like '*tv-added*') "got $($dAddons.Count)"
  Check 'TV push kept custom addon name and disabled state' ((($dAddons | ? url -like '*subs*').name -eq 'Subs') -and (($dAddons | ? url -like '*subs*').enabled -eq $false))
  $dwp = @(Rpc sync_pull_watch_progress_delta @{ p_profile_id = 1; p_since_event_id = 0; p_limit = 1000 } $DTok)
  Check 'desktop sees TV watch progress (delta)' (($dwp | Select-Object -Last 1).position -eq 1800000)
  $dlib = @(Rpc sync_pull_library_delta @{ p_profile_id = 1; p_since_event_id = 0; p_limit = 1000 } $DTok)
  Check 'desktop sees TV library add (delta)' (@($dlib | ? content_id -eq 'tt1375666').Count -eq 1)
  $dwi = @(Rpc sync_pull_watched_items_delta @{ p_profile_id = 1; p_since_event_id = 0; p_limit = 1000 } $DTok)
  Check 'desktop sees TV watched item (delta)' (@($dwi | ? content_id -eq 'tt0068646').Count -eq 1)

  # ---------------- desktop -> TV after pairing (keeps syncing) ----------------
  Rpc sync_push_watch_progress @{ p_profile_id = 1; p_entries = @(@{ content_id = 'tt0133093'; content_type = 'movie'; video_id = 'tt0133093'; position = 42000; duration = 8160000; last_watched = 1761700000000; progress_key = 'tt0133093' }) } $DTok | Out-Null
  Rpc sync_delete_library_items @{ p_profile_id = 1; p_keys = @(@{ content_id = 'tt0133093'; content_type = 'movie' }) } $DTok | Out-Null
  $twp = @(Rpc sync_pull_watch_progress_delta @{ p_profile_id = 1; p_since_event_id = $wpCursor; p_limit = 1000 } $TTok)
  Check 'TV gets new desktop progress via delta' (@($twp | ? progress_key -eq 'tt0133093').Count -eq 1)
  $tlib = @(Rpc sync_pull_library_delta @{ p_profile_id = 1; p_since_event_id = $libCursor; p_limit = 1000 } $TTok)
  Check 'TV gets desktop library delete via delta' (@($tlib | ? { $_.content_id -eq 'tt0133093' -and $_.operation -eq 'delete' }).Count -eq 1)

  # ---------------- session ----------------
  $r = Refresh $t.refresh_token
  Check 'TV token refresh works' ($r.access_token -and $r.user.id -eq $secrets.owner_id)
  $after = @(Rpc sync_pull_profiles @{} $r.access_token)
  Check 'refreshed token is accepted' ($after.Count -eq 2)
  $badTok = try { Rpc sync_pull_profiles @{} 'not.a.token' | Out-Null; 'ok' } catch { 'denied' }
  Check 'invalid token is rejected' ($badTok -eq 'denied')
  @{ refresh_token = $t.refresh_token; device_password = $pair.password; email = $pair.email; key = $pair.publishable_key } | ConvertTo-Json | Set-Content $StateFile
}

if ($Phase -eq 'after-restart') {
  $st = Get-Content $StateFile -Raw | ConvertFrom-Json
  $r = Refresh $st.refresh_token
  Check 'after PC app restart: TV refresh token still valid' ($r.access_token -and $r.user.id -eq $secrets.owner_id)
  $a = @(Api GET "/rest/v1/addons?select=*&user_id=eq.$($secrets.owner_id)&profile_id=eq.1" $null $r.access_token)
  Check 'after restart: data persisted (4 addons)' ($a.Count -eq 4) "got $($a.Count)"
  $s = SignIn $st.email $st.device_password
  Check 'after restart: TV device password still signs in' ($s.access_token -ne $null)
  $wk = Invoke-RestMethod "$Base/.well-known/nuvio"
  Check 'after restart/new address: discovery key matches paired key (TV re-find check)' ($wk.publishable_key -eq $st.key)
}
Write-Host ""; Write-Host "RESULT: $script:pass passed, $script:fail failed"
if ($script:fail) { exit 1 }
