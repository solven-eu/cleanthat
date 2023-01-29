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
package eu.solven.cleanthat;

/**
 * Constants to be used for any test related to git
 * 
 * @author Benoit Lacelle
 *
 */
@SuppressWarnings({ "checkstyle:ConstantName", "PMD.FieldNamingConventions" })
public interface IGitTestConstants {
	String someRepoName = "someRepoName";
	String someBranchRef = "refs/head/someBranchName";
}
