package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Arrays;
import java.util.Optional;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;

import com.google.common.collect.Iterables;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkableMutator;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
public class OpenrewriteMutator implements IWalkableMutator<J.CompilationUnit, Result> {
	final Recipe recipe;

	public OpenrewriteMutator(Recipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public Optional<Result> walkNode2(CompilationUnit pre) {
		ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

		Result result = Iterables.getOnlyElement(recipe.run(Arrays.asList(pre), ctx).getResults());
		return Optional.of(result);
	}

}
