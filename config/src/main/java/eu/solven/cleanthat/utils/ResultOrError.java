/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.utils;

import java.util.Optional;

/**
 * Helps working with method which may fail without throwing an Exception
 * 
 * @author Benoit Lacelle
 *
 * @param <R>
 * @param <E>
 */
public final class ResultOrError<R, E> {
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
