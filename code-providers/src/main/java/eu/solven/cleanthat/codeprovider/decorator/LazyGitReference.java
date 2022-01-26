package eu.solven.cleanthat.codeprovider.decorator;

import java.util.function.Supplier;

/**
 * Default implementation for {@link ILazyGitReference}
 * 
 * @author Benoit Lacelle
 *
 */
public class LazyGitReference implements ILazyGitReference {

	final String gitRef;
	final Supplier<IGitReference> supplier;

	public LazyGitReference(String gitRef, Supplier<IGitReference> supplier) {
		this.gitRef = gitRef;
		this.supplier = supplier;
	}

	@Override
	public String getFullRefOrSha1() {
		return gitRef;
	}

	@Override
	public Supplier<IGitReference> getSupplier() {
		return () -> {
			IGitReference reference = supplier.get();

			String initialRef = getFullRefOrSha1();
			String lazyRef = reference.getFullRefOrSha1();
			if (!initialRef.equals(lazyRef)) {
				throw new IllegalStateException("Inconsistency: " + initialRef + " vs " + lazyRef);
			}

			return reference;
		};
	}

}
