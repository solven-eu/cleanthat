package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryFullyQualifiedName;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class UnnecessaryFullyQualifiedNameCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
		return new UnnecessaryFullyQualifiedName();
	}

	// https://stackoverflow.com/questions/51257256/why-dont-we-import-java-lang-package
	@CompareMethods
	public static class JavaLangType {
		public Object pre() {
			java.lang.String s = new java.lang.String();
			return s;
		}

		public Object post() {
			String s = new String();
			return s;
		}
	}

	@Ignore("TODO This case is not a NodeWithType")
	@CompareMethods
	public static class StaticMethodCall {
		public Object pre() {
			LocalDate ld = java.time.LocalDate.now();
			return ld;
		}

		public Object post(List<?> input) {
			LocalDate ld = LocalDate.now();
			return ld;
		}
	}

	@CompareMethods
	public static class VariableType {
		public Object pre() {
			java.util.List<?> l = new ArrayList<>();
			return l;
		}

		public Object post() {
			List<?> l = new ArrayList<>();
			return l;
		}
	}

	@CompareMethods
	public static class ConstructorType {
		public Object pre() {
			List<?> l = new java.util.ArrayList<>();
			return l;
		}

		public Object post() {
			List<?> l = new ArrayList<>();
			return l;
		}
	}

	@CompareMethods
	public static class VariableAndConstructorType {
		public Object pre() {
			java.util.List<?> l = new java.util.ArrayList<>();
			return l;
		}

		public Object post() {
			List<?> l = new ArrayList<>();
			return l;
		}
	}

	@CompareMethods
	public static class ArgumentType {
		public Object pre(java.util.List<?> l) {
			return l;
		}

		public Object post(List<?> l) {
			return l;
		}
	}

	@UnchangedMethod
	public static class NotImportedType {
		public Object pre(java.util.Map<?, ?> m) {
			return m;
		}
	}
}
