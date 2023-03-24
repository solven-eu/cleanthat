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
package eu.solven.cleanthat;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

/**
 * This annotation enable a custom project to prevent cleanthat applying to given field/method/class/file (by annotating
 * the package). It is especially useful to workaround a cleanthat bug, or to prevent cleanthat cleaning some specific
 * block
 * 
 * @author Benoit Lacelle
 *
 */
@Retention(SOURCE)
public @interface SuppressCleanthat {

}
