package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.function.Supplier;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.MutatorsScanner;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator will apply all {@link IMutator}s,even those considered not production-ready
 * 
 * @author Benoit Lacelle
 *
 */
public class AllEvenNotProductionReadyMutators extends CompositeMutator {

	static final Supplier<List<IMutator>> ALL_EVENNOTREADY =
			Suppliers.memoize(() -> ImmutableList.copyOf(new MutatorsScanner().getMutators()));

	public AllEvenNotProductionReadyMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, ALL_EVENNOTREADY.get()));
	}

}
