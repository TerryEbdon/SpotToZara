@echo off
setlocal ENABLEDELAYEDEXPANSION
set spotDL="spotdl-4.2.11-win32.exe"

if not exist "%~dp0bin"\!spotDL! (
  echo.
  echo Installing !spotDL!
  call "%~dp0bin\SpotToZara.bat" install-spotdl "%~dp0bin"
)

if not exist "%~dp0bin"\!spotDL! (
  echo ERROR: spotDl install failed
  goto :EOF
)

if not exist "%~dp0bin"\ffmpeg.exe (
  echo.
  echo Installing ffmpeg
  call "%~dp0bin\SpotToZara.bat" install-ffmpeg "%~dp0bin"
)

if not exist "%~dp0bin"\ffmpeg.exe (
  echo ERROR: ffmpeg install failed
  goto :EOF
)
set/p url=Spotify playlist URL: 
if "!url!" neq "" (
  set/p plName=Playlist name: 
  if "!plName!" neq "" (
    if not exist "!plName!.lst" (
      echo Just a moment...

      "%~dp0bin\"!spotDL! --ffmpeg "%~dp0bin\ffmpeg.exe" --m3u "!plName!" "!url!"
      echo.
      set/p worked=Did the download succeed for all files [y/n]? 

      if /I "!worked!" equ "y" (
        echo Converting for ZaraRadio
        call "%~dp0bin\SpotToZara.bat" "!plName!"
      ) else (
        echo.
        echo Run the command again. It should continue from where it left off.
        echo If it still fails then contact support or log an issue at:
        echo https://github.com/TerryEbdon/SpotToZara/issues
        echo.
      )
    ) else (
      echo A ZaraRadio playlist with that name already exists.
    )
  ) else (
    echo A playlist name must be provided.
  )
) else (
  echo A Spotify playlist URL is required.
)

