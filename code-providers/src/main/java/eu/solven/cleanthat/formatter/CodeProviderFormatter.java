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

import cormoran.pepper.thread.PepperExecutorsHelper;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.IStringFormatterFactory;

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

	final IStringFormatterFactory formatterFactory;

	public CodeProviderFormatter(List<ObjectMapper> objectMappers, IStringFormatterFactory formatterFactory) {
		this.objectMappers = objectMappers;
		this.formatterFactory = formatterFactory;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public Map<String, ?> formatCode(CleanthatRepositoryProperties repoProperties,
			ICodeProviderWriter pr,
			boolean dryRun) {
		// A config change may be cleanthat.json

		// TODO or an indirect change leading to a full re-compute (e.g. a implicit
		// version upgrade led to a change of some engine, which should trigger a full re-compute)
		AtomicBoolean configIsChanged = new AtomicBoolean();

		List<String> prComments = new ArrayList<>();

		if (pr instanceof IListOnlyModifiedFiles) {
			// TODO Check if number of files is compatible with RateLimit
			try {
				pr.listFiles(fileChanged -> {
					if (CodeProviderHelpers.FILENAMES_CLEANTHAT.contains(fileChanged.getFilePath(pr))) {
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

		repoProperties.getLanguages().forEach(dirtyLanguageConfig -> {
			ILanguageProperties languageP = prepareLanguageConfiguration(repoProperties, dirtyLanguageConfig);

			// TODO Process all languages in a single pass
			AtomicLongMap<String> languageCounters =
					processFiles(pr, languageToNbAddedFiles, pathToMutatedContent, languageP);

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

		if (languageToNbAddedFiles.isEmpty() && !configIsChanged.get()) {
			LOGGER.info("Not a single file to commit ({})", pr.getHtmlUrl());
		} else if (configIsChanged.get()) {
			LOGGER.info("(Config change) About to check and possibly commit any files into {} ({})",
					pr.getHtmlUrl(),
					pr.getTitle());
			if (dryRun) {
				LOGGER.info("Skip persisting changes as dryRun=true");
			} else {
				pr.commitIntoBranch(pathToMutatedContent, prComments, repoProperties.getMeta().getLabels());
			}
		} else {
			LOGGER.info("(No config change) About to check and possibly commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					pr.getHtmlUrl(),
					pr.getTitle());
			if (dryRun) {
				LOGGER.info("Skip persisting changes as dryRun=true");
			} else {
				pr.commitIntoBranch(pathToMutatedContent, prComments, repoProperties.getMeta().getLabels());
			}
		}
		return new LinkedHashMap<>(languagesCounters.asMap());
	}

	private ILanguageProperties prepareLanguageConfiguration(CleanthatRepositoryProperties repoProperties,
			Map<String, ?> dirtyLanguageConfig) {
		ConfigHelpers configHelpers = new ConfigHelpers(objectMappers);

		ILanguageProperties languageP = configHelpers.mergeLanguageProperties(repoProperties, dirtyLanguageConfig);

		String language = languageP.getLanguage();
		LOGGER.info("About to prepare files for language: {}", language);

		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();
		List<String> includes = languageP.getSourceCodeProperties().getIncludes();
		if (includes.isEmpty()) {
			if ("java".equals(languageP.getLanguage())) {
				List<String> defaultIncludes = IncludeExcludeHelpers.DEFAULT_INCLUDES_JAVA;
				LOGGER.info("Default includes to: {}", defaultIncludes);
				// https://github.com/spring-io/spring-javaformat/blob/master/spring-javaformat-maven/spring-javaformat-maven-plugin/...
				// .../src/main/java/io/spring/format/maven/FormatMojo.java#L47
				languageP = configHelpers.forceIncludes(languageP, defaultIncludes);
				sourceCodeProperties = languageP.getSourceCodeProperties();
				includes = languageP.getSourceCodeProperties().getIncludes();
			} else {
				LOGGER.warn("No includes and no default for language={}", language);
			}
		}

		LOGGER.info("Applying includes rules: {}", includes);
		LOGGER.info("Applying excludes rules: {}", sourceCodeProperties.getExcludes());
		return languageP;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	protected AtomicLongMap<String> processFiles(ICodeProvider pr,
			AtomicLongMap<String> languageToNbMutatedFiles,
			Map<String, String> pathToMutatedContent,
			ILanguageProperties languageP) {
		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();

		AtomicLongMap<String> languageCounters = AtomicLongMap.create();

		List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getIncludes());
		List<PathMatcher> excludeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getExcludes());

		ListeningExecutorService executor =
				PepperExecutorsHelper.newShrinkableFixedThreadPool(CORES_FORMATTER, "CodeFormatter");
		CompletionService<Boolean> cs = new ExecutorCompletionService<>(executor);

		try {
			pr.listFiles(file -> {
				if (file.fileIsRemoved(pr)) {
					// Skip files deleted within PR
					return;
				}
				String filePath = file.getFilePath(pr);

				Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);
				Optional<PathMatcher> matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filePath);
				if (matchingInclude.isPresent()) {
					if (matchingExclude.isEmpty()) {
						cs.submit(() -> {
							try {
								return doFormat(pr, pathToMutatedContent, languageP, file, filePath);
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
					languageCounters.incrementAndGet("nb_files_neither_included_nor_included");
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
				throw new RuntimeException("Arg", e);
			}
		}

		return languageCounters;
	}

	private boolean doFormat(ICodeProvider pr,
			Map<String, String> pathToMutatedContent,
			ILanguageProperties languageP,
			ICodeProviderFile file,
			String filePath) throws IOException {
		Optional<String> optAlreadyMutated = Optional.ofNullable(pathToMutatedContent.get(filePath));
		String code = optAlreadyMutated.orElseGet(() -> {
			try {
				return file.loadContent(pr);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		LOGGER.info("Processing {}", filePath);
		String output = doFormat(languageP, filePath, code);
		if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
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

	private String doFormat(ILanguageProperties properties, String filepath, String code) throws IOException {
		return formatterFactory.makeStringFormatter(properties).format(properties, filepath, code);
	}
}
