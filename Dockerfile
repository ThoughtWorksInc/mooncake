FROM dcent/clojure-with-npm:0

RUN mkdir /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein with-profile production deps
COPY . /usr/src/app
RUN apt-get update
RUN apt-get -y install build-essential
RUN npm install -g gulp
RUN npm install
RUN rm -rf node_modules/optipng-bin
RUN npm install gulp-imagemin
RUN gulp build
RUN lein uberjar
WORKDIR /usr/src/app/target

CMD java -jar mooncake-0.1.0-SNAPSHOT-standalone.jar