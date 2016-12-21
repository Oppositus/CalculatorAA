#!/usr/bin/env bash

sleep 1

cp -rf ./update/* .
rm -rf ./update
./calcaa.sh
