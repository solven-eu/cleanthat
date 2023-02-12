package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ModifierOrder;
import eu.solven.cleanthat.engine.java.refactorer.mutators.PrimitiveBoxedForString;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamAnyMatch;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIsEmptyOnCollections;

/**
 * This mutator will apply all {@link IMutator} considered safe (e.g. by not impacting the {@link Runtime}, or only with
 * ultra-safe changes).
 * 
 * @author Benoit Lacelle
 *
 */
public class SafeAndConsensualMutators extends CompositeMutator {

	public static final List<IMutator> SAFE_AND_CONSENSUAL = ImmutableList.<IMutator>builder()
			.add(new ModifierOrder(),
					new UseIndexOfChar(),
					new UseIsEmptyOnCollections(),
					new StreamAnyMatch(),
					new PrimitiveBoxedForString())
			.build();

	public SafeAndConsensualMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, SAFE_AND_CONSENSUAL));
	}

}
