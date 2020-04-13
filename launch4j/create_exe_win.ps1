<#
# This script is used to pack the BHBot executable together with its dependencies in one signe zip file.
# To make sure this works correctly you have to add to your PATH environment valiables the directories containing the following executables:
# - launch4jc.exe -> this is used to create the exe file from the jar one
# - jlink.exe -> This is used to create the modularized java run time environment that will be distributed together with software
#
# Before running this script, make sure to bypass execution policies:
# Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
#>

$BHBotExe = ".\BHBot.exe"
$BHBotJar = "..\BHBot.jar"
$BHBotZip = ".\BHBot.zip"
$ChromeDriverPath = ".\chromedriver.exe"
$JavaRunTimePath = ".\java-runtime\"

# Unused paths
# $BHBotBatPath = "..\bhbot.bat"
# $InitBatPath = "..\init.bat"
# $SettingsIniPath = "..\src\main\resources\settings.ini"

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

# we clean already existing archives
safeDelete $BHBotZip

# we clean aready existing executables and re-create new from scratch
safeDelete $BHBotExe
Write-Host "Creating exe file..."
launch4jc conf.xml

# we clean the modularized run time and re-create it again
safeDelete $JavaRunTimePath $true
Write-Host "Creating Java Distro..."
# IMPRTANT: always remember to manually add jdk.crypto.ec if you change the linked libraries!!!
jlink.exe --no-header-files --no-man-pages --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,jdk.crypto.ec,java.rmi,java.scripting,java.security.jgss,java.sql,jdk.unsupported --output $JavaRunTimePath

# we pack everything together
Write-Host "Creating BHBot zip"
$compress = @{
	Path= $JavaRunTimePath, $BHBotExe, $ChromeDriverPath, $BHBotJar #, $BHBotBatPath, $InitBatPath, $SettingsIniPath
	CompressionLevel = "Optimal"
	DestinationPath = $BHBotZip
}
Compress-Archive @compress

# final clean-up
safeDelete $BHBotExe
safeDelete $JavaRunTimePath $true