package eu.solven.cleanthat.formatter;

/**
 * This is a special type of {@link ILintFixer} as it induces no change of logic. In other words, it manipulates only
 * whitespaces characters in a way to reformat the code, without changing anything to the compiled-code.
 *
 * @author Benoit Lacelle
 */
public interface IStyleEnforcer extends ILintFixer {

}
