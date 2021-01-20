package eu.solven.cleanthat.javaparser;

import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;

/**
 * Try to keep modified files in memory for JavaParser context
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubContentContext {
	final MemoryTypeSolver memoryTypeSolver;

	public GithubContentContext() {
		this(new MemoryTypeSolver());
	}

	public GithubContentContext(MemoryTypeSolver memoryTypeSolver) {
		this.memoryTypeSolver = memoryTypeSolver;
	}

	public void registerContent() {
		memoryTypeSolver.addDeclaration(null, new JavaParserClassDeclaration(null, memoryTypeSolver));
	}
}
