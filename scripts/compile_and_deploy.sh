#!/usr/bin/env bash

lein run -m clj-chorebot.deploy
lein uberjar
scp target/uberjar/clj-chorebot-0.1.0-SNAPSHOT-standalone.jar root@104.131.132.15:/chorebot/chorebot.jar
scp version.edn root@104.131.132.15:/chorebot/version.edn
ssh root@104.131.132.15 "service chorebot restart"
