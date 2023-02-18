package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;

/**
 * A custom {@link CompositeMutator} holding both a draft and production-ready {@link IMutator}
 * 
 * @author Benoit Lacelle
 *
 */
public class CustomCompositeMutator extends CompositeMutator {

	public CustomCompositeMutator(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, defaultUnderlyings()));
	}

	public CustomCompositeMutator(List<IMutator> mutators) {
		super(mutators);
	}

	private static List<IMutator> defaultUnderlyings() {
		return Arrays.asList(new CustomMutator(), new CustomDraftMutator());
	}

	public static CustomCompositeMutator customMutators() {
		return new CustomCompositeMutator(defaultUnderlyings());
	}

}
