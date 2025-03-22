package nl.paultegelaar.rabbitmq.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import nl.paultegelaar.rabbitmq.config.RabbitMQObjects;
import nl.paultegelaar.rabbitmq.util.config.ApplicationConfig;
import nl.paultegelaar.rabbitmq.util.exception.RabbitMQProvisioningException;

public class TestRabbitMQAdminClient {

	WireMockServer wireMockServer = null;
	ApplicationConfig applicationConfig = null;
	RabbitMQAdminClient rabbitMQAdminClient = null;
	static ObjectMapper OBJECTMAPPER = new ObjectMapper();
	
	@BeforeEach
	void init() throws RabbitMQProvisioningException {
		
		
		wireMockServer = new WireMockServer(35672);
		wireMockServer.start();
		
		System.out.println("Wiremock running: " + wireMockServer.isRunning());
		
		applicationConfig = new ApplicationConfig();
		applicationConfig.setApiBaseURL("http://localhost:35672");
		applicationConfig.setApiUsername("dummy");
		applicationConfig.setApiPassword("dummy".toCharArray());
		

		rabbitMQAdminClient = new RabbitMQAdminClient(applicationConfig);

		
	}

	
	@AfterEach
	void destroy() {		
		wireMockServer.stop();
	}
	
	@Test
	void testHappyFlow() throws StreamReadException, DatabindException, IOException, RabbitMQProvisioningException, InterruptedException {
		RabbitMQObjects rabbitMQObjects = OBJECTMAPPER.readValue(new File("src/test/resources/rabbitmq-test-config.json"), RabbitMQObjects.class);
		
		
		
		assertDoesNotThrow(() -> {
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects);     
	    }, "Happy flow in exception");
		
	}
	
}
