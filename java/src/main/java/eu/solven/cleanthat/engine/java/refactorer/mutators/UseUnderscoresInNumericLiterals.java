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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns 'int i = 1234567’ into ’int i = 1_234_567'
 *
 * @author Benoit Lacelle
 */
public class UseUnderscoresInNumericLiterals extends AJavaparserMutator {
	// We groups digits per block of thousands
	private static final int BLOCK_SIZE = 3;

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#useunderscoresinnumericliterals";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseUnderscoresInNumericLiterals");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2148");
	}

	/**
	 * Default is 4, as configured by PMD. Sonar tells requires 6 digits for decimal-base numbers
	 * 
	 * @return Length under which literals in base 10 are not required to have underscores
	 * @see https://sonarsource.atlassian.net/browse/RSPEC-2148
	 */
	@SuppressWarnings("PMD.MagicNumber")
	protected int getAcceptableDecimalLength() {
		return 4;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof LiteralStringValueExpr)) {
			return false;
		}

		var literalStringExpr = (LiteralStringValueExpr) node;

		var asString = literalStringExpr.getValue();
		Optional<String> optNewValue;

		if (node instanceof IntegerLiteralExpr || node instanceof LongLiteralExpr) {

			var noUnderscore = asString.replaceAll("_", "");

			if (noUnderscore.matches("\\d+")) {
				var nbDigits = noUnderscore.length();
				var sb = new StringBuilder(nbDigits + nbDigits / BLOCK_SIZE);

				appendWithUnderscores(noUnderscore, sb);

				optNewValue = Optional.of(sb.toString());
			} else {
				optNewValue = Optional.empty();
			}
		} else if (node instanceof DoubleLiteralExpr) {
			var noUnderscore = asString.replaceAll("_", "");

			if (noUnderscore.matches("\\d+(\\.\\d+)?([dDfF])?")) {
				OptionalInt optTrailingChar;
				int lastChar = noUnderscore.charAt(noUnderscore.length() - 1);
				if (lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
					optTrailingChar = OptionalInt.of(lastChar);
				} else {
					optTrailingChar = OptionalInt.empty();
				}

				int lastIndex;
				if (optTrailingChar.isPresent()) {
					lastIndex = noUnderscore.length() - 1;
				} else {
					lastIndex = noUnderscore.length();
				}

				StringBuilder sb;

				var indexOfDot = noUnderscore.indexOf('.');
				if (indexOfDot <= 0) {
					var nbDigits = noUnderscore.length();
					sb = new StringBuilder(nbDigits + nbDigits / BLOCK_SIZE);

					appendWithUnderscores(noUnderscore.substring(0, lastIndex), sb);
				} else {
					var totalNbDigits = noUnderscore.length() - 1;
					sb = new StringBuilder(totalNbDigits + 1 + totalNbDigits / BLOCK_SIZE);

					{
						var beforeDot = noUnderscore.substring(0, indexOfDot);
						appendWithUnderscores(beforeDot, sb);
					}
					sb.append('.');
					{
						var afterDot = noUnderscore.substring(indexOfDot + 1, lastIndex);
						appendWithUnderscores(afterDot, sb, true);
					}
				}

				if (optTrailingChar.isPresent()) {
					sb.append((char) optTrailingChar.getAsInt());
				}

				optNewValue = Optional.of(sb.toString());

			} else {
				// this may be a complex double representation (e.g. scientific exponent)
				optNewValue = Optional.empty();
			}
		} else {
			optNewValue = Optional.empty();
		}

		if (optNewValue.isPresent()) {
			var newValue = optNewValue.get();

			if (newValue.equals(asString)) {
				return false;
			} else {
				literalStringExpr.setValue(newValue);
				return true;
			}
		} else {
			return false;
		}
	}

	private void appendWithUnderscores(String noUnderscore, StringBuilder sb) {
		appendWithUnderscores(noUnderscore, sb, false);
	}

	private void appendWithUnderscores(String noUnderscore, StringBuilder sb, boolean reverseForDecimals) {
		if (noUnderscore.length() <= getAcceptableDecimalLength()) {
			sb.append(noUnderscore);

			return;
		}

		assert noUnderscore.matches("\\d+");

		var nbDigits = noUnderscore.length();

		var position = 0;
		var nbDigitsLeft = nbDigits;
		while (nbDigitsLeft > 0) {
			if (position != 0) {
				sb.append('_');
			}

			int nextBlockSize;
			if (reverseForDecimals) {
				nextBlockSize = Math.min(nbDigitsLeft, BLOCK_SIZE);
			} else {
				nextBlockSize = (nbDigitsLeft - 1) % BLOCK_SIZE + 1;
			}
			var nextPosition = position + nextBlockSize;
			var subString = noUnderscore.substring(position, nextPosition);
			sb.append(subString);
			nbDigitsLeft -= nextBlockSize;
			position = nextPosition;
		}
	}

}
