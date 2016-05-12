# Configuration

The following environment variables can be passed to the application.

## Required:

- **CLIENT_ID** - client id for your stonecutter auth config
- **CLIENT_SECRET** - client secret for your stonecutter auth config
- **AUTH_URL** - some auth provider

## Optional:

- **HOST** - defaults to localhost
- **PORT** - defaults to 3000
- **BASE_URL** - your application URI or IP address. Defaults to localhost:3000
- **ACTIVITY_SOURCE_FILE** - the relative path to the file containing the activity streams information - see resources/activity-sources.yml
- **SYNC_INTERVAL** - how often the URLS in your activity source file is polled in seconds, defaults to 60