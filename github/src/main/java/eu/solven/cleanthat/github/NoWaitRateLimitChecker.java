package eu.solven.cleanthat.github;

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