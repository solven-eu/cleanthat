package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class LocalVariableTypeInferenceCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
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

	// https://github.com/javaparser/javaparser/issues/3898
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseDifferentType_noReAssigment {
		public Object pre() {
			List<?> i = new ArrayList<String>();
			return i;
		}

		public Object post() {
			var i = new ArrayList<String>();
			return i;
		}
	}

	// If the variable type is replaced by var, it takes the initial type, which may not be compatible with further
	// allocation
	@UnmodifiedMethod
	public static class CaseDifferentType_reAssigment {
		public Object pre() {
			List<?> i = new ArrayList<>();

			i = ImmutableList.of();

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
	// @UnchangedMethod
	@CompareMethods
	public static class CaseAnonymous {
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
	public static class CaseCouic {
		public Optional<Object> pre() {
			Optional<Map.Entry<String, String>> optPathAndContent = Optional.empty();

			if (optPathAndContent.isEmpty()) {
				return Optional.empty();
			}

			Map.Entry<String, String> pathAndContent = optPathAndContent.get();
			return Optional.of(pathAndContent);
		}
	}
}
