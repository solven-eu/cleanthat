package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTestPost;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTestPre;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTest_wildcardImport_Post;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.junit4tojunit5.BeforeAfterTest_wildcardImport_Pre;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.JUnit4ToJUnit5;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class JUnit4ToJUnit5Cases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new JUnit4ToJUnit5();
	}

	@CompareClasses(pre = BeforeAfterTestPre.class, post = BeforeAfterTestPost.class)
	public static class CaseBeforeAfterTest {
	}

	@CompareClasses(pre = BeforeAfterTest_wildcardImport_Pre.class, post = BeforeAfterTest_wildcardImport_Post.class)
	public static class CaseBeforeAfterTest_wildcardImport {
	}

}
