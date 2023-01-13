package eu.solven.cleanthat.language.java.refactorer.test;

import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;

public abstract class ARefactorerCases {
	public String getId() {
		return getTransformer().getClass().getName();
	}

	public abstract IClassTransformer getTransformer();

}
