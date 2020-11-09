@echo off

set week=%date:~12,13%
if %week%==一 goto :minusThree

echo wscript.echo dateadd("d",-1,date) >%tmp%\tmp.vbs
goto :run

:minusThree
echo wscript.echo dateadd("d",-3,date) >%tmp%\tmp.vbs
goto :run

:run
for /f "tokens=1,2,3 delims=/- " %%i in ('cscript /nologo %tmp%\tmp.vbs') do set y=%%i
for /f "tokens=1,2,3 delims=/- " %%i in ('cscript /nologo %tmp%\tmp.vbs') do set m=%%j
for /f "tokens=1,2,3 delims=/- " %%i in ('cscript /nologo %tmp%\tmp.vbs') do set d=%%k
if %m% LSS 9 set m=0%m%
if %d% LSS 9 set d=0%d%


rem winrar.exe , zip dis, zip file
"C:\Program Files\WinRAR\WinRAR.exe" a -ep1 "G:\我的雲端硬碟\THU\Program\CheckReport\zip\%y%-%m%-%d%.rar" "G:\我的雲端硬碟\THU\Program\CheckReport\output\"

rd "G:\我的雲端硬碟\THU\Program\CheckReport\output\" /s /q

md "G:\我的雲端硬碟\THU\Program\CheckReport\output"

exit