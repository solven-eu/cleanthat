package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.base.Suppliers;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator will apply all {@link IMutator}s
 * 
 * @author Benoit Lacelle
 *
 */
public class AllMutators extends CompositeMutator {

	static final Supplier<List<IMutator>> ALL =
			Suppliers.memoize(() -> AllEvenNotProductionReadyMutators.ALL_EVENNOTREADY.get()
					.stream()
					// Sort by className, to always apply mutators in the same order
					.sorted(Comparator.comparing(m -> m.getClass().getName()))
					.filter(m -> m.isProductionReady())
					.collect(Collectors.toList()));

	public AllMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, ALL.get()));
	}

}
