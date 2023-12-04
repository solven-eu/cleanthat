package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.jupiter.api.Disabled;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ImportQualifiedTokens;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

@Disabled("TODO")
public class TestImportQualifiedTokensCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/ImportQualifiedTokens/";

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ImportQualifiedTokens();
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "NoWildcardImport_IsImported_Pre.java",
			post = PREFIX + "NoWildcardImport_IsImported_Post.java")
	public static class NoWildcardImport_IsImported {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "NoWildcardImport_IsNotImported_Pre.java",
			post = PREFIX + "NoWildcardImport_IsNotImported_Post.java")
	public static class NoWildcardImport_IsNotImported {
	}

	// We do not turn `java.time.LocalDate` into `LocalDate` as there is an ambiguity in which package would import it
	@UnmodifiedCompilationUnitAsString(pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
			+ "import java.time.*;\n"
			+ "import other.time.*;\n"
			+ "\n"
			+ "public class NoWildcardImport_IsNotImported_Pre {\n"
			+ "	public static boolean isEmpty(java.time.LocalDate date) {\n"
			+ "		return date.isLeapYear();\n"
			+ "	}\n"
			+ "}\n")
	public static class WithWildcardImport {
	}

	/**
	 * A wildcard-import is present, which does not prevent to qualification.
	 */
	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import static some.pkg.ImportedClass.*;\n"
					+ "\n"
					+ "public class NoWildcardImport_IsNotImported_Pre {\n"
					+ "	public static boolean isLeapYear(java.time.LocalDate date) {\n"
					+ "		return date.isLeapYear();\n"
					+ "	}\n"
					+ "}\n",
			post = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import static some.pkg.ImportedClass.*;\n"
					+ "import static java.time.LocalDate;\n"
					+ "\n"
					+ "public class NoWildcardImport_IsNotImported_Pre {\n"
					+ "	public static boolean isLeapYear(LocalDate date) {\n"
					+ "		return date.isLeapYear();\n"
					+ "	}\n"
					+ "}\n")
	public static class WithWildcardImport_qualify {
	}

	/**
	 * A fully qualified name may exist to prevent a conflict with a not-qualified same name.
	 * 
	 * With a wildcard.
	 */
	@UnmodifiedCompilationUnitAsString(pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
			+ "import static some.pkg.ImportedClass.*;\n"
			+ "\n"
			+ "public class NoWildcardImport_IsNotImported_Pre {\n"
			+ "	public static boolean isLeapYearFromJavaTime(java.time.LocalDate date) {\n"
			+ "		return date.isLeapYear();\n"
			+ "	}\n"
			+ "	public static boolean isLeapYearFromImportedClass(LocalDate date) {\n"
			+ "		return date.isLeapYear();\n"
			+ "	}\n"
			+ "}\n")
	public static class WithWildcardImport_conflictWithWildcard {
	}

	/**
	 * A fully qualified name may exist to prevent a conflict with a not-qualified same name.
	 * 
	 * With a wildcard.
	 */
	@UnmodifiedCompilationUnitAsString(pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
			+ "import static some.pkg.ImportedClass.LocalDate;\n"
			+ "\n"
			+ "public class NoWildcardImport_IsNotImported_Pre {\n"
			+ "	public static boolean isLeapYearFromJavaTime(java.time.LocalDate date) {\n"
			+ "		return date.isLeapYear();\n"
			+ "	}\n"
			+ "	public static boolean isLeapYearFromImportedClass(LocalDate date) {\n"
			+ "		return date.isLeapYear();\n"
			+ "	}\n"
			+ "}\n")
	public static class WithWildcardImport_conflictWithoutWildcard {
	}

	/**
	 * Nominal case where an import can be introduced. No wildcard is present.
	 */
	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "\n"
					+ "public class NoWildcardImport_IsNotImported_Pre {\n"
					+ "	public static boolean isLeapYear(java.time.LocalDate date) {\n"
					+ "		return date.isLeapYear();\n"
					+ "	}\n"
					+ "}\n",
			post = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import static java.time.LocalDate;\n"
					+ "\n"
					+ "public class NoWildcardImport_IsNotImported_Pre {\n"
					+ "	public static boolean isLeapYear(LocalDate date) {\n"
					+ "		return date.isLeapYear();\n"
					+ "	}\n"
					+ "}\n")
	public static class WithoutWildcardImport_qualify {
	}

}
