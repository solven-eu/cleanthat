/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.lambda;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;

/**
 * Based class for Lambda, Functions, etc
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public abstract class ACleanThatXxxFunction extends ACleanThatXxxApplication {

	static {
		// https://stackoverflow.com/questions/35298616/aws-lambda-and-inaccurate-memory-allocation
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		LOGGER.info("Input arguments: {}", inputArguments);
	}

	protected final Map<String, ?> processOneEvent(IWebhookEvent input) {
		try {
			return unsafeProcessOneEvent(input);
		} catch (RuntimeException e) {
			// Headers should hold the eventId,which enables fetching it from DB
			LOGGER.warn("Issue processing an event. headers=" + input.getHeaders(), e);
			Sentry.captureException(e);

			throw new RuntimeException(e);
		}
	}

	protected abstract Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input);

}
