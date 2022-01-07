package eu.solven.cleanthat.language.kotlin.ktlint;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Configuration for ScalaFmt formatter
 * 
 * See https://github.com/pinterest/ktlint
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class KtlintProperties {
}
