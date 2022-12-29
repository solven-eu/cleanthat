package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.ICodeFormatterApplier;
import eu.solven.cleanthat.language.ILanguageFormatterFactory;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.LanguagePropertiesAndBuildProcessors;
import eu.solven.pepper.thread.PepperExecutorsHelper;

/**
 * Unclear what is the point of this class
 *
 * @author Benoit Lacelle
 */
public class CodeProviderFormatter implements ICodeProviderFormatter {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CodeProviderFormatter.class);

	public static final String EOL = "\r\n";
	private static final int CORES_FORMATTER = 16;
	private static final int MAX_LOG_MANY_FILES = 128;

	final List<ObjectMapper> objectMappers;

	final ILanguageFormatterFactory formatterFactory;
	final ICodeFormatterApplier formatterApplier;

	final SourceCodeFormatterHelper sourceCodeFormatterHelper;

	public CodeProviderFormatter(List<ObjectMapper> objectMappers,
			ILanguageFormatterFactory formatterFactory,
			ICodeFormatterApplier formatterApplier) {
		this.objectMappers = objectMappers;
		this.formatterFactory = formatterFactory;
		this.formatterApplier = formatterApplier;

		this.sourceCodeFormatterHelper = new SourceCodeFormatterHelper(ConfigHelpers.getJson(objectMappers));
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public CodeFormatResult formatCode(CleanthatRepositoryProperties repoProperties,
			ICodeProviderWriter codeWriter,
			boolean dryRun) {
		// A config change may be cleanthat.json, or a processor configuration file

		// TODO or an indirect change leading to a full re-compute (e.g. a implicit
		// version upgrade led to a change of some engine, which should trigger a full re-compute)
		AtomicBoolean configIsChanged = new AtomicBoolean();

		List<String> prComments = new ArrayList<>();

		if (codeWriter instanceof IListOnlyModifiedFiles) {
			// TODO Check if number of files is compatible with RateLimit
			try {
				codeWriter.listFilesForFilenames(fileChanged -> {
					if (CodeProviderHelpers.FILENAMES_CLEANTHAT.contains(fileChanged.getPath())) {
						configIsChanged.set(true);
						prComments.add("Configuration has changed");
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException("Issue while checking for config change", e);
			}
		} else {
			// We are in a branch (but no base-branch as reference): meaningless to check for config change, and anyway
			LOGGER.debug("We will clean everything");
		}

		AtomicLongMap<String> languageToNbAddedFiles = AtomicLongMap.create();
		AtomicLongMap<String> languagesCounters = AtomicLongMap.create();
		Map<String, String> pathToMutatedContent = new LinkedHashMap<>();

		repoProperties.getLanguages().stream().filter(lp -> !lp.isSkip()).forEach(dirtyLanguageConfig -> {
			ILanguageProperties languageP = prepareLanguageConfiguration(repoProperties, dirtyLanguageConfig);

			// TODO Process all languages in a single pass
			// Beware about concurrency as multiple processors/languages may impact the same file
			AtomicLongMap<String> languageCounters =
					processFiles(codeWriter, languageToNbAddedFiles, pathToMutatedContent, languageP);

			String details = languageCounters.asMap()
					.entrySet()
					.stream()
					.map(e -> e.getKey() + ": " + e.getValue())
					.collect(Collectors.joining(EOL));

			prComments.add("language=" + languageP.getLanguage() + EOL + details);
			languageCounters.asMap().forEach((l, c) -> {
				languagesCounters.addAndGet(l, c);
			});
		});

		boolean isEmpty;
		if (languageToNbAddedFiles.isEmpty() && !configIsChanged.get()) {
			LOGGER.info("Not a single file to commit ({})", codeWriter.getHtmlUrl());
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
			LOGGER.info("(No config change) About to check and possibly commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					codeWriter.getHtmlUrl(),
					codeWriter.getTitle());
			if (dryRun) {
				LOGGER.info("Skip persisting changes as dryRun=true");
				isEmpty = true;
			} else {
				codeWriter.persistChanges(pathToMutatedContent, prComments, repoProperties.getMeta().getLabels());
				isEmpty = false;
			}
		}

		codeWriter.cleanTmpFiles();

		return new CodeFormatResult(isEmpty, new LinkedHashMap<>(languagesCounters.asMap()));
	}

	private ILanguageProperties prepareLanguageConfiguration(CleanthatRepositoryProperties repoProperties,
			LanguageProperties dirtyLanguageConfig) {
		ConfigHelpers configHelpers = new ConfigHelpers(objectMappers);

		ILanguageProperties languageP = configHelpers.mergeLanguageProperties(repoProperties, dirtyLanguageConfig);

		String language = languageP.getLanguage();
		LOGGER.info("About to prepare files for language: {}", language);

		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCode();
		List<String> includes = languageP.getSourceCode().getIncludes();
		if (includes.isEmpty()) {
			if ("java".equals(languageP.getLanguage())) {
				List<String> defaultIncludes = IncludeExcludeHelpers.DEFAULT_INCLUDES_JAVA;
				LOGGER.info("Default includes to: {}", defaultIncludes);
				// https://github.com/spring-io/spring-javaformat/blob/master/spring-javaformat-maven/spring-javaformat-maven-plugin/...
				// .../src/main/java/io/spring/format/maven/FormatMojo.java#L47
				languageP = configHelpers.forceIncludes(languageP, defaultIncludes);
				sourceCodeProperties = languageP.getSourceCode();
				includes = languageP.getSourceCode().getIncludes();
			} else {
				LOGGER.warn("No includes and no default for language={}", language);
			}
		}

		LOGGER.info("language={} Applying includes rules: {}", language, includes);
		LOGGER.info("language={} Applying excludes rules: {}", language, sourceCodeProperties.getExcludes());
		return languageP;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	protected AtomicLongMap<String> processFiles(ICodeProvider codeProvider,
			AtomicLongMap<String> languageToNbMutatedFiles,
			Map<String, String> pathToMutatedContent,
			ILanguageProperties languageP) {
		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCode();

		AtomicLongMap<String> languageCounters = AtomicLongMap.create();

		List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getIncludes());
		List<PathMatcher> excludeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getExcludes());

		ListeningExecutorService executor =
				PepperExecutorsHelper.newShrinkableFixedThreadPool(CORES_FORMATTER, "CodeFormatter");
		CompletionService<Boolean> cs = new ExecutorCompletionService<>(executor);

		LanguagePropertiesAndBuildProcessors compiledProcessors = buildProcessors(languageP, codeProvider);

		try {
			codeProvider.listFilesForContent(file -> {
				String filePath = file.getPath();

				if (filePath.contains("do_not_format_me")) {
					LOGGER.error("TODO");
				}

				Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);
				Optional<PathMatcher> matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filePath);
				if (matchingInclude.isPresent()) {
					if (matchingExclude.isEmpty()) {
						cs.submit(() -> {
							try {
								return doFormat(codeProvider, compiledProcessors, pathToMutatedContent, filePath);
							} catch (IOException e) {
								throw new UncheckedIOException("Issue with file: " + filePath, e);
							} catch (RuntimeException e) {
								throw new RuntimeException("Issue with file: " + filePath, e);
							}
						});
					} else {
						languageCounters.incrementAndGet("nb_files_both_included_excluded");
					}
				} else if (matchingExclude.isPresent()) {
					languageCounters.incrementAndGet("nb_files_excluded_not_included");
				} else {
					languageCounters.incrementAndGet("nb_files_neither_included_nor_excluded");
				}
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
				Future<Boolean> polled = cs.poll();

				if (polled == null) {
					break;
				}
				boolean result = polled.get();

				if (result) {
					languageToNbMutatedFiles.incrementAndGet(languageP.getLanguage());
					languageCounters.incrementAndGet("nb_files_formatted");
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

	private boolean doFormat(ICodeProvider codeProvider,
			LanguagePropertiesAndBuildProcessors compiledProcessors,
			Map<String, String> pathToMutatedContent,
			String filePath) throws IOException {
		// Rely on the latest code (possibly formatted by a previous processor)
		Optional<String> optCode = loadCodeOptMutated(codeProvider, pathToMutatedContent, filePath);

		if (optCode.isEmpty()) {
			LOGGER.warn("Skip processing {} as its content is not available", filePath);
			return false;
		}
		String code = optCode.get();

		LOGGER.debug("Processing path={}", filePath);
		String output = doFormat(compiledProcessors, filePath, code);
		if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
			LOGGER.info("Path={} successfully cleaned by {}", filePath, compiledProcessors);
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
			Map<String, String> pathToMutatedContent,
			String filePath) {
		Optional<String> optAlreadyMutated = Optional.ofNullable(pathToMutatedContent.get(filePath));

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

	private LanguagePropertiesAndBuildProcessors buildProcessors(ILanguageProperties properties,
			ICodeProvider codeProvider) {
		ILanguageLintFixerFactory formattersFactory = formatterFactory.makeLanguageFormatter(properties);

		return sourceCodeFormatterHelper.compile(properties, codeProvider, formattersFactory);
	}

	private String doFormat(LanguagePropertiesAndBuildProcessors compiledProcessors, String filepath, String code)
			throws IOException {
		return formatterApplier.applyProcessors(compiledProcessors, filepath, code);
	}
}
