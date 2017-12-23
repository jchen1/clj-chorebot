#!/usr/bin/env bash

echo "Writing version.edn..."
lein run -m clj-chorebot.deploy
echo "Compiling uberjar..."
lein uberjar > /dev/null
echo "Uploading files..."
scp target/uberjar/clj-chorebot-0.1.0-SNAPSHOT-standalone.jar root@104.131.132.15:/chorebot/chorebot.jar
scp version.edn root@104.131.132.15:/chorebot/version.edn
echo "Restarting service..."
ssh root@104.131.132.15 "service chorebot restart"
echo "Done!"
