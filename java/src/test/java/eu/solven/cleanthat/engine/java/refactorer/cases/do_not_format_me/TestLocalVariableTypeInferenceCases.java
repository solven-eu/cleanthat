package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestLocalVariableTypeInferenceCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new LocalVariableTypeInference();
	}

	@CompareMethods
	public static class PrimitiveInt {
		public Object pre() {
			int i = 10;
			return i;
		}

		public Object post() {
			var i = 10;
			return i;
		}
	}

	@CompareMethods
	public static class CaseConstructor {
		public Object pre() {
			String i = new String(new byte[] { 0, 1, 2 });
			return i;
		}

		public Object post() {
			var i = new String(new byte[] { 0, 1, 2 });
			return i;
		}
	}

	@CompareMethods
	public static class CaseLoop {
		public void pre() {
			for (int i = 0; i < 10; i++) {
				System.out.println(i);
			}
		}

		public void post() {
			for (var i = 0; i < 10; i++) {
				System.out.println(i);
			}
		}
	}

	@UnmodifiedMethod
	public static class CaseDifferentType_noReAssigment {
		public Object pre() {
			final CharSequence i = new StringBuilder();

			return i;
		}
	}

	// If the variable type is replaced by var, it takes the initial type, which may not be compatible with further
	// allocation
	@UnmodifiedMethod
	public static class CaseDifferentType_reAssigment {
		public Object pre() {
			CharSequence i = new StringBuilder();

			i = i.toString();

			return i;
		}
	}

	// https://github.com/javaparser/javaparser/issues/3898
	@UnmodifiedMethod
	public static class CaseGenerics {
		public Object pre() {
			ArrayList<String> i = new ArrayList<String>();
			return i;
		}

		public Object post() {
			var i = new ArrayList<String>();
			return i;
		}
	}

	@UnmodifiedMethod
	public static class NullInitializer {
		public Object pre() {
			List<?> i = null;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class LambdaInitializer {
		public Object pre() {
			Function<String, String> i = s -> s.toLowerCase();
			return i;
		}
	}

	@UnmodifiedMethod
	public static class MethodRefInitializer {
		public Object pre() {
			Function<String, String> i = String::toLowerCase;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class ArrayInitializer {
		public Object pre() {
			Object[] i = { 1, 2 };
			return i;
		}
	}

	@UnmodifiedCompilationUnitAsString(
			pre = "import java.util.List;import java.util.ArrayList;import custom.CustomType; class SomeClass{"
					+ "   void m(){ArrayList<CustomType> i = new ArrayList<>();}"
					+ "}",
			post = "import java.util.List;import java.util.ArrayList;import custom.CustomType; class SomeClass{"
					+ "   void m(){var i = new ArrayList<>();}"
					+ "}")
	public static class CaseSameType_unresolved {
	}

	// https://github.com/javaparser/javaparser/issues/3898
	@CompareMethods
	public static class CaseAnonymous_noGenerics {
		public Object pre() {
			Exception i = new Exception() {
				private static final long serialVersionUID = -7496095234003248150L;

				{
					setStackTrace(new StackTraceElement[] {});
				}
			};
			return i;
		}

		public Object post() {
			var i = new Exception() {
				private static final long serialVersionUID = -7496095234003248150L;

				{
					setStackTrace(new StackTraceElement[] {});
				}
			};
			return i;
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseAnonymous_withGenerics {
		public Object pre() {
			HashMap<String, Object> i = new HashMap<>() {
				private static final long serialVersionUID = -7496095234003248150L;

				{
					put("k", "v");
				}
			};
			return i;
		}

		public Object post() {
			var i = new HashMap<>() {
				private static final long serialVersionUID = -7496095234003248150L;

				{
					put("k", "v");
				}
			};
			return i;
		}
	}

	@CompareMethods
	public static class CaseMethodCalled {
		public Object pre() {
			final LocalDate i = LocalDate.now();
			return i;
		}

		public Object post() {
			final var i = LocalDate.now();
			return i;
		}
	}

	@UnmodifiedMethod
	public static class NotLocal {
		public Object pre(int i) {
			return i;
		}
	}

	// 'var' is not allowed by JDK in Compound declarations
	@UnmodifiedMethod
	public static class CompoundDeclarations {
		public Object pre() {
			int i = 1, j = 2;
			return i + j;
		}
	}

	@CompareMethods
	public static class WithAnnotation {
		public Object pre() {
			@edu.umd.cs.findbugs.annotations.SuppressWarnings
			int i = 10;
			return i;
		}

		public Object post() {
			@edu.umd.cs.findbugs.annotations.SuppressWarnings var i = 10;
			return i;
		}
	}

	@UnmodifiedMethod
	public static class ClassField {
		public Object pre() {
			return new HashMap<>() {
				private static final long serialVersionUID = -7496095234003248150L;

				final int i = 10;

				{
					put("k", i);
				}
			};
		}
	}

	@UnmodifiedMethod
	public static class Case_Lambda_unknownType {
		public Consumer<ICodeProviderFile> pre() {
			Consumer<ICodeProviderFile> consumer = file -> {
				System.out.println();
			};

			return consumer;
		}
	}

	@UnmodifiedMethod
	public static class Case_Lambda_knownType {
		public Consumer<String> pre() {
			Consumer<String> consumer = file -> {
				System.out.println();
			};

			return consumer;
		}
	}

	// Code preventing var in Case_UnclearGenericBounds breaks var in this one
	// @CompareMethods
	@UnmodifiedMethod
	public static class CaseGenericInType_NotInInitializer {
		public Optional<Map.Entry<String, String>> pre() {
			Optional<Map.Entry<String, String>> optPathAndContent = Optional.empty();

			if (optPathAndContent.isEmpty()) {
				return Optional.empty();
			}

			Map.Entry<String, String> pathAndContent = optPathAndContent.get();
			return Optional.of(pathAndContent);
		}

		public Optional<Map.Entry<String, String>> post() {
			Optional<Map.Entry<String, String>> optPathAndContent = Optional.empty();

			if (optPathAndContent.isEmpty()) {
				return Optional.empty();
			}

			var pathAndContent = optPathAndContent.get();
			return Optional.of(pathAndContent);
		}
	}

	@UnmodifiedMethod
	public static class Case_UnclearGenericBounds {
		public List<Class<? extends IMutator>> pre(List<String> classNames) {
			List<Class<? extends IMutator>> classes = classNames.stream().map(s -> {
				try {
					return Class.forName(s);
				} catch (ClassNotFoundException e) {
					return null;
				}
			}).map(c -> (Class<? extends IMutator>) c.asSubclass(IMutator.class)).collect(Collectors.toList());
			return classes;
		}
	}

}
