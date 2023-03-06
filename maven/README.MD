# Spotless-maven-plugin

For per-module configuration, one should integrate [spotless-maven-plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven#cleanthat).

# Cleanthat-maven-plugin

## Purpose and limitation

This maven plugin enables reproducing the Github App behavior (which process a repository from its root, not per-module).

It also enable additional features like:

- Generation of a default Eclipse stylesheet based on existing .java files

## This maven plugin enables CleanThat features as done by CleanThat Github App

Its release lifecycle is synchronized with cleanthat. Its latest version is available at:

    https://mvnrepository.com/artifact/io.github.solven-eu.cleanthat/cleanthat-maven-plugin

This plugin requires a JDK 11.X

## Configure CleanThat mvn plugin

Most CleanThat goals can be execute without a specific configuration in the pom.xml.
Many CleanThat goals can be execute without any pom.xml (e.g. to call CleanThat logic out of a maven project)

One can shorten its cleanthat commands by adding in ${user.home}/.m2/settings.xml:

<pluginGroups>
  <pluginGroup>io.github.solven-eu.cleanthat</pluginGroup>
</pluginGroups>

reference: https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

## Goals

### cleanthat:apply

This Mojo enables one-shot run of a mutator over current directory.

     mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:apply -Dcleanthat.includeDraft=true
     
You can call a specific single rule:

     mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:apply -Dcleanthat.mutators=LocalVariableTypeInference
     
You can call most CleanThat rules with:

     mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:apply -Dcleanthat.mutators=SafeAndConsensual -Dcleanthat.mutators=SafeNutNotConsensual -Dcleanthat.includeDraft=true

### cleanthat:init

This will initialize cleanthat configuration with a standard configuration. It essentially generates a `.cleanthat/cleanthat.yaml` file at the root of the repository.

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:init

### cleanthat:cleanthat

This will apply cleanthat linting logic over the whole directory.

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:cleanthat
    
### cleanthat:eclipse_formatter-stylesheet

CleanThat for Java holds a module to apply an Eclipse Stylesheet. However, generating this stylesheet may be complex if the team not already on it (e.g. IntelliJ users).

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet

#### Timeout

As this process can be very slow, we default a timeout at 1 minute. It can be overriden through:

    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:eclipse_formatter-stylesheet -Dduration.limit=PT1H

Details about the syntax: https://en.wikipedia.org/wiki/ISO_8601#Durations

## Developers

Install the plugin in local mvn repository (if only the plugin has been updated):

    mvn install -pl :cleanthat-maven-plugin
    
With dependencies (if cleanthat core has been updated):

    mvn install -pl :cleanthat-maven-plugin -am -PskipStyle -DskipITs -DskipTests

Call the installed plugin with current SNAPSHOT:
    
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:2.7-SNAPSHOT:init
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:2.7-SNAPSHOT:eclipse_formatter-stylesheet
    mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:2.7-SNAPSHOT:cleanthat
    
### DEPRECATED Configure within a pom.xml

** This is not relevant anymore as cleanthat-maven-plugin is now a pom-less plugin**

	<build>
		<pluginManagement>
			<plugins>
				<plugin>
					<!-- mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:cleanthat -->
					<groupId>io.github.solven-eu.cleanthat</groupId>
					<artifactId>cleanthat-maven-plugin</artifactId>
					<version>${cleanthat.version}</version>
					<executions>
						<execution>
							<id>Clean the code</id>
							<goals>
								<goal>cleanthat</goal>
							</goals>
						</execution>
						<execution>
							<id>Check the code is clean</id>
							<goals>
								<goal>check</goal>
							</goals>
						</execution>
					</executions>
				</plugin>
			</plugins>
		</pluginManagement>

		<plugins>
			<plugin>
				<groupId>io.github.solven-eu.cleanthat</groupId>
				<artifactId>cleanthat-maven-plugin</artifactId>
				<!-- CleanThat should be executed only in/from the top-directory -->
				<inherited>false</inherited>
			</plugin>
		</plugins>
	</build>