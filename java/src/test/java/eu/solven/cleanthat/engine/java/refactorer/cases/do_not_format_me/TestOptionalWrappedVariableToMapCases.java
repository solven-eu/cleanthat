package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Optional;
import java.util.OptionalInt;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestOptionalWrappedVariableToMapCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserAstMutator getTransformer() {
		return new OptionalWrappedVariableToMap();
	}

	@CompareMethods
	public static class Optional_ifPresent {
		public void pre(Optional<String> o) {
			o.ifPresent(s -> {
				String subString = s.substring(1);
				System.out.println(subString);
			});
		}

		public void post(Optional<String> o) {
			o.map(s -> s.substring(1)).ifPresent(subString -> {
				System.out.println(subString);
			});
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class OptionalInt_ifPresent {
		public void pre(OptionalInt o, String s) {
			o.ifPresent(i -> {
				String subString = s.substring(i);
				System.out.println(subString);
			});
		}

		public void post(OptionalInt o, String s) {
			o.stream().mapToObj(i -> s.substring(i)).findAny().ifPresent(subString -> {
				System.out.println(subString);
			});
		}
	}

	@CompareMethods
	public static class Optional_ifPresentOrElse {
		public void pre(Optional<String> o, int i) {
			o.ifPresentOrElse(s -> {
				String subString = s.substring(i);
				System.out.println(subString);
			}, () -> System.out.print(i));
		}

		public void post(Optional<String> o, int i) {
			o.map(s -> s.substring(i)).ifPresentOrElse(subString -> {
				System.out.println(subString);
			}, () -> System.out.print(i));
		}
	}

	@CompareMethods
	public static class Optional_ifPresent_multipleMaps {
		public void pre(Optional<String> o) {
			o.ifPresent(s -> {
				String subString = s.substring(1);
				String subString2 = subString.toUpperCase();
				System.out.println(subString2);
			});
		}

		public void post(Optional<String> o) {
			o.map(s -> s.substring(1)).map(subString -> subString.toUpperCase()).ifPresent(subString2 -> {
				System.out.println(subString2);
			});
		}
	}

	@CompareMethods
	public static class Optional_map {
		public void pre(Optional<String> o) {
			o.map(s -> {
				String subString = s.substring(1);
				return subString.toUpperCase();
			}).ifPresent(subString2 -> {
				System.out.println(subString2);
			});
		}

		public void post(Optional<String> o) {
			o.map(s -> s.substring(1)).map(subString -> {
				return subString.toUpperCase();
			}).ifPresent(subString2 -> {
				System.out.println(subString2);
			});
		}
	}
}
