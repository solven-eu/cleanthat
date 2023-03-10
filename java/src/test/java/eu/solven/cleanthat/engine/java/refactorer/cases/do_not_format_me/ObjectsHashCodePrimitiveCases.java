package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Objects;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectsHashCodePrimitive;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class ObjectsHashCodePrimitiveCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ObjectsHashCodePrimitive();
	}

	@CompareMethods
	public static class CaseInt {
		public int pre(int a) {
			return Objects.hashCode(a);
		}

		public int post(int a) {
			return Integer.hashCode(a);
		}
	}

	@CompareMethods
	public static class CaseDouble {
		public int pre(double a) {
			return Objects.hashCode(a);
		}

		public int post(double a) {
			return Double.hashCode(a);
		}
	}

	@UnmodifiedMethod
	public static class CaseObject {
		public int pre(Object a) {
			return Objects.hashCode(a);
		}
	}
}
