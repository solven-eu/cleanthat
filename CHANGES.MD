This document is intended for CleanThat developers and users (e.g. through [Spotless](https://github.com/diffplug/spotless)).

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format

## [2.18 - Unreleased]

### Fixed

* `UnnecessaryFullyQualifiedName` now handle FullyQualifiedName on anonymous/nested classes when the nesting class is imported.

### Changes

* Change notified by PR from @blacelle to @solven-eu/cleanthat-notify

## [2.17]

### Fixed

* `ArithmethicAssignment` should not assume String concatenation is symmetric.

## [2.16] - 2023-05-24

### Fixed

* [`UnnecessaryImport`] removes star import from same package](https://github.com/solven-eu/cleanthat/issues/553)

## [2.15] - 2023-05-23

### Added

* `ArithmeticOverFloats` is introduced
* `CastMathOperandsBeforeAssignement` is introduced
* `ArithmethicAssignment` is introduced
* `UseOptionalOfNullable` is introduced
* `SimplifyOptionalVariablesWithMap` is introduced
* `OptionalWrappedIfToFilter` is introduced
* `OptionalWrappedVariableToMap` is introduced
* `ForEachIfBreakToStreamFindFirst` is introduced
* `ForEachIfBreakElseToStreamTakeWhile` is introduced
* `UsePredefinedStandardCharset` is introduced
* `StringFromString` is introduced
* `OptionalMapIdentity` is introduced
* `StreamMapIdentity` is introduced
* `StreamForEachNestingForLoopToFlatMap` is introduced
* `StreamFlatMapStreamToFlatMap` is introduced

## [2.14] - 2023-04-06

### Fixed

* `SimplifyBooleanInitialization` will not produce invalid/corrupted initializer in case of negated initialization.
* `PrimitiveWrapperInstantiation` will not confuse class named like a boxed type with a boxed type.
* `EnhancedForLoopToStreamAnyMatch` will not accept a condition holding an `AssignExpr`

### Changes

* Each `IMutator` see its simple class name configured as default identifier.
* `FileSystemCodeProvider` will log but not throw when loading a file with an incompatible charset.
* `StringReplaceAllWithQuotableInput` covers more cases of quotable regexes.

### Added

* `SimplifyBooleanExpression` is introduced
* `StringReplaceAllWithQuotableInput` is introduced
* `EnhancedForLoopToForEach` is introduced
* `LoopIntRangeToIntStreamForEach` is introduced
* `FileSystemCodeProvider` now accepts a custom charset (default remains `UTF-8`)
* `SimplifyStreamVariablesWithMap` is introduced

## [2.13] - 2023-03-27

### Fixed

* `PrimitiveWrapperInstantiation` will not treat nested class as a boxed type anymore (e.g. `java.awt.geom.Rectangle2D.Double`)
* `SimplifyStartsWith` is renamed from `StringStartsWithChar` given the existing PMD rule

### Added

* `@SuppressCleanthat` enables skipping Cleanthat for specific pieces of code
* `RemoveDoubleNegation` is introduced
* `RemoveAllToClearCollection` is introduced
* `SpotBugsMutators` is introduced
* `CollectionIndexOfToContains` is introduced
* `StringIndexOfToContains` is introduced

## [2.12] - 2023-03-23

### Fixed

* In many cases, the presence of comment will prevent a mutator, as comments would be lost in many cases
* `EnhancedForLoopToStreamAnyMatch` will not produce invalid code in case of `return` over the `forEach` variable

### Changes

* `UseUnderscoresInNumericLiterals` now applies only over 5+ digits (like PMD default configuration)

### Added

* `EnhancedForLoopToStreamCollect` is introduced
* `@CaseNotYetImplemented` enables preparing unitTests for future features or limitations

## [2.11] - 2023-03-12

### Fixed

* `S1602` is renamed `RSPEC-1602`
* `S1612` is renamed `RSPEC-1612`
* `LambdaReturnsSingleStatement` now properly handles multiple arguments
* `LambdaReturnsSingleStatement` may lose comments

### Changes

* `UseStringIsEmpty` is extracted from `UseCollectionIsEmpty`

### Added

* `UnnecessaryLambdaEnclosingParameters` is introduced but it is not functional
* `ErrorProneMutators` is introduced
* `GuavaInlineStringsRepeat` is introduced, and added in `ErrorProneMutators`
* `ObjectEqualsForPrimitives` is introduced
* `ObjectsHashCodePrimitive` is introduced
* `EnhancedForLoopToStreamAnyMatch` is introduced
* `RedundantLogicalComplementsInStream` is introduced
* `SimplifyBooleanInitialization` is introduced
* `SafeButControversial` is introduced

## [2.10] - 2023-03-09

### Fixed

* `AvoidUncheckedExceptionsInSignatures` may encounter `UnsolvedSymbolExceptions`
* `AvoidInlineConditionals` will not produce invalid `var` declarations.

### Added

* `StringStartsWithChar` is added in `SafeButNotConsensual`
* `$.repository.meta.full_clean_on_configuration_change` is a new option to enable full repository cleaning on a configuration change. It is defaulted to `false` while previous behavior was as if it was always `true`.
* `EclipseStylesheetGenerator` has a new option to discard ambiguous options (`tryOptionsKeepingExistingFormat`)

## [2.9] - 2023-01-06

### Added

* `StringStartsWithChar` is an additional mutator
* `mvn cleanthat:apply` is a new Mojo enabling one-shotting a rules over current directory [README.MD](./maven/)

## [2.8] - 2023-01-05

### Fixed

* `ArraysDotStream` requires the argument to be an Array to convert use `Arrays.stream()`. It may also rely on `Stream.of` for other cases
* `ArraysDotStream` will apply only if the scope is `Arrays`
* `EmptyControlStatement` will not break empty method definitions.
* `LiteralsFirstInComparisons` better handles unresolved symbols.
* `LambdaIsMethodReference` will not rely on `T.class::cast` if T is a Type bound (e.g. in `<T> void method(...)`).
* `LocalVariableTypeInference` crashes lesss often on unresolved types

### Changes

* `UseTextBlocks` is temporarily dropped from `SafeButNotConsensual` as its results are too poor (https://github.com/javaparser/javaparser/issues/3936).
* `LambdaReturnsSingleStatement` is temporarily dropped from `SafeButNotConsensual` as its results are too poor (https://github.com/javaparser/javaparser/pull/3938).
* `BoxedPrimitiveConstructor` is renamed `PrimitiveWrapperInstantiation`, as it is the relevant PMD rule.

## [2.7] - 2023-03-01

### Fixed

* `LocalVariableTypeInference` crashes lesss often on unresolved types
* `UnnecessaryModifier` will not remove `static` from interface methods

### Changes

* `LineEnding` management is dropped from `ILintFixer` and `ILintFixerWithPath`
* `ModifierOrder` is promoted in `SafeAndConsensual`
* `StringToString` is promoted in `SafeAndConsensual`
* `UnnecessaryModifier` does not strip anymore the `static` keyword of `MethodDeclaration`

### Added

* `IMutator` is generated into [MUTATORS.generated.MD](MUTATORS.generated.MD) by `RunGenerateDocumentation`
* `UseUnderscoresInNumericLiterals` is an additional PMD mutator
* `UseTextBlocks` is an additional mutator
* `UnnecessaryImport` is an additional mutator
* `BoxedPrimitiveConstructor` is an additional mutator
* `LambdaIsMethodReference` is an additional mutator
* `AvoidUncheckedExceptionsInSignatures` is an additional mutator
* `PrimitiveBoxedForString` is renamed into `UnnecessaryBoxing`
* `SafeButNotAndConsensual` is an additional composite mutator

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

