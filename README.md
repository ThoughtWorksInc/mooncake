 A D-CENT project: Secure notifications combined with w3 activity streams

["Mooncake"](https://en.wikipedia.org/wiki/Mooncake#Ming_revolution)

## Running locally
Before starting the server, build the views by running:

    gulp build

To start the web server, run:

    lein run

## Environment variables for deployment

- ```HOST``` and ```PORT``` are used to configure the Jetty webserver --- these default to ```localhost``` and ```3000``` when running locally
- ```BASE_URL``` is the url (including scheme) for the deployment --- this defaults to ```http://localhost:3000``` when running locally.  Note that the ```BASE_URL```, ```HOST``` and ```PORT``` may in general need to be set independently, depending on how the application is deployed.
- ```CLIENT_ID```, ```CLIENT_SECRET``` and ```AUTH_URL``` configure interaction with a running Stonecutter SSO instance, for signing into the application.

## Running the static frontend

### Getting started

First install [brew](http://brew.sh/)

```
brew install node
npm install
```

You also require gulp to be installed globally.

```
npm install -g gulp
```

Depending on system privileges you may need to install it globally with sudo:

```
sudo npm install -g gulp
```


### Running the prototype

####Simply type
```
gulp server
```

### Preview on Github Pages
```
gulp deploy
```

####Visit:

[thoughtworksinc.github.io/mooncake](http://thoughtworksinc.github.io/mooncake)
