[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$distribution = Join-Path $root 'desktop\build\install\ViviMe-Desktop-Pet'
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$framework = Split-Path -Parent $compiler
$libraries = @('core-0.1.0-core-preview.jar', 'desktop-0.1.0-core-preview.jar', 'fastjson2-2.0.60.jar')
$notices = @('LICENSE', 'NOTICE.md', 'THIRD_PARTY_LICENSES.md', 'licenses')
$zipName = 'ViviMe-Desktop-Pet-v0.1.0-core-preview-windows-x64.zip'
$output = (New-Item -ItemType Directory -Path $OutputDirectory -Force).FullName
$destination = Join-Path $output $zipName
if (Test-Path -LiteralPath $destination) { throw "ZIP already exists: $destination" }
if (!(Test-Path -LiteralPath $compiler)) { throw '.NET Framework C# compiler is required.' }

$stage = Join-Path ([IO.Path]::GetTempPath()) ('vivime-package-' + [guid]::NewGuid().ToString('N'))
$preview = Join-Path $stage 'ViviMe-Core-Preview'
New-Item -ItemType Directory -Path (Join-Path $preview 'lib') -Force | Out-Null
try {
    $executable = Join-Path $preview 'ViviMeCorePreview.exe'
    & $compiler /nologo /target:winexe /platform:x64 /optimize+ /codepage:65001 "/out:$executable" `
        "/reference:$(Join-Path $framework 'System.IO.Compression.FileSystem.dll')" `
        (Join-Path $PSScriptRoot 'Program.cs')
    if ($LASTEXITCODE -ne 0) { throw 'C# launcher compilation failed.' }

    foreach ($name in $libraries) {
        Copy-Item -LiteralPath (Join-Path $distribution "lib\$name") -Destination (Join-Path $preview 'lib')
    }
    foreach ($name in $notices) {
        Copy-Item -LiteralPath (Join-Path $root $name) -Destination $preview -Recurse
    }
    Copy-Item -LiteralPath (Join-Path $root 'docs\WINDOWS_PREVIEW.md') -Destination (Join-Path $preview 'START-HERE.md')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $temporaryZip = Join-Path $stage $zipName
    [IO.Compression.ZipFile]::CreateFromDirectory($preview, $temporaryZip, [IO.Compression.CompressionLevel]::Optimal, $true)
    Move-Item -LiteralPath $temporaryZip -Destination $destination
    Get-FileHash -Algorithm SHA256 -LiteralPath $destination
} finally {
    if ((Split-Path -Parent $stage) -ne [IO.Path]::GetTempPath().TrimEnd('\') -or
        (Split-Path -Leaf $stage) -notmatch '^vivime-package-[0-9a-f]{32}$') {
        throw 'Unexpected temporary directory.'
    }
    Remove-Item -LiteralPath $stage -Recurse -Force
}
