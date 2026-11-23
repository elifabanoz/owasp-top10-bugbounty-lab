param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('secure', 'vulnerable')]
    [string]$Profile
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$javaHomePath = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Java\jdk-21' }
$javaPath = Join-Path $javaHomePath 'bin\java.exe'
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenPath = if ($mavenCommand) { $mavenCommand.Source } else { '' }

if (-not (Test-Path -LiteralPath $javaPath) -or -not $mavenPath) {
    throw 'Java 17 or newer and Maven are required. Set JAVA_HOME and add Maven to PATH.'
}

$env:JAVA_HOME = $javaHomePath
$env:SPRING_PROFILES_ACTIVE = $Profile
$env:LAB_BIND = '127.0.0.1'
$env:LAB_DB_USER = 'bugbounty'
$env:LAB_DB_PASSWORD = 'local-demo-only'
$env:LAB_DB_URL = "jdbc:postgresql://127.0.0.1:55432/bugbounty_$Profile"
$env:LAB_SEED = 'true'
if ($Profile -eq 'secure' -and -not $env:LAB_JWT_SECRET) {
    $secretBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($secretBytes)
        $env:LAB_JWT_SECRET = [Convert]::ToBase64String($secretBytes)
    } finally {
        $random.Dispose()
    }
}
if ($Profile -eq 'secure' -and -not $env:LAB_WEBHOOK_SECRET) {
    $secretBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($secretBytes)
        $env:LAB_WEBHOOK_SECRET = [Convert]::ToBase64String($secretBytes)
    } finally {
        $random.Dispose()
    }
}

Push-Location $repoRoot
try {
    & $mavenPath -o -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Backend package failed.' }
    & $javaPath -jar (Join-Path $repoRoot 'target\admin-lab-0.1.0.jar')
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
