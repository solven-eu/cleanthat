package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.HashMap;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EmptyControlStatement;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class EmptyControlStatementCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
		return new EmptyControlStatement();
	}

	// We keep the 'if' statements as they may call methods, hence having side-effects
	@CompareMethods
	public static class VariousCases {
		public void pre() {
			// empty if statement
			if (true)
				;

			// empty as well
			if (true) {
			}

			{

			}

			{
				{

					{

					}
				}
			}
		}

		public void post() {
			// empty if statement
			if (true)
				;

			// empty as well
			if (true) {
			}

		}
	}

	// In edgy-cases, one would consider this as a feature (e.g. if one wants a custom class)
	@Ignore("This may deserve a dedicated mutator")
	@CompareMethods
	public static class AnonymousClass {
		public Object pre() {
			return new HashMap<>() {

			};
		}

		public Object post() {
			return new HashMap<>();
		}
	}

	@CompareMethods
	public static class AnonymousClass_EmptyInitializer {
		public Object pre() {
			return new HashMap<>() {
				{

				}

				@Override
				public Object get(Object key) {
					return super.get(key);
				}

			};
		}

		public Object post() {
			return new HashMap<>() {

				@Override
				public Object get(Object key) {
					return super.get(key);
				}

			};
		}
	}

	@CompareMethods
	public static class AnonymousClass_EmptyInitializer_RecursiveEmpty {
		public Object pre() {
			return new HashMap<>() {
				{
					{

					}
				}

				@Override
				public Object get(Object key) {
					return super.get(key);
				}

			};
		}

		public Object post() {
			return new HashMap<>() {

				@Override
				public Object get(Object key) {
					return super.get(key);
				}

			};
		}
	}
}
