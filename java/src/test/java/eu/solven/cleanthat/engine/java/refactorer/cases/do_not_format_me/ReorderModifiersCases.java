package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ModifierOrder;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class ReorderModifiersCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ModifierOrder();
	}

	@CompareMethods
	public static class CaseFields {
		@SuppressWarnings("unused")
		public Object pre() {
			return new Object() {
				final static public String FINAL_STATIC_PUBLIC = "";
				static final public String STATIC_FINAL_PUBLIC = "";
				final public static String FINAL_PUBLIC_STATIC = "";
				public final static String PUBLIC_FINAL_STATIC = "";
				static public final String STATIC_PUBLIC_FINAL = "";
				public static final String PUBLIC_STATIC_FINAL = "";
			};
		}

		@SuppressWarnings("unused")
		public Object post() {
			return new Object() {
				public static final String FINAL_STATIC_PUBLIC = "";
				public static final String STATIC_FINAL_PUBLIC = "";
				public static final String FINAL_PUBLIC_STATIC = "";
				public static final String PUBLIC_FINAL_STATIC = "";
				public static final String STATIC_PUBLIC_FINAL = "";
				public static final String PUBLIC_STATIC_FINAL = "";
			};
		}
	}

	@CompareMethods
	public static class CaseMethods {
		public String getTitle() {
			return "Methods";
		}

		public Object pre() {
			return new Object() {
				@SuppressWarnings("unused")
				synchronized protected final void staticMethod() {
					// Empty
				}
			};
		}

		public Object post() {
			return new Object() {
				@SuppressWarnings("unused")
				protected final synchronized void staticMethod() {
					// Empty
				}
			};
		}
	}

	@CompareTypes
	public static class CaseTypes {
		static public class Pre {
			// empty
		}

		public static class Post {
			// empty
		}
	}
}
