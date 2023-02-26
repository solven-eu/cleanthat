This document is intended for CleanThat developers and users (e.g. through [Spotless](https://github.com/diffplug/spotless)).

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format

## [Unreleased]
### Fixed
* `LocalVariableTypeInference` handles better Unresolved types
* `LineEnding` management is dropped from `ILintFixer` and `ILintFixerWithPath`
* `ModifierOrder` is promoted in `SafeAndConsensual`
* `StringToString` is promoted in `SafeAndConsensual`
* `UnnecessaryModifier` does not strip anymore the `static` keyword of `MethodDeclaration`
* `IMutator` is generated into [MUTATORS.generated.MD](MUTATORS.generated.MD) by `RunGenerateDocumentation`
* `UseUnderscoresInNumericLiterals` is an additional PMD mutator
* `UseTextBlocks` is an additional mutator
* `UnnecessaryImport` is an additional mutator

## [2.6] - 2023-02-22
### Changes
* Add empty constructor in `CompositeWalkableMutator`
* `CompositeMutator.getIds()` do not return underlying identifiers anymore

## [2.5] - 2023-02-22
### Fixed
* Invalid reports of any IMutators as not being idempotent
* `CreateTempFilesUsingNioCases` now handle a null directory
* `LocalVariableTypeInference` does not replace assignments with different types (e.g. `ArrayList` assigned to a `List` variable)
* Introduces `IJavaparserMutator`, paving the way for other mutators (from Eclipse Cleanup, OpenRewrite, etc)

## [2.4] - 2023-02-19
### Fixed
* `AllIncludingDraftCompositeMutators` was misbehaving given `MutatorsScanner`

## [2.3] - 2023-02-19
### Fixed
* `CreateTempFilesUsingNioCases` now handle a null directory
### Added
* Additional mutator: `EmptyControlStatement` turns `{}` into ``
* Additional mutator: `UnnecessaryModifier` turns `public static final` into `` in interfaces
* Implemented generic `trimTrailingWhitespace`, `endWithNewline` and `index` from Spotless
* Composite Mutators can now be referenced by one of their identifier
### Changes
* `OptionalNotEmpty` is promoted in `SafeAndConsensual`
* `UnnecessaryModifier` is promoted in `SafeAndConsensual`
* `UnnecessaryFullyQualifiedName` is promoted in `SafeAndConsensual`
* `MarkdownFormatterFactory` includes pattern is now defaulted to `*.MD` and `*.md`
* `JsonFormatterFactory` includes pattern is now defaulted to `*.json`
* **BREAKING CHANGE** `isProductionReady` has been removed, and replaced by a fully-functional `includeDraft`
* **BREAKING CHANGE** A draft-mutator explicitly listed is applied even if `includeDraft` is false

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