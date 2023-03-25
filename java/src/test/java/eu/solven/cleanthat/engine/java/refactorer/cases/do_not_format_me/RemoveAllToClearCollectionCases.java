package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveAllToClearCollection;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class RemoveAllToClearCollectionCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new RemoveAllToClearCollection();
	}

	@CompareMethods
	public static class Nominal {
		public void pre(Collection<?> c) {
			c.removeAll(c);
		}

		public void post(Collection<?> c) {
			c.clear();
		}
	}

	@UnmodifiedMethod
	public static class DifferentCollections {
		public void pre(Collection<?> c1, Collection<?> c2) {
			c1.removeAll(c2);
		}
	}

	@CompareMethods()
	@CaseNotYetImplemented()
	public static class CaseReturn {
		public Object pre(Collection<?> c) {
			return c.removeAll(c);
		}

		public Object post(Collection<?> c) {
			boolean clearHasEffect = c.isEmpty();
			c.clear();
			return clearHasEffect;
		}
	}

	@UnmodifiedMethod
	public static class OutputInMethod {
		public void pre(Collection<?> c) {
			System.out.print(c.removeAll(c));
		}
	}
}
