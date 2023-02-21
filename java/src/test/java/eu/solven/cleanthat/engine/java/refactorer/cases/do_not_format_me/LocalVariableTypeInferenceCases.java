package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class LocalVariableTypeInferenceCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
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

	// https://github.com/javaparser/javaparser/issues/3898
	@UnchangedMethod
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
	@UnchangedMethod
	public static class CaseDifferentType_reAssigment {
		public Object pre() {
			List<?> i = new ArrayList<>();

			i = ImmutableList.of();

			return i;
		}
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

	@UnchangedMethod
	public static class NotLocal {
		public Object pre(int i) {
			return i;
		}
	}

	// 'var' is not allowed by JDK in Compound declarations
	@UnchangedMethod
	public static class CompoundDeclarations {
		public Object pre() {
			int i = 1, j = 2;
			return i + j;
		}
	}

	@UnchangedMethod
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
}
