To rebuild with dependencies from root:

     mvn clean install -pl :lambda -am

To deploy:

     mvn lambda:deploy-lambda -pl :lambda
    
    
    
https://github-api.kohsuke.org/githubappjwtauth.html
    
    openssl pkcs8 -topk8 -inform PEM -outform DER -in ~/Dropbox/Solven/Dev/CleanThat/cleanthat.2020-05-19.private-key.pem -out ~/Dropbox/Solven/Dev/CleanThat/github-api-app.private-key.der -nocrypt
