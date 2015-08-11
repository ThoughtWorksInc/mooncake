#!/bin/bash

set -e

heroku git:remote --app mooncake-staging
heroku maintenance:on
heroku buildpacks:clear
heroku buildpacks:set https://github.com/heroku/heroku-buildpack-clojure
heroku buildpacks:add --index 1 https://github.com/heroku/heroku-buildpack-nodejs
cat mooncake-staging.env | xargs heroku config:set
git push heroku master
heroku maintenance:off

