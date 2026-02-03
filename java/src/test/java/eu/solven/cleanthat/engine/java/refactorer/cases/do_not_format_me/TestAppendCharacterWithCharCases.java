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
        public Object pre(CustomStringBuilder builder) {
            return builder.append("a");
        }
    }

    @CompareMethods
    public static class CaseCharArrayWriter {
        public Object pre(CharArrayWriter writer) {
            return writer.append("a");
        }

        public Object post(CharArrayWriter writer) {
            return writer.append('a');
        }
    }

    @CompareMethods
    public static class CaseStringWriter {
        public Object pre(StringWriter writer) {
            return writer.append("a");
        }

        public Object post(StringWriter writer) {
            return writer.append('a');
        }
    }

    @CompareMethods
    public static class CaseStringBuffer {
        public Object pre(StringBuffer buffer) {
            return buffer.append("a");
        }

        public Object post(StringBuffer buffer) {
            return buffer.append('a');
        }
    }

    @CompareMethods
    public static class CaseStringBuilder {
        public Object pre(StringBuilder builder) {
            return builder.append("a");
        }

        public Object post(StringBuilder builder) {
            return builder.append('a');
        }
    }

    @CompareMethods
    public static class CaseCharChain {
        public Object pre(StringBuilder builder) {
            return builder.append("a").append("b");
        }

        public Object post(StringBuilder builder) {
            return builder.append('a').append('b');
        }
    }

    @UnmodifiedMethod
    public static class CaseString {
        public Object pre(StringBuilder builder) {
            return builder.append("aa");
        }
    }

    @UnmodifiedMethod
    public static class CaseStringChain {
        public Object pre(StringBuilder builder) {
            return builder.append("aa").append("bb");
        }
    }

    @CompareMethods
    public static class CaseMixedChain {
        public Object pre(StringBuilder builder) {
            return builder.append("a").append("bb");
        }

        public Object post(StringBuilder builder) {
            return builder.append('a').append("bb");
        }
    }

    @CompareMethods
    public static class CaseMixedChainFlipped {
        public Object pre(StringBuilder builder) {
            return builder.append("aa").append("b");
        }

        public Object post(StringBuilder builder) {
            return builder.append("aa").append('b');
        }
    }

    @UnmodifiedMethod
    public static class CaseEmptyString {
        public Object pre(StringBuilder builder) {
            return builder.append("");
        }
    }

    @UnmodifiedMethod
    public static class CaseApostrophe {
        public Object pre(StringBuilder builder) {
            return builder.append("'");
        }
    }

    @CompareMethods
    public static class CaseTab {
        public Object pre(StringBuilder builder) {
            return builder.append("\t");
        }

        public Object post(StringBuilder builder) {
            return builder.append('\t');
        }
    }

    @CompareMethods
    public static class CaseDelete {
        public Object pre(StringBuilder builder) {
            return builder.append("\u007F");
        }

        public Object post(StringBuilder builder) {
            return builder.append('\u007F');
        }
    }

}
