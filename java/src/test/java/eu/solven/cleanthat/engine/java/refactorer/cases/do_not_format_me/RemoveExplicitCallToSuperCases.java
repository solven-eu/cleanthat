package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveExplicitCallToSuper;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class RemoveExplicitCallToSuperCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new RemoveExplicitCallToSuper();
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

}
