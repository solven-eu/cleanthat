package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.multiple;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryCaseChange;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

import java.util.Arrays;

public class TestLiteralsFirstInComparisons_UnnecessaryCaseChange_Cases extends AJavaparserRefactorerCases {

    @Override
    public IJavaparserAstMutator getTransformer() {
        return new CompositeJavaparserMutator(Arrays.asList(
                new LiteralsFirstInComparisons(),
                new UnnecessaryCaseChange()
        ));
    }

    @CompareMethods
    public static class CaseToLowerCaseWithHardcodedLowercase {
        public Object pre(String string) {
            return string.toLowerCase().equals("lowercase");
        }

        public Object post(String string) {
            return "lowercase".equalsIgnoreCase(string);
        }
    }

}
