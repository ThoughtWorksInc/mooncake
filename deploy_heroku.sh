#!/bin/bash

set -e

heroku git:remote --app mooncake-staging
heroku maintenance:on
heroku buildpacks:clear
heroku buildpacks:set https://github.com/heroku/heroku-buildpack-clojure
heroku buildpacks:add --index 1 https://github.com/heroku/heroku-buildpack-nodejs
heroku config -s | awk -F "=" '{print $1}' ORS=' ' | xargs heroku config:unset
echo "Setting environment variables and suppressing output"
cat mooncake-staging.env | xargs heroku config:set > /dev/null 2> /dev/null
git push heroku master
heroku maintenance:off

