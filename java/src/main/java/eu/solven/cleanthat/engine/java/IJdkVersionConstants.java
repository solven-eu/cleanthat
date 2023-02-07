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
package eu.solven.cleanthat.engine.java;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * List the JDK versions
 *
 * @author Benoit Lacelle
 */
// https://www.java.com/releases/
public interface IJdkVersionConstants {

	String JDK_1 = "1";

	String JDK_4 = "1.4";

	String JDK_5 = "1.5";

	String JDK_6 = "1.6";

	String JDK_7 = "1.7";

	String JDK_8 = "1.8";

	// https://www.infoq.com/news/2015/12/java-version-strings-evolve/
	String JDK_9 = "9";

	String JDK_11 = "11";

	String JDK_17 = "17";

	@Deprecated(
			since = "Unclear if safe given this is doomed to be deprecated (as JDK won't stop releasing new versions)")
	String JDK_LATEST = JDK_17;

	List<String> ORDERED = ImmutableList.of(JDK_1, JDK_4, JDK_5, JDK_6, JDK_7, JDK_8, JDK_9, JDK_11, JDK_17);
}
