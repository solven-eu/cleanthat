package eu.solven.cleanthat.rules;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * List the JDK versions
 *
 * @author Benoit Lacelle
 */
public interface IJdkVersionConstants {

	String JDK_1 = "1";

	String JDK_4 = "1.4";

	String JDK_5 = "1.5";

	String JDK_6 = "1.6";

	String JDK_7 = "1.7";

	String JDK_8 = "8";

	String JDK_9 = "9";

	String JDK_11 = "11";

	@Deprecated(
			since = "Unclear if safe given this is doomed to be deprecated (as JDK won't stop releasing new versions)")
	String JDK_LATEST = JDK_11;

	List<String> ORDERED = ImmutableList.of(JDK_1, JDK_4, JDK_5, JDK_6, JDK_7, JDK_8, JDK_9, JDK_11);
}
