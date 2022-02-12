package eu.solven.cleanthat.language.java.eclipse.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatterConfiguration;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Default implementation for {@link IEclipseStylesheetGenerator}
 * 
 * @author Benoit Lacelle
 *
 */
// Convert from Checkstyle
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/jobs/TransformCheckstyleRulesJob.java
@SuppressWarnings("PMD.GodClass")
public class EclipseStylesheetGenerator implements IEclipseStylesheetGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseStylesheetGenerator.class);

	private static final String SETTING_TABULATION_CHAR = "org.eclipse.jdt.core.formatter.tabulation.char";

	final CodeDiffHelper diffHelper = new CodeDiffHelper();

	// This is useful to start optimizing these parameters, before optimizing other
	// parameters which behavior depends on these first parameters
	protected Set<String> getMostImpactfulSettings() {
		return ImmutableSet.of(SETTING_TABULATION_CHAR,
				"org.eclipse.jdt.core.formatter.tabulation.size",
				"org.eclipse.jdt.core.formatter.comment.line_length",
				"org.eclipse.jdt.core.formatter.lineSplit",
				"org.eclipse.jdt.core.formatter.join_wrapped_lines");
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
	public Map<String, String> generateSettings(OffsetDateTime timeout, Map<Path, String> pathToFile) {
		ScoredOption<Map<String, String>> bestDefaultConfig = findBestDefaultSetting(timeout, pathToFile);

		if (pathToFile.isEmpty()) {
			return bestDefaultConfig.getOption();
		}

		// Search for an optimal configuration given the biggest file
		// If there is only 1 or 2 files to process, skip the single_file preparation
		// step
		if (pathToFile.size() >= 2) {
			Map.Entry<Path, String> biggestFile = findBiggestFile(pathToFile);

			LOGGER.info("Prepare the configuration over the (biggest) file: {} ({})",
					biggestFile.getKey(),
					PepperLogHelper.humanBytes(biggestFile.getValue().length()));

			Map<Path, String> biggestFileAsMap = Collections.singletonMap(biggestFile.getKey(), biggestFile.getValue());

			EclipseJavaFormatter formatter =
					new EclipseJavaFormatter(new EclipseJavaFormatterConfiguration(bestDefaultConfig.getOption()));
			long singleFileScore = computeDiffScore(formatter, biggestFileAsMap.values());
			LOGGER.info("On this biggest file, the score for the best default configuration is {}", singleFileScore);

			bestDefaultConfig = searchForOptimalConfiguration(timeout,
					biggestFileAsMap,
					new ScoredOption<Map<String, String>>(bestDefaultConfig.getOption(), singleFileScore));
		}

		// Now we have an optimal configuration for the biggest file, try processing all
		// other files
		LOGGER.info("Prepare the configuration over all files: {}", pathToFile.size());
		return searchForOptimalConfiguration(timeout, pathToFile, bestDefaultConfig).getOption();
	}

	private Map.Entry<Path, String> findBiggestFile(Map<Path, String> pathToFile) {
		if (pathToFile.isEmpty()) {
			throw new IllegalStateException("There is not a single java file");
		}

		Optional<Entry<Path, String>> optSrcMainJavaBiggest = pathToFile.entrySet()
				.stream()
				.filter(e -> e.getKey().toString().contains("src/main/java"))
				.max(Comparator.comparingLong(e -> e.getValue().length()));

		Map.Entry<Path, String> plainBiggest =
				pathToFile.entrySet().stream().max(Comparator.comparingLong(e -> e.getValue().length())).get();

		if (optSrcMainJavaBiggest.isEmpty()) {
			LOGGER.info("There is no file in '{}'", "src/main/java");
			return plainBiggest;
		} else {
			Map.Entry<Path, String> srcMainJavaBiggest = optSrcMainJavaBiggest.get();
			if (plainBiggest.getValue().length() > srcMainJavaBiggest.getValue().length()) {
				// We prefer a smaller applicative file, than a bigger test file
				LOGGER.info("We prefer relying on {} ({}) than {} ({})",
						srcMainJavaBiggest.getKey(),
						PepperLogHelper.humanBytes(srcMainJavaBiggest.getValue().length()),
						plainBiggest.getKey(),
						PepperLogHelper.humanBytes(plainBiggest.getValue().length()));
			}
			return srcMainJavaBiggest;
		}
	}

	/**
	 * 
	 * @param timeout
	 *            at this point of time, we'll interrupt the process
	 * @param pathToFile
	 * @param bestDefaultConfig
	 * @return
	 */
	public ScoredOption<Map<String, String>> searchForOptimalConfiguration(OffsetDateTime timeout,
			Map<Path, String> pathToFile,
			ScoredOption<Map<String, String>> bestDefaultConfig) {
		ScoredOption<Map<String, String>> bestSettings = bestDefaultConfig;

		// Start optimizing comment parameters
		// It is useful as may lines/diff score may come from comments
		bestSettings = optimizeSetOfSettings(timeout, pathToFile, bestSettings, getCommentSettings());

		// Start optimizing some crucial parameters
		bestSettings = optimizeSetOfSettings(timeout, pathToFile, bestSettings, getMostImpactfulSettings());

		// Then we optimize parameters which differs from standard configurations: these are parameters which are often
		// changed
		Set<String> commonlyChangedSettings = getSettingsChangingThroughStandardConfig();
		bestSettings = optimizeSetOfSettings(timeout, pathToFile, bestSettings, commonlyChangedSettings);

		// Do not limit ourselves to settings in the current bestSetting, as it may not be exhaustive
		Set<String> settingsToSwitch = getAllSetttings();
		bestSettings = optimizeSetOfSettings(timeout, pathToFile, bestSettings, settingsToSwitch);

		logPathsImpactedByConfiguration(pathToFile, bestSettings);

		return bestSettings;
	}

	private Set<String> getCommentSettings() {
		return getAllSetttings().stream().filter(s -> s.contains(".comment.")).collect(Collectors.toSet());
	}

	protected Set<String> getAllSetttings() {
		Map<String, Map<String, String>> keyToConfig = loadDefaultConfigurations();
		return keyToConfig.values()
				.stream()
				.flatMap(s -> s.keySet().stream())
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private Set<String> getSettingsChangingThroughStandardConfig() {
		Map<String, Map<String, String>> keyToConfig = loadDefaultConfigurations();

		SetMultimap<String, String> settingToDefaultValues = MultimapBuilder.treeKeys().hashSetValues().build();

		// Register standard configurations settings to detect which parameters are often changed
		keyToConfig.values()
				.stream()
				.flatMap(m -> m.entrySet().stream())
				.forEach(e -> settingToDefaultValues.put(e.getKey(), e.getValue()));

		Set<String> commonlyChangedSettings = settingToDefaultValues.asMap()
				.entrySet()
				.stream()
				.sorted((l, r) -> Integer.compare(l.getValue().size(), r.getValue().size()))
				// Filter parameters which are not identical through all standard configurations
				.filter(e -> e.getValue().size() >= 2)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
		return commonlyChangedSettings;
	}

	protected ScoredOption<Map<String, String>> optimizeSetOfSettings(OffsetDateTime timeout,
			Map<Path, String> pathToFile,
			ScoredOption<Map<String, String>> bestSettings,
			Set<String> settingsToSwitch) {
		analyticsOverOptions(settingsToSwitch);

		// Process one by one only options still appearing as relevant
		bestSettings = optimizeParametersOneByOne(timeout, pathToFile, bestSettings, settingsToSwitch);
		return bestSettings;
	}

	private void analyticsOverOptions(Set<String> settingsToSwitch) {
		SetMultimap<String, String> rawOptionToValues = MultimapBuilder.treeKeys().hashSetValues().build();

		settingsToSwitch.forEach(key -> {
			rawOptionToValues.putAll(key, possibleOptions(key));
		});

		SetMultimap<String, String> optionToValues = ImmutableSetMultimap.copyOf(rawOptionToValues);

		long nbOptions = optionToValues.size();
		LOGGER.debug("nbOptions to consider: {}", nbOptions);
	}

	private ScoredOption<Map<String, String>> optimizeParametersOneByOne(OffsetDateTime timeout,
			Map<Path, String> pathToFile,
			ScoredOption<Map<String, String>> bestSettings,
			Set<String> settingsToSwitch) {
		// This is a greedy algorithm, trying to find the Set of options minimizing the
		// diff with existing files
		// We iterate targeting to reach a score of 0 (meaning we spot a configuration
		// matching exactly current code-style)
		Set<String> improvedSettings = new TreeSet<>();
		do {
			// Set<String> previousHasMutated = new TreeSet<>(hasMutated);
			improvedSettings.clear();

			// This assumes there is no N-tuple impacting the code, where each individual
			// change would impact the code
			// Hence, we can process each parameter independently
			for (String settingToSwitch : settingsToSwitch) {
				LOGGER.debug("Setting about to be optimized: {}", settingToSwitch);
				ScoredOption<Map<String, String>> newBestSettings =
						pickOptimalOption(pathToFile.values(), bestSettings, settingToSwitch);
				if (!bestSettings.getOption().equals(newBestSettings.getOption())) {
					bestSettings = newBestSettings;
					improvedSettings.add(settingToSwitch);
				}

				if (OffsetDateTime.now().isAfter(timeout)) {
					LOGGER.warn("We interrupt the optimization process as now() is after {}. Score={}",
							timeout,
							bestSettings.getScore());
					break;
				}
			}
			if (!improvedSettings.isEmpty()) {
				// We go through another pass as it is possible a new tweak lead to other tweaks
				// having different impacts
				// e.g. changing the max length of rows leads many other rules to behave
				// differently
				LOGGER.info(
						"The configuration has mutated: we will go again through all options to look for a better set of settings");
			}
		} while (!improvedSettings.isEmpty()
				// If score is 0, we found a perfect configuration
				&& bestSettings.getScore() > 0
				&& OffsetDateTime.now().isBefore(timeout));
		return bestSettings;
	}

	protected void logPathsImpactedByConfiguration(Map<Path, String> pathToFile,
			ScoredOption<Map<String, String>> bestOption) {
		if (bestOption.getScore() > 0) {
			LOGGER.warn("We did not succeed crafting a configuration matching perfectly existing code");

			EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(bestOption.getOption());
			EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);

			pathToFile.entrySet().stream().filter(entry -> {
				long tweakedDiffScoreDiff = computeDiffScore(formatter, Collections.singleton(entry.getValue()));

				return tweakedDiffScoreDiff > 0;
			}).forEach(entry -> {
				LOGGER.warn("Path needing formatting with 'optimal' configuration: {}", entry.getKey());
			});
		}
	}

	/**
	 * @param max
	 * @param pathToFile
	 * @return the best configuration amongst a small set of standard configurations
	 */
	protected ScoredOption<Map<String, String>> findBestDefaultSetting(OffsetDateTime max,
			Map<Path, String> pathToFile) {
		Map<String, Map<String, String>> keyToConfig = loadDefaultConfigurations();

		ScoredOption<Map<String, String>> bestDefaultConfig = keyToConfig.entrySet().stream().parallel().map(e -> {
			Map<String, String> config = e.getValue();
			if (OffsetDateTime.now().isAfter(max)) {
				LOGGER.warn("We skip {} as now() is after {}", e.getKey(), max);
			}

			EclipseJavaFormatter formatter = new EclipseJavaFormatter(new EclipseJavaFormatterConfiguration(config));
			long score = computeDiffScore(formatter, pathToFile.values());
			return new ScoredOption<Map<String, String>>(new TreeMap<String, String>(config), score);
		}).min(Comparator.comparingLong(ScoredOption::getScore)).get();
		logBestDefaultConfig(keyToConfig, bestDefaultConfig);
		return bestDefaultConfig;
	}

	private long computeDiffScore(EclipseJavaFormatter formatter, Collection<String> values) {
		return diffHelper.computeDiffScore(formatter, values);
	}

	protected Map<String, Map<String, String>> loadDefaultConfigurations() {
		Map<String, Map<String, String>> keyToConfig = new TreeMap<>();

		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getDefaultSettings();
			keyToConfig.put("default", defaultSettings.getMap());
		}

		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getEclipseDefaultSettings();
			keyToConfig.put("eclipse", defaultSettings.getMap());
		}

		{
			DefaultCodeFormatterOptions defaultSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
			keyToConfig.put("java-convention", defaultSettings.getMap());
		}

		{
			Map<String, String> googleConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/eclipse-java-google-style.xml"))
					.getSettings();
			keyToConfig.put("google", googleConvention);
		}

		{
			Map<String, String> springConvention = EclipseJavaFormatterConfiguration
					.loadResource(new ClassPathResource("/eclipse/spring-eclipse-code-formatter.xml"))
					.getSettings();
			keyToConfig.put("spring", springConvention);
		}

		// https://stackoverflow.com/questions/3754405/best-eclipse-code-formatters
		// Many people seems not to appreciate much mixed
		// The issue with mixed is that a small issue in the source code will lead to prefering mixed instead of spaces
		// or tabs (as this change will lead to difference with the original code, while mixed will keep the spaces and
		// tabs)
		prepareConfig(keyToConfig);

		return keyToConfig;
	}

	protected void prepareConfig(Map<String, Map<String, String>> keyToConfig) {
		keyToConfig.entrySet()
				.stream()
				.filter(e -> DefaultCodeFormatterConstants.MIXED.equals(e.getValue().get(SETTING_TABULATION_CHAR)))
				.forEach(e -> {
					String configName = e.getKey();
					LOGGER.info("Config {} has {}='{}'. We force it to '{}'",
							configName,
							SETTING_TABULATION_CHAR,
							DefaultCodeFormatterConstants.MIXED,
							JavaCore.SPACE);

					e.getValue().put(SETTING_TABULATION_CHAR, JavaCore.SPACE);
				});
	}

	public void logBestDefaultConfig(Map<String, Map<String, String>> keyToConfig,
			ScoredOption<Map<String, String>> bestDefaultConfig) {
		Map<String, String> selectedOption = bestDefaultConfig.getOption();
		String bestOptionName = getDefaultOptionName(keyToConfig, selectedOption);
		LOGGER.info("Best standard configuration: {} (score={})", bestOptionName, bestDefaultConfig.getScore());
	}

	private String getDefaultOptionName(Map<String, Map<String, String>> keyToConfig,
			Map<String, String> selectedOption) {
		return keyToConfig.entrySet()
				.stream()
				.filter(e -> e.getValue().equals(selectedOption))
				.findFirst()
				.map(Map.Entry::getKey)
				.orElse("???");
	}

	/**
	 * 
	 * @param contents
	 * @param initialOptions
	 * @param parameterToSwitch
	 * @return the best option, excluding the current setting.
	 */
	public ScoredOption<Map<String, String>> pickOptimalOption(Collection<String> contents,
			ScoredOption<Map<String, String>> initialOptions,
			String parameterToSwitch) {
		Set<String> possibleOptions = possibleOptions(parameterToSwitch);

		ScoredOption<Map<String, String>> output =
				pickOptimalOption(contents, initialOptions, parameterToSwitch, possibleOptions);

		if (SETTING_TABULATION_CHAR.equals(parameterToSwitch)) {
			String choseOption = output.getOption().get(parameterToSwitch);

			if (DefaultCodeFormatterConstants.MIXED.equals(choseOption)) {
				LOGGER.warn("{}={} is a signal of a awkwardly formatted initial state",
						SETTING_TABULATION_CHAR,
						DefaultCodeFormatterConstants.MIXED);

				// We exclude 'DefaultCodeFormatterConstants.MIXED' as it is generally a badly formatted code
				// i.e. if by optimizing, we choose 'mixed', it means the input code has both spaces and tabs
				// We prefer choosing the optimal between spaces and tabs
				// The user can still manually force 'mixed'
				possibleOptions = new TreeSet<>(possibleOptions);
				possibleOptions.remove(DefaultCodeFormatterConstants.MIXED);

				LOGGER.warn("We force choosing an option out of {}", DefaultCodeFormatterConstants.MIXED);
				output = pickOptimalOption(contents, initialOptions, parameterToSwitch, possibleOptions);
				String forcedOption = output.getOption().get(parameterToSwitch);
				LOGGER.warn("We forced: {}", forcedOption);
			}
		}

		return output;
	}

	private ScoredOption<Map<String, String>> pickOptimalOption(Collection<String> contents,
			ScoredOption<Map<String, String>> initialOptions,
			String parameterToSwitch,
			Set<String> possibleOptions) {
		LOGGER.debug("Considering parameter: {} ({} candidates)", parameterToSwitch, possibleOptions.size());
		Optional<ScoredOption<Map<String, String>>> optMin = possibleOptions.parallelStream().map(possibleValue -> {
			Map<String, String> tweakedConfiguration = new TreeMap<>(initialOptions.getOption());
			String currentBestOption = tweakedConfiguration.put(parameterToSwitch, possibleValue);
			if (currentBestOption == null) {
				LOGGER.debug("This happens when we consider a parameter not explicit in the current settings");
			} else if (currentBestOption.equals(possibleValue)) {
				// No-need to check with current value
				return Optional.<ScoredOption<Map<String, String>>>empty();
			}
			ScoredOption<Map<String, String>> scoredOption = computeScore(contents, tweakedConfiguration);
			return Optional.of(scoredOption);
		}).flatMap(Optional::stream).min(Comparator.comparingLong(ScoredOption::getScore));
		long initialScore = initialOptions.getScore();

		ScoredOption<Map<String, String>> output;
		if (optMin.isPresent()) {
			long optimizedScore = optMin.get().getScore();
			if (optimizedScore < initialScore) {
				String oldValue = initialOptions.getOption().get(parameterToSwitch);
				String newValue = optMin.get().getOption().get(parameterToSwitch);
				LOGGER.info("Score optimized from {} to {} ({} from '{}' to '{}')",
						initialScore,
						optimizedScore,
						parameterToSwitch,
						oldValue,
						newValue);
				output = optMin.get();
			} else {
				output = initialOptions;
			}
		} else {
			output = initialOptions;
		}
		return output;
	}

	protected ScoredOption<Map<String, String>> computeScore(Collection<String> contents,
			Map<String, String> tweakedConfiguration) {
		EclipseJavaFormatterConfiguration config = new EclipseJavaFormatterConfiguration(tweakedConfiguration);
		EclipseJavaFormatter formatter = new EclipseJavaFormatter(config);
		long tweakedDiffScoreDiff = computeDiffScore(formatter, contents);
		ScoredOption<Map<String, String>> scoredOption =
				new ScoredOption<Map<String, String>>(tweakedConfiguration, tweakedDiffScoreDiff);
		return scoredOption;
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
			// see
			// org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions.getAlignment(int)
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
			// } else if
			// (parameterToSwitch.startsWith("org.eclipse.jdt.core.formatter.align_")
			// && parameterToSwitch.endsWith("_on_columns")) {
			// return Set.of(DefaultCodeFormatterConstants.TRUE,
			// DefaultCodeFormatterConstants.FALSE);
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
		} else if (SETTING_TABULATION_CHAR.equals(parameterToSwitch)) {
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
					// There is some spelling issues, and sometimes comment is put in plural in the
					// field
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

	@VisibleForTesting
	public CodeDiffHelper getCodeDiffHelper() {
		return diffHelper;
	}
}
