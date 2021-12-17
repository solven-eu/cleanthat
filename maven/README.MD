# This maven plugin enables CleanThat features

Its release lifecycle is synchronized with cleanthat. Its latest version is available at:

    https://mvnrepository.com/artifact/io.github.solven-eu.cleanthat/cleanthat-maven-plugin
    
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

# Developers

Install the plugin in local mvn repository (if only the plugin has been updated):

    mvn install -pl :cleanthat-maven-plugin
    
With dependencies (if cleanthat core has been updated):

    mvn install -pl :cleanthat-maven-plugin -am -PskipStyle -DskipITs -DskipTests

Call the installed plugin with current SNAPSHOT:
    
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:init
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:eclipse_formatter-stylesheet
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:1.1-SNAPSHOT:cleanthat
