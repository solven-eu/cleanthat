/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.MoreExecutors;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.codeprovider.CodeProviderDecoratingWriter;
import eu.solven.cleanthat.codeprovider.CodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.IUpgradableToHeadFullScan;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.EngineAndLinters;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.engine.IEngineFormatterFactory;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.pepper.thread.PepperExecutorsHelper;

/**
 * Unclear what is the point of this class
 *
 * @author Benoit Lacelle
 */
public class CodeProviderFormatter implements ICodeProviderFormatter {
	private static final String KEY_NB_FILES_FORMATTED = "nb_files_formatted";

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CodeProviderFormatter.class);

	public static final String EOL = "\r\n";
	private static final int MAX_LOG_MANY_FILES = 128;

	final IEngineFormatterFactory formatterFactory;
	final ICodeFormatterApplier formatterApplier;

	final SourceCodeFormatterHelper sourceCodeFormatterHelper;

	final ConfigHelpers configHelpers;

	public CodeProviderFormatter(ConfigHelpers configHelpers,
			IEngineFormatterFactory formatterFactory,
			ICodeFormatterApplier formatterApplier) {
		this.configHelpers = configHelpers;
		this.formatterFactory = formatterFactory;
		this.formatterApplier = formatterApplier;

		this.sourceCodeFormatterHelper = new SourceCodeFormatterHelper();
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public CodeFormatResult formatCode(CleanthatRepositoryProperties repoProperties,
			ICodeProviderWriter codeWriter,
			boolean dryRun) {
		// A config change may be spotless.yaml, or a processor configuration file

		// TODO or an indirect change leading to a full re-compute (e.g. a implicit
		// version upgrade led to a change of some engine, which should trigger a full re-compute)
		var configIsChanged = new AtomicBoolean();

		List<String> prComments = new ArrayList<>();

		ICodeProviderWriter finalCodeWriter;
		if (ACodeCleaner.isLimittedSetOfFiles(codeWriter)) {
			// TODO Check if number of files is compatible with RateLimit
			try {
				codeWriter.listFilesForFilenames(fileChanged -> {
					var path = fileChanged.getPath();
					if (path.startsWith(ICleanthatConfigConstants.FILENAME_CLEANTHAT_FOLDER)) {
						// We hit on any change in the '.cleanthat' directory
						// Then we catch changes in spotless (or any other engine)
						configIsChanged.set(true);
						prComments.add("Spotless configuration has changed");

						// BEWARE this may be due to merge-commits
						// see https://github.com/orgs/community/discussions/45166
						// https://docs.github.com/en/rest/commits/commits#compare-two-commits
						LOGGER.info("Configuration change over path=`{}`", path);
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException("Issue while checking for config change", e);
			}

			if (configIsChanged.get()) {
				if (repoProperties.getMeta().isFullCleanOnConfigurationChange()) {
					LOGGER.info("The configuration has changed, then we will process all files in the repository");
					finalCodeWriter = upgradeToFullRepoReader(codeWriter);
				} else {
					LOGGER.info("The configuration has changed, but $.meta.full_clean_on_configuration_change=false");
					finalCodeWriter = codeWriter;
				}
			} else {
				finalCodeWriter = codeWriter;
			}

		} else {
			// We are in a branch (but no base-branch as reference): meaningless to check for config change, and anyway
			LOGGER.debug("We will clean everything");
			finalCodeWriter = codeWriter;
		}

		AtomicLongMap<String> languageToNbAddedFiles = AtomicLongMap.create();
		AtomicLongMap<String> languagesCounters = AtomicLongMap.create();
		Map<Path, String> pathToMutatedContent = new LinkedHashMap<>();

		var cleanthatSession = new CleanthatSession(codeWriter.getRepositoryRoot(), finalCodeWriter, repoProperties);

		repoProperties.getEngines().stream().filter(lp -> !lp.isSkip()).forEach(dirtyLanguageConfig -> {
			var languageP = prepareLanguageConfiguration(repoProperties, dirtyLanguageConfig);

			// TODO Process all languages in a single pass
			// Beware about concurrency as multiple processors/languages may impact the same file
			var languageCounters =
					processFiles(cleanthatSession, languageToNbAddedFiles, pathToMutatedContent, languageP);

			var details = languageCounters.asMap()
					.entrySet()
					.stream()
					.map(e -> e.getKey() + ": " + e.getValue())
					.collect(Collectors.joining(EOL));

			prComments.add("engine=" + languageP.getEngine() + EOL + details);
			languageCounters.asMap().forEach((l, c) -> {
				languagesCounters.addAndGet(l, c);
			});
		});

		boolean isEmpty;
		if (languageToNbAddedFiles.isEmpty() && !configIsChanged.get()) {
			LOGGER.info("Not a single file to commit ({})", codeWriter);
			isEmpty = true;
			// } else if (configIsChanged.get()) {
			// LOGGER.info("(Config change) About to check and possibly commit any files into {} ({})",
			// codeWriter.getHtmlUrl(),
			// codeWriter.getTitle());
			// if (dryRun) {
			// LOGGER.info("Skip persisting changes as dryRun=true");
			// isEmpty = true;
			// } else {
			// codeWriter.persistChanges(pathToMutatedContent, prComments, repoProperties.getMeta().getLabels());
			// }
		} else {
			LOGGER.info("About to commit+push {} files into {} (configChange={})",
					languageToNbAddedFiles.sum(),
					codeWriter,
					configIsChanged.get());
			if (dryRun) {
				// TODO Nice-diff like in eu.solven.cleanthat.engine.java.refactorer.it.ITTestLocalFile
				LOGGER.info("Skip persisting changes as dryRun=true");
				isEmpty = true;
			} else {
				ICodeWritingMetadata metadata =
						new CodeWritingMetadata(prComments, repoProperties.getMeta().getLabels());
				
				isEmpty = codeWriter.persistChanges(pathToMutatedContent, metadata);
			}
		}

		codeWriter.cleanTmpFiles();

		return new CodeFormatResult(isEmpty, new LinkedHashMap<>(languagesCounters.asMap()));
	}

	private ICodeProviderWriter upgradeToFullRepoReader(ICodeProviderWriter codeWriter) {
		ICodeProvider codeProvider = codeWriter;
		while (codeProvider instanceof CodeProviderDecoratingWriter) {
			codeProvider = ((CodeProviderDecoratingWriter) codeWriter).getDecorated();
		}

		if (codeProvider instanceof IUpgradableToHeadFullScan) {
			codeProvider = ((IUpgradableToHeadFullScan) codeProvider).upgradeToFullScan();
		} else {
			LOGGER.warn("TODO {} does not implements {}",
					codeProvider.getClass().getName(),
					IUpgradableToHeadFullScan.class.getName());
		}

		var fullRepoCodeWriter = new CodeProviderDecoratingWriter(codeProvider, () -> codeWriter);
		LOGGER.info("We upgraded {} to {}", codeWriter, fullRepoCodeWriter);
		return fullRepoCodeWriter;
	}

	private IEngineProperties prepareLanguageConfiguration(CleanthatRepositoryProperties repoProperties,
			CleanthatEngineProperties dirtyEngine) {

		var cleanEngine = configHelpers.mergeEngineProperties(repoProperties, dirtyEngine);

		var language = cleanEngine.getEngine();
		LOGGER.info("About to prepare files for language: {}", language);

		var sourceCodeProperties = cleanEngine.getSourceCode();
		var includes = cleanEngine.getSourceCode().getIncludes();
		if (includes.isEmpty()) {
			var defaultIncludes = formatterFactory.getDefaultIncludes(cleanEngine.getEngine());

			LOGGER.info("Default includes to: {}", defaultIncludes);
			// https://github.com/spring-io/spring-javaformat/blob/master/spring-javaformat-maven/spring-javaformat-maven-plugin/...
			// .../src/main/java/io/spring/format/maven/FormatMojo.java#L47
			cleanEngine = configHelpers.forceIncludes(cleanEngine, defaultIncludes);
			sourceCodeProperties = cleanEngine.getSourceCode();
			includes = cleanEngine.getSourceCode().getIncludes();
		}

		LOGGER.info("language={} Applying includes rules: {}", language, includes);
		LOGGER.info("language={} Applying excludes rules: {}", language, sourceCodeProperties.getExcludes());
		return cleanEngine;
	}

	// PMD.CloseResource: False positive as we did not open it ourselves
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.CloseResource" })
	protected AtomicLongMap<String> processFiles(CleanthatSession cleanthatSession,
			AtomicLongMap<String> engineToNbMutatedFiles,
			Map<Path, String> pathToMutatedContent,
			IEngineProperties engineP) {
		List<AutoCloseable> closeUs = new ArrayList<>();
		// We rely on a ThreadLocal as Engines may not be threadSafe
		// Hence, each new thread will compile its own engine
		ThreadLocal<EngineAndLinters> currentThreadEngine = ThreadLocal.withInitial(() -> {
			var lintFixer = buildProcessors(engineP, cleanthatSession);

			closeUs.add(lintFixer);

			return lintFixer;
		});

		try {
			var languageCounters = processFiles(cleanthatSession, pathToMutatedContent, engineP, currentThreadEngine);
			engineToNbMutatedFiles.addAndGet(engineP.getEngine(), languageCounters.get(KEY_NB_FILES_FORMATTED));

			return languageCounters;
		} finally {
			closeUs.forEach(t -> {
				try {
					t.close();
				} catch (Exception e) {
					LOGGER.warn("Issue while closing {}", t, e);
				}
			});
		}
	}

	@SuppressWarnings("PMD.CloseResource")
	protected AtomicLongMap<String> processFiles(CleanthatSession cleanthatSession,
			Map<Path, String> pathToMutatedContent,
			IEngineProperties engineP,
			ThreadLocal<EngineAndLinters> currentThreadEngine) {
		var sourceCodeProperties = engineP.getSourceCode();

		AtomicLongMap<String> languageCounters = AtomicLongMap.create();

		var fs = cleanthatSession.getRepositoryRoot().getFileSystem();
		var includeMatchers = IncludeExcludeHelpers.prepareMatcher(fs, sourceCodeProperties.getIncludes());
		var excludeMatchers = IncludeExcludeHelpers.prepareMatcher(fs, sourceCodeProperties.getExcludes());

		// https://github.com/diffplug/spotless/issues/1555
		// If too many threads, we would load too many Spotless engines
		var executor = PepperExecutorsHelper.newShrinkableFixedThreadPool("Cleanthat-CodeFormatter-");
		CompletionService<Boolean> cs = new ExecutorCompletionService<>(executor);

		try {
			cleanthatSession.getCodeProvider().listFilesForContent(file -> {
				var optRunMe = onEachFile(cleanthatSession,
						pathToMutatedContent,
						currentThreadEngine,
						languageCounters,
						includeMatchers,
						excludeMatchers,
						file);

				optRunMe.ifPresent(cs::submit);
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue listing files", e);
		} finally {
			// TODO Should wait given time left in Lambda
			if (!MoreExecutors.shutdownAndAwaitTermination(executor, 1, TimeUnit.DAYS)) {
				LOGGER.warn("Executor not terminated");
			}
		}

		// Once here, we are guaranteed all tasks has been pushed: we can poll until null.
		while (true) {
			try {
				var polled = cs.poll();

				if (polled == null) {
					break;
				}
				boolean result = polled.get();

				if (result) {
					languageCounters.incrementAndGet(KEY_NB_FILES_FORMATTED);
				} else {
					languageCounters.incrementAndGet("nb_files_already_formatted");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Issue while one of the asynchronous tasks", e);
			}
		}

		return languageCounters;
	}

	private Optional<Callable<Boolean>> onEachFile(CleanthatSession cleanthatSession,
			Map<Path, String> pathToMutatedContent,
			ThreadLocal<EngineAndLinters> currentThreadEngine,
			AtomicLongMap<String> languageCounters,
			List<PathMatcher> includeMatchers,
			List<PathMatcher> excludeMatchers,
			ICodeProviderFile file) {
		var filePath = file.getPath();

		var matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);
		var matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filePath);
		if (matchingInclude.isPresent()) {
			if (matchingExclude.isEmpty()) {
				Callable<Boolean> runMe = () -> {
					var engineSteps = currentThreadEngine.get();

					try {
						return doFormat(cleanthatSession, engineSteps, pathToMutatedContent, filePath);
					} catch (IOException e) {
						throw new UncheckedIOException("Issue with file: " + filePath, e);
					} catch (RuntimeException e) {
						throw new RuntimeException("Issue with file: " + filePath, e);
					}
				};

				return Optional.of(runMe);
			} else {
				languageCounters.incrementAndGet("nb_files_both_included_excluded");
				return Optional.empty();
			}
		} else if (matchingExclude.isPresent()) {
			languageCounters.incrementAndGet("nb_files_excluded_not_included");
			return Optional.empty();
		} else {
			languageCounters.incrementAndGet("nb_files_neither_included_nor_excluded");
			return Optional.empty();
		}
	}

	private boolean doFormat(CleanthatSession cleanthatSession,
			EngineAndLinters engineAndLinters,
			Map<Path, String> pathToMutatedContent,
			Path filePath) throws IOException {
		// Rely on the latest code (possibly formatted by a previous processor)
		var optCode = loadCodeOptMutated(cleanthatSession.getCodeProvider(), pathToMutatedContent, filePath);

		if (optCode.isEmpty()) {
			LOGGER.warn("Skip processing {} as its content is not available", filePath);
			return false;
		}
		var code = optCode.get();

		LOGGER.debug("Processing path={}", filePath);
		var output = doFormat(engineAndLinters, new PathAndContent(filePath, code));
		if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
			LOGGER.info("Path={} successfully cleaned by {}", filePath, engineAndLinters);
			pathToMutatedContent.put(filePath, output);

			if (pathToMutatedContent.size() > MAX_LOG_MANY_FILES
					&& Integer.bitCount(pathToMutatedContent.size()) == 1) {
				LOGGER.warn("We are about to commit {} files. That's quite a lot.", pathToMutatedContent.size());
			}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * The file may be missing for various reasons (e.g. too big to be fetched)
	 * 
	 * @param codeProvider
	 * @param pathToMutatedContent
	 * @param filePath
	 * @return an {@link Optional} of the content.
	 */
	public Optional<String> loadCodeOptMutated(ICodeProvider codeProvider,
			Map<Path, String> pathToMutatedContent,
			Path filePath) {
		var optAlreadyMutated = Optional.ofNullable(pathToMutatedContent.get(filePath));

		if (optAlreadyMutated.isPresent()) {
			return optAlreadyMutated;
		} else {
			try {
				return codeProvider.loadContentForPath(filePath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private EngineAndLinters buildProcessors(IEngineProperties properties, CleanthatSession cleanthatSession) {
		var formattersFactory = formatterFactory.makeLanguageFormatter(properties);

		return sourceCodeFormatterHelper.compile(properties, cleanthatSession, formattersFactory);
	}

	private String doFormat(EngineAndLinters compiledProcessors, PathAndContent pathAndContent) throws IOException {
		return formatterApplier.applyProcessors(compiledProcessors, pathAndContent);
	}
}
