#!/usr/bin/env bash

lein uberjar
scp target/uberjar/clj-chorebot-0.1.0-SNAPSHOT-standalone.jar root@104.131.132.15:/chorebot
ssh root@104.131.132.15 "service chorebot restart"
