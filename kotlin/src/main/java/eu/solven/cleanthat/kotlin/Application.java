/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.kotlin;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public final class Application {

	private static final String JAVA = "java";
	private static final String KOTLIN = "kotlin";

	private Application() {
		// hidden
	}

	public static void main(String[] args) {
		String language = args[0];
		switch (language) {
		case JAVA:
			new JavaService().sayHello();
			break;
		case KOTLIN:
			new KotlinService().sayHello();
			break;
		default:
			// Do nothing
			break;
		}
	}
}