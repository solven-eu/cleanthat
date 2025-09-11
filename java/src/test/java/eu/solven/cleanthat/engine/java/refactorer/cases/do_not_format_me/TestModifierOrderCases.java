package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import org.junit.jupiter.api.Disabled;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ModifierOrder;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestModifierOrderCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
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

	// The weird indentation in `post` relates with 'https://github.com/javaparser/javaparser/issues/3935'
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

	// https://github.com/javaparser/javaparser/issues/3935
	@CompareCompilationUnitsAsStrings(
			pre = "package org.eclipse.mat.snapshot.model;\n" + "\n"
					+ "import java.io.Serializable;\n"
					+ "\n"
					+ "import org.eclipse.mat.internal.Messages;\n"
					+ "\n"
					+ "abstract public class GCRootInfo implements Serializable {\n"
					+ "	private static final long serialVersionUID = 2L;\n"
					+ "\n"
					+ "}\n"
					+ "",
			post = "package org.eclipse.mat.snapshot.model;\n" + "\n"
					+ "import java.io.Serializable;\n"
					+ "\n"
					+ "import org.eclipse.mat.internal.Messages;\n"
					+ "\n"
					+ "public abstract class GCRootInfo implements Serializable {\n"
					+ "	private static final long serialVersionUID = 2L;\n"
					+ "\n"
					+ "}\n")
	public static class Issue_MissingWhitespaceAfterLastModifier {
	}

	// https://github.com/solven-eu/cleanthat/issues/713
	@UnmodifiedCompilationUnitAsString(
			pre = "public sealed interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}")
	public static class Issue_SealedClass_rightOrder {
	}

	// https://github.com/javaparser/javaparser/issues/4245
	@UnmodifiedCompilationUnitAsString(
			pre = "sealed public interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}",
			post = "public sealed interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}")
	public static class Issue_SealedClass_wrongOrder {
	}

	// https://github.com/solven-eu/cleanthat/issues/897
	@CompareCompilationUnitsAsStrings(
			pre = "public class A { private transient final @Deprecated String ANNOTATED; }",
			post = "public class A { @Deprecated private final transient String ANNOTATED; }")
	public static class Issue_MissingWhiteSpaceBetweenAnnotationAndFirstModifier {
	}
}
