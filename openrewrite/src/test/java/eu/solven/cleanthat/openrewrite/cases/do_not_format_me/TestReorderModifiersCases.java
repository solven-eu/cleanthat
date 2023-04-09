package eu.solven.cleanthat.openrewrite.cases.do_not_format_me;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Disabled;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.cases.AParameterizesRefactorerCases;

@Disabled
public class TestReorderModifiersCases extends AParameterizesRefactorerCases<J.CompilationUnit, Result> {
	final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());

	@Override
	public OpenrewriteMutator getTransformer() {
		Environment environment = Environment.builder().scanRuntimeClasspath().build();
		Recipe recipe = environment.activateRecipes("org.openrewrite.java.cleanup.ModifierOrder");

		return new OpenrewriteMutator(recipe);
	}

	@Override
	public J.CompilationUnit convertToAst(Node pre) {
		var asString = pre.toString();

		return AAstRefactorer.parse(refactorer, asString)
				.orElseThrow(() -> new IllegalArgumentException("Invalid input"));
	}

	@Override
	public String resultToString(Result post) {
		return post.getAfter().printAll();
	}

	@Override
	public String astToString(CompilationUnit asAst) {
		return asAst.toString();
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

		public Object post(Collection<?> input) {
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
}
