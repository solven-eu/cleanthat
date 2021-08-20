package eu.solven.cleanthat.language.java.eclipse;

/**
 * Couple an option with its score
 * 
 * @author Benoit Lacelle
 *
 * @param <T>
 */
public class ScoredOption<T> {
	final T option;
	final long score;

	public ScoredOption(T option, long score) {
		this.option = option;
		this.score = score;
	}

	public T getOption() {
		return option;
	}

	public long getScore() {
		return score;
	}

}
