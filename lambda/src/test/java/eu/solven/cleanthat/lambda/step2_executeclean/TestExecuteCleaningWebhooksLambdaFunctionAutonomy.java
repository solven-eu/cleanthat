package eu.solven.cleanthat.lambda.step2_executeclean;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.TestCheckConfigWebhooksLambdaFunctionAutonomy;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ExecuteCleaningWebhooksLambdaFunction.class,
		TestCheckConfigWebhooksLambdaFunctionAutonomy.CheckConfigWebhooksLambdaFunctionComplement.class })
public class TestExecuteCleaningWebhooksLambdaFunctionAutonomy {

	@Autowired
	ApplicationContext appContext;

	@Autowired
	ExecuteCleaningWebhooksLambdaFunction function;

	@Test
	public void testAppRun() {
		// When
		// {
		// IGitWebhookHandler gitWebhookHandler = appContext.getBean(IGitWebhookHandler.class);
		//
		// WebhookRelevancyResult value =
		// new WebhookRelevancyResult(Optional.empty(), Optional.of("someUnitTestReason"));
		// Mockito.when(
		// gitWebhookHandler.filterWebhookEventTargetRelevantBranch(Mockito.any(ICodeCleanerFactory.class),
		// Mockito.any(IWebhookEvent.class)))
		// .thenReturn(value);
		// }

		// Then
		Map<String, ?> payload = ImmutableMap.<String, Object>builder().build();
		Map<String, Object> headers = Map.of();
		function.unsafeProcessOneEvent(new CleanThatWebhookEvent(headers, payload));
	}
}
