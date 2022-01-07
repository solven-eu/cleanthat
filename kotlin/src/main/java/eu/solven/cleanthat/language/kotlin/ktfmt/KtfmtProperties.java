package eu.solven.cleanthat.language.kotlin.ktfmt;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Configuration for Ktfmt formatter
 * 
 * See https://github.com/facebookincubator/ktfmt
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class KtfmtProperties {
}
