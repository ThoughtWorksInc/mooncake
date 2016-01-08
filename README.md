[![Build Status](https://snap-ci.com/d-cent/mooncake/branch/master/build_image)](https://snap-ci.com/d-cent/mooncake/branch/master)

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
- ```CLIENT_ID```, ```CLIENT_SECRET``` and ```AUTH_URL``` configure interaction with a running [Stonecutter](https://github.com/d-cent/stonecutter) 
SSO instance, for signing into the application.
- ```ACTIVITY_SOURCE_FILE``` is the YAML file containing the data pertaining to your activity streams --- if this 
variable is not provided, activities are loaded from the sources in the ```resources/activity-sources.yml``` file.

### Activity sources

Activities are expected in the following format:

    {
      @context:       "http://www.w3.org/ns/activitystreams",
      published:      "2015-12-18T14:25:40.240Z",
      @type:          "Add",  --- used to customise the feed
      
      object: {
        url:          "http://objective8.dcentproject.eu/objectives/41/questions/28",  (optional)
        displayName:  "Why?",
        @type:        "Question"  --- used to set the action text
      },
        
      actor: {
        displayName:  "Jane Doe"
      },
      
      target:  (optional) {
        url:          "http://objective8.dcentproject.eu/objectives/41",  (optional)
        displayName:  "This Objective"
      }
    }
    
The timestamp uses the format YYYY-MM-DDThh:mm:ss.sZ as specified in the international standard ISO 8601.

The activity source must support the ```to``` and ```from``` query parameters, returning activities with a 
published time less than or greater than the provided timestamp respectively. For example, 
```https://objective8.dcentproject.eu/as2/activities?from=2015-12-22T14:00:00.000Z``` should return all activities that 
occurred after 2pm on the 22nd December 2015.

#### Adding translations

The translation files can be found at ```resources/lang/{language}.yml```. You should add any unsupported activity or 
object types to the ```activity-type``` section in the following format:

    action-text-{object type}: action text here
    customise-feed-{activity type}: text here for customise feed view

For example, 

    activity-type:
      action-text-objective: created an objective
      customise-feed-create: Created content


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