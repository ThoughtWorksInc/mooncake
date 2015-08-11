#!/bin/bash

set -e

heroku git:remote --app mooncake-staging
heroku maintenance:on
heroku buildpacks:clear
heroku buildpacks:set https://github.com/heroku/heroku-buildpack-clojure
heroku buildpacks:add --index 1 https://github.com/heroku/heroku-buildpack-nodejs
heroku config:unset $(heroku config -s | awk -F "=" '{print $1}' ORS=' ')
echo "Setting heroku environment variables and suppressing output"
heroku config:set $(cat mooncake-staging.env | awk '{print}' ORS=' ') &> /dev/null
git push heroku master
heroku maintenance:off

