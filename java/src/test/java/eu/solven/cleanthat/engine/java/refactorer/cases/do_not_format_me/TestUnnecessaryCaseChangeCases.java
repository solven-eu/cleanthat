package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryCaseChange;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;
import org.junit.Ignore;

import java.util.Locale;

public class TestUnnecessaryCaseChangeCases extends AJavaparserRefactorerCases {

    @Override
    public IJavaparserAstMutator getTransformer() {
        return new UnnecessaryCaseChange();
    }

    // Cases that should be replaced with equalsIgnoreCase

    @CompareMethods
    public static class CaseToLowerCaseWithHardcodedLowercase {
        public Object pre(String string) {
            return string.toLowerCase().equals("lowercase");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("lowercase");
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithHardcodedUppercase {
        public Object pre(String string) {
            return string.toUpperCase().equals("UPPERCASE");
        }

        public Object post(String string) {
            return string.equalsIgnoreCase("UPPERCASE");
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithHardcodedNull {
        public Object pre(String string) {
            return string.toLowerCase().equals(null);
        }

        public Object post(String string) {
            return string.equalsIgnoreCase(null);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithHardcodedNull {
        public Object pre(String string) {
            return string.toUpperCase().equals(null);
        }

        public Object post(String string) {
            return string.equalsIgnoreCase(null);
        }
    }

    @CompareMethods
    public static class CaseToLowerCaseWithHardcodedLowercaseFlipped {
        public Object pre(String string) {
            return "lowercase".equals(string.toLowerCase());
        }

        public Object post(String string) {
            return "lowercase".equalsIgnoreCase(string);
        }
    }

    @CompareMethods
    public static class CaseToUpperCaseWithHardcodedUppercaseFlipped {
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

    // Cases that should be ignored as the replacement would change execution behavior

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
    public static class CaseToLowerCaseWithHardcodedUppercase {
        public Object pre(String string) {
            return string.toLowerCase().equals("UPPERCASE");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithHardcodedLowercase {
        public Object pre(String string) {
            return string.toUpperCase().equals("lowercase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithHardcodedMixedCase {
        public Object pre(String string) {
            return string.toLowerCase().equals("MixedCase");
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithHardcodedMixedCase {
        public Object pre(String string) {
            return string.toUpperCase().equals("MixedCase");
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
    public static class CaseToLowerCaseWithHardcodedUppercaseFlipped {
        public Object pre(String string) {
            return "UPPERCASE".equals(string.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithHardcodedLowercaseFlipped {
        public Object pre(String string) {
            return "lowercase".equals(string.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToLowerCaseWithHardcodedMixedCaseFlipped {
        public Object pre(String string) {
            return "MixedCase".equals(string.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseToUpperCaseWithHardcodedMixedCaseFlipped {
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

    // Cases that should be ignored as the replacement could change execution behavior

    @UnmodifiedMethod
    public static class CaseRightToLowerCase {
        public Object pre(String left, String right) {
            return left.equals(right.toLowerCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseRightToUpperCase {
        public Object pre(String left, String right) {
            return left.equals(right.toUpperCase());
        }
    }

    @UnmodifiedMethod
    public static class CaseLeftToLowerCase {
        public Object pre(String left, String right) {
            return left.toLowerCase().equals(right);
        }
    }

    @UnmodifiedMethod
    public static class CaseLeftToUpperCase {
        public Object pre(String left, String right) {
            return left.toUpperCase().equals(right);
        }
    }

}
