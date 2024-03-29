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
package eu.solven.cleanthat.engine.java.refactorer.meta;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;

/**
 * Some {@link IMutator} can be applied multiple times in a row. Instead of holding this logic in the {@link IMutator},
 * the {@link IMutator} indicates to the {@link AAstRefactorer} it should apply it multiple times (until no-op).
 *
 * @author Benoit Lacelle
 */
@Deprecated(
		since = "Unclear if an Interface is the good way to go, as it prevent dynamic computation in CompositeMutators")
public interface IReApplyUntilNoop {

}
