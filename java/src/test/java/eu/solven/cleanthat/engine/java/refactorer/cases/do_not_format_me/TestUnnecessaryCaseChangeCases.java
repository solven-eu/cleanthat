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

    @Ignore("WIP")
    @CompareMethods
    public static class CaseToLowerCaseWithHardcodedLowercaseFlipped {
        public Object pre(String string) {
            return "lowercase".equals(string.toLowerCase());
        }

        public Object post(String string) {
            return "lowercase".equalsIgnoreCase(string);
        }
    }

    @Ignore("WIP")
    @CompareMethods
    public static class CaseToUpperCaseWithHardcodedUppercaseFlipped {
        public Object pre(String string) {
            return "UPPERCASE".equals(string.toUpperCase());
        }

        public Object post(String string) {
            return "UPPERCASE".equalsIgnoreCase(string);
        }
    }

    // Cases thats should be ignored as the replacement would change execution behavior

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

    // Out-of-scope cases, where the replacement should be `equals()` instead of `equalsIgnoreCase()`

    @Ignore("WIP")
    @UnmodifiedMethod
    public static class CaseToLowerCaseWithToLowerCase {
        public Object pre(String first, String second) {
            return first.toLowerCase().equals(second.toLowerCase());
        }
    }

    @Ignore("WIP")
    @UnmodifiedMethod
    public static class CaseToUpperCaseWithToUpperCase {
        public Object pre(String first, String second) {
            return first.toUpperCase().equals(second.toUpperCase());
        }
    }

    @Ignore("WIP")
    @UnmodifiedMethod
    public static class CaseToLowerCaseWithToUpperCase {
        public Object pre(String first, String second) {
            return first.toLowerCase().equals(second.toUpperCase());
        }
    }

    @Ignore("WIP")
    @UnmodifiedMethod
    public static class CaseToUpperCaseWithToLowerCase {
        public Object pre(String first, String second) {
            return first.toUpperCase().equals(second.toLowerCase());
        }
    }

}
