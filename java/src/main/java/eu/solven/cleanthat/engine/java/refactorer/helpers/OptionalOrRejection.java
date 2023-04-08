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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * This may return an {@link Optional} result, or a rejection flag. It is useful to prevent having complex code
 * difficult to wrap in a method.
 * 
 * @author Benoit Lacelle
 *
 */
public class OptionalOrRejection<T> {
	final boolean rejected;
	final Optional<T> optResult;

	protected OptionalOrRejection(boolean rejected, Optional<T> optResult) {
		this.rejected = rejected;
		this.optResult = optResult;
	}

	public boolean isRejected() {
		return rejected;
	}

	public Optional<T> getOptional() {
		return optResult;
	}

	public static <T> OptionalOrRejection<T> reject() {
		return new OptionalOrRejection<>(true, Optional.empty());
	}

	public static <T> OptionalOrRejection<T> empty() {
		return new OptionalOrRejection<>(false, Optional.empty());
	}

	public static <T> OptionalOrRejection<T> present(T result) {
		return new OptionalOrRejection<>(false, Optional.of(result));
	}

	public static <T> OptionalOrRejection<T> optional(Optional<T> optional) {
		return new OptionalOrRejection<>(false, optional);
	}

	public void ifPresent(Consumer<T> consumer) {
		optResult.ifPresent(consumer);
	}
}
