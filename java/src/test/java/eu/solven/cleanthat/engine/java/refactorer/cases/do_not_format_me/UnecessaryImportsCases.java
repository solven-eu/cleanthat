package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryImport;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class UnecessaryImportsCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/UnnecessaryImport/";

	@Override
	public IJavaparserMutator getTransformer() {
		return new UnnecessaryImport();
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCode_Pre.java", post = PREFIX + "JavaCode_Post.java")
	public static class caseJavaCode {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithLicense_Pre.java",
			post = PREFIX + "JavaCodeWithLicense_Post.java")
	public static class caseJavaCodeWithLicense {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithLicensePackage_Pre.java",
			post = PREFIX + "JavaCodeWithLicensePackage_Post.java")
	public static class caseJavaCodeWithLicensePackage {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithPackage_Pre.java",
			post = PREFIX + "JavaCodeWithPackage_Post.java")
	public static class caseJavaCodeWithPackage {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "Jdk17TextBlock_Pre.java",
			post = PREFIX + "Jdk17TextBlock_Post.java")
	public static class caseJdk17TextBlock {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "Revelc_Pre.java", post = PREFIX + "Revelc_Post.java")
	public static class caseRevelc {
	}

}
