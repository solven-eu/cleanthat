# Mutators
## Single Mutators PRD-READY

### [ArraysDotStream](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ArraysDotStream.java)


languageLevel: jdk1.8

```
@CompareMethods
public static class Ellipse {

    public Object pre(Object... objects) {
        return Arrays.asList(objects).stream();
    }

    public Object post(Object... objects) {
        return Arrays.stream(objects);
    }
}
```

See[ArraysDotStreamCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/ArraysDotStreamCases.java)

### [AvoidInlineConditionals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/AvoidInlineConditionals.java)


CheckStyle: [AvoidInlineConditionals](https://checkstyle.sourceforge.io/config_coding.html#AvoidInlineConditionals)

Sonar: [RSPEC-3358](https://rules.sonarsource.com/java/RSPEC-3358)

languageLevel: jdk1

```
@UnmodifiedMethod
public static class Checkstyle_booleanExpr {

    public Object pre(int x) {
        boolean foobar = (x == 5);
        return foobar;
    }
}
```

See[AvoidInlineConditionalsCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/AvoidInlineConditionalsCases.java)

### [ComparisonWithNaN](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ComparisonWithNaN.java)


PMD: [ComparisonWithNaN](https://pmd.github.io/latest/pmd_rules_java_errorprone.html#comparisonwithnan)

languageLevel: jdk11

```
@CompareMethods
public static class CaseDouble {

    public Object pre(double d) {
        return d == Double.NaN;
    }

    public Object post(double d) {
        return Double.isNaN(d);
    }
}
```

See[ComparisonWithNaNCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/ComparisonWithNaNCases.java)

### [LiteralsFirstInComparisons](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LiteralsFirstInComparisons.java)


PMD: [LiteralsFirstInComparisons](https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#literalsfirstincomparisons)

languageLevel: jdk1

```
@CompareMethods
public static class CaseConstantString {

    public Object pre(String input) {
        return input.equals("hardcoded");
    }

    public Object post(String input) {
        return "hardcoded".equals(input);
    }
}
```

See[LiteralsFirstInComparisonsCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/LiteralsFirstInComparisonsCases.java)

### [ModifierOrder](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ModifierOrder.java)


CheckStyle: [ModifierOrder](https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/modifier/ModifierOrderCheck.html)

Sonar: [RSPEC-1124](https://rules.sonarsource.com/java/RSPEC-1124)

jSparrow: [ReorderModifiers](https://jsparrow.github.io/rules/reorder-modifiers.html)

languageLevel: jdk1

```
@CompareMethods
public static class CaseFields {

    @SuppressWarnings("unused")
    public Object pre() {
        return new Object() {

            final static public String FINAL_STATIC_PUBLIC = "";

            static final public String STATIC_FINAL_PUBLIC = "";

            final public static String FINAL_PUBLIC_STATIC = "";

            public final static String PUBLIC_FINAL_STATIC = "";

            static public final String STATIC_PUBLIC_FINAL = "";

            public static final String PUBLIC_STATIC_FINAL = "";
        };
    }

    @SuppressWarnings("unused")
    public Object post() {
        return new Object() {

            public static final String FINAL_STATIC_PUBLIC = "";

            public static final String STATIC_FINAL_PUBLIC = "";

            public static final String FINAL_PUBLIC_STATIC = "";

            public static final String PUBLIC_FINAL_STATIC = "";

            public static final String STATIC_PUBLIC_FINAL = "";

            public static final String PUBLIC_STATIC_FINAL = "";
        };
    }
}
```

See[ModifierOrderCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/ModifierOrderCases.java)

### [OptionalNotEmpty](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/OptionalNotEmpty.java)


languageLevel: jdk11

```
@CompareMethods
public static class CaseNotEmpty {

    public Object pre(Optional<?> input) {
        return !input.isEmpty();
    }

    public Object post(Optional<?> input) {
        return input.isPresent();
    }
}
```

See[OptionalNotEmptyCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/OptionalNotEmptyCases.java)

### [StreamAnyMatch](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/StreamAnyMatch.java)


Sonar: [RSPEC-4034](https://rules.sonarsource.com/java/RSPEC-4034)

languageLevel: jdk1.8

```
@CompareMethods
public static class CaseFindAnyIsPresent {

    public Object pre(List<?> input) {
        return input.stream().filter(o -> null != o).findAny().isPresent();
    }

    public Object post(List<?> input) {
        return input.stream().anyMatch(o -> null != o);
    }
}
```

See[StreamAnyMatchCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/StreamAnyMatchCases.java)

### [StringToString](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/StringToString.java)


PMD: [StringToString](https://pmd.github.io/latest/pmd_rules_java_performance.html#stringtostring)

jSparrow: [RemoveToStringOnString](https://jsparrow.github.io/rules/remove-to-string-on-string.html)

languageLevel: jdk1

```
@UnmodifiedMethod
public static class CaseNotString {

    public Object pre(Number o) {
        return o.toString();
    }
}
```

See[StringToStringCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/StringToStringCases.java)

### [UnnecessaryModifier](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryModifier.java)


PMD: [UnnecessaryModifier](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessarymodifier)

CheckStyle: [RedundantModifier](https://checkstyle.sourceforge.io/config_modifier.html#RedundantModifier)

Sonar: [RSPEC-2333](https://rules.sonarsource.com/java/RSPEC-2333)

jSparrow: [RemoveModifiersInInterfaceProperties](https://jsparrow.github.io/rules/remove-modifiers-in-interface-properties.html)

languageLevel: jdk1

```
@CompareInnerClasses
public static class SomeInterface {

    public interface Pre {

        // both abstract and public are ignored by the compiler
        public abstract void bar();

        // public, static, and final all ignored
        public static final int X = 0;

        // public, static ignored
        public static class Bar {
        }

        // ditto
        public static interface Baz {
        }

        static String parse() {
            return "parsed";
        }
    }

    public interface Post {

        // both abstract and public are ignored by the compiler
        void bar();

        // public, static, and final all ignored
        int X = 0;

        // public, static ignored
        class Bar {
        }

        // ditto
        interface Baz {
        }

        static String parse() {
            return "parsed";
        }
    }
}
```

See[UnnecessaryModifierCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UnnecessaryModifierCases.java)

### [UseCollectionIsEmpty](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseCollectionIsEmpty.java)


PMD: [UseCollectionIsEmpty](https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#usecollectionisempty)

Sonar: [RSPEC-1155](https://rules.sonarsource.com/java/RSPEC-1155)

jSparrow: [UseIsEmptyOnCollections](https://jsparrow.github.io/rules/use-is-empty-on-collections.html)

languageLevel: jdk1.2

```
@CompareMethods
public static class CaseCollection {

    public Object pre(Collection<?> input) {
        return input.size() == 0;
    }

    public Object post(Collection<?> input) {
        return input.isEmpty();
    }
}
```

See[UseCollectionIsEmptyCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseCollectionIsEmptyCases.java)

### [UseIndexOfChar](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseIndexOfChar.java)


PMD: [UseIndexOfChar](https://pmd.github.io/latest/pmd_rules_java_performance.html#useindexofchar)

Sonar: [RSPEC-1155](https://rules.sonarsource.com/java/RSPEC-1155)

languageLevel: jdk1

```
@CompareMethods
public static class indexOf {

    public Object pre(String s) {
        return s.indexOf("d");
    }

    public Object post(String s) {
        return s.indexOf('d');
    }
}
```

See[UseIndexOfCharCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseIndexOfCharCases.java)

### [UseStringIsEmpty](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseStringIsEmpty.java)


languageLevel: jdk1.6

```
@UnmodifiedMethod
public static class CaseCollection {

    public Object pre(Collection<?> input) {
        return input.size() == 0;
    }
}
```

See[UseStringIsEmptyCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseStringIsEmptyCases.java)

### [UseUnderscoresInNumericLiterals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseUnderscoresInNumericLiterals.java)


PMD: [UseUnderscoresInNumericLiterals](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#useunderscoresinnumericliterals)

Sonar: [RSPEC-2148](https://rules.sonarsource.com/java/RSPEC-2148)

languageLevel: jdk1.7

```
@CompareMethods
public static class CaseReturnDouble {

    public Object pre() {
        return 123456789.0123456789;
    }

    public Object post() {
        return 123_456_789.012_345_678_9;
    }
}
```

See[UseUnderscoresInNumericLiteralsCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseUnderscoresInNumericLiteralsCases.java)
## Single Mutators (DRAFTs)

### [AvoidFileStream](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/AvoidFileStream.java)


PMD: [AvoidFileStream](https://pmd.github.io/latest/pmd_rules_java_performance.html#avoidfilestream)

isDraft

languageLevel: jdk1.7

```
@CompareMethods
public static class FileInputStreamOverFile {

    public FileInputStream pre(File fileName) throws FileNotFoundException {
        return new FileInputStream(fileName);
    }

    public InputStream post(File fileName) throws IOException {
        return Files.newInputStream(fileName.toPath());
    }
}
```

See[AvoidFileStreamCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/AvoidFileStreamCases.java)

### [AvoidUncheckedExceptionsInSignatures](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/AvoidUncheckedExceptionsInSignatures.java)


PMD: [AvoidUncheckedExceptionsInSignatures](https://pmd.github.io/latest/pmd_rules_java_design.html#avoiduncheckedexceptionsinsignatures)

isDraft

languageLevel: jdk1

```
@CompareMethods
public static class CaseRuntimeException {

    public void pre() throws RuntimeException {
    }

    public void post() {
    }
}
```

See[AvoidUncheckedExceptionsInSignaturesCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/AvoidUncheckedExceptionsInSignaturesCases.java)

### [CreateTempFilesUsingNio](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/CreateTempFilesUsingNio.java)


Sonar: [RSPEC-2976](https://rules.sonarsource.com/java/RSPEC-2976)

isDraft

languageLevel: jdk1.7

```
@CompareMethods
public static class CasePrefixSuffix {

    public Object pre() throws IOException {
        return File.createTempFile("myFile", ".tmp");
    }

    public Object post() throws IOException {
        return Files.createTempFile("myFile", ".tmp").toFile();
    }
}
```

See[CreateTempFilesUsingNioCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/CreateTempFilesUsingNioCases.java)

### [EmptyControlStatement](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/EmptyControlStatement.java)


CheckStyle: [EmptyStatementCheck](https://javadoc.io/static/com.puppycrawl.tools/checkstyle/8.37/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html)

Sonar: [RSPEC-1116](https://rules.sonarsource.com/java/RSPEC-1116)

jSparrow: [RemoveEmptyStatement](https://jsparrow.github.io/rules/remove-empty-statement.html)

isDraft

languageLevel: jdk1

```
// We keep the 'if' statements as they may call methods, hence having side-effects
@CompareMethods
public static class VariousCases {

    public void pre() {
        // empty if statement
        if (true)
            ;
        // empty as well
        if (true) {
        }
        {
        }
        {
            {
                {
                }
            }
        }
    }

    public void post() {
        // empty if statement
        if (true)
            ;
        // empty as well
        if (true) {
        }
    }
}
```

See[EmptyControlStatementCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/EmptyControlStatementCases.java)

### [EnhancedForLoopToStreamAnyMatch](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/EnhancedForLoopToStreamAnyMatch.java)


jSparrow: [EnhancedForLoopToStreamAnyMatch](https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html)

isDraft

languageLevel: jdk1.8

```
// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-anymatch
@CompareMethods
public static class breakToStreamAnyMatch {

    public boolean pre(List<String> strings) {
        boolean containsEmpty = false;
        for (String value : strings) {
            if (value.isEmpty()) {
                containsEmpty = true;
                break;
            }
        }
        return containsEmpty;
    }

    public boolean post(List<String> strings) {
        boolean containsEmpty = false;
        if (strings.stream().anyMatch(value -> value.isEmpty())) {
            containsEmpty = true;
        }
        return containsEmpty;
    }
}
```

See[EnhancedForLoopToStreamAnyMatchCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/EnhancedForLoopToStreamAnyMatchCases.java)

### [EnumsWithoutEquals](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/EnumsWithoutEquals.java)


isDraft

languageLevel: jdk1.5

```
@CompareMethods
public static class EnumInRightHandSide {

    public boolean pre(RoundingMode roundingMode) {
        return roundingMode.equals(RoundingMode.UP);
    }

    public boolean post(RoundingMode roundingMode) {
        return roundingMode == RoundingMode.UP;
    }
}
```

See[EnumsWithoutEqualsCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/EnumsWithoutEqualsCases.java)

### [GuavaInlineStringsRepeat](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/GuavaInlineStringsRepeat.java)


ErrorProne: [InlineMeInliner](https://errorprone.info/bugpattern/InlineMeInliner)

isDraft

languageLevel: jdk11

```
@CompareMethods
public static class CaseNotString {

    public String pre() {
        return Strings.repeat("abc", 2);
    }

    public String post() {
        return "abc".repeat(2);
    }
}
```

See[GuavaInlineStringsRepeatCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/GuavaInlineStringsRepeatCases.java)

### [GuavaStringsIsNullOrEmpty](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/GuavaStringsIsNullOrEmpty.java)


isDraft

languageLevel: jdk11

```
@CompareMethods
public static class CaseNullElseIsEmpty {

    public boolean pre(String s) {
        return s == null || s.isEmpty();
    }

    public boolean post(String s) {
        return Strings.isNullOrEmpty(s);
    }
}
```

See[GuavaStringsIsNullOrEmptyCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/GuavaStringsIsNullOrEmptyCases.java)

### [JUnit4ToJUnit5](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/JUnit4ToJUnit5.java)


isDraft

languageLevel: jdk1.8

```
@CompareClasses(pre = BeforeAfterTestPre.class, post = BeforeAfterTestPost.class)
public static class CaseBeforeAfterTest {
}
```

See[JUnit4ToJUnit5Cases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/JUnit4ToJUnit5Cases.java)

### [LambdaIsMethodReference](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LambdaIsMethodReference.java)


Sonar: [RSPEC-1612](https://rules.sonarsource.com/java/RSPEC-1612)

jSparrow: [LambdaToMethodReference](https://jsparrow.github.io/rules/lambda-to-method-reference.html#code-changes)

isDraft

languageLevel: jdk1.8

```
// https://sonarsource.atlassian.net/browse/RSPEC-1612
// .map(B::<String>getObject)
@CompareMethods
public static class CaseSonar {

    class B extends CaseSonar {

        <T> T getObject() {
            return null;
        }
    }

    void pre(List<CaseSonar> list) {
        list.stream().filter(a -> a instanceof B).map(a -> (B) a).map(b -> b.<String>getObject()).forEach(b -> System.out.println(b));
    }

    void post(List<CaseSonar> list) {
        list.stream().filter(B.class::isInstance).map(B.class::cast).map(b -> b.<String>getObject()).forEach(System.out::println);
    }
}
```

See[LambdaIsMethodReferenceCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/LambdaIsMethodReferenceCases.java)

### [LambdaReturnsSingleStatement](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LambdaReturnsSingleStatement.java)


Sonar: [RSPEC-1602](https://rules.sonarsource.com/java/RSPEC-1602)

jSparrow: [StatementLambdaToExpression](https://jsparrow.github.io/rules/statement-lambda-to-expression.html)

isDraft

languageLevel: jdk1.8

```
@CompareMethods
public static class CaseString {

    public Object pre(List<String> l) {
        return l.stream().map(s -> {
            return s.charAt(0);
        }).findFirst().get();
    }

    public Object post(List<String> l) {
        return l.stream().map(s -> s.charAt(0)).findFirst().get();
    }
}
```

See[LambdaReturnsSingleStatementCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/LambdaReturnsSingleStatementCases.java)

### [LocalVariableTypeInference](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/LocalVariableTypeInference.java)


Sonar: [RSPEC-6212](https://rules.sonarsource.com/java/RSPEC-6212)

jSparrow: [LocalVariableTypeInference](https://jsparrow.github.io/rules/local-variable-type-inference.html)

isDraft

languageLevel: jdk10

```
@CompareMethods
public static class PrimitiveInt {

    public Object pre() {
        int i = 10;
        return i;
    }

    public Object post() {
        var i = 10;
        return i;
    }
}
```

See[LocalVariableTypeInferenceCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/LocalVariableTypeInferenceCases.java)

### [ObjectEqualsForPrimitives](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ObjectEqualsForPrimitives.java)


ErrorProne: [ObjectEqualsForPrimitives](https://errorprone.info/bugpattern/ObjectEqualsForPrimitives)

isDraft

languageLevel: jdk1

```
@CompareMethods
public static class CaseInt {

    public boolean pre(int a, int b) {
        return Objects.equals(a, b);
    }

    public boolean post(int a, int b) {
        return a == b;
    }
}
```

See[ObjectEqualsForPrimitivesCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/ObjectEqualsForPrimitivesCases.java)

### [ObjectsHashCodePrimitive](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/ObjectsHashCodePrimitive.java)


ErrorProne: [ObjectsHashCodePrimitive](https://errorprone.info/bugpattern/ObjectsHashCodePrimitive)

isDraft

languageLevel: jdk1.8

```
@CompareMethods
public static class CaseInt {

    public int pre(int a) {
        return Objects.hashCode(a);
    }

    public int post(int a) {
        return Integer.hashCode(a);
    }
}
```

See[ObjectsHashCodePrimitiveCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/ObjectsHashCodePrimitiveCases.java)

### [PrimitiveWrapperInstantiation](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/PrimitiveWrapperInstantiation.java)


PMD: [PrimitiveWrapperInstantiation](https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#primitivewrapperinstantiation)

isDraft

languageLevel: jdk1.5

### [RedundantLogicalComplementsInStream](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/RedundantLogicalComplementsInStream.java)


jSparrow: [RedundantLogicalComplementsInStream](https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html)

isDraft

languageLevel: jdk1.8

```
// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-anymatch
@UnmodifiedMethod
public static class breakToStreamAnyMatch {

    public boolean pre(List<String> strings) {
        boolean containsEmpty = strings.stream().anyMatch(value -> value.isEmpty());
        return containsEmpty;
    }
}
```

See[RedundantLogicalComplementsInStreamCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/RedundantLogicalComplementsInStreamCases.java)

### [RemoveExplicitCallToSuper](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/RemoveExplicitCallToSuper.java)


jSparrow: [RemoveExplicitCallToSuper](https://jsparrow.github.io/rules/remove-explicit-call-to-super.html)

isDraft

languageLevel: jdk1

```
@CompareInnerClasses
public static class SuperInConstructor {

    public class Pre {

        public Pre() {
            super();
            "".toString();
        }
    }

    public class Post {

        public Post() {
            "".toString();
        }
    }
}
```

See[RemoveExplicitCallToSuperCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/RemoveExplicitCallToSuperCases.java)

### [SimplifyBooleanInitialization](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/SimplifyBooleanInitialization.java)


isDraft

languageLevel: jdk1.8

```
// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-anymatch
@CompareMethods
public static class breakToStreamAnyMatch {

    public boolean pre(List<String> strings) {
        boolean containsEmpty = false;
        if (strings.stream().anyMatch(value -> value.isEmpty())) {
            containsEmpty = true;
        }
        return containsEmpty;
    }

    public boolean post(List<String> strings) {
        boolean containsEmpty = strings.stream().anyMatch(value -> value.isEmpty());
        return containsEmpty;
    }
}
```

See[SimplifyBooleanInitializationCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/SimplifyBooleanInitializationCases.java)

### [SimplifyStreamFilterWithMap](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/SimplifyStreamFilterWithMap.java)


isDraft

languageLevel: jdk1.8

```
@CompareMethods
public static class CaseMethodRef {

    void pre(List<String> list) {
        list.stream().anyMatch(element -> element.toLowerCase().equals("searchMe"));
    }

    void post(List<String> list) {
        list.stream().map(String::toLowerCase).anyMatch(element -> element.equals("searchMe"));
    }
}
```

See[SimplifyStreamFilterWithMapCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/SimplifyStreamFilterWithMapCases.java)

### [StringStartsWithChar](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/StringStartsWithChar.java)


isDraft

languageLevel: jdk1.6

```
@CompareMethods
public static class startsWithElseIsEmpty {

    public Object pre(String s) {
        return s.startsWith("#") || s.isEmpty();
    }

    public Object post(String s) {
        return s.isEmpty() || s.charAt(0) == '#';
    }
}
```

See[StringStartsWithCharCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/StringStartsWithCharCases.java)

### [UnnecessaryBoxing](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryBoxing.java)


Sonar: [RSPEC-1158](https://rules.sonarsource.com/java/RSPEC-1158)

isDraft

languageLevel: jdk1.1

```
@CompareMethods
public static class CaseBoolean_valueOf {

    public Object pre(boolean input) {
        return Boolean.valueOf(input).toString();
    }

    public Object post(boolean input) {
        return Boolean.toString(input);
    }
}
```

See[UnnecessaryBoxingCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UnnecessaryBoxingCases.java)

### [UnnecessaryFullyQualifiedName](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryFullyQualifiedName.java)


PMD: [UnnecessaryFullyQualifiedName](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessaryfullyqualifiedname)

isDraft

languageLevel: jdk1

```
// https://stackoverflow.com/questions/51257256/why-dont-we-import-java-lang-package
@CompareMethods
public static class JavaLangType {

    public Object pre() {
        java.lang.String s = new java.lang.String();
        return s;
    }

    public Object post() {
        String s = new String();
        return s;
    }
}
```

See[UnnecessaryFullyQualifiedNameCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UnnecessaryFullyQualifiedNameCases.java)

### [UnnecessaryImport](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryImport.java)


CheckStyle: [UnusedImports](https://checkstyle.sourceforge.io/config_imports.html#UnusedImports)

isDraft

languageLevel: jdk1

### [UnnecessaryLambdaEnclosingParameters](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessaryLambdaEnclosingParameters.java)


isDraft

languageLevel: jdk1.8

```
@UnmodifiedMethod
public static class CaseRunnable {

    public Runnable pre() {
        return () -> System.out.print("ok");
    }
}
```

See[UnnecessaryLambdaEnclosingParametersCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UnnecessaryLambdaEnclosingParametersCases.java)

### [UnnecessarySemicolon](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UnnecessarySemicolon.java)


PMD: [UnnecessarySemicolon](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessarysemicolon)

Sonar: [RSPEC-2959](https://rules.sonarsource.com/java/RSPEC-2959)

isDraft

languageLevel: jdk1

```
@CompareMethods
public static class caseDoubleSemiColon {

    public int pre() {
        int i = 2;
        ;
        return i;
    }

    public int post() {
        int i = 2;
        return i;
    }
}
```

See[UnnecessarySemicolonCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UnnecessarySemicolonCases.java)

### [UseDiamondOperator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseDiamondOperator.java)


PMD: [UseDiamondOperator](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator)

Sonar: [RSPEC-2293](https://rules.sonarsource.com/java/RSPEC-2293)

isDraft

languageLevel: jdk1.7

```
@CompareMethods
public static class CaseCollection {

    public Map<String, List<String>> pre() {
        return new HashMap<String, List<String>>();
    }

    public Map<String, List<String>> post() {
        return new HashMap<>();
    }
}
```

See[UseDiamondOperatorCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseDiamondOperatorCases.java)

### [UseDiamondOperatorJdk8](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseDiamondOperatorJdk8.java)


PMD: [UseDiamondOperator](https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator)

Sonar: [RSPEC-2293](https://rules.sonarsource.com/java/RSPEC-2293)

isDraft

languageLevel: jdk1.8

```
@CompareMethods
public static class CaseCollection {

    private <T> List<T> genericMethod(List<T> list) {
        return list;
    }

    public Object pre() {
        return genericMethod(new ArrayList<Number>());
    }

    public Object post(Collection<?> input) {
        return genericMethod(new ArrayList<>());
    }
}
```

See[UseDiamondOperatorJdk8Cases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseDiamondOperatorJdk8Cases.java)

### [UseTextBlocks](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/UseTextBlocks.java)


isDraft

languageLevel: jdk15

```
@CompareCompilationUnitsAsResources(pre = PREFIX + "html_Pre.java", post = PREFIX + "html_Post.java")
public static class caseHtml {
}
```

See[UseTextBlocksCases](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/cases/do_not_format_me/UseTextBlocksCases.java)
## Composite Mutators

### [AllIncludingDraftSingleMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/AllIncludingDraftSingleMutators.java)


isDraft

languageLevel: jdk1

### [CheckStyleMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CheckStyleMutators.java)


CheckStyle: CheckStyle

isDraft

languageLevel: jdk1

### [CompositeMutator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CompositeMutator.java)


isDraft

languageLevel: jdk1

### [CompositeWalkingMutator](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/CompositeWalkingMutator.java)


isDraft

languageLevel: jdk1

### [ErrorProneMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/ErrorProneMutators.java)


PMD: ErrorProne

isDraft

languageLevel: jdk1

### [JSparrowMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/JSparrowMutators.java)


PMD: jSparrow

isDraft

languageLevel: jdk1

### [PMDMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/PMDMutators.java)


PMD: PMD

isDraft

languageLevel: jdk1

### [SafeAndConsensualMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SafeAndConsensualMutators.java)


isDraft

languageLevel: jdk1

### [SafeButNotConsensualMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SafeButNotConsensualMutators.java)


isDraft

languageLevel: jdk1

### [SafeButNotTrivialMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SafeButNotTrivialMutators.java)


isDraft

languageLevel: jdk1

### [SonarMutators](java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators/composite/SonarMutators.java)


Sonar: [Sonar](https://rules.sonarsource.com/java/Sonar)

isDraft

languageLevel: jdk1

---

Generated by [TestGenerateDocumentation](java/src/test/java/eu/solven/cleanthat/engine/java/refactorer/it/TestGenerateDocumentation.java)