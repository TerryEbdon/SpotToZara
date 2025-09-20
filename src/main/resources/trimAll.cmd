@echo off
md trimmed
for %%f IN (*.mp3) DO (
  echo Trimming "%%f"
  call "%~dp0\trim.cmd" "%%f"
)
