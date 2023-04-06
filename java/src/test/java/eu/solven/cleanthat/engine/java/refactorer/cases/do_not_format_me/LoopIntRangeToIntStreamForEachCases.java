package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.stream.IntStream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LoopIntRangeToIntStreamForEach;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class LoopIntRangeToIntStreamForEachCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new LoopIntRangeToIntStreamForEach();
	}

	@CompareMethods
	public static class Nominal {
		public void pre() {
			for (int j = 0; j < 10; ++j) {
				System.out.println(j);
			}
		}

		public void post() {
			IntStream.range(0, 10).forEach(j -> {
				System.out.println(j);
			});
		}
	}

	@CompareMethods
	public static class forEach_upperBoundFromMethod {
		public void pre(String s, StringBuilder sb) {
			for (int j = 0; j < s.length(); ++j) {
				char c = s.charAt(j);
				sb.append(c);
				if (c == '\'')
					sb.append(c);
			}
		}

		public void post(String s, StringBuilder sb) {
			IntStream.range(0, s.length()).forEach(j -> {
				char c = s.charAt(j);
				sb.append(c);
				if (c == '\'')
					sb.append(c);
			});
		}
	}

	@UnmodifiedMethod
	public static class EditLoopVariable {
		public void pre() {
			for (int j = 0; j < 10; ++j) {
				if (j == 5) {
					j++;
				} else {
					System.out.println(j);
				}
			}
		}
	}

	@UnmodifiedMethod
	public static class assignExpr {
		public int pre() {
			int sum = 0;

			for (int j = 0; j < 10; ++j) {
				sum += j;
			}
			return sum;
		}
	}

	@UnmodifiedMethod
	public static class returnExpr {
		public int pre() {
			for (int j = 0; j < 10; ++j) {
				if (j == 5) {
					return 0;
				} else {
					System.out.println(j);
				}
			}
			return 1;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class continueStmt {
		public void pre() {
			for (int j = 0; j < 10; ++j) {
				if (j == 5) {
					continue;
				} else {
					System.out.println(j);
				}
			}
		}

		public void post() {
			IntStream.range(0, 10).forEach(j -> {
				if (j == 5) {
					return;
				} else {
					System.out.println(j);
				}
			});
		}
	}

	@UnmodifiedMethod
	public static class breakExpr {
		public int pre() {
			for (int j = 0; j < 10; ++j) {
				if (j == 5) {
					break;
				} else {
					System.out.println(j);
				}
			}
			return 1;
		}
	}

	@UnmodifiedMethod
	public static class multipleVariable {
		public void pre() {
			for (int a = 3, b = 5; a < 99; a++, b++)
				System.out.println(a + b);
		}
	}

	@UnmodifiedMethod
	public static class declaredPreviously {
		public void pre() {
			int a, b = 0;

			for (a = 3, b = 5; a < 99; a++) {
				System.out.println(a + b);
			}
		}
	}

	@UnmodifiedMethod
	public static class complexInitialization {
		private Object a() {
			return null;
		}

		private Object b() {
			return null;
		}

		public void pre() {
			for (a(), b();;)
				System.out.println("Hello");
		}
	}

	@UnmodifiedMethod
	public static class NotSimpleIncrementation {
		public void pre() {
			for (int j = 0; j < 10; j += 2) {
				System.out.println(j);
			}
		}
	}

	@CompareCompilationUnitsAsStrings(
			pre = "public class WithoutImport {\n" + "	public void pre() {\n"
					+ "		for (int j = 0; j < 10; ++j) {\n"
					+ "			System.out.println(j);\n"
					+ "		}\n"
					+ "	}\n"
					+ "}",
			post = "public class WithoutImport {\n" + "	public void pre() {\n"
					+ "		        java.util.stream.IntStream.range(0, 10).forEach(j -> {\n"
					+ "            System.out.println(j);\n"
					+ "        });"
					+ "	}\n"
					+ "}")
	public static class IntStreamNotYetImported {

	}
}
