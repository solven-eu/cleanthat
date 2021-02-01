package eu.solven.cleanthat.rules.cases;

import java.util.Collection;

import eu.solven.cleanthat.rules.ReorderModifiers;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverClass;

public class ReorderModifiersCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new ReorderModifiers();
	}

	public static class CaseFields {
		public String getTitle() {
			return "Fields";
		}

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
		public Object post(Collection<?> input) {
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

		public Object post(Collection<?> input) {
			return new Object() {
				@SuppressWarnings("unused")
				protected final synchronized void staticMethod() {
					// Empty
				}
			};
		}
	}

	public static class CaseTypes implements ICaseOverClass {
		public String getTitle() {
			return "Types";
		}

		static private class Pre {
			// empty
		}

		public static class Post {
			// empty
		}
	}
}
