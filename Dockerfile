FROM dcent/clojure-npm-grunt-gulp

COPY . /usr/src/app
RUN lein with-profile production deps && \
    npm install && \
    rm -rf node_modules/optipng-bin && \
    npm install gulp-imagemin && \
    gulp build && \
    lein uberjar

WORKDIR /usr/src/app/target

CMD java -jar mooncake-0.1.0-SNAPSHOT-standalone.jar