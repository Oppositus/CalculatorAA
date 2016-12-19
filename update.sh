#!/usr/bin/env bash

sleep 1000

cp -rf ./update/* .
rm -rf ./update
./calcaa.sh
