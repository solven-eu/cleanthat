package eu.solven.cleanthat.language.java.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * Execute the procedure to generate a minifying Eclipse Formatter configuration
 * 
 * @author Benoit Lacelle
 *
 */
public class RunGenerateEclipseStylesheet {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEclipseStylesheet.class);

	protected RunGenerateEclipseStylesheet() {
		// hidden
	}

	public static void main(String[] args)
			throws TransformerException, ParserConfigurationException, IOException, PatchFailedException {
		GenerateEclipseStylesheet stylesheetGenerator = new GenerateEclipseStylesheet();
		// Path writtenPath = stylesheetGenerator.writeInTmp();

		Path rootForFiles = Paths.get("/Users/blacelle/workspace2/cleanthat");
		Pattern fileMatcher = Pattern.compile(".*\\.java");

		Map<Path, String> pathToFile = loadConcernedFiles(rootForFiles, fileMatcher);
		{

			ScoredOption<Map<String, String>> bestDefaultConfig = findBestDefaultOption(pathToFile);

			ScoredOption<Map<String, String>> bestOption = bestDefaultConfig;

			// Start optimizing some crucial parameters
			for (String option : Arrays.asList("org.eclipse.jdt.core.formatter.tabulation.char",
					"org.eclipse.jdt.core.formatter.tabulation.size",
					"org.eclipse.jdt.core.formatter.tabulation.char",
					"org.eclipse.jdt.core.formatter.comment.line_length",
					"org.eclipse.jdt.core.formatter.lineSplit")) {
				bestOption = optimizeOption(pathToFile, bestOption, option);
			}

			Set<String> parametersToSwitch = new TreeSet<>(bestOption.getOption().keySet());

			// This is a greedy algorithm, trying to find the Set of options minimizing the diff with existing files
			boolean hasMutated = false;
			do {
				hasMutated = false;
				for (String parameterToSwitch : parametersToSwitch) {
					ScoredOption<Map<String, String>> newBestOption =
							optimizeOption(pathToFile, bestOption, parameterToSwitch);

					if (!bestOption.getOption().equals(newBestOption.getOption())) {
						bestOption = newBestOption;
						hasMutated = true;
					}
				}

				if (hasMutated) {
					// We go through another pass as it is possible a new tweak lead to other tweaks having different
					// impacts
					// e.g. changing the max length of rows leads many other rules to behave differently
					LOGGER.info(
							"The configuration has muted: we will go again through all options to look for a better set of options");
				}
			} while (hasMutated);

			logDiffWithPepper(pathToFile, bestOption);

			stylesheetGenerator.writeConfigurationToTmpPath(bestOption.getOption());
		}
	}

	public static void logDiffWithPepper(Map<Path, String> pathToFile, ScoredOption<Map<String, String>> bestOption) {
		Map<String, String> pepperConvention;
		{
			pepperConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/pepper-eclipse-code-formatter.xml"))
					.getOptions();
		}

		MapDifference<String, String> diff = Maps.difference(pepperConvention, bestOption.getOption());
		diff.entriesDiffering().forEach((k, v) -> {
			LOGGER.info("{} -> {}", k, v);
		});

		EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(pepperConvention);
		EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);

		long pepperDiffScoreDiff = computeDiffScore(formatter, pathToFile.values());
		LOGGER.info("Pepper diff: {}", pepperDiffScoreDiff);
	}

	/**
	 * 
	 * @param pathToFile
	 * @return the best configuration amongst a small set of standard configurations
	 */
	public static ScoredOption<Map<String, String>> findBestDefaultOption(Map<Path, String> pathToFile) {
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
					.getOptions();
		}
		Map<String, String> springConvention;
		{
			springConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/spring-eclipse-code-formatter.xml"))
					.getOptions();
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

	public static void logBestDefaultConfig(Map<String, String> eclipseDefault,
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
		LOGGER.info("Best default configuration: {}", bestOptionName);
	}

	public static ScoredOption<Map<String, String>> optimizeOption(Map<Path, String> pathToFile,
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

			long tweakedDiffScoreDiff = computeDiffScore(formatter, pathToFile.values());

			return Optional.of(new ScoredOption<Map<String, String>>(tweakedConfiguration, tweakedDiffScoreDiff));
		}).flatMap(Optional::stream).min(Comparator.comparingLong(ScoredOption::getScore));

		if (optMin.isPresent() && optMin.get().getScore() < bestOption.getScore()) {
			LOGGER.info("Tweaked diff improves score: {} ({})", optMin.get().getScore(), parameterToSwitch);
			return optMin.get();
		}

		return bestOption;
	}

	public static Map<Path, String> loadConcernedFiles(Path rootForFiles, Pattern fileMatcher) throws IOException {
		LOGGER.info("Loading files content");
		Map<Path, String> pathToFile = new ConcurrentHashMap<>();
		Files.walk(rootForFiles).forEach(path -> {
			if (path.toFile().isFile() && fileMatcher.matcher(path.toString()).matches()) {
				try {
					String pathAsString = Files.readString(path);

					pathToFile.put(path, pathAsString);
				} catch (IOException e) {
					LOGGER.warn("Issue loading " + path, e);
				}
			} else {
				LOGGER.debug("Rejected: {}", path);
			}
		});
		LOGGER.info("Loaded files content");
		return pathToFile;
	}

	// see DefaultCodeFormatterOptions
	private static Set<String> possibleOptions(String parameterToSwitch) {
		if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.enabling_tag")
				|| parameterToSwitch.equals("org.eclipse.jdt.core.formatter.disabling_tag")) {
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
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.align_fields_grouping_blank_lines")) {
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
		} else if (
		// The field is named comment_format_source
		parameterToSwitch.equals("org.eclipse.jdt.core.formatter.comment.format_source_code")
				// The field is named keep_guardian_clause_on_one_line
				|| parameterToSwitch.equals("org.eclipse.jdt.core.formatter.format_guardian_clause_on_one_line")
				// The field is named comment_format_line_comment_starting_on_first_column
				|| parameterToSwitch
						.equals("org.eclipse.jdt.core.formatter.format_line_comment_starting_on_first_column")
				// The field is named use_tags
				|| parameterToSwitch.equals("org.eclipse.jdt.core.formatter.use_on_off_tags")) {
			// edge-cases handled manually
			return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.comment.line_length")
				|| parameterToSwitch.equals("org.eclipse.jdt.core.formatter.lineSplit")) {
			// We assume a limited set of standard values
			return IntStream.range(8, 25).map(i -> i * 10).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.continuation_indentation")
				|| parameterToSwitch
						.equals("org.eclipse.jdt.core.formatter.continuation_indentation_for_array_initializer")) {
			// This is the indentation when a single statement is split on multiple lines
			// We assume a limited set of standard values
			return IntStream.range(0, 5).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.indentation.size")) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 4).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.keep_")) {
			if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.keep_code_block_on_one_line")) {
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
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve")) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 4).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.parentheses_positions_in_")) {
			return Set.of(DefaultCodeFormatterConstants.COMMON_LINES,
					DefaultCodeFormatterConstants.SEPARATE_LINES_IF_NOT_EMPTY,
					DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
					DefaultCodeFormatterConstants.SEPARATE_LINES,
					DefaultCodeFormatterConstants.PRESERVE_POSITIONS);
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.tabulation.char")) {
			return Set.of(JavaCore.TAB, JavaCore.SPACE, DefaultCodeFormatterConstants.MIXED);
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.tabulation.size")) {
			// We assume a limited set of standard values
			return IntStream.rangeClosed(0, 8).mapToObj(String::valueOf).collect(Collectors.toSet());
		} else if (parameterToSwitch.equals("org.eclipse.jdt.core.formatter.text_block_indentation")) {
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
				try {
					Field field = DefaultCodeFormatterOptions.class.getField(parameterName);

					if (field.getType() == boolean.class) {
						return Set.of(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);
					}
				} catch (NoSuchFieldException | SecurityException e) {
					LOGGER.debug("Introspection strategy failed for " + parameterToSwitch, e);

					if (parameterName.endsWith("_comments")) {
						LOGGER.debug("Many fields ending with 'comments' are misspelled");
					} else {
						LOGGER.info("Introspection strategy failed for " + parameterToSwitch);
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
						LOGGER.debug("Introspection strategy failed for " + parameterToSwitch, e);
						LOGGER.info("Introspection strategy failed for " + parameterToSwitch);
					}
				}
			}

			LOGGER.warn("Parameter not managed: {}", parameterToSwitch);
			return Set.of();
		}
	}

	@Deprecated
	public static long computeDiffScore(EclipseJavaFormatter formatter) throws IOException, PatchFailedException {
		File file = new File("src/main/java/" + GenerateEclipseStylesheet.class.getName().replace(".", "/") + ".java");
		LOGGER.debug("Process: {}", file);

		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}

		String pathAsString = Files.readString(file.toPath());
		return computeDiffScore(formatter, pathAsString);
	}

	private static long computeDiffScore(EclipseJavaFormatter formatter, Collection<String> values) {
		return values.parallelStream().mapToLong(content -> {
			try {
				return computeDiffScore(formatter, content);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).sum();
	}

	public static long computeDiffScore(EclipseJavaFormatter formatter, String pathAsString) throws IOException {
		String formatted = formatter.doFormat(pathAsString, LineEnding.KEEP);

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
