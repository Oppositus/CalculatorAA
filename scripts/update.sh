#!/usr/bin/env bash

sleep 1

cp -rf ./update/* .
rm -rf ./update
rm -f instruments.sqlite

./calcaa.sh
