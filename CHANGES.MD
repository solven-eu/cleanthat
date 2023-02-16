This document is intended for CleanThat developers and users (e.g. through [Spotless](https://github.com/diffplug/spotless)).

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format

## [Unreleased]
### Fixed
* `CreateTempFilesUsingNioCases` now handle a null directory
### Added
* Additional mutator: `EmptyControlStatement` turns `{}` into ``
* Additional mutator: `UnnecessaryModifier` turns `public static final` into `` in interfaces
* Implemented generic `trimTrailingWhitespace`, `endWithNewline` and `index` from Spotless
### Changes
* `OptionalNotEmpty` is promoted in `SafeAndConsensualMutators`
* `UnnecessaryModifier` is promoted in `SafeAndConsensualMutators`
* `UnnecessaryFullyQualifiedName` is promoted in `SafeAndConsensualMutators`
* `MarkdownFormatterFactory` includes pattern is now defaulted to `*.MD` and `*.md`
* `JsonFormatterFactory` includes pattern is now defaulted to `*.json`

## [2.2] - 2023-01-07
### Fixed
* Fixes ImportOrderStep when the ordersFile parameters is used.
### Added
* Additional mutator: `LocalVariableTypeInference` turns `int i = 10;` into `var i = 10;`
* Additional composite mutators: `OptionalNotEmpty`, `PMDMutators`, `CheckStyleMutators` and `SonarMutators`
### Changes
* CleanThat Robot will open a single Review-Request per protected-branch (instead of opening one for each dirty event, with a random). The naming convention is `cleanthat/headfor-XXX-yyyy-MM-dd` where `XXX` is the protected branch name and `yyyy-MM-dd` is current day.
* Handle any source JDK version (with the help of [JavaVersion](https://github.com/codehaus-plexus/plexus-languages/blob/master/plexus-java/src/main/java/org/codehaus/plexus/languages/java/version/JavaVersion.java))
* Mutators can be included by their fully-qualified class name

## [2.1] - 2023-01-07
### Fixed
* Fix issue related to [dynamic IMutators detection](https://github.com/solven-eu/cleanthat/blob/master/java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/MutatorsScanner.java)

## [2.0] - 2023-01-06
* Spotless oriented release
### Added
* Added Spotless as an Engine
* Additional mutator for PMD.ComparisonWithNaN
* Additional mutator for JDK8 Streams: StreamAnyMatch
### Changes
* The concept of language has been replaced by engine. Instead of implementing language-specific formatters, CleanThat now targets wiring linter-engines (like Spotless).
* **POTENTIALLY BREAKING** Most CleanThat linters has been dropped


## [1.0] - 2021-08-12
### Added
* Initial release!