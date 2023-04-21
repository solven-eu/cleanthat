package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.SuppressCleanthat;
import eu.solven.cleanthat.engine.java.refactorer.DeleteAnythingMutator;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestDeleteAnythingCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new DeleteAnythingMutator();
	}

	@UnmodifiedMethod
	public static class onMethod {
		@SuppressCleanthat
		public void pre() {
			// keep me
		}
	}

	@CompareMethods
	public static class onVariable {
		public void pre() {
			@SuppressCleanthat
			int i = 1;
			int j = 2;
		}

		void post() {
			@SuppressCleanthat
			int i = 1;
		}
	}

	@UnmodifiedInnerClass
	public static class onClass_keepClass {
		@SuppressCleanthat
		public class Pre {
			final int i = 1;

			void someMethod() {
				int i = 1;
				int j = 2;
			}

			void otherMethod() {

			}
		}

		class Post {
			@SuppressCleanthat
			void someMethod() {
				int i = 1;
				int j = 2;
			}
		}
	}

	@CompareInnerClasses
	public static class onClass_keepMethod {
		public class Pre {
			final int i = 1;

			@SuppressCleanthat
			void someMethod() {
				int i = 1;
				int j = 2;
			}

			void otherMethod() {

			}
		}

		class Post {
			@SuppressCleanthat
			void someMethod() {
				int i = 1;
				int j = 2;
			}
		}
	}

	@CompareInnerClasses
	public static class onClass_keepField {
		public class Pre {
			@SuppressCleanthat
			final int i = 1;

			void someMethod() {
				int i = 1;
				int j = 2;
			}

			void otherMethod() {

			}
		}

		class Post {
			@SuppressCleanthat
			final int i = 1;
		}
	}

	@CompareInnerClasses
	public static class onClass_keepVariable {
		public class Pre {
			final int i = 1;

			void someMethod() {
				@SuppressCleanthat
				int i = 1;
				int j = 2;
			}

			void otherMethod() {

			}
		}

		class Post {
			void someMethod() {
				@SuppressCleanthat
				int i = 1;
			}
		}
	}

}