package eu.solven.cleanthat.jgit;

import java.util.Optional;

/**
 * Helpers for Git
 *
 * @author Benoit Lacelle
 */
public class GitHelper {

	protected GitHelper() {
		// hidden
	}

	public static String getDefaultBranch(Optional<String> optDefault) {
		// If there is no explicit default, we suppose 'master' is the default branch
		return optDefault.orElse("master");
	}
}
