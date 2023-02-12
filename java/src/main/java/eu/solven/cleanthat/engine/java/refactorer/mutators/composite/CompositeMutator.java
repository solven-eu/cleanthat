package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.github.javaparser.ast.Node;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator make it easy to composite multiple {@link IMutator}s in a single one.
 * 
 * @author Benoit Lacelle
 *
 */
public class CompositeMutator implements IMutator {

	final List<IMutator> mutators;

	public CompositeMutator(List<IMutator> mutators) {
		this.mutators = ImmutableList.copyOf(mutators);
	}

	public List<IMutator> getUnderlyings() {
		return mutators;
	}

	@Override
	public Set<String> getIds() {
		return mutators.stream()
				.flatMap(ct -> ct.getIds().stream())
				.sorted()
				.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public boolean walkNode(Node pre) {
		boolean modified = false;

		for (IMutator mutator : mutators) {
			modified |= mutator.walkNode(pre);
		}

		return modified;
	}

	public static List<IMutator> filterWithJdk(JavaVersion sourceJdkVersion, List<IMutator> mutators) {
		return mutators.stream()
				.filter(m -> sourceJdkVersion.isAtLeast(m.minimalJavaVersion()))
				.collect(Collectors.toList());
	}

}
