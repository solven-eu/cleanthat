# Mutators
## Single MutatorsPRD-READY

### [AvoidFileStream](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/AvoidFileStream.java)

PMD: AvoidFileStream

Require sourceJdk: 1.7

### [EmptyControlStatement](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/EmptyControlStatement.java)

PMD: EmptyControlStatement
CheckStyle: EmptyStatementCheck
Sonar: RSPEC-1116

Require sourceJdk: 1

### [LambdaReturnsSingleStatement](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LambdaReturnsSingleStatement.java)

Sonar: S1602

Require sourceJdk: 1.8

### [LocalVariableTypeInference](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LocalVariableTypeInference.java)

Sonar: RSPEC-6212

Require sourceJdk: 10

### [UnnecessaryImport](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryImport.java)

CheckStyle: UnusedImports

Require sourceJdk: 1

### [UnnecessaryModifier](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryModifier.java)

PMD: UnnecessaryModifier
CheckStyle: RedundantModifier
Sonar: RSPEC-2333

Require sourceJdk: 1

### [UseDiamondOperator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseDiamondOperator.java)

PMD: UseDiamondOperator
Sonar: RSPEC-2293

Require sourceJdk: 1.7

### [UseDiamondOperatorJdk8](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseDiamondOperatorJdk8.java)

PMD: UseDiamondOperator
Sonar: RSPEC-2293

Require sourceJdk: 1.8
## Single Mutators (DRAFTs)

### [ArraysDotStream](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ArraysDotStream.java)


isDraft

Require sourceJdk: 1.8

### [AvoidInlineConditionals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/AvoidInlineConditionals.java)

CheckStyle: AvoidInlineConditionals

isDraft

Require sourceJdk: 1.8

### [BoxedPrimitiveConstructor](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/BoxedPrimitiveConstructor.java)


isDraft

Require sourceJdk: 1.5

### [ComparisonWithNaN](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ComparisonWithNaN.java)

PMD: ComparisonWithNaN

isDraft

Require sourceJdk: 11

### [CreateTempFilesUsingNio](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/CreateTempFilesUsingNio.java)

Sonar: RSPEC-2976

isDraft

Require sourceJdk: 1.7

### [EnumsWithoutEquals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/EnumsWithoutEquals.java)


isDraft

Require sourceJdk: 1.5

### [JUnit4ToJUnit5](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/JUnit4ToJUnit5.java)


isDraft

Require sourceJdk: 1.8

### [LambdaIsMethodReference](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LambdaIsMethodReference.java)

Sonar: S1612

isDraft

Require sourceJdk: 1.8

### [LiteralsFirstInComparisons](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LiteralsFirstInComparisons.java)

PMD: LiteralsFirstInComparisons

isDraft

Require sourceJdk: 1

### [ModifierOrder](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ModifierOrder.java)

CheckStyle: ModifierOrder
Sonar: RSPEC-1124

isDraft

Require sourceJdk: 1

### [OptionalNotEmpty](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/OptionalNotEmpty.java)


isDraft

Require sourceJdk: 11

### [StreamAnyMatch](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/StreamAnyMatch.java)


isDraft

Require sourceJdk: 1.8

### [StringToString](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/StringToString.java)

PMD: StringToString

isDraft

Require sourceJdk: 1

### [UnnecessaryBoxing](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryBoxing.java)

Sonar: RSPEC-1158

isDraft

Require sourceJdk: 1.1

### [UnnecessaryFullyQualifiedName](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryFullyQualifiedName.java)

PMD: UnnecessaryFullyQualifiedName

isDraft

Require sourceJdk: 1

### [UnnecessarySemicolon](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessarySemicolon.java)

PMD: UnnecessarySemicolon
Sonar: RSPEC-2959

isDraft

Require sourceJdk: 1

### [UseIndexOfChar](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseIndexOfChar.java)

PMD: UseIndexOfChar
Sonar: RSPEC-1155

isDraft

Require sourceJdk: 1

### [UseIsEmptyOnCollections](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseIsEmptyOnCollections.java)

PMD: UseCollectionIsEmpty
Sonar: RSPEC-1155

isDraft

Require sourceJdk: 1.6

### [UseTextBlocks](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseTextBlocks.java)


isDraft

Require sourceJdk: 15

### [UseUnderscoresInNumericLiterals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseUnderscoresInNumericLiterals.java)

PMD: UseUnderscoresInNumericLiterals

isDraft

Require sourceJdk: 1.7
## Composite Mutators

### [AllIncludingDraftSingleMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/AllIncludingDraftSingleMutators.java)


isDraft

Require sourceJdk: 1

### [CheckStyleMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CheckStyleMutators.java)

CheckStyle: [CheckStyle]()

isDraft

Require sourceJdk: 1

### [CompositeMutator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CompositeMutator.java)


isDraft

Require sourceJdk: 1

### [CompositeWalkingMutator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CompositeWalkingMutator.java)


isDraft

Require sourceJdk: 1

### [PMDMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/PMDMutators.java)

PMD: [PMD]()

isDraft

Require sourceJdk: 1

### [SafeAndConsensualMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SafeAndConsensualMutators.java)


isDraft

Require sourceJdk: 1

### [SonarMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SonarMutators.java)

Sonar: Sonar

isDraft

Require sourceJdk: 1
---

Generated by TestGenerateDocumentation](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/it/TestGenerateDocumentation.java)
