package eu.solven.cleanthat.engine.java.refactorer;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This is used to test the inclusion of a custom {@link IMutator} (e.g. for a third-party jar)
 * 
 * @author Benoit Lacelle
 *
 */
public class CustomMutator implements IMutator {

	@Override
	public boolean walkNode(Node pre) {
		return false;
	}

}
