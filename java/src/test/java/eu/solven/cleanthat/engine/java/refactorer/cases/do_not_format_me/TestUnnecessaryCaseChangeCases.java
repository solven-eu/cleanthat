package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryCaseChange;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

import java.util.Locale;

public class TestUnnecessaryCaseChangeCases extends AJavaparserRefactorerCases {

    @Override
    public IJavaparserAstMutator getTransformer() {
        return new UnnecessaryCaseChange();
    }

    private static final class CustomClass {
        private CustomClass equalsIgnoreCase(String string) {
            return this;
        }

        private CustomClass toLowerCase() {
            return this;
        }
    }

    @UnmodifiedMethod
    public static class CaseCustomClassWithEqualsIgnoreCase {
        public Object pre() {
            return new CustomClass().toLowerCase().equalsIgnoreCase("lowercase");
        }
    }

    // Cases that should be replaced with equalsIgnoreCase

    @CompareMethods
    public static class CaseToLowerCaseWithLiteralEmpty {
        public Object pre(String string) {
            return string.toLowerCase().equals("");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("");
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithLiteralEmpty {
        public Object pre(String string) {
            return string.toUpperCase().equals("");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("");
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithLiteralLowercase {
        public Object pre(String string) {
            return string.toLowerCase().equals("lowercase");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("lowercase");
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithLiteralUppercase {
        public Object pre(String string) {
            return string.toUpperCase().equals("UPPERCASE");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("UPPERCASE");
        }
    }

    @CompareMethods
    public static class CaseLiteralEmptyWithToLowerCaseWith {
        public Object pre(String string) {
            return "".equals(string.toLowerCase());
        }

        public Object post(String string) {
            return "".equalsIgnoreCase(string);
        }
    }

    @CompareMethods
    public static class CaseLiteralEmptyWithToUpperCase {
        public Object pre(String string) {
            return "".equals(string.toUpperCase());
        }

        public Object post(String string) {
            return "".equalsIgnoreCase(string);
        }
    }

    @CompareMethods
    public static class CaseLiteralLowercaseWithToLowerCaseWith {
        public Object pre(String string) {
            return "lowercase".equals(string.toLowerCase());
        }

        public Object post(String string) {
            return "lowercase".equalsIgnoreCase(string);
        }
    }

    @CompareMethods
    public static class CaseLiteralUppercaseWithToUpperCase {
        public Object pre(String string) {
            return "UPPERCASE".equals(string.toUpperCase());
        }

        public Object post(String string) {
            return "UPPERCASE".equalsIgnoreCase(string);
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithToLowerCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equals(right.toLowerCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithToUpperCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equals(right.toUpperCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    // Cases where case change should be omitted

    @CompareMethods
    public static class CaseToLowerCaseWithEqualsIgnoreCaseToLowerCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equalsIgnoreCase(right.toLowerCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithEqualsIgnoreCaseToUpperCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equalsIgnoreCase(right.toUpperCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithEqualsIgnoreCaseToUpperCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equalsIgnoreCase(right.toUpperCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithEqualsIgnoreCaseToLowerCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equalsIgnoreCase(right.toLowerCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithEqualsIgnoreCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equalsIgnoreCase(right);
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithEqualsIgnoreCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equalsIgnoreCase(right);
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseVariableWithEqualsIgnoreCaseToLowerCase {
        public Object pre(String left, String right) {
            return left.equalsIgnoreCase(right.toLowerCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    @CompareMethods
    public static class CaseVariableWithEqualsIgnoreCaseToUpperCase {
        public Object pre(String left, String right) {
            return left.equalsIgnoreCase(right.toUpperCase());
        }

        public Object post(String left, String right) {
            return left.equalsIgnoreCase(right);
        }
    }

    // Cases that could be replaced, but are ignored for now

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithNull {
        public Object pre(String string) {
            return string.toLowerCase().equals(null);
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithNull {
        public Object pre(String string) {
            return string.toUpperCase().equals(null);
        }
    }

    // Cases that should be ignored as the replacement WOULD change execution behavior

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithLiteralUppercase {
        public Object pre(String string) {
            return string.toLowerCase().equals("UPPERCASE");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithLiteralLowercase {
        public Object pre(String string) {
            return string.toUpperCase().equals("lowercase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithLiteralMixedCase {
        public Object pre(String string) {
            return string.toLowerCase().equals("MixedCase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithLiteralMixedCase {
        public Object pre(String string) {
            return string.toUpperCase().equals("MixedCase");
        }
    }

    @UnmodifiedMethod
    public static class CaseLiteralUppercaseWithToLowerCase {
        public Object pre(String string) {
            return "UPPERCASE".equals(string.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseLiteralLowercaseWithToUpperCase {
        public Object pre(String string) {
            return "lowercase".equals(string.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseLiteralMixedCaseWithToLowerCase {
        public Object pre(String string) {
            return "MixedCase".equals(string.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseLiteralMixedCaseWithToUpperCase {
        public Object pre(String string) {
            return "MixedCase".equals(string.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithToUpperCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equals(right.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithToLowerCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equals(right.toLowerCase());
        }
    }

    // Cases that should be ignored as the replacement COULD change execution behavior

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithLocaleAndEqualsIgnoreCase {
        public Object pre(String string) {
            return string.toLowerCase(Locale.ENGLISH).equalsIgnoreCase("lowercase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithLocaleAndEqualsIgnoreCase {
        public Object pre(String string) {
            return string.toUpperCase(Locale.ENGLISH).equalsIgnoreCase("UPPERCASE");
        }
    }

    @UnmodifiedMethod
    public static class CaseAndEqualsIgnoreCaseWithToLowerCaseAndLocale {
        public Object pre(String string) {
            return "lowercase".equalsIgnoreCase(string.toLowerCase(Locale.ENGLISH));
        }
    }

    @UnmodifiedMethod
    public static class CaseAndEqualsIgnoreCaseWithToUpperCaseAndLocale {
        public Object pre(String string) {
            return "UPPERCASE".equalsIgnoreCase(string.toUpperCase(Locale.ENGLISH));
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithLocale {
        public Object pre(String string) {
            return string.toLowerCase(Locale.ENGLISH).equals("lowercase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithLocale {
        public Object pre(String string) {
            return string.toUpperCase(Locale.ENGLISH).equals("UPPERCASE");
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithLocaleFlipped {
        public Object pre(String string) {
            return "lowercase".equals(string.toLowerCase(Locale.ENGLISH));
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithLocaleFlipped {
        public Object pre(String string) {
            return "UPPERCASE".equals(string.toUpperCase(Locale.ENGLISH));
        }
    }

    @UnmodifiedMethod
    public static class CaseVariableWithToLowerCase {
        public Object pre(String left, String right) {
            return left.equals(right.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseVariableWithToUpperCase {
        public Object pre(String left, String right) {
            return left.equals(right.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithVariable {
        public Object pre(String left, String right) {
            return left.toLowerCase().equals(right);
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithVariable {
        public Object pre(String left, String right) {
            return left.toUpperCase().equals(right);
        }
    }

    // Cases that should be ignored as there is no case change involved

    @UnmodifiedMethod
    public static class CaseLiteralEqualsLiteral {
        public Object pre() {
            return "lowercase".equals("lowercase");
        }
    }

    @UnmodifiedMethod
    public static class CaseLiteralEqualsIgnoreCaseLiteral {
        public Object pre() {
            return "lowercase".equalsIgnoreCase("lowercase");
        }
    }

}
