@echo off
setlocal 

rmdir /s /q deps
mkdir deps
SET TARGET_DIR=%~dp0deps
SET DEPS_ROOT=%~dp0..\..\Grouped-Projects\programming-articles\

for %%s in (app-providers model full-access-dynamodb ) do (
  echo %DEPS_ROOT%%%s
  cd  %DEPS_ROOT%%%s
  rmdir /s /q build
  call gradle jar
  move build\libs\*.jar %TARGET_DIR%
)
