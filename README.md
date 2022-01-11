CleanThat

WARNING: `cleanthat` is new. Expect issues of all kinds which is not to say that you should not
https://github.com/solven-eu/cleanthat/issues[report] them :)

# Motivation

The point of this project is to enable automatic cleaning of your code-base. Some excellent maven plugin exists (e.g. )

Related projects:

 - https://github.com/revelc/formatter-maven-plugin
 - https://github.com/revelc/impsort-maven-plugin
 - https://jsparrow.github.io/

# Compatibility

Cleanthat is currently compatible with the following languages:

 - java
 - scala 
 - json

# Installation

If your repository is hosted on Github.com:

    https://github.com/marketplace/cleanthat/
    
Else: You can spsonsor it (Gitlab, OnPremise, ...)

# Last considerations

This software is provided WITHOUT ANY WARRANTY, and is available under the Apache License, Version 2. Any code loss caused by using this plugin is not the responsibility of the author(s). Be sure to use some source repository management system such as GIT before using this plugin.

Contributions are welcome.

# Configuration

CleanThat is configured through a cleanthat.json file at the root of the repository.

Here is an example configuration:

```yaml
syntax_version: "2021-08-02"
meta:
  labels:
  - "cleanthat"
  refs:
    branches:
    - "refs/heads/develop"
    - "refs/heads/main"
    - "refs/heads/master"
source_code:
  excludes:
  - "regex:.*/generated/.*"
  encoding: "UTF-8"
  line_ending: "LF"
languages:
- language: "java"
  language_version: "11"
  source_code:
    includes:
    - "regex:.*\\.java"
  processors:
  - engine: "rules"
    parameters:
      production_ready_only: true
  - engine: "revelc_imports"
    parameters:
      # Organize imports like in Eclipse
      remove_unused: true
      groups: "java.,javax.,org.,com."
      static_groups: "java,*"
  # Use Spring formatting convention
  - engine: "spring_formatter"
    parameters: {}
```

This can be generated into an existing repository with:

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:init

# Using Eclipse Formatter

An alternative to spring_formatter is eclipse_formatter. It takes as parameter an url like:

A public http(s) URL:

    https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml
    
A file in local repository (root being assumed based on Git root/Maven top module)
    
    code:/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml

Eclipse Stylesheets can exported to XML through:

- Go into Preferences
- Select Java > Code Style > Formatter
- Click on 'Export All'

CleanThat will accept only configuration with a single profile in them. If multiple profiles are found, we will rely on the first profile.

## Automatic generation of Eclipse Stylesheet

The maven plugin enables generating an Eclipse Stylesheet minimizing changes over a clean repository:

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet

see maven/README.MD

# Notes for maintainers

## Deploy into Production (AWS):

    git push origin master:deploy-prd

## Release a new version (and deploy jars to Sonatype m2central):

    mvn release:clean release:prepare release:perform

## Re-run locally events in AWS:

See ITProcessLocallyDynamoDbEvent_CheckConfig
