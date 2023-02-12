package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.base.Suppliers;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator will apply all {@link IMutator} fixing a CheckStyle rules.
 * 
 * @author Benoit Lacelle
 *
 */
public class CheckStyleMutators extends CompositeMutator {

	static final Supplier<List<IMutator>> CHECKSTYLE = Suppliers.memoize(() -> AllMutators.ALL.get()
			.stream()
			.filter(m -> m.getCheckstyleId().isPresent())
			.collect(Collectors.toList()));

	public CheckStyleMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, CHECKSTYLE.get()));
	}

}
