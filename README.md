CleanThat

WARNING: `cleanthat` is new. Expect issues of all kinds, which is not to say that you should not
[report](https://github.com/solven-eu/cleanthat/issues) them :)

# Motivation

The point of this project is to enable automatic refactoring of your code-base. As of 2022-12, it focuses on Java projects

Related projects:

- https://jsparrow.github.io/
- https://github.com/JnRouvignac/AutoRefactor

# Compatibility

Cleanthat is currently compatible with the following languages:

- java

# Installation

## Maven (Free)

See README: https://github.com/solven-eu/cleanthat/tree/master/maven

## Gradle (Free)

Pending for Spotless integration: https://github.com/diffplug/spotless/tree/main/plugin-gradle

## Github (Paid)

If your repository is hosted on Github.com:

        https://github.com/marketplace/cleanthat/

It is configured through a cleanthat.yaml file at the root of the repository (e.g. https://github.com/solven-eu/cleanthat/blob/master/cleanthat.yml).

It differs with mvn integration by fetching only relevant (e.g. modified) files, based on Github Events.

- https://docs.github.com/en/developers/webhooks-and-events/events/github-event-types#pullrequestevent
- https://docs.github.com/en/developers/webhooks-and-events/events/github-event-types#pushevent

# Key design decisions

As of 2022-12, this projects focuses on typical Java projects. Hence, it enables:

- Advanced Refactoring of .java files
- Advanced Formatting of .java files (to be dropped, to rely on Spotless)
- Advanced Formatting of pom.xml files (to be dropped, to rely on Spotless)
- Basic Formatting of .json, .xml, etc files (to be dropped, to rely on Spotless)

## Github App does not rely on Maven and Gradle
While we work on integrating CleanThat into Spotless, the Github CleanThat App does not rely on existing Maven (https://github.com/diffplug/spotless/tree/main/plugin-maven) and Gradle (https://github.com/diffplug/spotless/tree/main/plugin-gradle) plugins. The main reason for that is security. Indeed, while it would enable very setup over a project already integrating Spotless, it would open dangerous security breach as one could easily inject custom code as dependency of the maven/gradle plugin, which would enable one to extract CleanThat secrets (Github token, GPG key, etc).

## About Advanced Formatting of .java files

Refactoring .java files would break the code conventions. Hence, any refactoring operation should be followed by a formatting operation.
With mvn integration, once should follow the cleanthat step with some mvn formatter step.
With github integration, once may rely on CleanThat own .java formatting abilities.

There is multiple good options for formatting Java files:

- https://github.com/revelc/formatter-maven-plugin
- https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md (e.g. https://github.com/JnRouvignac/AutoRefactor/blob/master/pom.xml#L240)

# Last considerations

This software is provided WITHOUT ANY WARRANTY, and is available under the Apache License, Version 2. Any code loss caused by using this plugin is not the responsibility of the author(s). Be sure to use some source repository management system such as GIT before using this plugin.

Contributions are welcome.

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

### New computer, new GPG Key

https://stackoverflow.com/questions/29885887/gpg-no-default-secret-key-error-using-maven

        gpg --list-keys

https://keyring.debian.org/creating-key.html

        gpg --gen-key --default-new-key-algo=rsa4096/cert,sign+rsa4096/encr
        gpg --keyserver https://keyserver.ubuntu.com/ --send-key 90A8________________________________AAB7

BEWARE pool.sks-keyservers.net is down: https://www.reddit.com/r/GnuPG/comments/o5tb6a/keyservers_are_gone/

If it fails, upload manually:

        gpg \-\-armor \-\-export 90A8________________________________AAB7

## Re-run locally events in AWS:

See ITProcessLocallyDynamoDbEvent_CheckConfig
