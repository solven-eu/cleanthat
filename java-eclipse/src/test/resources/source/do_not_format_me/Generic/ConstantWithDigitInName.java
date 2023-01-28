package io.mitrust.pgsql;

/**
 * This was failing when the regex guessing constants (when the ClassLoader does not know the constant provider) was not
 * including digits
 * 
 * @author Benoit Lacelle
 *
 */
public class ConstantWithDigitInName {

	private boolean processState(String kString) {
		return IMiTrustOAuth2Constants.KEY_STATE_B2B2B.equals(kString);
	}

}
