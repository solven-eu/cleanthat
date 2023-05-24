package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryImport;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUnnecessaryImportsCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/UnnecessaryImport/";

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new UnnecessaryImport();
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCode_Pre.java", post = PREFIX + "JavaCode_Post.java")
	public static class CaseJavaCode {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithLicense_Pre.java",
			post = PREFIX + "JavaCodeWithLicense_Post.java")
	public static class CaseJavaCodeWithLicense {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithLicensePackage_Pre.java",
			post = PREFIX + "JavaCodeWithLicensePackage_Post.java")
	public static class CaseJavaCodeWithLicensePackage {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "JavaCodeWithPackage_Pre.java",
			post = PREFIX + "JavaCodeWithPackage_Post.java")
	public static class CaseJavaCodeWithPackage {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "Jdk17TextBlock_Pre.java",
			post = PREFIX + "Jdk17TextBlock_Post.java")
	public static class CaseJdk17TextBlock {
	}

	// @Ignore("https://github.com/javaparser/javaparser/issues/3924")
	@CompareCompilationUnitsAsResources(pre = PREFIX + "Revelc_Pre.java", post = PREFIX + "Revelc_Post.java")
	public static class CaseRevelc {
	}

	// This demonstrate a case where an import from same package must not be discarded. Here, we want to import
	// from `ImportedClass.SOME_CONSTANT`referring it as `SOME_CONSTANT`
	// https://github.com/solven-eu/cleanthat/issues/553
	@UnmodifiedCompilationUnitAsString(pre = "package some.pkg;\n" + "\n"
			+ "import static some.pkg.ImportedClass.*;\n"
			+ "\n"
			+ "class SomeClass {\n"
			+ "  String s = SOME_CONSTANT;"
			+ "}")
	public static class Case553_wildcard {
	}

	@UnmodifiedCompilationUnitAsString(pre = "package some.pkg;\n" + "\n"
			+ "import static some.pkg.ImportedClass.SOME_CONSTANT;\n"
			+ "\n"
			+ "class SomeClass {\n"
			+ "  String s = SOME_CONSTANT;"
			+ "}")
	public static class Case553_explicit {
	}

}
