timeout 1

xcopy update . /E /R /Y
rd /S /Q update

start calcaa.cmd