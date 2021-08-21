package eu.solven.cleanthat.formatter;

/**
 * Some {@link ILintFixer} needs a {@link IStyleEnforcer} to prevent them applying unwanted changed of code. It is the
 * case of Javaparser-based {@link ILintFixer} as JavaParser will apply unexpected changes (e.g. dropping consecutive
 * EOL).
 *
 * @author Benoit Lacelle
 */
public interface ILintFixerHelpedByCodeStyleFixer extends ILintFixer {
	void registerCodeStyleFixer(IStyleEnforcer codeStyleFixer);
}
