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

The file should map a Map, with 1+N entries (1 entry for language-agnostic configuration (meta), and N entries for the N configured languages).

ps: CleanThat handles only Java for now.

Here is an example configuration:

    {
    "java": {
        "excludes": [
            "regex:.*/generated/.*"
        ],
        "encoding": "UTF-8",
        "processors": [
            {
                "engine": "eclipse_formatter",
                "configuration": "https://raw.githubusercontent.com/solven-eu/pepper/master/static/src/main/resources/eclipse/eclipse_java_code_formatter.xml"
            },
            {
                "engine": "revelc_imports",
                "configuration": {
                    "remove_unused": true,
                    "groups": "java.,javax.,org.,com.",
                    "staticGroups": "java,*"
                }
            },
            {
                "engine": "rules",
                "configuration": {
                }
            }
        ]
    },
    "meta": {
        "labels": [
            "cleanthat"
        ],
        "mutate_pull_requests": true,
        "mutate_main_branch": true
    }
    }
    
"java.excludes": an array to 