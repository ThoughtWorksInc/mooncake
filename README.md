[![Build Status](https://snap-ci.com/d-cent/mooncake/branch/master/build_image)](https://snap-ci.com/d-cent/mooncake/branch/master)

 A D-CENT project: Secure notifications combined with w3 activity streams

["Mooncake"](https://en.wikipedia.org/wiki/Mooncake#Ming_revolution)

## Development VM

You can develop and run the application in a VM to ensure that the correct versions of Mooncake's dependencies
are installed. You will need [VirtualBox][], [Vagrant][] and [Ansible][] installed.

First, clone the repository.

Navigate to the ops/ directory of the project and run:

    vagrant up development

The first time this is run, it will provision and configure a new VM.

When the VM has started, access the virtual machine by running:

    vagrant ssh

The source folder will be located at `/var/mooncake`.

After initial setup, navigate to the source directory with:

    cd /var/mooncake

[Vagrant]: https://www.vagrantup.com
[Ansible]: http://docs.ansible.com/ansible/intro_installation.html
[VirtualBox]: https://www.virtualbox.org/

### Running

To start the app, run:

    ./start_app_vm.sh
    
### Running test suite
    
To run all tests, use this command:

    lein test
    
### Running the prototype

Simply type:

```
gulp server
```


Commands and aliases can be found in the project.clj file. 

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


## Deployment
 
## Deploying the application using Docker
  
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

You can acquire an SSL certificate and key online inexpensively. You should receive a pair of files, for instance mooncake.crt and mooncake.key. Store them in their own directory somewhere safe.

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
    	    proxy_pass http://<docker ip>:3000;
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

To get a mooncake.env file, replace the values in the template found in the config folder with new ones for your application. The auth config is for integration with [stonecutter](https://github.com/d-cent/stonecutter), and the base URL should be your domain address.
  
Then run this command, replacing <env file path> with the path to wherever your environment variable file is stored.  

    docker run --env-file=<path to env file>/mooncake.env -p 3000:3000 --link mooncake-mongo:mongo -d --name mooncake d-cent/mooncake
