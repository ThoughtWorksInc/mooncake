## Deploying the application using Docker

First, install [Docker](https://www.docker.com/).
  
You can deploy the application using Docker. To do so, you will need three containers:
Mongo, Nginx and Mooncake.

### Starting a mongo container

To start a mongo container, run 

    docker run -d --name mooncake-mongo mongo
         
### Starting a Mooncake container

To run Mooncake you need 
 
* a mooncake.env file

To get a mooncake.env file, replace the values in the template found in the config folder with new ones for your application. The auth config is for integration with [stonecutter](https://github.com/d-cent/stonecutter), and the base URL should be your domain address.
  
Then run this command, replacing <env file path> with the path to wherever your environment variable file is stored.  

    docker run --env-file=<path to env file>/mooncake.env -p 3000:3000 --link mooncake-mongo:mongo -d --name mooncake --restart=on-failure dcent/mooncake


### Starting an Nginx container

To set up Nginx you need 

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
    	  return 301 https://<ip address>$request_uri;
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
    	    proxy_pass http://mooncake:3000;
    	    proxy_set_header Host $host;
    	    proxy_set_header X-Real-IP $remote_addr;
    	    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	        proxy_set_header X-Forwarded-Proto $scheme;
    	  }
    	}
    }



Finally, run the following command:

    docker run -v <absolute path to SSL certificates and keys directory>:/etc/nginx/ssl -v <absolute path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <absolute path to dhparam file>/dhparam.pem:/etc/nginx/cert/dhparam.pem -p 443:443 -p 80:80 --link mooncake:mooncake -d --name nginx-container nginx

