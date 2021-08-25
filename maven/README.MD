Can be tested with:

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:cleanthat

Can be rebuild through:

    mvn install -pl :cleanthat-maven-plugin
    
If cleanthat dependencies are outdates:

    mvn install -PskipStyle -DskipITs -pl :cleanthat-maven-plugin -am -DskipTests

# Eclipse Stylesheet Generation

mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet