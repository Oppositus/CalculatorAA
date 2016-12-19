timeout 1

xcopy ./update/* . /s /e
rd ./update /s /q

start calcaa.cmd