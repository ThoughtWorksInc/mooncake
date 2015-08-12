#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/mooncake/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/mooncake/config"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/mooncake/target/mooncake-standalone.jar
scp mooncake-$SNAP_STAGE_NAME.env $REMOTE_USER@$SERVER_IP:/var/mooncake/config/mooncake.env
#scp logo.svg $REMOTE_USER@$SERVER_IP:/data/mooncake/static/logo.svg
#scp dcent-favicon.ico $REMOTE_USER@$SERVER_IP:/data/mooncake/static/dcent-favicon.ico
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop mooncake || echo 'Failed to stop mooncake container'
  sudo docker rm mooncake || echo 'Failed to remove mooncake container'
  sudo docker run -d -v /var/mooncake/target:/var/mooncake \
                     -p 127.0.0.1:5000:3000 --name mooncake \
                     --env-file=/var/mooncake/config/mooncake.env \
                     java:8 bash -c 'java -Dlog4j.configuration=log4j.dev -jar /var/mooncake/mooncake-standalone.jar'
EOF
