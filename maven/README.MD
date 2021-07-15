Can be tested with:

    mvn com.github.cormoran-io.cleanthat:cleanthat-maven-plugin:1.0-SNAPSHOT:cleanthat

Can be rebuild through:

    mvn install -pl :cleanthat-maven-plugin
    
If cleanthat dependencies are outdates:

    mvn install -PskipStyle -DskipITs -pl :cleanthat-maven-plugin -am -DskipTests

