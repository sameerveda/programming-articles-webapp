@echo off
setlocal

if "%1"=="" (
 echo no command specified
 goto:help
)

if %1==commit goto:git

if %1==yarn (
 copy package.json  .myfiles\backup\package-1.json
 copy yarn.lock  .myfiles\backup\yarn-1.lock
 echo copy package.json  .myfiles\backup\package-1.json
 echo copy yarn.lock  .myfiles\backup\yarn-1.lock
 goto:eof
)
IF %1==s  (
node live-server.js
goto:eof
)

if %1==swa (
  start cmd /c "gulp wa"
  start cmd /c "node live-server.js"
  goto:eof
)

if %1==collect (
  cd C:\Users\Sameer\Documents\MEGA\programming_backup\CodePens\app
  run pc
  goto:eof
)


set first=%1
set second=%2
set RESTVAR=

for /f "tokens=1,* delims= " %%a in ("%*") do set RESTVAR=%%b

if %first%==change (
   echo %RESTVAR%
  node my_dev\change-mode.js %RESTVAR%
  goto:eof
)
echo unknown command: %*
:help
echo usage  
 echo     s    run live-server
 
goto:eof

:git
git add -A
git status

set MSG=
set /P MSG=git commit message: 
if "%MSG%"=="" (
  set MSG=modified
)
git commit -m "%MSG%"

