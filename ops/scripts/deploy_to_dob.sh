#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP "mkdir -p /var/mooncake/config"
scp mooncake.env $REMOTE_USER@$SERVER_IP:/var/mooncake/config/mooncake.env
scp activity-sources.yml $REMOTE_USER@$SERVER_IP:/var/mooncake/config/activity-sources.yml
ssh $REMOTE_USER@$SERVER_IP <<EOF
  sudo docker stop mooncake || echo 'Failed to stop mooncake container'
  sudo docker rm mooncake || echo 'Failed to remove mooncake container'
  sudo docker run -d \
                  --env-file=/var/mooncake/config/mooncake.env \
                  -p 127.0.0.1:5000:3000 \
                  --link mongo:mongo \
                  --name mooncake \
                  dcent/mooncake
EOF
