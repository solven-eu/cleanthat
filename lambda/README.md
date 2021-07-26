To rebuild with dependencies from root:

     mvn clean install -pl :lambda -am

To deploy:

     mvn lambda:deploy-lambda -pl :lambda
    
Issue with startup on Lambda? Start by testing in local:

    java -jar target/lambda-1.0-SNAPSHOT-aws.jar eu.solven.cleanthat.lambda.step0_checkWebhook.CheckWebhooksHandler

java.lang.ClassNotFoundException
-> Check Handlers in pom

https://github-api.kohsuke.org/githubappjwtauth.html
    
    openssl pkcs8 -topk8 -inform PEM -outform DER -in ~/Dropbox/Solven/Dev/CleanThat/cleanthat.2020-05-19.private-key.pem -out ~/Dropbox/Solven/Dev/CleanThat/github-api-app.private-key.der -nocrypt



Lost note:

    java -jar lambda/target/*.jar
    
    
    
    
    https://aws.amazon.com/fr/premiumsupport/knowledge-center/custom-headers-api-gateway-lambda/
    
    
reference: https://stackoverflow.com/questions/66132915/what-is-the-specific-format-message-attributes-in-aws-api-gateway-integration-wi

Request method: POST
Accept: */*
content-type: application/json
User-Agent: GitHub-Hookshot/077ef99
X-GitHub-Delivery: d16c2602-ed91-11eb-89c6-737a85c68172
X-GitHub-Event: push
X-GitHub-Hook-ID: 212898303
X-GitHub-Hook-Installation-Target-ID: 65550
X-GitHub-Hook-Installation-Target-Type: integration

https://docs.aws.amazon.com/apigateway/latest/developerguide/request-response-data-mappings.html


    {"X-GitHub-Event": {"DataType": "String", "StringValue": "$request.header.X-GitHub-Event"}, "X-GitHub-Delivery": {"DataType": "String", "StringValue": "$request.header.X-GitHub-Delivery"}, "X-GitHub-Hook-ID": {"DataType": "String", "StringValue": "$request.header.X-GitHub-Hook-ID"}, "User-Agent": {"DataType": "String", "StringValue": "$request.header.User-Agent"}}
    FAILS
    
    {"CUSTOM-ATTRIBUTE-NAME": {"DataType": "String", "StringValue": "$request.header.User-Agent"}}
    FAILS
    
    {"CUSTOM-ATTRIBUTE-NAME": {"DataType": "String", "StringValue": "Hardcoded"}}
    OK
    
    MessageAttribute.1.Name=my_attribute_name_1&MessageAttribute.1.Value=my_attribute_value_1&MessageAttribute.1.Type=String