package eu.solven.cleanthat.language.java.eclipse.generator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.google.common.collect.Sets;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatterConfiguration;

/**
 * Default implementation for {@link IEclipseStylesheetGenerator}
 * 
 * @author Benoit Lacelle
 *
 */
// Convert from Checkstyle
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/jobs/TransformCheckstyleRulesJob.java
public class EclipseStylesheetGenerator implements IEclipseStylesheetGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseStylesheetGenerator.class);

	// This is useful to start optimizing these parameters, before optimizing other parameters which behavior
	// depends on these first parameters
	protected List<String> getMainSettings() {
		return Arrays.asList("org.eclipse.jdt.core.formatter.tabulation.char",
				"org.eclipse.jdt.core.formatter.tabulation.size",
				"org.eclipse.jdt.core.formatter.tabulation.char",
				"org.eclipse.jdt.core.formatter.comment.line_length",
				"org.eclipse.jdt.core.formatter.lineSplit");
	}

	/**
	 * This method is useful to generate automatically an Eclipse configuration which match an existing code-base. It is
	 * especially useful for people NOT using Eclipse IDE.
	 * 
	 * @param pathToFile
	 *            a {@link Map} of {@link Path} to the content of the {@link File}.
	 * @return the Set of options which minimizes the modifications over input contents.
	 */
	@Override
	public Map<String, String> generateSettings(Map<Path, String> pathToFile) {
		ScoredOption<Map<String, String>> bestDefaultConfig = findBestDefaultSetting(pathToFile);
		ScoredOption<Map<String, String>> bestSettings = bestDefaultConfig;

		// Start optimizing some crucial parameters
		for (String option : getMainSettings()) {
			bestSettings = pickOptimalOption(pathToFile.values(), bestSettings, option);
		}

		Set<String> settingsToSwitch = new TreeSet<>(bestSettings.getOption().keySet());

		// This is a greedy algorithm, trying to find the Set of options minimizing the diff with existing files
		// We iterate targeting to reach a score of 0 (meaning we spot a configuration matching exactly current
		// code-style)
		boolean hasMutated;
		do {
			hasMutated = false;
			for (String settingToSwitch : settingsToSwitch) {
				ScoredOption<Map<String, String>> newBestSettings =
						pickOptimalOption(pathToFile.values(), bestSettings, settingToSwitch);
				if (!bestSettings.getOption().equals(newBestSettings.getOption())) {
					bestSettings = newBestSettings;
					hasMutated = true;
				}
			}
			if (hasMutated) {
				// We go through another pass as it is possible a new tweak lead to other tweaks having different
				// impacts
				// e.g. changing the max length of rows leads many other rules to behave differently
				LOGGER.info(
						"The configuration has muted: we will go again through all options to look for a better set of settings");
			}
		} while (hasMutated && bestSettings.getScore() > 0);

		logPathsImpactedByConfiguration(pathToFile, bestSettings);

		// logDiffWithPepper(pathToFile, bestOption);
		return bestSettings.getOption();
	}

	protected void logPathsImpactedByConfiguration(Map<Path, String> pathToFile,
			ScoredOption<Map<String, String>> bestOption) {
		if (bestOption.getScore() > 0) {
			LOGGER.info("We did not succeed crafting a configuration matching perfectly existing code");

			EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(bestOption.getOption());
			EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);

			pathToFile.entrySet().stream().filter(entry -> {
				long tweakedDiffScoreDiff;
				try {
					tweakedDiffScoreDiff = computeDiffScore(formatter, entry.getValue());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				return tweakedDiffScoreDiff > 0;
			}).forEach(entry -> {
				LOGGER.info("Path needing formatting with 'optimal' configuration: {}", entry.getKey());
			});
		}
	}

	/**
	 * @param pathToFile
	 * @return the best configuration amongst a small set of standard configurations
	 */
	protected ScoredOption<Map<String, String>> findBestDefaultSetting(Map<Path, String> pathToFile) {
		Map<String, String> eclipseDefault;
		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getDefaultSettings();
			eclipseDefault = defaultSettings.getMap();
		}
		Map<String, String> eclipseEclipseDefault;
		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getEclipseDefaultSettings();
			eclipseEclipseDefault = defaultSettings.getMap();
		}
		Map<String, String> eclipseJavaConventions;
		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
			eclipseJavaConventions = defaultSettings.getMap();
		}
		Map<String, String> googleConvention;
		{
			googleConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/eclipse-java-google-style.xml"))
					.getSettings();
		}
		Map<String, String> springConvention;
		{
			springConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/spring-eclipse-code-formatter.xml"))
					.getSettings();
		}
		ScoredOption<Map<String, String>> bestDefaultConfig = Stream
				.of(eclipseDefault, eclipseEclipseDefault, eclipseJavaConventions, googleConvention, springConvention)
				.map(config -> {
					EclipseJavaFormatter formatter =
							new EclipseJavaFormatter(new EclipseJavaFormatterConfiguration(config));
					long score = computeDiffScore(formatter, pathToFile.values());
					return new ScoredOption<>(config, score);
				})
				.min(Comparator.comparingLong(ScoredOption::getScore))
				.get();
		logBestDefaultConfig(eclipseDefault,
				eclipseEclipseDefault,
				eclipseJavaConventions,
				googleConvention,
				springConvention,
				bestDefaultConfig);
		return bestDefaultConfig;
	}

	public void logBestDefaultConfig(Map<String, String> eclipseDefault,
			Map<String, String> eclipseEclipseDefault,
			Map<String, String> eclipseJavaConventions,
			Map<String, String> googleConvention,
			Map<String, String> springConvention,
			ScoredOption<Map<String, String>> bestDefaultConfig) {
		Map<String, String> selectedOption = bestDefaultConfig.getOption();
		String bestOptionName;
		if (selectedOption.equals(eclipseDefault)) {
			bestOptionName = "Eclipse-Default";
		} else if (selectedOption.equals(eclipseEclipseDefault)) {
			bestOptionName = "Eclipse-Eclipse";
		} else if (selectedOption.equals(eclipseJavaConventions)) {
			bestOptionName = "Eclipse-Java";
		} else if (selectedOption.equals(googleConvention)) {
			bestOptionName = "Google";
		} else if (selectedOption.equals(springConvention)) {
			bestOptionName = "Spring";
		} else {
			bestOptionName = "???";
		}
		LOGGER.info("Best standard configuration: {}", bestOptionName);
	}

	public ScoredOption<Map<String, String>> pickOptimalOption(Collection<String> contents,
			ScoredOption<Map<String, String>> bestOption,
			String parameterToSwitch) {
		Set<String> possibleOptions = possibleOptions(parameterToSwitch);
		LOGGER.debug("Considering parameter: {} ({} candidates)", parameterToSwitch, possibleOptions.size());
		Optional<ScoredOption<Map<String, String>>> optMin = possibleOptions.parallelStream().map(possibleValue -> {
			Map<String, String> tweakedConfiguration = new TreeMap<>(bestOption.getOption());
			String currentBestOption = tweakedConfiguration.put(parameterToSwitch, possibleValue);
			if (currentBestOption.equals(possibleValue)) {
				// No-need to check with current value
				return Optional.<ScoredOption<Map<String, String>>>empty();
			}
			EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(tweakedConfiguration);
			EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);
			long tweakedDiffScoreDiff = computeDiffScore(formatter, contents);
			return Optional.of(new ScoredOption<Map<String, String>>(tweakedConfiguration, tweakedDiffScoreDiff));
		}).flatMap(Optional::stream).min(Comparator.comparingLong(ScoredOption::getScore));
		if (optMin.isPresent() && optMin.get().getScore() < bestOption.getScore()) {
			LOGGER.info("Tweaked diff improves score: {} ({})", optMin.get().getScore(), parameterToSwitch);
			return optMin.get();
		}
		return bestOption;
	}

	@Override
	public Map<Path, String> loadFilesContent(Path rootForFiles, Pattern fileMatcher) throws IOException {
		LOGGER.debug("Loading files content from {}", rootForFiles);
		Map<Path, String> pathToFile = new ConcurrentHashMap<>();
		Files.walk(rootForFiles).forEach(path -> {
			if (path.toFile().isFile() && fileMatcher.matcher(path.toString()).matches()) {
				try {
					String content = Files.readString(path);

					pathToFile.put(path, content);
				} catch (IOException e) {
					LOGGER.warn("Issue loading " + path, e);
				}
			} else {
				LOGGER.debug("Rejected: {}", path);
			}
		});
		LOGGER.debug("Loaded files content from {}", rootForFiles);
		return pathToFile;
	}

	/**
	 * 
	 * @param parameterToSwitch
	 * @return the different values to consider for given Eclipse {@link IStyleEnforcer} option
	 */
	// see DefaultCodeFormatterOptions
	@SuppressWarnings({ "checkstyle:MagicNumber", "PMD.ExcessiveMethodLength", "PMD.CognitiveComplexity" })
	private Set<String> possibleOptions(String parameterToSwitch) {
		if ("org.eclipse.jdt.core.formatter.enabling_tag".equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.disabling_tag".equals(parameterToSwitch)) {
			// This parameters are left to their default value
			return Set.of();
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.alignment_for_")) {
			// see org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions.getAlignment(int)
			return Sets.<Object>cartesianProduct(Arrays.asList(Set.of(false, true),
					Set.of(DefaultCodeFormatterConstants.WRAP_NO_SPLIT,
							DefaultCodeFormatterConstants.WRAP_COMPACT,
							DefaultCodeFormatterConstants.WRAP_COMPACT_FIRST_BREAK,
							DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
							DefaultCodeFormatterConstants.WRAP_NEXT_SHIFTED,
							DefaultCodeFormatterConstants.WRAP_NEXT_PER_LINE)))
					.stream()
					.map(list -> {
						boolean forceSplit = (boolean) list.get(0);
						int wrapStyle = (int) list.get(1);
						return DefaultCodeFormatterConstants.createAlignmentValue(forceSplit, wrapStyle);
					})
					.collect(Collectors.toSet());
		} else if ("org.eclipse.jdt.core.formatter.align_fields_grouping_blank_lines".equals(parameterToSwitch)) {
			return IntStream.of(0, 1, 2, 3, 4, Integer.MAX_VALUE).mapToObj(String::valueOf).collect(Collectors.toSet());
			// } else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.align_")
			// && parameterToSwitch.endsWith("_on_columns")) {
			// return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.blank_lines_before_")
				|| parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.blank_lines_between_")
				|| parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.blank_lines_after_")) {
			// We assume a limited set of standard values
			return IntStream.range(0, 2).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.brace_position_for_")) {
			return Set.of(DefaultCodeFormatterConstants.END_OF_LINE,
					DefaultCodeFormatterConstants.NEXT_LINE,
					DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP,
					DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED);
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.insert_")
				|| parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.comment.insert_")) {
			return Set.of(JavaCore.INSERT, JavaCore.DO_NOT_INSERT);
		} else if (// The field is named comment_format_source
		"org.eclipse.jdt.core.formatter.comment.format_source_code".equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.format_guardian_clause_on_one_line".equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.format_line_comment_starting_on_first_column"
						.equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.use_on_off_tags".equals(parameterToSwitch)) {
			// edge-cases handled manually
			return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
		} else if ("org.eclipse.jdt.core.formatter.comment.line_length".equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.lineSplit".equals(parameterToSwitch)) {
			// We assume a limited set of standard values
			return IntStream.range(8, 25).map(i -> i * 10).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if ("org.eclipse.jdt.core.formatter.continuation_indentation".equals(parameterToSwitch)
				|| "org.eclipse.jdt.core.formatter.continuation_indentation_for_array_initializer"
						.equals(parameterToSwitch)) {
			// This is the indentation when a single statement is split on multiple lines
			// We assume a limited set of standard values
			return IntStream.range(0, 5).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if ("org.eclipse.jdt.core.formatter.indentation.size".equals(parameterToSwitch)) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 4).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.keep_")) {
			if ("org.eclipse.jdt.core.formatter.keep_code_block_on_one_line".equals(parameterToSwitch)) {
				return Set.of(DefaultCodeFormatterConstants.ONE_LINE_NEVER,
						DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY);
			} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.keep_simple_")) {
				return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
			} else {
				return Set.of(DefaultCodeFormatterConstants.ONE_LINE_NEVER,
						DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY,
						DefaultCodeFormatterConstants.ONE_LINE_IF_SINGLE_ITEM,
						DefaultCodeFormatterConstants.ONE_LINE_ALWAYS,
						DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
			}
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.number_of_blank_lines_")) {
			// We assume a limited set of standard values
			// If negative, this would override 'number_of_empty_lines_to_preserve'
			return IntStream.rangeClosed(-4, 4).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if ("org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve".equals(parameterToSwitch)) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 4).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.parentheses_positions_in_")) {
			return Set.of(DefaultCodeFormatterConstants.COMMON_LINES,
					DefaultCodeFormatterConstants.SEPARATE_LINES_IF_NOT_EMPTY,
					DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
					DefaultCodeFormatterConstants.SEPARATE_LINES,
					DefaultCodeFormatterConstants.PRESERVE_POSITIONS);
		} else if ("org.eclipse.jdt.core.formatter.tabulation.char".equals(parameterToSwitch)) {
			return Set.of(JavaCore.TAB, JavaCore.SPACE, DefaultCodeFormatterConstants.MIXED);
		} else if ("org.eclipse.jdt.core.formatter.tabulation.size".equals(parameterToSwitch)) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 8).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if ("org.eclipse.jdt.core.formatter.text_block_indentation".equals(parameterToSwitch)) {
			return IntStream
					.of(DefaultCodeFormatterConstants.INDENT_PRESERVE,
							DefaultCodeFormatterConstants.INDENT_BY_ONE,
							DefaultCodeFormatterConstants.INDENT_DEFAULT,
							DefaultCodeFormatterConstants.INDENT_ON_COLUMN)
					.mapToObj(String::valueOf)
					.collect(Collectors.toSet());
		} else {
			if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.")) {
				// Replace '.' by '_' to handle
				// 'org.eclipse.jdt.core.formatter.comment.align_tags_descriptions_grouped'
				String parameterName =
						parameterToSwitch.substring("org.eclipse.jdt.core.formatter.".length()).replace('.', '_');

				String logPrefix = "Introspection strategy failed for ";
				try {
					Field field = DefaultCodeFormatterOptions.class.getField(parameterName);
					if (field.getType() == boolean.class) {
						return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
					}
				} catch (NoSuchFieldException | SecurityException e) {
					LOGGER.debug(logPrefix + parameterToSwitch, e);
					if (parameterName.endsWith("_comments")) {
						LOGGER.debug("Many fields ending with 'comments' are misspelled");
					} else {
						LOGGER.info(logPrefix + parameterToSwitch);
					}
				}
				if (parameterName.endsWith("_comments")) {
					// There is some spelling issues, and sometimes comment is put in plural in the field
					// e.g. 'org.eclipse.jdt.core.formatter.comment.format_block_comments'
					parameterName = parameterName.substring(0, parameterName.length() - 1);
					try {
						Field field = DefaultCodeFormatterOptions.class.getField(parameterName);
						if (field.getType() == boolean.class) {
							return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						LOGGER.debug(logPrefix + parameterToSwitch, e);
						LOGGER.info(logPrefix + parameterToSwitch);
					}
				}
			}

			// If this happens, it may be due to an update of Eclipse engine
			LOGGER.warn("Parameter not managed: {}", parameterToSwitch);
			return Set.of();
		}
	}

	protected long computeDiffScore(EclipseJavaFormatter formatter, Collection<String> contents) {
		return contents.parallelStream().mapToLong(content -> {
			try {
				return computeDiffScore(formatter, content);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).sum();
	}

	/**
	 * 
	 * @param stylEnforcer
	 * @param pathAsString
	 * @return a score indicating how much this formatter impacts given content. If 0, the formatter has no impacts. A
	 *         higher score means a bigger difference
	 * @throws IOException
	 */
	protected long computeDiffScore(IStyleEnforcer stylEnforcer, String pathAsString) throws IOException {
		String formatted = stylEnforcer.doFormat(pathAsString, LineEnding.KEEP);
		List<String> originalRows = Arrays.asList(pathAsString.split("[\r\n]+"));
		List<String> formattedRows = Arrays.asList(formatted.split("[\r\n]+"));
		Patch<String> diff = DiffUtils.diff(originalRows, formattedRows, new MyersDiff<String>());
		List<String> patchApplied;
		try {
			patchApplied = diff.applyTo(originalRows);
		} catch (PatchFailedException e) {
			throw new RuntimeException(e);
		}
		if (!formattedRows.equals(patchApplied)) {
			throw new IllegalArgumentException("Issue computing the diff?");
		}
		long deltaDiff = diff.getDeltas().stream().mapToLong(d -> {
			if (d.getType() == DeltaType.EQUAL) {
				return 0L;
			}
			// We count the number of impacted characters
			long sourceSize = d.getSource().getLines().stream().mapToLong(String::length).sum();
			long targetSize = d.getTarget().getLines().stream().mapToLong(String::length).sum();
			// Given a diff, we consider the biggest square between the source and the target
			return Math.max(sourceSize, targetSize);
		}).sum();
		return deltaDiff;
	}
}
