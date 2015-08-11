sudo docker run 
    -v /var/mooncake/target:/var/mooncake \
    -p 5000:3000 \
    -e "SECURE=false" \
    -e "CLIENT_ID=FOO" \
    -e "CLIENT_SECRET=SHHHH" \
    -e "AUTH_URL=https://some.auth.provider.com" \
    -e "BASE_URL=https://192.168.50.70" \
    -e "HOST=0.0.0.0" \
    --name mooncake java:8 bash -c 'java -jar /var/mooncake/mooncake-0.1.0-SNAPSHOT-standalone.jar'
