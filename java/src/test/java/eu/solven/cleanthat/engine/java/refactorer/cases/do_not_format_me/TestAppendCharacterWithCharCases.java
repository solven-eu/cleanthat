package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AppendCharacterWithChar;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

import java.io.CharArrayWriter;
import java.io.StringWriter;

public class TestAppendCharacterWithCharCases extends AJavaparserRefactorerCases {

    @Override
    public IJavaparserAstMutator getTransformer() {
        return new AppendCharacterWithChar();
    }

    private static final class CustomStringBuilder {
        private CustomStringBuilder append(String string) {
            return this;
        }
    }

    @UnmodifiedMethod
    public static class CaseCustomStringBuilder {
        public Object pre() {
            return new CustomStringBuilder().append("a");
        }
    }

    @CompareMethods
    public static class CaseCharArrayWriter {
        public Object pre() {
            return new CharArrayWriter().append("a");
        }

        public Object post() {
            return new CharArrayWriter().append('a');
        }
    }

    @CompareMethods
    public static class CaseStringWriter {
        public Object pre() {
            return new StringWriter().append("a");
        }

        public Object post() {
            return new StringWriter().append('a');
        }
    }

    @CompareMethods
    public static class CaseStringBuffer {
        public Object pre() {
            return new StringBuffer().append("a");
        }

        public Object post() {
            return new StringBuffer().append('a');
        }
    }

    @CompareMethods
    public static class CaseStringBuilder {
        public Object pre() {
            return new StringBuilder().append("a");
        }

        public Object post() {
            return new StringBuilder().append('a');
        }
    }

    @CompareMethods
    public static class CaseCharChain {
        public Object pre() {
            return new StringBuilder().append("a").append("b");
        }

        public Object post() {
            return new StringBuilder().append('a').append('b');
        }
    }

    @UnmodifiedMethod
    public static class CaseString {
        public Object pre() {
            return new StringBuilder().append("aa");
        }
    }

    @UnmodifiedMethod
    public static class CaseStringChain {
        public Object pre() {
            return new StringBuilder().append("aa").append("bb");
        }
    }

    @CompareMethods
    public static class CaseMixedChain {
        public Object pre() {
            return new StringBuilder().append("a").append("bb");
        }

        public Object post() {
            return new StringBuilder().append('a').append("bb");
        }
    }

    @CompareMethods
    public static class CaseMixedChainFlipped {
        public Object pre() {
            return new StringBuilder().append("aa").append("b");
        }

        public Object post() {
            return new StringBuilder().append("aa").append('b');
        }
    }

    @UnmodifiedMethod
    public static class CaseEmptyString {
        public Object pre() {
            return new StringBuilder().append("");
        }
    }

    @UnmodifiedMethod
    public static class CaseApostrophe {
        public Object pre() {
            return new StringBuilder().append("'");
        }
    }

    @CompareMethods
    public static class CaseTab {
        public Object pre() {
            return new StringBuilder().append("\t");
        }

        public Object post() {
            return new StringBuilder().append('\t');
        }
    }

    @CompareMethods
    public static class CaseDelete {
        public Object pre() {
            return new StringBuilder().append("\u007F");
        }

        public Object post() {
            return new StringBuilder().append('\u007F');
        }
    }

}
