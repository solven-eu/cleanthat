CleanThat

# Disclaimer

`cleanthat` is fairly new. While it is tested on many large projects, you may encounter issues of all kinds. [**Please report them**](https://github.com/solven-eu/cleanthat/issues).

[![codecov](https://codecov.io/github/solven-eu/cleanthat/branch/master/graph/badge.svg?token=4K4O5GNH32)](https://codecov.io/github/solven-eu/cleanthat)

# Motivation

The point of this project is to enable **automatic cleaning of your code-base**. By cleaning, we include :

1. formatting (with an Eclipse stylesheet, Google-Java-Format, etc)
2. linting (given any [IMutator](MUTATORS.generated.MD))
3. refactoring (WIP)
4. migrating (from JUnit4 to JUnit5)

As of 2022-12, it focuses on Java projects, but enabling formatting various languages through Spotless.

Related projects:

- https://jsparrow.github.io/ (paid)
- https://github.com/JnRouvignac/AutoRefactor (free)
- https://github.com/openrewrite/rewrite-migrate-java (free)
- https://www.moderne.io/ (paid)
- https://eslint.org/docs/latest/extend/custom-formatters (free)
- https://errorprone.info/docs/refaster (free)
- https://github.com/walkmod/walkmod-pmd-plugin (free)
- https://github.com/XenoAmess/remove-unused-imports-maven-plugin (free, focused on imports)

# Changes

See [CHANGES.MD](CHANGES.MD)

- 2023-03-23: 2.12 Latest release
- ...
- 2023-02-06: 2.0 Major Release leveraging from/to [Spotless](https://github.com/diffplug/spotless)
- ...
- 2021-08-12: 1.0 Initial Release

# List of mutators

[![javadoc](https://javadoc.io/badge2/io.github.solven-eu.cleanthat/java/javadoc.svg)](https://javadoc.io/doc/io.github.solven-eu.cleanthat/java)

[MUTATORS.generated.MD](MUTATORS.generated.MD)

# Language Coverage

## As a Robot

Cleanthat Robot is currently compatible with the following languages:

- java (Spotless and OpenRewrite)
- pom.xml (Spotless)
- json (Spotless)
- xml (Spotless)
- yaml (Spotless)
- kotlin (Spotless)

see [FormatterFactory](spotless/src/main/java/eu/solven/cleanthat/spotless/FormatterFactory.java)

## As a library

Cleanthat Refactorer is currently compatible with the following languages:

- java (Cleanthat)
- java (OpenRewrite)
- java (Eclipse Cleanup)

# Limitations

- CleanThat processes files individually, which indices limited `Type` resolution. This enables cleaning files on a per-impacted-file basis (e.g. in a Github Pull-Request).
- The type resolution may be lifted through `cleanthat-maven-plugin` (TODO)

# Installation

## Maven (Free)

### Spotless `mvn` plugin (for pure `mvn` users)

`mvn` integration is available through Spotless: https://github.com/diffplug/spotless/tree/main/plugin-maven#cleanthat

[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](https://mvnrepository.com/artifact/com.diffplug.spotless/spotless-maven-plugin)

One can then clean its codebase with `mvn spotless:apply`

### Cleanthat `mvn` plugin (for GitHub App users)

See README: https://github.com/solven-eu/cleanthat/tree/master/maven

![Maven Central](https://img.shields.io/maven-central/v/io.github.solven-eu.cleanthat/java)(https://mvnrepository.com/artifact/io.github.solven-eu.cleanthat/cleanthat-maven-plugin)

Features :

- Apply full refactoring as it would be executed by the Github App, based on `.cleanthat` configuration
- Generate automatically an optimal Eclipse stylesheet based on your code-case

One liner (even without a `pom.xml`):

        mvn io.github.solven-eu.cleanthat:cleanthat-maven-plugin:cleanthat

or simply `mvn cleanthat:apply`

## Gradle (Free)

`gradle` integration is available through Spotless: https://github.com/diffplug/spotless/tree/main/plugin-gradle#cleanthat

[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.spotless-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.spotless)

## Github App (Free+Paid)

If your repository is hosted by Github.com, get zero-configuration cleaning with our [Github App](https://github.com/marketplace/cleanthat/) on Github marketplace.

It can configured through a `/.cleanthat/cleanthat.yaml` file at the root of the repository (e.g. [https://github.com/solven-eu/cleanthat/blob/master/.cleanthat/cleanthat.yaml](https://github.com/solven-eu/cleanthat/blob/master/.cleanthat/cleanthat.yaml)).

It differs from mvn/gradle integration by fetching only relevant (e.g. modified) files, based on Github Events.

- https://docs.github.com/en/developers/webhooks-and-events/events/github-event-types#pullrequestevent
- https://docs.github.com/en/developers/webhooks-and-events/events/github-event-types#pushevent

### Key Features

- Automatic generation of the initial configuration
- Clean branches which are not protected and head of a PullRequest

### Example configurations of happy Users:

- [Cleanthat](https://github.com/solven-eu/cleanthat/tree/master/.cleanthat) itself
- [Pepper](https://github.com/solven-eu/pepper/tree/master/.cleanthat)

## CI/CD (Github Actions, CircleCI, Jenkins, etc)

If you integrated Cleanthat through its maven or gradle options, you can get automatic remote cleaning with:

        ./mvnw spotless:check || ./mvnw spotless:apply && git commit -m"Spotless" && git push

# Key design decisions

As of 2022-12, this projects focuses on typical JVM projects. Hence, it enables:

- Advanced Linting of .java files
- Advanced Formatting of .java files (through Spotless)
- Advanced Formatting of pom.xml files (through Spotless)
- Basic Formatting of .json, .xml, etc files (through Spotless)

## Refactoring on a `per-single-source file` basis

One major goal of this project is to enable incremental refactoring on a per Pull-Request basis. Hence, the availability of the whole code-base and related binaries (e.g. `mvn` dependencies) is limited. Cleanthat focuses on cleaning individual source files.

Limitations :

- Can not refactor based on multiple source files information (e.g. type definition in a different file)
- Can not refactor based on binaries information (e.g. type definition from dependencies)
- Can refactor based on standard JRE classpath

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

## Compile locally

        git clone git@github.com:solven-eu/cleanthat.git
        mvn install -Pfast

The `-Pfast` profile may be necessary to circumvent cleanthat depenending on itself to apply `spotless` on itself.

Once done (or re-done after a release), you can simply:

        mvn install

## Deploy into Production (AWS):

        git push origin master:deploy-prd

## Release a new version (and deploy jars to Sonatype m2central):

        mvn release:clean release:prepare release:perform

### In case of failure

The release process may fail for various reasons:

- Sonatype timed-out

They, one would typically need to revert its local head, and force push master before the release. And delete the falsy tags:

    git tag -d v2.XX.RELEASE
    git push --delete origin v2.XX.RELEASE

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

## Release Issues

Issue with Javadoc. For instance:

- https://github.com/projectlombok/lombok/issues/2737

> mvn org.apache.maven.plugins:maven-javadoc-plugin:3.4.1:jar -Dmaven.javadoc.skip=false -PSonatype

