CleanThat

# Motivation

The point of this project is to enable automatic cleaning of your code-base. Some excellent maven plugin exists (e.g. )

Related projects:

 - https://github.com/revelc/formatter-maven-plugin
 - https://github.com/revelc/impsort-maven-plugin
 - https://jsparrow.github.io/

# Compatibility

Cleanthat is currently compatible with the following languages:

 - java

# Installation

If your repository is hosted on Github.com:

    https://github.com/marketplace/cleanthat/

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

Deploy into Production (AWS):

    git push origin master:deploy-prd

Release a new version (and deploy jars to Sonatype m2central):

    mvn release:clean release:prepare release:perform