package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestOptionalWrappedIfToFilterCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new OptionalWrappedIfToFilter();
	}

	@CompareMethods
	public static class Nominal {
		public void pre(Optional<String> o) {
			o.ifPresent(s -> {
				if (s.startsWith("_"))
					System.out.println(s);
			});
		}

		public void post(Optional<String> o) {
			o.filter(s -> s.startsWith("_")).ifPresent(s -> System.out.println(s));
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class ConditionNotOnVariable {
		public void pre(Optional<String> o, boolean b) {
			o.ifPresent(s -> {
				if (b) {
					System.out.println(s);
				}
			});
		}

		public void post(Optional<String> o, boolean b) {
			if (b) {
				o.ifPresent(s -> {
					System.out.println(s);
				});
			}
		}
	}

	@UnmodifiedMethod
	public static class WithElse {
		public void pre(Optional<String> o) {
			o.ifPresent(s -> {
				if (s.startsWith("_"))
					System.out.println(s);
				else
					System.out.println("Arg");
			});
		}
	}

	@CompareMethods
	public static class IsPresentOrElse {
		public void pre(Optional<String> o) {
			o.ifPresentOrElse(s -> {
				if (s.startsWith("_"))
					System.out.println(s);
			}, () -> System.out.println("Empty"));
		}

		public void post(Optional<String> o) {
			o.filter(s -> s.startsWith("_"))
					.ifPresentOrElse(s -> System.out.println(s), () -> System.out.println("Empty"));
		}
	}
}
