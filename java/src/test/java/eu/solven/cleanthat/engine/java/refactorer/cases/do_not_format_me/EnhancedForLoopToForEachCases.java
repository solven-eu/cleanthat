package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnhancedForLoopToForEach;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class EnhancedForLoopToForEachCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new EnhancedForLoopToForEach();
	}

	@CompareMethods
	public static class forEach_singleStatement {
		public void pre(List<String> strings) {
			for (String string : strings) {
				System.out.println(string);
			}
		}

		public void post(List<String> strings) {
			strings.forEach(string -> {
				System.out.println(string);
			});
		}
	}

	@CompareMethods
	public static class forEach_singleStatement_noBlock {
		public void pre(List<String> strings) {
			for (String string : strings)
				System.out.println(string);

		}

		public void post(List<String> strings) {
			strings.forEach(string -> System.out.println(string));
		}
	}

	@CompareMethods
	public static class forEach_multipleStatement {
		public void pre(Collection<String> strings) {
			for (String value : strings) {
				int length = value.length();
				if (length > 2) {
					length /= 2;
				}
				System.out.println(value.substring(0, length));
			}
		}

		public void post(Collection<String> strings) {
			strings.forEach(value -> {
				int length = value.length();
				if (length > 2) {
					length /= 2;
				}
				System.out.println(value.substring(0, length));
			});
		}
	}

	@UnmodifiedMethod
	public static class forEach_assignExpr {
		public String pre(List<String> strings) {
			String s = null;

			for (String string : strings) {
				System.out.println(string);
				s = string;
			}

			return s;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class forEach_assignExpr_outer {
		String s = null;

		public String pre(List<String> strings) {
			strings.forEach(string -> {
				System.out.println(string);
				s = string;
			});

			return s;
		}

		public String post(List<String> strings) {
			for (String string : strings) {
				System.out.println(string);
				s = string;
			}

			return s;
		}
	}

	@UnmodifiedMethod
	public static class ThrowException {
		public void pre(List<String> strings) throws Exception {
			for (String string : strings) {
				throw new Exception(string);
			}
		}
	}

	@CompareMethods
	public static class ThrowImplicitException {
		public void pre(List<String> strings) throws IllegalArgumentException {
			for (String string : strings) {
				throw new IllegalArgumentException(string);
			}
		}

		public void post(List<String> strings) throws IllegalArgumentException {
			strings.forEach(string -> {
				throw new IllegalArgumentException(string);
			});
		}
	}

	@UnmodifiedMethod
	public static class ThrowExplicitException {
		public void pre(List<String> strings) throws TimeoutException {
			for (String string : strings) {
				throw new IllegalArgumentException(string);
			}
		}
	}

	@CompareMethods
	public static class ThrowError {
		public void pre(List<String> strings) throws OutOfMemoryError {
			for (String string : strings) {
				throw new OutOfMemoryError(string);
			}
		}

		public void post(List<String> strings) throws OutOfMemoryError {
			strings.forEach(string -> {
				throw new OutOfMemoryError(string);
			});
		}
	}

	@UnmodifiedMethod
	public static class ThrowThrowable {
		public void pre(List<String> strings) throws Throwable {
			for (String string : strings) {
				throw new Throwable(string);
			}
		}
	}

	@UnmodifiedMethod
	public static class continueKeyword {
		public void pre(List<String> strings) {
			for (String string : strings) {
				if (string.length() >= 3) {
					continue;
				}
			}
		}
	}

	@UnmodifiedMethod
	public static class forEachArray {
		public void pre(String... strings) {
			for (String string : strings) {
				System.out.println(string);
			}
		}
	}
}