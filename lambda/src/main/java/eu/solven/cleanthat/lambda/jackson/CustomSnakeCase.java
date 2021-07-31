package eu.solven.cleanthat.lambda.jackson;

import java.util.Locale;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * Helps interpreting fields in capital case
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/30205006/why-does-jackson-2-not-recognize-the-first-capital-letter-if-the-leading-camel-c
public class CustomSnakeCase extends PropertyNamingStrategies.NamingBase {
	private static final long serialVersionUID = -537037017048878391L;

	@Override
	public String translate(String input) {
		if (input == null)
			return input; // garbage in, garbage out

		return input.toUpperCase(Locale.US);
	}
}