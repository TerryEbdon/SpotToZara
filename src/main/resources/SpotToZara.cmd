@echo off
setlocal ENABLEDELAYEDEXPANSION
set spotdl=spotdl-4.2.5-win32
where/q !spotDL!
if ERRORLEVEL 1 (
  echo.
  echo ERROR: Can't find !spotDL!
) else (
  where/q ffmpeg
  if ERRORLEVEL 1 (
    echo.
    echo ERROR: Can't find ffmpeg
  ) else (
    set/p url=Spotify playlist URL: 
    if not !url!=="" (
      set/p plName=Playlist name: 
      if not !plName!=="" (
        if not exist "!plName!.lst" (
          echo Just a moment...
          !spotDL! --m3u "!plName!" "!url!"
          echo.
          set/p worked=Did the download succeed for all files [y/n]? 

          if /I "!worked!"=="y" (
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
  )
)
