package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.jupiter.api.Disabled;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTestPost;
import eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTestPre;
import eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTest_wildcardImport_Post;
import eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTest_wildcardImport_Pre;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.JUnit4ToJUnit5;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

@Disabled("IMutator not-ready")
public class TestJUnit4ToJUnit5Cases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new JUnit4ToJUnit5();
	}

	@CompareClasses(pre = BeforeAfterTestPre.class, post = BeforeAfterTestPost.class)
	public static class CaseBeforeAfterTest {
	}

	@CompareClasses(pre = BeforeAfterTest_wildcardImport_Pre.class, post = BeforeAfterTest_wildcardImport_Post.class)
	public static class CaseBeforeAfterTest_wildcardImport {
	}

}
