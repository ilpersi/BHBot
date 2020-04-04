$BHBotExe = ".\BHBot.exe"
$BHBotZip = ".\BHBot.zip"
$ChromeDriverPath = ".\chromedriver.exe"
$JavaRunTimePath = ".\java-runtime\"
$BHBotBatPath = "..\bhbot.bat"
$InitBatPath = "..\init.bat"
$SettingsIniPath = "..\src\main\resources\settings.ini"

function safeDelete([string] $path, [bool] $Recurse = $false) {   
	if (Test-Path $path) {
		Write-Host "Deleting $path ..."
		if ($Recurse) {
			Remove-Item $path -Recurse
		} else {
			Remove-Item $path
		}
	}
}

safeDelete $BHBotZip

safeDelete $BHBotExe
Write-Host "Creating exe file..."
launch4jc conf.xml

safeDelete $JavaRunTimePath $true
Write-Host "Creating Java Distro..."
jlink.exe --no-header-files --no-man-pages --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.rmi,java.scripting,java.security.jgss,java.sql,jdk.unsupported --output $JavaRunTimePath

Write-Host "Creating BHBot zip"
$compress = @{
	Path= $JavaRunTimePath, $BHBotExe, $ChromeDriverPath #, $BHBotBatPath, $InitBatPath, $SettingsIniPath
	CompressionLevel = "Optimal"
	DestinationPath = $BHBotZip
}
Compress-Archive @compress

safeDelete $BHBotExe
safeDelete $JavaRunTimePath $true