param(
    [string]$MinecraftJar = (Join-Path $env:USERPROFILE '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'),
    [string]$MalumJar = (Join-Path $env:USERPROFILE '.gradle/caches/forge_gradle/deobf_dependencies/curse/maven/malum-484064/6646111_mapped_official_1.20.1/malum-484064-6646111_mapped_official_1.20.1.jar')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$textureDir = Join-Path $projectRoot 'src/main/resources/assets/maledict/textures/block'
New-Item -ItemType Directory -Path $textureDir -Force | Out-Null

function Read-Texture($Archive, [string]$EntryName) {
    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) { throw "Missing texture: $EntryName" }
    $stream = $entry.Open()
    try {
        $image = [Drawing.Bitmap]::new($stream)
        try { return [Drawing.Bitmap]::new($image) }
        finally { $image.Dispose() }
    } finally { $stream.Dispose() }
}

$minecraft = [IO.Compression.ZipFile]::OpenRead($MinecraftJar)
$malum = [IO.Compression.ZipFile]::OpenRead($MalumJar)
try {
    $obsidian = Read-Texture $minecraft 'assets/minecraft/textures/block/obsidian.png'
    $crying = Read-Texture $minecraft 'assets/minecraft/textures/block/crying_obsidian.png'
    $tainted = Read-Texture $malum 'assets/malum/textures/block/arcane_rock/tainted/tainted_rock.png'
    $twisted = Read-Texture $malum 'assets/malum/textures/block/arcane_rock/twisted/twisted_rock.png'
} finally {
    $minecraft.Dispose()
    $malum.Dispose()
}

$images = @($obsidian, $crying, $tainted, $twisted)
foreach ($image in $images) {
    if ($image.Width -ne 16 -or $image.Height -ne 16) { throw 'Expected the original 16 x 16 textures.' }
}

$overlay = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$cryingTainted = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$cryingTwisted = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$changed = 0
for ($pixelY = 0; $pixelY -lt 16; $pixelY++) {
    for ($pixelX = 0; $pixelX -lt 16; $pixelX++) {
        # Exact source-pixel comparison: no thresholds, recoloring, resampling, or hand-drawn streaks.
        $tear = $obsidian.GetPixel($pixelX, $pixelY).ToArgb() -ne $crying.GetPixel($pixelX, $pixelY).ToArgb()
        $taintedPixel = $tainted.GetPixel($pixelX, $pixelY)
        $twistedPixel = $twisted.GetPixel($pixelX, $pixelY)
        if ($tear) {
            $tearPixel = $crying.GetPixel($pixelX, $pixelY)
            $overlay.SetPixel($pixelX, $pixelY, $tearPixel)
            $taintedPixel = $tearPixel
            $twistedPixel = $tearPixel
            $changed++
        }
        $cryingTainted.SetPixel($pixelX, $pixelY, $taintedPixel)
        $cryingTwisted.SetPixel($pixelX, $pixelY, $twistedPixel)
    }
}

$cryingTainted.Save((Join-Path $textureDir 'crying_tainted_rock.png'), [Drawing.Imaging.ImageFormat]::Png)
$cryingTwisted.Save((Join-Path $textureDir 'crying_twisted_rock.png'), [Drawing.Imaging.ImageFormat]::Png)
$overlay.Save((Join-Path $PSScriptRoot 'tears-overlay.png'), [Drawing.Imaging.ImageFormat]::Png)

$tiles = @($obsidian, $crying, $tainted, $cryingTainted, $twisted, $cryingTwisted)
$labels = @('Obsidian', 'Crying obsidian', 'Tainted rock', 'Crying tainted rock', 'Twisted rock', 'Crying twisted rock')
$preview = [Drawing.Bitmap]::new(1536, 288, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($preview)
$font = [Drawing.Font]::new('Segoe UI', 15, [Drawing.GraphicsUnit]::Pixel)
$graphics.Clear([Drawing.Color]::FromArgb(255, 24, 24, 28))
for ($tileIndex = 0; $tileIndex -lt $tiles.Count; $tileIndex++) {
    $graphics.DrawString($labels[$tileIndex], $font, [Drawing.Brushes]::White, [single]($tileIndex * 256 + 8), [single]6)
    for ($pixelY = 0; $pixelY -lt 16; $pixelY++) {
        for ($pixelX = 0; $pixelX -lt 16; $pixelX++) {
            $brush = [Drawing.SolidBrush]::new($tiles[$tileIndex].GetPixel($pixelX, $pixelY))
            $graphics.FillRectangle($brush, $tileIndex * 256 + $pixelX * 16, 32 + $pixelY * 16, 16, 16)
            $brush.Dispose()
        }
    }
}
$preview.Save((Join-Path $PSScriptRoot 'preview.png'), [Drawing.Imaging.ImageFormat]::Png)
$font.Dispose()
$graphics.Dispose()
$preview.Dispose()
$overlay.Dispose()
foreach ($image in @($obsidian, $crying, $tainted, $twisted, $cryingTainted, $cryingTwisted)) { $image.Dispose() }
Write-Output "Copied $changed original tear pixels onto each stone; preserved $(256 - $changed) base pixels unchanged."
