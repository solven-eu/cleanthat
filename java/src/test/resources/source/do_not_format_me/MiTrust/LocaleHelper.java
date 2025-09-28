/*
 * Copyright © 2021 M-iTrust (cto@m-itrust.com). Unauthorized copying of this file, via any medium is strictly prohibited. Proprietary and confidential
 */
package io.mitrust.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Locale.IsoCountryCode;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AtomicLongMap;

/**
 * Helps working with {@link Locale}
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class LocaleHelper {

	private static final Supplier<Set<String>> ISO_LANGUAGES =
			Suppliers.memoize(() -> ImmutableSet.copyOf(Locale.getISOLanguages()));

	private static final Supplier<Set<String>> ISO_COUNTRIES =
			Suppliers.memoize(() -> ImmutableSet.copyOf(Locale.getISOCountries(IsoCountryCode.PART1_ALPHA2)));

	// We do not use '#' else it may confuse GET parameters
	public static final String SEPARATOR_I18N_VARIANT = ">";
	public static final String SUFFIX_I18N_NDSP = "_newds";

	protected LocaleHelper() {
		// hidden
	}

	public static Set<String> getIsoLanguages() {
		return ISO_LANGUAGES.get();
	}

	public static List<String> splitLanguages(String language) {
		String[] splitted = language.split(SEPARATOR_I18N_VARIANT);

		if (splitted.length == 0 || splitted.length == 1 && splitted[0].isEmpty()) {
			throw new IllegalArgumentException("A language can not be empty ('')");
		}

		return Arrays.asList(splitted);
	}

	public static String makeNdspLanguage(Locale locale) {
		String language = locale.getLanguage();
		return makeNdspVariant(locale) + SEPARATOR_I18N_VARIANT + language;
	}

	public static String makeNdspVariant(Locale locale) {
		String language = locale.getLanguage();

		if (language.endsWith(SUFFIX_I18N_NDSP)) {
			// Already an NDSP language
			return language;
		}

		return language + SUFFIX_I18N_NDSP;
	}

	public static String getLanguageFromCountry(String country) {
		checkCountry(country);

		Optional<Locale> optSimpleLocale = Stream.of(Locale.getAvailableLocales())
				.filter(l -> l.getCountry().equals(country) && l.getLanguage().equals(toLowerCase(country)))
				.findFirst();

		if (optSimpleLocale.isPresent()) {
			// There is a country where language.toUpperCase() matches the country: this is
			// a peferred Locale
			// It handle IT_de, FR_ca
			return optSimpleLocale.get().getLanguage();
		}

		AtomicLongMap<String> languageToScore = AtomicLongMap.create();

		Stream.of(Locale.getAvailableLocales()).forEach(l -> languageToScore.incrementAndGet(l.getLanguage()));

		Optional<Locale> optLanguage = Stream.of(Locale.getAvailableLocales())
				.filter(l -> l.getCountry().equals(country))
				// Prefer the language applying it self to the higher number of countries
				.sorted(Comparator.comparing(l -> -languageToScore.get(l.getLanguage())))
				.findFirst();

		if (optLanguage.isEmpty()) {
			LOGGER.warn("We failed spotting the default language for country={}", country);
			return IMiTrustCoreConstants.LOCALE_DEFAULT.getLanguage();
		} else {
			return optLanguage.get().getLanguage();
		}
	}

	public static void checkLanguageNoVariant(String language) {
		if (Strings.isNullOrEmpty(language)) {
			throw new IllegalArgumentException("language can not be empty");
		}

		if (splitLanguages(language).size() != 1) {
			throw new IllegalArgumentException("Variants are forbidden. languages=" + language);
		}

		String notSuffixedLang;
		if (language.endsWith(SUFFIX_I18N_NDSP)) {
			notSuffixedLang = language.substring(0, language.length() - SUFFIX_I18N_NDSP.length());
		} else {
			notSuffixedLang = language;
		}

		if (!getIsoLanguages().contains(notSuffixedLang)) {
			throw new IllegalArgumentException("Invalid language: " + notSuffixedLang);
		}
	}

	public static void checkCountry(String country) {
		if (Strings.isNullOrEmpty(country)) {
			throw new IllegalArgumentException("'country' is required");
		}

		if (!IMiTrustCoreConstants.ALL.equals(country)) {
			if (!country.equals(country.toUpperCase(Locale.US))) {
				throw new IllegalArgumentException("A country has to be in upperCase: country='" + country + "'");
			}

			if (!ISO_COUNTRIES.get().contains(country)) {
				throw new IllegalArgumentException("Given country is invalid: " + country);
			}
		}
	}

	public static String toLowerCase(String string) {
		return string.toLowerCase(Locale.US);
	}

	public static String toUpperCase(String string) {
		return string.toUpperCase(Locale.US);
	}
}
