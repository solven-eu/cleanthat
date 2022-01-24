package eu.solven.cleanthat.config;

/**
 * Similarly to maven mojo, any language/processor should be skippable
 * 
 * @author Benoit Lacelle
 *
 */
public interface ISkippable {
	String KEY_SKIP = "skip";

	boolean isSkip();
}
