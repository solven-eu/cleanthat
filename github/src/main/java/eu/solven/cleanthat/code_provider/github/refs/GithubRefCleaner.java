/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHCompare.Commit;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubBranchCodeProvider;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubRefCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
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
import eu.solven.cleanthat.config.CleanthatConfigInitializer;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.RepoInitializerResult;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.IGitRefsConstants;
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
			List<IEngineLintFixerFactory> factories,
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
		List<String> protectedPatterns = properties.getMeta().getRefs().getProtectedPatterns();

		String headRef = head.getRef();
		if (canCleanInPlace(eventBaseRefs, protectedPatterns, headRef)) {
			logWhyCanCleanInPlace(eventBaseRefs, protectedPatterns, result, headRef);

			// TODO We should take as base the base from 'canCleanInPlace'
			// This is especially important in pushes after a rr-open, as the push before the rr-open would not be
			// cleaned
			// It would also help workaround previous clean having failed (e.g. by cleaning the RR on each event, not
			// just the commits of the latest push)
			return cleanHeadInPlace(result, head);
		}

		if (canCleanInNewRR(protectedPatterns, headRef)) {
			return cleanInNewRR(result, head, protectedPatterns, headRef);
		} else {
			// Cleanable neither in-place nor in-rr
			LOGGER.info("This branch seems not cleanable: {}. Regex: {}. eventBaseBranches: {}",
					headRef,
					protectedPatterns,
					eventBaseRefs);
			return Optional.empty();
		}
	}

	protected Optional<HeadAndOptionalBase> cleanInNewRR(IExternalWebhookRelevancyResult result,
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
		GitRepoBranchSha1 actualBase = new GitRepoBranchSha1(base.getRepoFullName(), head.getRef(), base.getSha());

		GitRepoBranchSha1 actualHead = new GitRepoBranchSha1(head.getRepoFullName(), newBranchRef, head.getSha());
		return Optional.of(new HeadAndOptionalBase(actualHead, Optional.of(actualBase)));
	}

	private boolean canCleanInNewRR(List<String> cleanableRefsRegexes, String headRef) {
		Optional<String> optHeadMatchingRule = selectPatternOfSensibleHead(cleanableRefsRegexes, headRef);

		return optHeadMatchingRule.isPresent();
	}

	private boolean canCleanInPlace(Set<String> eventBaseRefs, List<String> protectedPatterns, String headRef) {
		Optional<String> optHeadMatchingRule = selectPatternOfSensibleHead(protectedPatterns, headRef);
		if (optHeadMatchingRule.isPresent()) {
			// We never clean in place the cleanable branches, as they are considered sensible
			LOGGER.info("Not cleaning in-place as head={} is a sensible/cleanable ref (rule={})",
					headRef,
					optHeadMatchingRule.get());
			return false;
		}

		Optional<String> optBaseMatchingRule = selectValidBaseBranch(eventBaseRefs, protectedPatterns);

		return optBaseMatchingRule.isPresent();
	}

	// https://github.com/pmd/pmd/issues?q=is%3Aissue+is%3Aopen+InvalidLogMessageFormat
	@SuppressWarnings("PMD.InvalidLogMessageFormat")
	private void logWhyCanCleanInPlace(Set<String> eventBaseRefs,
			List<String> refToCleanRegexes,
			IExternalWebhookRelevancyResult result,
			String headRef) {
		Optional<String> optBaseMatchingRule = selectValidBaseBranch(eventBaseRefs, refToCleanRegexes);

		if (optBaseMatchingRule.isEmpty()) {
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

	private Optional<String> selectPatternOfSensibleHead(List<String> protectedPatterns, String fullRef) {
		return protectedPatterns.stream().filter(regex -> Pattern.matches(regex, fullRef)).findAny();
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
			GitRepoBranchSha1 head) {
		// The base is cleanable: we are allowed to clean its head in-place
		Optional<GitRepoBranchSha1> optBase = result.optBaseRef();
		if (optBase.isPresent() && IGitRefsConstants.SHA1_CLEANTHAT_UP_TO_REF_ROOT.equals(optBase.get().getSha())) {
			// Typically a refs has been created, or forced-push
			// Its base would be the ancestor commit which is in the default branch
			GitRepoBranchSha1 ambiguousBase = optBase.get();
			try {
				GHRepository repo = githubAndToken.getGithub().getRepository(ambiguousBase.getRepoFullName());
				GHBranch defaultBranch = GithubHelper.getDefaultBranch(repo);

				// https://docs.github.com/en/rest/commits/commits#compare-two-commits
				GHCompare compare = repo.getCompare(defaultBranch.getSHA1(), head.getSha());
				Commit mergeBase = compare.getMergeBaseCommit();

				GitRepoBranchSha1 newBase =
						new GitRepoBranchSha1(ambiguousBase.getRef(), ambiguousBase.getRef(), mergeBase.getSHA1());
				LOGGER.info("We will use as base: {}", newBase);
				optBase = Optional.of(newBase);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return Optional.of(new HeadAndOptionalBase(head, optBase));
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
			String repoName = theRef.getRepoFullName();
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
		// Typically used to load the configuration
		// ICodeProvider headCodeProvider = new GithubCommitToCommitDiffCodeProvider(token, theRepo, ghBase, head);
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
				LOGGER.info("There is already a ref preparing cleanthat integration. Do not open a new PR (url={})",
						refToPr.getUrl().toExternalForm());
				repo.listPullRequests(GHIssueState.ALL).forEach(pr -> {
					if (headRef.equals(pr.getHead().getRef())) {
						LOGGER.info("Related PR: {}", pr.getHtmlUrl());
					}
				});
				return false;
			} else {
				RepoInitializerResult result = new CleanthatConfigInitializer(codeProvider,
						ConfigHelpers.getYaml(getObjectMappers()),
						getFactories()).prepareFile();

				GHCommit commit = commitConfig(defaultBranch, repo, result);
				GHRef refToPr = repo.createRef(headRef, commit.getSHA1());
				boolean force = false;
				refToPr.updateTo(commit.getSHA1(), force);

				// Issue using '/' in the base, while renovate succeed naming branches: 'renovate/configure'
				// TODO What is this issue exactly? We seem to success naming our ref 'cleanthat/configure'
				GHPullRequest pr = repo.createPullRequest("Configure CleanThat",
						refToPr.getRef(),
						baseRef,
						result.getPrBody(),
						true,
						false);
				LOGGER.info("Open PR: {}", pr.getHtmlUrl());
				return true;
			}
		} catch (IOException e) {
			// TODO If 401, it probably means the Installation is not allowed to modify given repo
			throw new UncheckedIOException(e);
		}
	}

	private GHCommit commitConfig(GHBranch defaultBranch, GHRepository repo, RepoInitializerResult result)
			throws IOException {
		GHTreeBuilder baseTreeBuilder = repo.createTree().baseTree(defaultBranch.getSHA1());

		result.getPathToContents().forEach((path, content) -> baseTreeBuilder.add(path, content, false));

		GHTree createTree = baseTreeBuilder.create();
		GHCommit commit = GithubRefWriterLogic.prepareCommit(repo)
				.message(result.getCommitMessage())
				.parent(defaultBranch.getSHA1())
				.tree(createTree.getSha())
				.create();
		return commit;
	}

}
