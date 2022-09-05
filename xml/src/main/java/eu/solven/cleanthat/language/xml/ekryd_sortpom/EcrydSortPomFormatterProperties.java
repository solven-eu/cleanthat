package eu.solven.cleanthat.language.xml.ekryd_sortpom;

import eu.solven.cleanthat.language.xml.DefaultXmlFormatterProperties;

/**
 * Configuration for EC4J Xml formatter
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings({ "PMD.TooManyFields", "PMD.FieldDeclarationsShouldBeAtStartOfClass" })
public class EcrydSortPomFormatterProperties extends DefaultXmlFormatterProperties {

	// See defaults in AbstractImpSortMojo

	/** Should a backup copy be created for the sorted pom. */
	boolean createBackupFile = false;

	/** Name of the file extension for the backup file. */
	String testPomBackupExtension = ".bak";
	String violationFile = null;
	/** Whether to keep the file timestamps of old POM file when creating new POM file. */
	boolean keepTimestamp = false;

	// LineEnding lineSeparator = sourceCodeProperties.getLineEndingAsEnum();

	/**
	 * Should empty xml elements be expanded or not. Example: &lt;configuration&gt;&lt;/configuration&gt; or
	 * &lt;configuration/&gt;
	 */
	boolean expandEmptyElements = true;
	/**
	 * Should non-expanded empty xml element have space before closing tag. Example: &lt;configuration /&gt; or
	 * &lt;configuration/&gt;
	 */
	boolean spaceBeforeCloseEmptyElement = false;
	/**
	 * Should blank lines in the pom-file be preserved. A maximum of one line is preserved between each tag.
	 */
	boolean keepBlankLines = true;

	/**
	 * Number of space characters to use as indentation. A value of -1 indicates that tab character should be used
	 * instead.
	 */
	// int nrOfIndentSpace = properties.getIndent();
	protected int getNrOfIndentSpace() {
		return this.getIndentationAsWhitespaces();
	}

	/** Should blank lines (if preserved) have indentation. */
	boolean indentBlankLines = false;
	/**
	 * Should the schema location attribute of project (top level xml element) be placed on a new line. The attribute
	 * will be indented (2 * nrOfIndentSpace + 1 space) characters.
	 */
	boolean indentSchemaLocation = false;

	/**
	 * Comma-separated ordered list how dependencies should be sorted. Example: scope,groupId,artifactId. If scope is
	 * specified in the list then the scope ranking is IMPORT, COMPILE, PROVIDED, SYSTEM, RUNTIME and TEST. The list can
	 * be separated by ",;:"
	 */
	String sortDependencies = null;
	/**
	 * Comma-separated ordered list how exclusions, for dependencies, should be sorted. Example: groupId,artifactId The
	 * list can be separated by ",;:"
	 */
	String sortDependencyExclusions = null;
	/**
	 * Comma-separated ordered list how dependencies in dependency management should be sorted. Example:
	 * scope,groupId,artifactId. If scope is specified in the list then the scope ranking is IMPORT, COMPILE, PROVIDED,
	 * SYSTEM, RUNTIME and TEST. The list can be separated by ",;:". It would take precedence if present and would fall
	 * back to {@link #sortDependencies} if not present. The value NONE can be used to avoid sorting dependency
	 * management at all.
	 */
	String sortDependencyManagement = null;
	/**
	 * Comma-separated ordered list how plugins should be sorted. Example: groupId,artifactId The list can be separated
	 * by ",;:"
	 */
	String sortPlugins = null;
	/**
	 * Should the Maven pom properties be sorted alphabetically. Affects both project/properties and
	 * project/profiles/profile/properties
	 */
	boolean sortProperties = false;
	/** Should the Maven pom sub modules be sorted alphabetically. */
	boolean sortModules = false;
	/** Should the Maven pom execution sections be sorted by phase and then alphabetically. */
	boolean sortExecutions = false;

	/** Custom sort order file. */
	String sortOrderFile = null;
	/** Choose between a number of predefined sort order files. */
	String predefinedSortOrder = "recommended_2008_06";

	// Used in Check, not in Sort
	// String verifyFail;
	// String verifyFailOn;

	/** Ignore line separators when comparing current POM with sorted one */
	boolean ignoreLineSeparators = true;

}
