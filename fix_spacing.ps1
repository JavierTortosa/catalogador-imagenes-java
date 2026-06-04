param([string]$FilePath)
$lines = Get-Content -Path $FilePath -Encoding UTF8
$result = [System.Collections.Generic.List[string]]::new()
$prevClosing = $false
$blanks = 0
for ($j = 0; $j -lt $lines.Count; $j++) {
    $line = $lines[$j]; $t = $line.Trim()
    $isClosing = $t -match '^\} // ---'
    $isBlank = $t -eq ''
    $isAnnotation = $t.StartsWith('@')
    # Next method, description comment, or annotation
    $isNextMethodDesc = ($t -match '^// [A-Za-z]') -or ($t -match '^\s*(public|private|protected)\s') -or $isAnnotation
    if ($isClosing) { 
        $result.Add($line); $prevClosing = $true; $blanks = 0
    } elseif ($prevClosing -and $isBlank -and $blanks -lt 2) {
        $result.Add($line); $blanks++
    } elseif ($prevClosing -and $isBlank -and $blanks -ge 2) {
        # skip
    } elseif ($prevClosing) {
        while ($blanks -lt 2) { $result.Add(''); $blanks++ }
        $result.Add($line); $prevClosing = $false; $blanks = 0
    } else {
        $result.Add($line); $prevClosing = $false; $blanks = 0
    }
}
$result | Set-Content -Path $FilePath -Encoding UTF8
Write-Host "Spacing fixed."
