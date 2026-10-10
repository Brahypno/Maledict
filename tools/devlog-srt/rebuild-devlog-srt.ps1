# -*- coding: utf-8 -*-
# Regenerates devlog-01-cn.srt with a repaired, strictly increasing timeline.
#
# The source material carried broken minute/second carries: cue starts jump
# backwards by exactly 1.2 s in three places because an edit pass advanced the
# "minute" field of some lines but not their neighbours.
#
# Repair rule: keep each cue's net duration as an integer frame count at
# 29.97 fps (30000/1001), read as round(chars / 13 * 30) -- 13 characters per
# line is this project's subtitle reading-speed budget -- with a 24-frame floor.
# Timeline positions come from a prefix sum of those frame counts, so a cue can
# never start before its predecessor ends.
#
# Every emitted timecode is parsed back and compared with the millisecond value
# it was built from; the script fails loudly instead of writing a bad file.
[CmdletBinding()]
param(
    [string] $Path = (Join-Path $PSScriptRoot '..\..\devlog-01-cn.srt'),
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'
$Path = [System.IO.Path]::GetFullPath($Path)

$encoding = [System.Text.UTF8Encoding]::new($false)
$textReader = [System.IO.File]::ReadAllLines($Path, $encoding)

# --- collect cue bodies -------------------------------------------------------
# A cue is an index line, a timestamp line, then the body line.
$bodyList = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt ($textReader.Count - 2); $i++) {
    if ($textReader[$i] -match '^\d+$' -and $textReader[$i + 1] -match '^\d\d:\d\d:\d\d,\d\d\d --> ') {
        $bodyList.Add($textReader[$i + 2])
    }
}
$bodies = $bodyList.ToArray()
if ($bodies.Count -eq 0) { throw "no cues parsed from $Path" }

# --- frame plan ---------------------------------------------------------------
$fps = 30000.0 / 1001.0

$frameSpans = [int[]]::new($bodies.Count)
for ($i = 0; $i -lt $bodies.Count; $i++) {
    $span = [int][Math]::Floor($bodies[$i].Length / 13.0 * 30.0 + 0.5)
    if ($span -lt 24) { $span = 24 }
    $frameSpans[$i] = $span
}

# frameStart[i] is the first frame of cue i; frameStart[last+1] is the end.
$frameStart = [int[]]::new($bodies.Count + 1)
for ($i = 0; $i -lt $bodies.Count; $i++) {
    $frameStart[$i + 1] = $frameStart[$i] + $frameSpans[$i]
}

function ConvertTo-Timecode {
    param([Parameter(Mandatory)][int] $Milliseconds)

    $hours = [int][Math]::Floor($Milliseconds / 3600000)
    $rest = $Milliseconds - $hours * 3600000
    $minutes = [int][Math]::Floor($rest / 60000)
    $rest = $rest - $minutes * 60000
    $seconds = [int][Math]::Floor($rest / 1000)
    $millis = $rest - $seconds * 1000
    return '{0:00}:{1:00}:{2:00},{3:000}' -f $hours, $minutes, $seconds, $millis
}

function ConvertFrom-Timecode {
    param([Parameter(Mandatory)][string] $Timecode)

    if ($Timecode -notmatch '^(\d\d):(\d\d):(\d\d),(\d\d\d)$') { throw "malformed timecode: $Timecode" }
    return [int]$Matches[1] * 3600000 + [int]$Matches[2] * 60000 + [int]$Matches[3] * 1000 + [int]$Matches[4]
}

# --- emit ---------------------------------------------------------------------
$builder = [System.Text.StringBuilder]::new()
$report = [System.Collections.Generic.List[string]]::new()
$problems = [System.Collections.Generic.List[string]]::new()
$previousEnd = -1

for ($i = 0; $i -lt $bodies.Count; $i++) {
    $startMs = [int][Math]::Round($frameStart[$i] * 1000.0 / $fps)
    $endMs = [int][Math]::Round($frameStart[$i + 1] * 1000.0 / $fps)

    $startTs = ConvertTo-Timecode -Milliseconds $startMs
    $endTs = ConvertTo-Timecode -Milliseconds $endMs

    if ((ConvertFrom-Timecode -Timecode $startTs) -ne $startMs) {
        $problems.Add("round-trip failed for start of cue $($i + 1): $startMs -> $startTs")
    }
    if ((ConvertFrom-Timecode -Timecode $endTs) -ne $endMs) {
        $problems.Add("round-trip failed for end of cue $($i + 1): $endMs -> $endTs")
    }
    if ($startMs -lt $previousEnd -or $endMs -le $startMs) {
        $problems.Add("non-monotonic cue $($i + 1): $startMs -> $endMs")
    }
    $previousEnd = $endMs

    [void]$builder.AppendLine([string]($i + 1))
    [void]$builder.AppendLine($startTs + ' --> ' + $endTs)
    [void]$builder.AppendLine($bodies[$i])
    [void]$builder.AppendLine()

    $report.Add(('{0,3}  {1} --> {2}  frames={3,3}  chars={4,3}  {5}' -f `
        ($i + 1), $startTs, $endTs, $frameSpans[$i], $bodies[$i].Length, $bodies[$i]))
}

if ($problems.Count -gt 0) {
    $problems | ForEach-Object { Write-Error $_ }
    throw "$($problems.Count) timeline problem(s); nothing written"
}

if ($DryRun) {
    $report -join "`n"
} else {
    [System.IO.File]::WriteAllText($Path, $builder.ToString(), $encoding)
}

'cues          : {0}' -f $bodies.Count
'total runtime : {0} ms ({1:0.00} s)' -f $previousEnd, ($previousEnd / 1000.0)
'output        : {0}' -f $Path
