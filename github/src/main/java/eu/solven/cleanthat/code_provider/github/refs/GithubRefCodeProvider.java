package eu.solven.cleanthat.code_provider.github.refs;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.jgit.JGitCodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefCodeProvider extends AGithubSha1CodeProvider {
	final GHRef ref;

	final AtomicReference<JGitCodeProvider> localClone = new AtomicReference<>();

	public GithubRefCodeProvider(String token, GHRepository repo, GHRef ref) {
		super(token, repo);

		Objects.requireNonNull(ref, "ref is null");

		this.ref = ref;
	}

	@Override
	public String getSha1() {
		return ref.getObject().getSha();
	}

	@Override
	public String getRef() {
		return ref.getRef();
	}

	@Override
	public String getHtmlUrl() {
		return ref.getUrl().toExternalForm();
	}

	@Override
	public String getTitle() {
		return getRef();
	}
}
