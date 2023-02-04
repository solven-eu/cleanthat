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
package eu.solven.cleanthat.engine.java.eclipse.generator;

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

	@Override
	public String toString() {
		// Do not print the options as it can be quite large
		return "Option score: " + score;
	}
}
