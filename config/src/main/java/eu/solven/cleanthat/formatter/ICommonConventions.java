package eu.solven.cleanthat.formatter;

/**
 * Some convention may need to be shared in multiple places
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICommonConventions {
	// https://stackoverflow.com/questions/18698738/what-is-the-json-indentation-level-convention
	// '____' seems to be slightly more standard than '\t' or '__'
	String DEFAULT_INDENTATION = "    ";

	/**
	 * The number of ' ' in the default indentation (0 if the default indentation is based on '\t')
	 */
	int DEFAULT_INDENT_WHITESPACES = ICommonConventions.DEFAULT_INDENTATION.length();

	/**
	 * The number of ' ' indentations to mean we request a '\t' indentation
	 */
	int DEFAULT_INDENT_FOR_TAB = -1;
}
