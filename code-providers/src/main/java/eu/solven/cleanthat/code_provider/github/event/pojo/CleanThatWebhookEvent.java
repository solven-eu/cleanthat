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
package eu.solven.cleanthat.code_provider.github.event.pojo;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import java.util.Map;

/**
 * A generate {@link IWebhookEvent} used internally in CleanThat pipeline
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanThatWebhookEvent implements IWebhookEvent {
	final Map<String, ?> headers;
	final Map<String, ?> body;

	public CleanThatWebhookEvent(Map<String, ?> headers, Map<String, ?> body) {
		this.headers = headers;
		this.body = body;
	}

	@Override
	public Map<String, ?> getHeaders() {
		return headers;
	}

	@Override
	public Map<String, ?> getBody() {
		return body;
	}

}
