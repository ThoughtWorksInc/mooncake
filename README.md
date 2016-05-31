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

    lein stub

Go to localhost:3000 see the application running.
    
### Running test suite
    
To run all tests, use this command:

    lein test
    
Commands and aliases can be found in the project.clj file. 
    
### Running the prototype

Simply type:

```
gulp server
```

### Activity sources

Activities are expected in the following format:

    {
      @context:  "http://www.w3.org/ns/activitystreams",
      published: "2015-12-18T14:25:40.240Z",
      type:      "Add",  --- used to customise the feed
      
      object: {
        url:     "http://objective8.dcentproject.eu/objectives/41/questions/28",  (optional)
        name:    "Why?",
        type:    "Question"  --- used to set the action text
      },
        
      actor: {
        name:    "Jane Doe"
      },
      
      target:  (optional) {
        url:   "http://objective8.dcentproject.eu/objectives/41",  (optional)
        name:  "This Objective"
      }
    }
    
The timestamp uses the format YYYY-MM-DDThh:mm:ss.sZ as specified in the international standard ISO 8601.

They should be presented inside a collection:

    {
      @context:   "http://www.w3.org/ns/activitystreams",
      type:       "Collection",
      name:       "Activity stream",
      totalItems: 23,
      items: [
        <activities go here>
      ]
    }

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

To deploy using Docker, see [here](doc/Docker.md).

To deploy to Heroku, see [here](doc/Heroku.md).