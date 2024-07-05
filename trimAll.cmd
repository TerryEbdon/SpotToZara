@echo off
md trimmed
for %%f IN (*.mp3) DO call %~dp0\trim.cmd %%f
