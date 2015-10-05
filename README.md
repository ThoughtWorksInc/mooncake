 A D-CENT project: Secure notifications combined with w3 activity streams

["Mooncake"](https://en.wikipedia.org/wiki/Mooncake#Ming_revolution)

## Running locally
Before starting the server, build the views by running:

    lein gulp

To start the web server, run:

    lein run
    
### Running test suite
    
#### To run all tests, run this command

    lein test


Commands and aliases can be found in the project.clj file. 

## Environment variables for deployment

- ```HOST``` and ```PORT``` are used to configure the Jetty webserver --- these default to ```localhost``` and
```3000``` when running locally
- ```BASE_URL``` is the url (including scheme) for the deployment --- this defaults to ```http://localhost:3000``` when
running locally.  Note that the ```BASE_URL```, ```HOST``` and ```PORT``` may in general need to be set independently,
depending on how the application is deployed.
- ```CLIENT_ID```, ```CLIENT_SECRET``` and ```AUTH_URL``` configure interaction with a running Stonecutter SSO instance,
for signing into the application.

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

#Build and Deploy

##Development Deployment
Run the develop code in a VM. May require some manual steps

##Production Like Deployments
These deployments are trying to be as production like as possible. All use the same Ansible playbook with different 
inventories per environment.

###Local 
This is used to run the production like deployment locally. It can be started up by using the following Vagrant command
from within the ops folder.

```
vagrant up default
```

Once this has run the VM is ready to go and the mooncake repo is mapped into /var/mooncake on the VM. At this
point you need to do so some manual steps:

- Compile the uberjar

```
lein uberjar
```

- Start the docker container. Within the VM run the following:

```
sudo docker run \
         -v /var/mooncake/target:/var/mooncake \
         -p 5000:3000 \
         -e "CLIENT_ID=FOO" \
         -e "CLIENT_SECRET=SHHHH" \
         -e "AUTH_URL=https://some.auth.provider.com" \
         -e "BASE_URL=https://192.168.50.70" \
         -e "HOST=0.0.0.0" \
         --name mooncake java:8 bash -c 'java -jar /var/mooncake/mooncake-0.1.0-SNAPSHOT-standalone.jar'
```
###Remote
Provisioning is handled by the build pipeline. It runs ```./ops/scripts/provision_dob_staging.sh``` to provision the Digital 
Ocean droplet. Then once that is complete it runs: ```./ops/scripts/deploy_to_dob.sh``` to start the mooncake container. 