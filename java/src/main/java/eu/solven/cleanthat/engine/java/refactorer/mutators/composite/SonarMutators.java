package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.base.Suppliers;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator will apply all {@link IMutator} fixing a Sonar rule.
 * 
 * @author Benoit Lacelle
 *
 */
public class SonarMutators extends CompositeMutator {

	static final Supplier<List<IMutator>> SONAR = Suppliers.memoize(
			() -> AllMutators.ALL.get().stream().filter(m -> m.getSonarId().isPresent()).collect(Collectors.toList()));

	public SonarMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, SONAR.get()));
	}

}
