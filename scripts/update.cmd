timeout 1

xcopy update . /E /R /Y
rd /S /Q update
del instruments.sqlite /F /Q

start calcaa.cmd