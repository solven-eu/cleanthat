package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryModifier;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUnnecessaryModifierCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new UnnecessaryModifier();
	}

	@CompareInnerClasses
	public static class SomeInterface {

		public interface Pre {
			// both abstract and public are ignored by the compiler
			public abstract void bar();

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class Bar {
			}

			// ditto
			public static interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractClass implements MyInterface {}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractClass {}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {}

			// public and static are redundant
			public static enum MyEnum {}
		}

		public interface Post {
			// both abstract and public are ignored by the compiler
			void bar();

			// public, static and final all ignored
			int X = 0;

			// public, static ignored
			class Bar {
			}

			// ditto
			interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			interface MyInterface {}

			// public and static are redundant, but not abstract
			abstract class MyAbstractClass implements MyInterface {}

			// public and static are redundant, but not final
			final class MyFinalClass extends MyAbstractClass {}

			// public, static, and abstract are redundant
			@interface MyAnnotation {}

			// public and static are redundant
			enum MyEnum {}
		}
	}

	@CompareInnerAnnotations
	public static class SomeAnnotation {

		public @interface Pre {
			// both abstract and public are ignored by the compiler
			public abstract String bar();

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class Bar {
			}

			// ditto
			public static interface Baz {
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractClass implements MyInterface {}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractClass {}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {}

			// public and static are redundant
			public static enum MyEnum {}
		}

		public @interface Post {
			// both abstract and public are ignored by the compiler
			String bar();

			// public, static and final all ignored
			int X = 0;

			// public, static ignored
			class Bar {
			}

			// ditto
			interface Baz {
			}

			// public, static and abstract are redundant
			interface MyInterface {}

			// public and static are redundant, but not abstract
			abstract class MyAbstractClass implements MyInterface {}

			// public and static are redundant, but not final
			final class MyFinalClass extends MyAbstractClass {}

			// public, static, and abstract are redundant
			@interface MyAnnotation {}

			// public and static are redundant
			enum MyEnum {}
		}
	}

	// https://github.com/skylot/jadx/pull/1792
	@CompareInnerClasses
	public static class Jadx {

		public interface Pre {
			void test1();

			default void test2() {
			}

			static void test3() {
			}

			abstract void test4();
		}

		public interface Post {
			void test1();

			default void test2() {
			}

			static void test3() {
			}

			void test4();
		}
	}

	// https://github.com/solven-eu/cleanthat/issues/807
	@UnmodifiedInnerClass
	public static class InterfacePrivateMethod {
		public interface Pre {
			private void privateMethod() {
			}

			default void callingPrivateMethod() {
				privateMethod();
			}
		}
	}

	@Ignore("Still broken")
	// https://github.com/solven-eu/cleanthat/issues/807
	@CompareCompilationUnitsAsResources(pre = "/source/do_not_format_me/UnnecessaryModifier/Issue807.java",
			post = "/source/do_not_format_me/UnnecessaryModifier/Issue807.java")
	public static class Issue807 {
	}
}
