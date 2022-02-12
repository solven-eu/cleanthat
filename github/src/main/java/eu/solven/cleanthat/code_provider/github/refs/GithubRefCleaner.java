package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubBranchCodeProvider;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubRefCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.decorator.IGitBranch;
import eu.solven.cleanthat.codeprovider.decorator.IGitCommit;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.IGitRepository;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;
import eu.solven.cleanthat.codeprovider.git.IExternalWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.CleanthatRefFilterProperties;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * Default for {@link IGitRefCleaner}
 *
 * @author Benoit Lacelle
 */
public class GithubRefCleaner extends ACodeCleaner implements IGitRefCleaner {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefCleaner.class);

	private static final String REF_DOMAIN_CLEANTHAT = "cleanthat";
	public static final String REF_DOMAIN_CLEANTHAT_WITH_TRAILING_SLASH = REF_DOMAIN_CLEANTHAT + "/";

	public static final String PREFIX_REF_CLEANTHAT =
			CleanthatRefFilterProperties.BRANCHES_PREFIX + REF_DOMAIN_CLEANTHAT_WITH_TRAILING_SLASH;
	public static final String REF_NAME_CONFIGURE = PREFIX_REF_CLEANTHAT + "configure";

	public static final String PREFIX_REF_CLEANTHAT_TMPHEAD = PREFIX_REF_CLEANTHAT + "headfor-";
	public static final String PREFIX_REF_CLEANTHAT_MANUAL = PREFIX_REF_CLEANTHAT + "manual-";

	final GithubAndToken githubAndToken;

	public GithubRefCleaner(List<ObjectMapper> objectMappers,
			List<ILanguageLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider,
			GithubAndToken githubAndToken) {
		super(objectMappers, factories, formatterProvider);
		this.githubAndToken = githubAndToken;
	}

	// We may have no ref to clean (e.g. there is no cleanthat configuration, or the ref is excluded)
	// We may have to clean current ref (e.g. a PR is open, and we want to clean the PR head)
	// We may have to clean a different ref (e.g. a push to the main branch needs to be cleaned through a PR)
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public Optional<HeadAndOptionalBase> prepareRefToClean(IExternalWebhookRelevancyResult result,
			GitRepoBranchSha1 head,
			// There can be multiple eventBaseBranches in case of push events
			Set<String> eventBaseRefs) {
		ICodeProvider codeProvider = getCodeProviderForRef(head);
		ResultOrError<CleanthatRepositoryProperties, String> optConfig = loadAndCheckConfiguration(codeProvider);

		if (optConfig.getOptError().isPresent()) {
			return Optional.empty();
		}

		CleanthatRepositoryProperties properties = optConfig.getOptResult().get();

		// TODO If the configuration changed, trigger full-clean only if the change is an effective change (and not just
		// json/yaml/etc formatting)
		migrateConfigurationCode(properties);
		List<String> cleanableRefsRegexes = properties.getMeta().getRefs().getBranches();

		String headRef = head.getRef();
		if (canCleanInPlace(eventBaseRefs, cleanableRefsRegexes, headRef)) {
			logWhyCanCleanInPlace(eventBaseRefs, cleanableRefsRegexes, result, headRef);

			return cleanHeadInPlace(result, head);
		}

		if (canCleanInRR(cleanableRefsRegexes, headRef)) {
			return cleanInRR(result, head, cleanableRefsRegexes, headRef);
		} else {
			// Cleanable neither in-place nor in-rr
			LOGGER.info("This branch seems not cleanable: {}. Regex: {}. eventBaseBranches: {}",
					headRef,
					cleanableRefsRegexes,
					eventBaseRefs);
			return Optional.empty();
		}
	}

	protected Optional<HeadAndOptionalBase> cleanInRR(IExternalWebhookRelevancyResult result,
			GitRepoBranchSha1 head,
			List<String> cleanableRefsRegexes,
			String headRef) {
		// We'll have to open a dedicated ReviewRequest if necessary
		// As we prefer not to pollute a random existing PR
		String newBranchRef = prepareRefNameForHead(headRef);
		// We may open a branch later if it appears this branch is relevant
		// BEWARE we do not open the branch right now: we wait to detect at least one fail is relevant to be clean
		// In case of concurrent events, we may end opening multiple PR to clean the same branch
		LOGGER.info(
				"ref={} is not cleanable in-place, but cleanable in-rr. Commits would be pushed into {} (cleanableRegex={})",
				headRef,
				newBranchRef,
				cleanableRefsRegexes);

		// See GithubEventHelper.doOpenPr(WebhookRelevancyResult, GithubRepositoryFacade, GitRepoBranchSha1)
		Optional<GitRepoBranchSha1> optBaseRef = result.optBaseRef();
		if (optBaseRef.isEmpty()) {
			// This may happen on the event of branch creation, when the branch is cleanable
			throw new IllegalStateException("No baseRef? headRef=" + headRef);
		}

		GitRepoBranchSha1 base = optBaseRef.get();
		// We keep the base sha1, as it will be used for diff computations (i.e. listing the concerned files)
		// We use as refName the pushedRef/rrHead as it is to this ref that a RR has to be open
		GitRepoBranchSha1 actualBase = new GitRepoBranchSha1(base.getRepoName(), head.getRef(), base.getSha());

		GitRepoBranchSha1 actualHead = new GitRepoBranchSha1(head.getRepoName(), newBranchRef, head.getSha());
		return Optional.of(new HeadAndOptionalBase(actualHead, Optional.of(actualBase)));
	}

	private boolean canCleanInRR(List<String> cleanableRefsRegexes, String headRef) {
		Optional<String> optHeadMatchingRule = selectPatternOfSensibleHead(cleanableRefsRegexes, headRef);

		return optHeadMatchingRule.isPresent();
	}

	private boolean canCleanInPlace(Set<String> eventBaseRefs, List<String> refToCleanRegexes, String headRef) {
		Optional<String> optHeadMatchingRule = selectPatternOfSensibleHead(refToCleanRegexes, headRef);
		if (optHeadMatchingRule.isPresent()) {
			// We never clean in place the cleanable branches, as they are considered sensible
			LOGGER.info("Not cleaning {} in-place as it is a sensible/cleanable ref (rule={})",
					headRef,
					optHeadMatchingRule.get());
			return false;
		}

		Optional<String> optBaseMatchingRule = selectValidBaseBranch(eventBaseRefs, refToCleanRegexes);

		return optBaseMatchingRule.isPresent();
	}

	// https://github.com/pmd/pmd/issues?q=is%3Aissue+is%3Aopen+InvalidLogMessageFormat
	@SuppressWarnings("PMD.InvalidLogMessageFormat")
	private void logWhyCanCleanInPlace(Set<String> eventBaseRefs,
			List<String> refToCleanRegexes,
			IExternalWebhookRelevancyResult result,
			String headRef) {
		Optional<String> optBaseMatchingRule = selectValidBaseBranch(eventBaseRefs, refToCleanRegexes);

		if (!optBaseMatchingRule.isPresent()) {
			throw new IllegalStateException("Should be called only if .canCleanInPlace() returns true");
		}

		// TODO We should ensure the HEAD does not match any regex (i.e. not clean in-place a sensible branch, even
		// if it is the head of a RR)
		String baseMatchingRule = optBaseMatchingRule.get();

		String prefix = "Cleaning {} in-place as ";
		String suffix = " a sensible/cleanable base (rule={})";
		if (result.isReviewRequestOpen()) {
			LOGGER.info(prefix + "RR has" + suffix, headRef, baseMatchingRule);
		} else {
			LOGGER.info(prefix + "pushed used as head of a RR with" + suffix, headRef, baseMatchingRule);
		}
	}

	private Optional<String> selectPatternOfSensibleHead(List<String> cleanableRefsRegexes, String fullRef) {
		return cleanableRefsRegexes.stream().filter(regex -> Pattern.matches(regex, fullRef)).findAny();
	}

	/**
	 * 
	 * @param refs
	 *            eligible full refs
	 * @param regexes
	 *            the regex of the branches allowed to be clean. Fact is these branches should never be cleaned by
	 *            themselves, but only through RR
	 * @return
	 */
	private Optional<String> selectValidBaseBranch(Set<String> refs, List<String> regexes) {
		Optional<String> optBaseMatchingRule;
		if (refs.isEmpty()) {
			optBaseMatchingRule = Optional.empty();
		} else {
			optBaseMatchingRule = regexes.stream().filter(regex -> {
				Optional<String> matchingBase = refs.stream().filter(base -> {
					return Pattern.matches(regex, base);
				}).findAny();

				if (matchingBase.isEmpty()) {
					LOGGER.info("Not a single base with open RR matches cleanableBranchRegex={}", regex);
					return false;
				} else {
					LOGGER.info("We have a match for ruleBranch={} eventBaseBranch={}", regex, matchingBase.get());
				}

				return true;
			}).findAny();
		}
		return optBaseMatchingRule;
	}

	private Optional<HeadAndOptionalBase> cleanHeadInPlace(IExternalWebhookRelevancyResult result,
			GitRepoBranchSha1 theRef) {
		// The base is cleanable: we are allowed to clean its head in-place

		GitRepoBranchSha1 head = new GitRepoBranchSha1(theRef.getRepoName(), theRef.getRef(), theRef.getSha());
		return Optional.of(new HeadAndOptionalBase(head, result.optBaseRef()));
	}

	/**
	 * @return a new/unique reference, useful when opening a branch to clean a cleanable branch.
	 */
	public String prepareRefNameForHead(String baseToClean) {
		UUID random = UUID.randomUUID();
		String ref = PREFIX_REF_CLEANTHAT_TMPHEAD + baseToClean.replace('/', '_').replace('-', '_') + "-" + random;
		LOGGER.info("We generated a temporary ref: {}", ref);
		return ref;
	}

	public ICodeProvider getCodeProviderForRef(GitRepoBranchSha1 theRef) {
		String ref = theRef.getRef();

		try {
			String repoName = theRef.getRepoName();
			GithubFacade facade = new GithubFacade(githubAndToken.getGithub(), repoName);
			GHRef refObject = facade.getRef(ref);
			return new GithubRefCodeProvider(githubAndToken.getToken(), facade.getRepository(), refObject);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue with ref: " + ref, e);
		}
	}

	private CodeFormatResult formatRefDiff(GHRepository theRepo,
			ICodeProvider codeProvider,
			ILazyGitReference headSupplier) {
		ICodeProviderWriter codeProviderWriter = new CodeProviderDecoratingWriter(codeProvider, () -> {
			// Get the head lazily, else it means we create branch which may remain empty
			GHRef headWhereToWrite = headSupplier.getSupplier().get().getDecorated();
			return new GithubRefCodeProvider(githubAndToken.getToken(), theRepo, headWhereToWrite);
		});
		return formatCodeGivenConfig(codeProviderWriter, false);
	}

	@Deprecated(
			since = "We clean on push events. This would be used on open PR events, but we may still fallback on sha1 diff cleaning")
	@Override
	public CodeFormatResult formatRefDiff(IGitRepository repo, IGitReference base, ILazyGitReference headSupplier) {
		String refOrSha1 = headSupplier.getFullRefOrSha1();
		GHRef ghBase = base.getDecorated();

		LOGGER.info("Base: {} Head: {}", ghBase.getRef(), refOrSha1);
		GHRepository theRepo = repo.getDecorated();
		String token = githubAndToken.getToken();
		GHCommit head = new GithubRepositoryFacade(theRepo).getCommit(refOrSha1);
		ICodeProvider codeProvider = new GithubRefToCommitDiffCodeProvider(token, theRepo, ghBase, head);
		return formatRefDiff(theRepo, codeProvider, headSupplier);
	}

	@Override
	public CodeFormatResult formatCommitToRefDiff(IGitRepository repo,
			IGitCommit base,
			ILazyGitReference headSupplier) {
		String refOrSha1 = headSupplier.getFullRefOrSha1();
		GHCommit ghBase = base.getDecorated();

		LOGGER.info("Base: {} Head: {}", ghBase.getSHA1(), refOrSha1);
		GHRepository theRepo = repo.getDecorated();
		String token = githubAndToken.getToken();
		GHCommit head = new GithubRepositoryFacade(theRepo).getCommit(refOrSha1);
		ICodeProvider codeProvider = new GithubCommitToCommitDiffCodeProvider(token, theRepo, ghBase, head);
		return formatRefDiff(theRepo, codeProvider, headSupplier);
	}

	@Override
	public CodeFormatResult formatRef(IGitRepository repo, IGitBranch branchSupplier, ILazyGitReference headSupplier) {
		GHBranch branch = branchSupplier.getDecorated();

		ICodeProviderWriter codeProvider =
				new GithubBranchCodeProvider(githubAndToken.getToken(), repo.getDecorated(), branch);
		return formatRefDiff(repo.getDecorated(), codeProvider, headSupplier);
	}

	@Override
	public boolean tryOpenPRWithCleanThatStandardConfiguration(IGitBranch branch) {
		GHBranch defaultBranch = branch.getDecorated();
		GHRepository repo = defaultBranch.getOwner();

		String branchName = defaultBranch.getName();
		String baseRef = CleanthatRefFilterProperties.BRANCHES_PREFIX + branchName;
		ICodeProvider codeProvider =
				getCodeProviderForRef(new GitRepoBranchSha1(repo.getFullName(), baseRef, defaultBranch.getSHA1()));
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);
		if (optPrConfig.isPresent()) {
			LOGGER.info("There is a configuration (valid or not) in the default branch ({})", branchName);
			return false;
		} else {
			LOGGER.info("There is no configuration in the default branch ({})", branchName);
		}

		String headRef = REF_NAME_CONFIGURE;
		// String fullRefName = GithubFacade.toFullGitRef(refName);
		Optional<GHRef> optRefToPR;
		try {
			try {
				optRefToPR = Optional.of(new GithubRepositoryFacade(repo).getRef(headRef));
				LOGGER.info("There is already a ref: " + headRef);
			} catch (GHFileNotFoundException e) {
				LOGGER.trace("There is not yet a ref: " + headRef, e);
				LOGGER.info("There is not yet a ref: " + headRef);
				optRefToPR = Optional.empty();
			}
		} catch (IOException e) {
			// TODO If 401, it probably means the Installation is not allowed to see/modify given repository
			throw new UncheckedIOException(e);
		}
		try {
			if (optRefToPR.isPresent()) {
				GHRef refToPr = optRefToPR.get();
				LOGGER.info(
						"There is already a ref about to introduce a cleanthat default configuration. Do not open a new PR (url={})",
						refToPr.getUrl().toExternalForm());
				repo.listPullRequests(GHIssueState.ALL).forEach(pr -> {
					if (headRef.equals(pr.getHead().getRef())) {
						LOGGER.info("Related PR: {}", pr.getHtmlUrl());
					}
				});
				return false;
			} else {
				GHCommit commit = commitConfig(defaultBranch, repo);
				GHRef refToPr = repo.createRef(headRef, commit.getSHA1());
				boolean force = false;
				refToPr.updateTo(commit.getSHA1(), force);
				// Let's follow Renovate and its configuration PR
				// https://github.com/solven-eu/agilea/pull/1
				String body = readResource("/templates/onboarding-body.md");
				body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());
				// Issue using '/' in the base, while renovate succeed naming branches: 'renovate/configure'
				// TODO What is this issue exactly? We seem to success naming our ref 'cleanthat/configure'
				GHPullRequest pr =
						repo.createPullRequest("Configure CleanThat", refToPr.getRef(), baseRef, body, true, false);
				LOGGER.info("Open PR: {}", pr.getHtmlUrl());
				return true;
			}
		} catch (IOException e) {
			// TODO If 401, it probably means the Installation is not allowed to modify given repo
			throw new UncheckedIOException(e);
		}
	}

	private GHCommit commitConfig(GHBranch defaultBranch, GHRepository repo) throws IOException {
		GithubBranchCodeProvider codeProvider =
				new GithubBranchCodeProvider(githubAndToken.getToken(), repo, defaultBranch);
		CleanthatRepositoryProperties defaultConfig = generateDefaultConfig(codeProvider);

		GHTree createTree = repo.createTree()
				.baseTree(defaultBranch.getSHA1())
				.add(CodeProviderHelpers.FILENAME_CLEANTHAT_YAML, toYaml(defaultConfig), false)
				.create();
		GHCommit commit = GithubRefWriterLogic.prepareCommit(repo)
				.message(readResource("/templates/commit-message.txt"))
				.parent(defaultBranch.getSHA1())
				.tree(createTree.getSha())
				.create();
		return commit;
	}

	private String readResource(String path) {
		String body;
		try (InputStreamReader reader =
				new InputStreamReader(new ClassPathResource(path).getInputStream(), Charsets.UTF_8)) {
			body = CharStreams.toString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return body;
	}

}
