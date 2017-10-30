#!/bin/bash

killall postgres
rm -rf pg
initdb pg
postgres -D pg &
sleep 1
createdb chores

