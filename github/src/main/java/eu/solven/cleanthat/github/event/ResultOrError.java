package eu.solven.cleanthat.github.event;

import java.util.Optional;

/**
 * Helps working with method which may fail without throwing an Exception
 * 
 * @author Benoit Lacelle
 *
 * @param <R>
 * @param <E>
 */
public class ResultOrError<R, E> {
	final Optional<R> optResult;
	final Optional<E> optError;

	public ResultOrError(Optional<R> optResult, Optional<E> optError) {
		this.optResult = optResult;
		this.optError = optError;

		if (optResult.isEmpty() && optError.isEmpty()) {
			throw new IllegalArgumentException("Need a result or an error");
		} else if (optResult.isPresent() && optError.isPresent()) {
			throw new IllegalArgumentException("Can not have both a result and an error");
		}
	}

	public Optional<R> getOptResult() {
		return optResult;
	}

	public Optional<E> getOptError() {
		return optError;
	}

	public static <R, E> ResultOrError<R, E> result(R result) {
		return new ResultOrError<R, E>(Optional.of(result), Optional.empty());
	}

	public static <R, E> ResultOrError<R, E> error(E error) {
		return new ResultOrError<R, E>(Optional.empty(), Optional.of(error));
	}

}
