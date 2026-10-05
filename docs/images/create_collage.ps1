Add-Type -AssemblyName System.Drawing

$scriptDir = $PSScriptRoot
if (-not $scriptDir) { $scriptDir = $PWD.Path }

$canvasWidth = 3500
$canvasHeight = 2220

# Helper function to create rounded rect path
function Get-RoundedRectPath([System.Drawing.RectangleF]$rect, [float]$radius) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $diameter = $radius * 2
    if ($diameter -gt $rect.Width) { $diameter = $rect.Width }
    if ($diameter -gt $rect.Height) { $diameter = $rect.Height }
    $arc = [System.Drawing.RectangleF]::new($rect.X, $rect.Y, $diameter, $diameter)

    $path.AddArc($arc, 180, 90)
    $arc.X = $rect.Right - $diameter
    $path.AddArc($arc, 270, 90)
    $arc.Y = $rect.Bottom - $diameter
    $path.AddArc($arc, 0, 90)
    $arc.X = $rect.X
    $path.AddArc($arc, 90, 90)
    $path.CloseFigure()
    return $path
}

# Items definition for Canvas 3500 x 2220
$items = @(
    # --- LAYER 1: FAR BACK / TOP CORNERS & BACKGROUND ---
    # Left background:
    [PSCustomObject]@{ File="permissions.png";                 X=210;  Y=500;  W=420; Angle=-12; Layer=1.0 },
    [PSCustomObject]@{ File="alarm_profiles.png";             X=610;  Y=480;  W=420; Angle=-10; Layer=1.0 },

    # Top-center background:
    [PSCustomObject]@{ File="insulin_types.png";              X=1850; Y=520;  W=430; Angle=-4;  Layer=1.0 },
    [PSCustomObject]@{ File="insulin_profiles.png";           X=2220; Y=450;  W=430; Angle=3;   Layer=1.0 },

    # Right background:
    [PSCustomObject]@{ File="meal_types.png";                  X=3020; Y=500;  W=420; Angle=10;  Layer=1.0 },
    [PSCustomObject]@{ File="preferences.png";                 X=3340; Y=500;  W=420; Angle=12;  Layer=1.0 },

    # --- LAYER 2: MID BACK ---
    [PSCustomObject]@{ File="manual_control.png";              X=300;  Y=1320; W=430; Angle=-12; Layer=2.0 },
    [PSCustomObject]@{ File="alarm_profile_editor.png";        X=960;  Y=600;  W=450; Angle=-6;  Layer=2.0 },
    [PSCustomObject]@{ File="setup_units_step.png";           X=740;  Y=1160; W=470; Angle=-9;  Layer=2.0 },
    [PSCustomObject]@{ File="insulin_type_editor.png";         X=1450; Y=460;  W=450; Angle=-7;  Layer=2.0 },
    [PSCustomObject]@{ File="setup_glucose_source.png";        X=2630; Y=600;  W=450; Angle=9;   Layer=2.0 },
    [PSCustomObject]@{ File="meal_type_editor.png";            X=3200; Y=780;  W=430; Angle=8;   Layer=2.0 },
    [PSCustomObject]@{ File="system_control.png";              X=3280; Y=1680; W=430; Angle=11;  Layer=2.0 },

    # --- LAYER 3: FREED-UP MIDDLE AREA ---
    [PSCustomObject]@{ File="master_data.png";                X=1470; Y=1040; W=540; Angle=-5;  Layer=3.0 },
    [PSCustomObject]@{ File="dashboard.png";                  X=1950; Y=940;  W=600; Angle=5;   Layer=3.2 },
    [PSCustomObject]@{ File="current_therapy_settings.png";   X=1050; Y=1380; W=510; Angle=-8;  Layer=3.5 },
    [PSCustomObject]@{ File="therapy_adjustment_editor.png";  X=2470; Y=1060; W=510; Angle=7;   Layer=3.5 },
    [PSCustomObject]@{ File="app_data.png";                   X=2970; Y=1070; W=480; Angle=10;  Layer=3.5 },

    # --- LAYER 4: FRONT BOTTOM ---
    [PSCustomObject]@{ File="setup.png";                      X=600;  Y=1700; W=520; Angle=-7;  Layer=4.0 },
    [PSCustomObject]@{ File="meal_correction_bolus.png";      X=1510; Y=1720; W=510; Angle=-3;  Layer=4.0 },
    [PSCustomObject]@{ File="therapy_adjustment.png";         X=2350; Y=1640; W=500; Angle=8;   Layer=4.0 }
)

