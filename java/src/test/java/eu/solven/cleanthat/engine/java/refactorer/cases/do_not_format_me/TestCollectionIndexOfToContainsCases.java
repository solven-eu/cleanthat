package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionIndexOfToContains;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestCollectionIndexOfToContainsCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new CollectionIndexOfToContains();
	}

	@CompareMethods
	public static class Nominal {
		public boolean pre(List<?> list, Object o) {
			return list.indexOf(o) >= 0;
		}

		public boolean post(List<?> list, Object o) {
			return list.contains(o);
		}
	}

	@UnmodifiedMethod
	public static class CpmpareWith1 {
		public boolean pre(List<?> list, Object o) {
			return list.indexOf(o) >= 1;
		}
	}

	@UnmodifiedMethod
	public static class IndexOfEquals0 {
		public boolean pre(List<?> list, Object o) {
			return list.indexOf(o) == 0;
		}
	}

	@UnmodifiedMethod
	public static class IndexOfLowerEquals0 {
		public boolean pre(List<?> list, Object o) {
			return list.indexOf(o) <= 0;
		}
	}

	@CompareMethods
	public static class IndexOfNegative {
		public boolean pre(List<?> list, Object o) {
			return list.indexOf(o) < 0;
		}

		public boolean post(List<?> list, Object o) {
			return !list.contains(o);
		}
	}

	@CompareMethods
	public static class ZeroLowerThanIndexOf {
		public boolean pre(List<?> list, Object o) {
			return 0 <= list.indexOf(o);
		}

		public boolean post(List<?> list, Object o) {
			return list.contains(o);
		}
	}

}
