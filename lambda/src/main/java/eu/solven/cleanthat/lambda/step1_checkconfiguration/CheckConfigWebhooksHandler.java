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
package eu.solven.cleanthat.lambda.step1_checkconfiguration;

import java.util.Map;
import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

/**
 * Beware a lambda does not need an HTTP server: it can be exclusive to processing events, or files in S3. This will
 * enable AWS to receive events through API calls
 *
 * @author Benoit Lacelle
 */
public class CheckConfigWebhooksHandler extends SpringBootRequestHandler<Map<String, ?>, Map<String, ?>> {
	public CheckConfigWebhooksHandler() {
		// We declare the function explicitly as given jar holds multiple lambda functions
		super(CheckConfigWebhooksLambdaFunction.class);
	}
}
