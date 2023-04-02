package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;

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
		public void pre(List<String> strings) {
			String s = null;

			for (String string : strings) {
				System.out.println(string);
				s = string;
			}
		}
	}
}
