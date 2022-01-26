package eu.solven.cleanthat.code_provider.github.refs.all_files;

import java.util.Objects;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefCodeProvider extends AGithubSha1CodeProviderWriter {
	final GHRef ref;

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
	protected GHRef getAsGHRef() {
		return ref;
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
