package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalMapIdentity;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestOptionalMapIdentityCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new OptionalMapIdentity();
	}

	@CompareMethods
	public static class Nominal {
		public boolean pre(Optional<String> values) {
			return values.map(s -> s).isPresent();
		}

		public boolean post(Optional<String> values) {
			return values.isPresent();
		}
	}

	@CompareMethods
	public static class Nominal_enclosed {
		public boolean pre(Optional<String> values) {
			return values.map((s) -> s).isPresent();
		}

		public boolean post(Optional<String> values) {
			return values.isPresent();
		}
	}

	@UnmodifiedMethod
	public static class MapToOtherVariable {
		public boolean pre(Optional<String> values, String k) {
			return values.map(s -> k).isPresent();
		}
	}
}
