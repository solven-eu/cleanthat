package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UsePredefinedStandardCharset;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUsePredefinedStandardCharsetCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new UsePredefinedStandardCharset();
	}

	@CompareMethods
	public static class UTF8 {
		public Charset pre() {
			return Charset.forName("UTF-8");
		}

		public Charset post() {
			return StandardCharsets.UTF_8;
		}
	}

	@CompareMethods
	public static class UTF16 {
		public Charset pre() {
			return Charset.forName("UTF-16");
		}

		public Charset post() {
			return StandardCharsets.UTF_16;
		}
	}

	@CompareMethods
	public static class UTF16BE {
		public Charset pre() {
			return Charset.forName("UTF-16BE");
		}

		public Charset post() {
			return StandardCharsets.UTF_16BE;
		}
	}

	@CompareMethods
	public static class UTF16LE {
		public Charset pre() {
			return Charset.forName("UTF-16LE");
		}

		public Charset post() {
			return StandardCharsets.UTF_16LE;
		}
	}

	@CompareMethods
	public static class UsAscii {
		public Charset pre() {
			return Charset.forName("US-ASCII");
		}

		public Charset post() {
			return StandardCharsets.US_ASCII;
		}
	}

	@CompareMethods
	public static class Iso8859_1 {
		public Charset pre() {
			return Charset.forName("ISO-8859-1");
		}

		public Charset post() {
			return StandardCharsets.ISO_8859_1;
		}
	}

	@UnmodifiedMethod
	public static class ClassNameConflict {

		private static class Charset {

			public static Charset forName(String string) {
				return new Charset();
			}

		}

		public Charset pre() {
			return Charset.forName("UTF-8");
		}
	}

	@CompareCompilationUnitsAsStrings(
			pre = "	import java.nio.charset.Charset;\n" + "\n"
					+ "	public class UTF8 {\n"
					+ "		public Charset pre() {\n"
					+ "			return Charset.forName(\"UTF-8\");\n"
					+ "		}\n"
					+ "	}",
			post = "import java.nio.charset.Charset;\n" + "\n"
					+ "public class UTF8 {\n"
					+ "\n"
					+ "    public Charset pre() {\n"
					+ "        return java.nio.charset.StandardCharsets.UTF_8;\n"
					+ "    }\n"
					+ "}\n"
					+ "")
	public static class TargetNotImported {
	}

}
