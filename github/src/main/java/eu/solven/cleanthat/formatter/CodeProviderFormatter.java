package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.ListeningExecutorService;

import cormoran.pepper.thread.PepperExecutorsHelper;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.github.event.GithubPRCodeProvider;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;

/**
 * Unclear what is the point of this class
 *
 * @author Benoit Lacelle
 */
public class CodeProviderFormatter implements ICodeProviderFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderFormatter.class);

	public static final List<String> DEFAULT_INCLUDES_JAVA = Arrays.asList("glob:**/*.java");

	public static final String EOL = "\r\n";
	private static final int CORES_FORMATTER = 16;
	private static final int MAX_LOG_MANY_FILES = 128;

	final ObjectMapper objectMapper;

	final IStringFormatter formatter;

	public CodeProviderFormatter(ObjectMapper objectMapper, IStringFormatter formatter) {
		this.objectMapper = objectMapper;
		this.formatter = formatter;
	}

	@Override
	public Map<String, ?> formatCode(CleanthatRepositoryProperties repoProperties, ICodeProvider pr) {
		// A config change may be cleanthat.json

		// TODO or an indirect change leading to a full re-compute (e.g. a implicit
		// version upgrade led to a change of some engine, which should trigger a full re-compute)
		AtomicBoolean configIsChanged = new AtomicBoolean();

		List<String> prComments = new ArrayList<>();

		if (pr instanceof GithubPRCodeProvider) {
			try {
				pr.listFiles(fileChanged -> {
					if (GithubPullRequestCleaner.PATH_CLEANTHAT_JSON.equals(fileChanged.getFilePath(pr))) {
						configIsChanged.set(true);
						prComments.add("Configuration has changed");
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException("Issue while checking for config change", e);
			}
		}
		// else {
		// // We are in a branch: all files are candidates, which makes too many files to iterate over them here
		// // https://www.baeldung.com/jgit
		// try {
		// Git git = pr.makeGitRepo();
		// } catch (RuntimeException e) {
		// throw new IllegalStateException("Issue while cloning the repository");
		// }
		// }

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
			pr.commitIntoPR(pathToMutatedContent, prComments);
		} else {
			LOGGER.info("(No config change) About to check and possibly commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					pr.getHtmlUrl(),
					pr.getTitle());
			pr.commitIntoPR(pathToMutatedContent, prComments);
		}
		return new LinkedHashMap<>(languagesCounters.asMap());
	}

	private ILanguageProperties prepareLanguageConfiguration(CleanthatRepositoryProperties repoProperties,
			Map<String, ?> dirtyLanguageConfig) {
		ConfigHelpers configHelpers = new ConfigHelpers(objectMapper);

		ILanguageProperties languageP = configHelpers.mergeLanguageProperties(repoProperties, dirtyLanguageConfig);

		String language = languageP.getLanguage();
		LOGGER.info("About to prepare files for language: {}", language);

		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();
		List<String> includes = languageP.getSourceCodeProperties().getIncludes();
		if (includes.isEmpty()) {
			if ("java".equals(languageP.getLanguage())) {
				List<String> defaultIncludes = DEFAULT_INCLUDES_JAVA;
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

		LOGGER.info("Applying includes rules: {}", sourceCodeProperties.getIncludes());
		LOGGER.info("Applying excludes rules: {}", sourceCodeProperties.getExcludes());
		return languageP;
	}

	private AtomicLongMap<String> processFiles(ICodeProvider pr,
			AtomicLongMap<String> languageToNbMutatedFiles,
			Map<String, String> pathToMutatedContent,
			ILanguageProperties languageP) {
		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();

		AtomicLongMap<String> languageCounters = AtomicLongMap.create();

		List<PathMatcher> includeMatchers = prepareMatcher(sourceCodeProperties.getIncludes());
		List<PathMatcher> excludeMatchers = prepareMatcher(sourceCodeProperties.getExcludes());

		ListeningExecutorService executor =
				PepperExecutorsHelper.newShrinkableFixedThreadPool(CORES_FORMATTER, "CodeFormatter");
		CompletionService<Boolean> cs = new ExecutorCompletionService<>(executor);

		try {
			pr.listFiles(file -> {
				if (file.fileIsRemoved(pr)) {
					// Skip files deleted within PR
					return;
				}
				String fileName = file.getFilePath(pr);

				Optional<PathMatcher> matchingInclude = findMatching(includeMatchers, fileName);
				Optional<PathMatcher> matchingExclude = findMatching(excludeMatchers, fileName);
				if (matchingInclude.isPresent()) {
					if (matchingExclude.isEmpty()) {
						cs.submit(() -> {
							try {
								return doFormat(pr, pathToMutatedContent, languageP, file, fileName);
							} catch (IOException e) {
								throw new UncheckedIOException("Issue with file: " + fileName, e);
							} catch (RuntimeException e) {
								throw new RuntimeException("Issue with file: " + fileName, e);
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
			executor.shutdown();
			// TODO Should wait given time left in Lambda
			try {
				executor.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}

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
			Object file,
			String fileName) throws IOException {
		Optional<String> optAlreadyMutated = Optional.ofNullable(pathToMutatedContent.get(fileName));
		String code = optAlreadyMutated.orElseGet(() -> {
			try {
				return new DummyCodeProviderFile(file).loadContent(pr);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		LOGGER.info("Processing {}", fileName);
		String output = doFormat(languageP, code);
		if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
			pathToMutatedContent.put(fileName, output);

			if (pathToMutatedContent.size() > MAX_LOG_MANY_FILES
					&& Integer.bitCount(pathToMutatedContent.size()) == 1) {
				LOGGER.warn("We are about to commit {} files. That's quite a lot.", pathToMutatedContent.size());
			}

			return true;
		} else {
			return false;
		}
	}

	// https://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
	public static Optional<PathMatcher> findMatching(List<PathMatcher> includeMatchers, String fileName) {
		return includeMatchers.stream().filter(pm -> pm.matches(Paths.get(fileName))).findFirst();
	}

	public static List<PathMatcher> prepareMatcher(List<String> regex) {
		return regex.stream().map(r -> FileSystems.getDefault().getPathMatcher(r)).collect(Collectors.toList());
	}

	private String doFormat(ILanguageProperties properties, String code) throws IOException {
		return formatter.format(properties, code);
	}
}
