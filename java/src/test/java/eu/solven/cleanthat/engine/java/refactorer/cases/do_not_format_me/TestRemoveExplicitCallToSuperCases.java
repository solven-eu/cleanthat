package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveExplicitCallToSuper;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestRemoveExplicitCallToSuperCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new RemoveExplicitCallToSuper();
	}

	private static class SomeParent {
		SomeParent(String s) {
			System.out.print(s);
		}
	}

	@CompareInnerClasses
	public static class SuperInConstructor {
		public class Pre {
			public Pre() {
				super();

				"".toString();
			}
		}

		public class Post {
			public Post() {

				"".toString();
			}
		}
	}

	@UnmodifiedInnerClass
	public static class SuperInConstructor_withArguments {
		public class Pre extends SomeParent {
			public Pre() {
				super("");

				"".toString();
			}
		}

	}

	@UnmodifiedInnerClass
	public static class SuperInMethod {
		public class SomeClass {
			public void someMethod() {
				"".toString();
			}
		}

		public class Pre extends SomeClass {
			public void someMethod() {
				super.someMethod();
			}
		}

		public class Post extends SomeClass {
			public void someMethod() {
				super.someMethod();
			}
		}
	}

	@CompareInnerClasses
	@CaseNotYetImplemented
	public static class WithComment {
		public class Pre {
			public Pre() {
				// This is an important comment
				super();

				"".toString();
			}
		}

		public class Post {
			public Post() {

				"".toString();
			}
		}
	}

	// `this();` can be removed for equivalent behavior only because `Pre()` exists but is a noop
	@UnmodifiedInnerClass
	public static class ThisEmptyInConstructor {
		public class Pre {

			public Pre() {
			}

			public Pre(String s) {
				this();

				s.toString();
			}
		}
	}

	// Dropping `this` would lead to compilation error as `s` in final
	@UnmodifiedInnerClass
	public static class ThisEmptyInConstructor_notEmptyCtor_finalField {
		public class Pre {
			final String s;

			public Pre() {
				s = "";
			}

			public Pre(String s) {
				this();

				s.toString();
			}
		}
	}

	// Dropping `this` would prevent `s` initialization
	@UnmodifiedInnerClass
	public static class ThisEmptyInConstructor_notEmptyCtor_notFinalField {
		public class Pre {
			String s;

			public Pre() {
				s = "";
			}

			public Pre(String s) {
				this();

				s.toString();
			}
		}
	}

	@CompareInnerClasses
	public static class SuperEmptyInConstructor_notEmptyCtor {
		class Parent {
			final String s;

			public Parent() {
				s = "";
			}

		}

		public class Pre extends Parent {

			public Pre(String s) {
				super();

				s.toString();
			}
		}

		public class Post extends Parent {

			public Post(String s) {

				s.toString();
			}
		}
	}

	@UnmodifiedInnerClass
	public static class StandardExceptionConstructors {
		public class Pre extends Exception {
			private static final long serialVersionUID = 5838323818892271987L;

			public Pre() {
				super();
			}

			public Pre(String message, Throwable cause) {
				super(message, cause);
			}

			protected Pre(String message) {
				super(message);
			}

			public Pre(Throwable cause) {
				super(cause);
			}
		}

	}
}
