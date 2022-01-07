# This maven plugin enables CleanThat features

Its release lifecycle is synchronized with cleanthat. Its latest version is available at:

    https://mvnrepository.com/artifact/io.github.solven-eu.cleanthat/cleanthat-maven-plugin

This plugin requires a JDK 11.X

# Configure CleanThat mvn plugin

Most CleanThat goals can be execute without a specific configuration in the pom.xml.
Many CleanThat goals can be execute without any pom.xml (e.g. to call CleanThat logic out of a maven project)

One can shorten its cleanthat commands by adding in ${user.home}/.m2/settings.xml:

<pluginGroups>
  <pluginGroup>io.github.solven-eu.cleanthat</pluginGroup>
</pluginGroups>

reference: https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

# Goals

## cleanthat:init

This will initialize cleanthat configuration with a standard configuration. It essentially generates a /cleanthat.yaml file at the root of the repository.

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:init

## cleanthat:init

This will initialize cleanthat configuration with a standard configuration. It essentially generates a /cleanthat.yaml file at the root of the repository.

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:init

## cleanthat:cleanthat

This will apply cleanthat linting logic.

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:cleanthat
    
## cleanthat:eclipse_formatter-stylesheet

CleanThat for Java holds a module to apply an Eclipse Stylesheet. However, generating this stylesheet may be complex if the team not already on it (e.g. IntelliJ users).

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet

### Timeout

As this process can be very slow, we default a timeout at 1 minute. It can be overriden through:

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet -Dduration.limit=PT1H

Details about the syntax: https://en.wikipedia.org/wiki/ISO_8601#Durations

# Developers

Install the plugin in local mvn repository (if only the plugin has been updated):

    mvn install -pl :cleanthat-maven-plugin
    
With dependencies (if cleanthat core has been updated):

    mvn install -pl :cleanthat-maven-plugin -am -PskipStyle -DskipITs -DskipTests

Call the installed plugin with current SNAPSHOT:
    
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:init
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:eclipse_formatter-stylesheet
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:cleanthat
