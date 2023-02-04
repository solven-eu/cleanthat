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
package eu.solven.cleanthat.code_provider.github;

import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;

/**
 * This is useful to ensure we never wait for a RateLimit
 * 
 * @author Benoit Lacelle
 *
 */
public class NoWaitRateLimitChecker extends RateLimitChecker {
	@Override
	protected boolean checkRateLimit(GHRateLimit.Record rateLimitRecord, long count) throws InterruptedException {
		throw new IllegalStateException("Early failure on RateLimit: " + rateLimitRecord + " count=" + count);
	}
}