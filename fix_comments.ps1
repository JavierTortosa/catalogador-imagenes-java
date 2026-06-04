param(
    [string]$FilePath = "D:\Programacion\Eclipse\Workspace 2024-12R\VisorImagenes\src\controlador\ProjectController.java"
)

function Is-DescComment([string]$t) {
    return ($t -match '^// [A-Za-z]' -and $t -notmatch '^// ---')
}

function Is-Annotation([string]$t) {
    return $t.TrimStart().StartsWith('@')
}

function Is-MethodDecl([string]$t) {
    # Check if this line looks like a method declaration (not a field, not a comment, not inside a class body as anonymous)
    # Must match patterns like: public void foo(), private String bar(), void baz(), etc.
    if ($t.Contains('//')) { return $false }
    if ($t.Contains('=')) { return $false }
    if ($t -match '^\s*(public|private|protected)\s') {
        # Has access modifier, check if it's a method (has parentheses with params)
        if ($t -match '\(\s*(?:\w[\w\.\[\]]*\s+\w+\s*,?\s*)*\)\s*\{?\s*$' -or $t -match '\(\s*\)\s*\{?\s*$') {
            return $true
        }
    }
    # package-private: starts with return type then name then (
    if ($t -match '^\s*(void|boolean|String|int|long|double|float|char|byte|short)\s+\w+\s*\(' -and -not $t.Contains(';')) {
        return $true
    }
    # Class return type (e.g., JTable getFoo(), ExportItem getBar())
    if ($t -match '^\s*[A-Z]\w*\s+[a-z]\w*\s*\(' -and -not $t.Contains(';') -and -not $t.Contains('=') -and -not $t.Contains('new ')) {
        return $true
    }
    return $false
}

$lines = Get-Content -Path $FilePath -Encoding UTF8

# Pass 1: Identify all method start lines and whether they need comments
$insertions = @{}  # key = line index, value = method name
$i = 0

while ($i -lt $lines.Count) {
    $trimmed = $lines[$i].Trim()
    
    # Constructor
    if ($trimmed -match '^public\s+ProjectController\s*\(' -and -not $trimmed.Contains('//')) {
        $methodName = "Constructor"
        $methodStart = $i
        
        # Look backwards for @Override (unlikely for constructor but general logic)
        $searchIdx = $i - 1
        while ($searchIdx -ge 0 -and ($lines[$searchIdx].Trim() -eq '' -or (Is-Annotation $lines[$searchIdx]))) {
            if (Is-Annotation $lines[$searchIdx]) { $methodStart = $searchIdx }
            $searchIdx--
        }
        
        # Check for existing description comment
        $hasComment = $false
        $searchIdx = $methodStart - 1
        while ($searchIdx -ge 0 -and ($lines[$searchIdx].Trim() -eq '' -or $lines[$searchIdx].Trim() -match '^// ---')) {
            if (Is-DescComment $lines[$searchIdx].Trim()) { $hasComment = $true; break }
            $searchIdx--
        }
        
        if (-not $hasComment) {
            $insertions[$methodStart] = $methodName
        }
    }
    elseif (Is-MethodDecl $trimmed -and -not $trimmed.StartsWith('@')) {
        # Extract method name
        $methodName = ""
        if ($trimmed -match '(?:public|private|protected)\s+(?:\w[\w\.]*\s+)?(\w[\w\d]*)\s*\(') {
            $methodName = $matches[1]
        } elseif ($trimmed -match '^\s*(?:void|boolean|String|int|long|double|float|char|byte|short)\s+(\w[\w\d]*)\s*\(') {
            $methodName = $matches[1]
        } elseif ($trimmed -match '^\s*[A-Z]\w*\s+(\w[\w\d]*)\s*\(') {
            $methodName = $matches[1]
        }
        
        # Exclude anonymous inner methods
        if ($methodName -match '^(mousePressed|mouseReleased|mouseClicked|mouseEntered|mouseExited|actionPerformed|insertUpdate|removeUpdate|changedUpdate|propertyChange|showMenu|notificar|paintComponent|compare|get\d|set\d|add\d)$') {
            $i++; continue
        }
        
        if ($methodName -eq "") { $i++; continue }
        
        # Find method start (including @Override lines before)
        $methodStart = $i
        $searchIdx = $i - 1
        while ($searchIdx -ge 0 -and ($lines[$searchIdx].Trim() -eq '' -or (Is-Annotation $lines[$searchIdx]))) {
            if (Is-Annotation $lines[$searchIdx]) { $methodStart = $searchIdx }
            $searchIdx--
        }
        
        # Check for existing description comment
        $hasComment = $false
        $searchIdx = $methodStart - 1
        while ($searchIdx -ge 0 -and ($lines[$searchIdx].Trim() -eq '' -or $lines[$searchIdx].Trim() -match '^// ---')) {
            if (Is-DescComment $lines[$searchIdx].Trim()) { $hasComment = $true; break }
            $searchIdx--
        }
        
        if (-not $hasComment) {
            $insertions[$methodStart] = $methodName
        }
    }
    
    $i++
}

# Pass 2: Build output with insertions (sorted by key descending to insert bottom-up)
$sortedKeys = $insertions.Keys | Sort-Object -Descending
$outputLines = [System.Collections.Generic.List[string]]::new($lines)

foreach ($key in $sortedKeys) {
    $name = $insertions[$key]
    # Get indentation from that line
    $originalLine = $outputLines[$key]
    $indent = ""
    $m = [regex]::Match($originalLine, '^(\s*)')
    if ($m.Success) { $indent = $m.Groups[1].Value }
    
    $commentLine = $indent + "// $name"
    $outputLines.Insert($key, $commentLine)
}

# Pass 3: Fix blank lines - ensure exactly 2 blanks between closing } // and next desc/method
$resultLines = [System.Collections.Generic.List[string]]::new()
$prevClosing = $false
$blanks = 0

for ($j = 0; $j -lt $outputLines.Count; $j++) {
    $line = $outputLines[$j]
    $t = $line.Trim()
    $isClosing = $t -match '^\} // ---'
    $isBlank = $t -eq ''
    $isDescOrMethod = (Is-DescComment $t) -or (Is-MethodDecl $t) -or (Is-Annotation $t) -or ($t -match '^\s*public\s+\w+\s*\(')
    
    if ($isClosing) {
        $resultLines.Add($line)
        $prevClosing = $true
        $blanks = 0
    }
    elseif ($prevClosing -and $isBlank -and $blanks -lt 2) {
        $resultLines.Add($line)
        $blanks++
    }
    elseif ($prevClosing -and $isBlank -and $blanks -ge 2) {
        # skip extra blanks
    }
    elseif ($prevClosing -and ($isDescOrMethod -or -not $isBlank)) {
        while ($blanks -lt 2) {
            $resultLines.Add('')
            $blanks++
        }
        $resultLines.Add($line)
        $prevClosing = $false
        $blanks = 0
    }
    else {
        $resultLines.Add($line)
        $prevClosing = $false
        $blanks = 0
    }
}

$resultLines | Set-Content -Path $FilePath -Encoding UTF8
Write-Host ("Done. Inserted " + $insertions.Count + " description comments.")
