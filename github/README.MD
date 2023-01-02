# For users: 

    https://github.com/marketplace/cleanthat/


## Configuration

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
  
# For app administrator:

    https://github.com/organizations/solven-eu/settings/apps/cleanthat
    https://github.com/marketplace/cleanthat/hook