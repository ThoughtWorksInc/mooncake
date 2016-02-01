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
- ```CLIENT_ID```, ```CLIENT_SECRET``` and ```AUTH_URL``` configure interaction with a running [stonecutter](https://github.com/d-cent/stonecutter) 
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

## Adding translations

The translation files can be found at ```resources/lang/```. To support a new language, you should create a new 
```{language}.yml``` file and add to the ```client_translations.clj``` and ```time_translations.cljs``` files.

You can add any unsupported activity or object types to the ```activity-type``` section in ```{language}.yml``` in the following format:

    action-text-{object type}: action text here
    customise-feed-{activity type}: text here for customise feed view

For example, 

    activity-type:
      action-text-objective: created an objective
      customise-feed-create: Created content


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
 
## Deploying the application using docker
  
You can deploy the application using Docker. To do so, you will need three containers:
Mongo, Nginx and Mooncake.

#### Starting a mongo container

To start a mongo container, run 

    docker run -d --name mooncake-mongo mongo
    
#### Starting an Nginx container

To access the application you must run a reverse proxy, for example Nginx, that redirects to it, adding the following to the headers
    
    "X-Real-IP: <proxy ip>" 
    "X-Forwarded-For: <proxy ip>"
    "X-Forwarded-Proto: https"

To use Nginx for this you need 

* an SSL certificate and key
* a dhparam.pem file
* an nginx.conf file

You can acquire an SSL certificate and key online inexpensively. You should receive a pair of files, for instance stonecutter.crt and stonecutter.key. Store them in their own directory somewhere safe.

You can generate a dhparam.pem file by running: 
    
    openssl dhparam -rand – 2048 > dhparam.pem
 
You can create an nginx.conf file by copying the following into a new file and replacing the <> appropriately:

    
    events {
    }
    http {
      	server {
    	  listen 80;
    	  return 301 $request_uri;
    	}
    	server {
    	  listen 443 ssl;
    
    	  ssl_certificate /etc/nginx/ssl/<ssl certificate name>.crt;
    	  ssl_certificate_key /etc/nginx/ssl/<ssl key name>.key;
    
    	  ssl_session_cache shared:SSL:32m;
    	  ssl_session_timeout 10m;
    
    	  ssl_dhparam /etc/nginx/cert/dhparam.pem;
    	  ssl_protocols TLSv1.2 TLSv1.1 TLSv1;
    
    	  location / {
    	    proxy_pass http://<docker ip>:5000;
    	    proxy_set_header Host $host;
    	    proxy_set_header X-Real-IP $remote_addr;
    	    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	    proxy_set_header X-Forwarded-Proto $scheme;
    	  }
    	}
    }



Finally, run the following command:

    docker run -v <absolute path to SSL certificates and keys directory>:/etc/nginx/ssl -v <absolute path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <absolute path to dhparam file>/dhparam.pem:/etc/nginx/cert/dhparam.pem -p 443:443 -d --name nginx-container nginx

        
#### Starting a Mooncake container

To run Mooncake you need 
 
* a mooncake.env file

To get a mooncake.env, copy the template that is found in the config folder and replace the values with new ones for your application. The client ID and secret are for integration with stonecutter, the auth_url should be your domain address and the base URL should be your docker IP address.
 
Then run the following command
    
    docker build -t mooncake <path to mooncake project files>/mooncake
 
Finally, run this command, replacing <env file path> with the path to wherever your environment variable file is stored.  

    docker run --env-file=<path to env file>mooncake.env -p 5000:3000 --link mooncake-mongo:mongo -d --name mooncake mooncake
