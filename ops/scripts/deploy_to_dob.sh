#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/mooncake/target"
ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/mooncake/config"
scp target/*-standalone.jar $REMOTE_USER@$SERVER_IP:/var/mooncake/target/mooncake-standalone.jar
scp mooncake.env $REMOTE_USER@$SERVER_IP:/var/mooncake/config/mooncake.env
scp activity-sources.yml $REMOTE_USER@$SERVER_IP:/var/mooncake/config/activity-sources.yml
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop mooncake || echo 'Failed to stop mooncake container'
  sudo docker rm mooncake || echo 'Failed to remove mooncake container'
  sudo docker run -d -v /var/mooncake/target:/var/mooncake \
                     -v /var/mooncake/config:/var/mooncake/config \
                     -p 127.0.0.1:5000:3000 \
                     --name mooncake \
                     --env-file=/var/mooncake/config/mooncake.env \
                     --link mongo:mongo \
                     java:8 bash -c 'java -Dlog4j.configuration=log4j.dev -jar /var/mooncake/mooncake-standalone.jar'
EOF
