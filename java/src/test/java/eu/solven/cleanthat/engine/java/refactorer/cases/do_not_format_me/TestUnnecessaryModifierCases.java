package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerEnums;
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
			public static class SomeNestedClass {
			}

			// ditto
			public static interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractNestedClass {
			}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {
			}

			// public and static are redundant
			public static enum MyEnum {
			}
		}

		public interface Post {
			// both abstract and public are ignored by the compiler
			void bar();

			// public, static and final all ignored
			int X = 0;

			// public, static ignored
			class SomeNestedClass {
			}

			// ditto
			interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			interface MyInterface {
			}

			// public and static are redundant, but not abstract
			abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			final class MyFinalClass extends MyAbstractNestedClass {
			}

			// public, static, and abstract are redundant
			@interface MyAnnotation {
			}

			// public and static are redundant
			enum MyEnum {
			}
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
			public static class SomeNestedClass {
			}

			// ditto
			public static interface Baz {
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractClass {
			}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {
			}

			// public and static are redundant
			public static enum MyEnum {
			}
		}

		public @interface Post {
			// both abstract and public are ignored by the compiler
			String bar();

			// public, static and final all ignored
			int X = 0;

			// public, static ignored
			class SomeNestedClass {
			}

			// ditto
			interface Baz {
			}

			// public, static and abstract are redundant
			interface MyInterface {
			}

			// public and static are redundant, but not abstract
			abstract class MyAbstractClass implements MyInterface {
			}

			// public and static are redundant, but not final
			final class MyFinalClass extends MyAbstractClass {
			}

			// public, static, and abstract are redundant
			@interface MyAnnotation {
			}

			// public and static are redundant
			enum MyEnum {
			}
		}
	}

	@CompareInnerClasses
	public static class SomeClass {

		public static class Pre {
			public final int x = 0;

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class SomeNestedClass {
			}

			// ditto
			public static interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractNestedClass {
			}

			public class MyInnerClass {
				{
					System.out.print(x);
				}
			}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {
			}

			// public and static are redundant
			public static enum MyEnum {
			}
		}

		public static class Post {
			public final int x = 0;

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class SomeNestedClass {
			}

			// ditto
			public interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractNestedClass {
			}

			public class MyInnerClass {
				{
					System.out.print(x);
				}
			}

			// public, static, and abstract are redundant
			public @interface MyAnnotation {
			}

			// public and static are redundant
			public enum MyEnum {
			}
		}
	}

	@CompareInnerEnums
	public static class SomeEnum {

		public enum Pre {
			;
			public final int x = 0;

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class SomeNestedClass {
			}

			// ditto
			public static interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public static abstract interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractNestedClass {
			}

			public class MyInnerClass {
				{
					System.out.print(x);
				}
			}

			// public, static, and abstract are redundant
			public static abstract @interface MyAnnotation {
			}

			// public and static are redundant
			public static enum MyEnum {
			}
		}

		public enum Post {
			;
			public final int x = 0;

			// public, static and final all ignored
			public static final int X = 0;

			// public, static ignored
			public static class SomeNestedClass {
			}

			// ditto
			public interface Baz {
			}

			static String parse() {
				return "parsed";
			}

			// public, static and abstract are redundant
			public interface MyInterface {
			}

			// public and static are redundant, but not abstract
			public static abstract class MyAbstractNestedClass implements MyInterface {
			}

			// public and static are redundant, but not final
			public static final class MyFinalClass extends MyAbstractNestedClass {
			}

			public class MyInnerClass {
				{
					System.out.print(x);
				}
			}

			// public, static, and abstract are redundant
			public @interface MyAnnotation {
			}

			// public and static are redundant
			public enum MyEnum {
			}
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

	@CompareCompilationUnitsAsStrings(pre = "public final record Person (String name, String address) {}",
			post = "public record Person (String name, String address) {}")
	public static class Issue844_record {
	}

	@CompareCompilationUnitsAsStrings(
			pre = "public class NestingRecord {\n"
					+ "		public static final record Person (String name, String address) {}\n"
					+ "	}",
			post = "public class NestingRecord {\n" + "		public record Person (String name, String address) {}\n"
					+ "	}")
	public static class Issue846_record_nested {
	}

	@CompareCompilationUnitsAsResources(
			pre = "/source/do_not_format_me/UnnecessaryModifier/" + "NestingRecord_Issue846_Pre.java",
			post = "/source/do_not_format_me/UnnecessaryModifier/" + "NestingRecord_Issue846_Post.java")
	public static class Issue846_record_nesting {
	}

}