function Build-Collage([string]$outFileName, [bool]$isDarkTheme) {
    $outPath = Join-Path $scriptDir $outFileName

    $canvas = [System.Drawing.Bitmap]::new($canvasWidth, $canvasHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)

    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    if ($isDarkTheme) {
        # Dark Theme Background: Deep Dark Slate / Teal Navy matching app's dark palette (#11140F / #8FCEF3)
        $cBgStart = [System.Drawing.Color]::FromArgb(255, 12, 18, 26)       # #0C121A
        $cBgEnd   = [System.Drawing.Color]::FromArgb(255, 22, 34, 48)       # #162230
        $cGlowCenter = [System.Drawing.Color]::FromArgb(65, 143, 206, 243)    # #8FCEF3 soft cyan glow
        $cGlowSurround = [System.Drawing.Color]::FromArgb(0, 12, 18, 26)
        $cBorder = [System.Drawing.Color]::FromArgb(90, 143, 206, 243)        # #8FCEF3 border accent
        $shadowR = 0; $shadowG = 0; $shadowB = 0
        $shadowMult = 1.0
    } else {
        # Light Theme Background: Soft Elegant Ice / Slate Blue matching app's light palette (#F8FBF1 / #1C6585)
        $cBgStart = [System.Drawing.Color]::FromArgb(255, 236, 243, 248)    # #ECF3F8
        $cBgEnd   = [System.Drawing.Color]::FromArgb(255, 205, 223, 235)    # #CDDFEB
        $cGlowCenter = [System.Drawing.Color]::FromArgb(90, 255, 255, 255)    # Soft white central highlight
        $cGlowSurround = [System.Drawing.Color]::FromArgb(0, 236, 243, 248)
        $cBorder = [System.Drawing.Color]::FromArgb(65, 28, 101, 133)       # #1C6585 light primary border
        $shadowR = 15; $shadowG = 30; $shadowB = 50
        $shadowMult = 1.3
    }

    # Background gradient
    $rectBackground = [System.Drawing.Rectangle]::new(0, 0, $canvasWidth, $canvasHeight)
    $bgBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $rectBackground,
        $cBgStart,
        $cBgEnd,
        45
    )
    $g.FillRectangle($bgBrush, $rectBackground)
    $bgBrush.Dispose()

    # Soft central glow behind hero area
    $pathGlow = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $pathGlow.AddEllipse(670, 310, 2160, 1600)
    $pgb = [System.Drawing.Drawing2D.PathGradientBrush]::new($pathGlow)
    $pgb.CenterColor = $cGlowCenter
    $pgb.SurroundColors = @($cGlowSurround)
    $g.FillPath($pgb, $pathGlow)
    $pgb.Dispose()
    $pathGlow.Dispose()

    # Sort items by layer ascending so lower layers are drawn first
    $sortedItems = $items | Sort-Object Layer, File

    foreach ($item in $sortedItems) {
        $targetFile = $item.File

        if ($isDarkTheme) {
            $baseName = [System.IO.Path]::GetFileNameWithoutExtension($item.File)
            $ext = [System.IO.Path]::GetExtension($item.File)
            $darkCandidate = "${baseName}-dark${ext}"
            $darkCandidatePath = Join-Path $scriptDir $darkCandidate

            if (Test-Path $darkCandidatePath) {
                $targetFile = $darkCandidate
            }
        }

        $filePath = Join-Path $scriptDir $targetFile
        if (-not (Test-Path $filePath)) {
            Write-Host "Warning: File not found - $filePath"
            continue
        }

        $srcImg = [System.Drawing.Image]::FromFile($filePath)
        $aspect = $srcImg.Height / $srcImg.Width
        $cardW = [float]$item.W
        $cardH = [float]($cardW * $aspect)

        $radius = [float]([Math]::Min($cardW, $cardH) * 0.05)

        $cardBmp = [System.Drawing.Bitmap]::new([int]$cardW, [int]$cardH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $cg = [System.Drawing.Graphics]::FromImage($cardBmp)
        $cg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $cg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $cg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $cardRect = [System.Drawing.RectangleF]::new(0, 0, $cardW, $cardH)
        $cardPath = Get-RoundedRectPath $cardRect $radius

        $cg.SetClip($cardPath)
        $cg.DrawImage($srcImg, 0, 0, $cardW, $cardH)
        $cg.ResetClip()

        $borderPen = [System.Drawing.Pen]::new($cBorder, 2.5)
        $cg.DrawPath($borderPen, $cardPath)
        $borderPen.Dispose()

        $cardPath.Dispose()
        $cg.Dispose()
        $srcImg.Dispose()

        $state = $g.Save()
        $g.TranslateTransform($item.X, $item.Y)
        $g.RotateTransform($item.Angle)

        $layerInt = [Math]::Floor($item.Layer)
        $shadowPasses = 5 + ($layerInt * 2)
        $baseAlpha = 10 + ($layerInt * 4)

        for ($p = $shadowPasses; $p -ge 1; $p--) {
            $sInflate = $p * 2.5
            $sOffsetX = ($layerInt * 3) + ($p * 1.5)
            $sOffsetY = ($layerInt * 5) + ($p * 2.5)

            $sRect = [System.Drawing.RectangleF]::new(
                (-$cardW / 2.0) - $sInflate + $sOffsetX,
                (-$cardH / 2.0) - $sInflate + $sOffsetY,
                $cardW + ($sInflate * 2),
                $cardH + ($sInflate * 2)
            )
            $sRadius = $radius + $sInflate
            $sPath = Get-RoundedRectPath $sRect $sRadius
            $sAlpha = [Math]::Min(255, [int]($baseAlpha * $shadowMult))
            $sBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($sAlpha, $shadowR, $shadowG, $shadowB))
            $g.FillPath($sBrush, $sPath)
            $sBrush.Dispose()
            $sPath.Dispose()
        }

        $g.DrawImage($cardBmp, [float](-$cardW / 2.0), [float](-$cardH / 2.0), $cardW, $cardH)
        $g.Restore($state)

        $cardBmp.Dispose()
        Write-Host "Processed $targetFile -> $outFileName"
    }

    $canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $canvas.Dispose()

    Write-Host "Collage saved to $outPath"
}

# Generate Light Collage
Build-Collage -outFileName "collage-light.png" -isDarkTheme $false

# Generate Dark Collage
Build-Collage -outFileName "collage-dark.png" -isDarkTheme $true