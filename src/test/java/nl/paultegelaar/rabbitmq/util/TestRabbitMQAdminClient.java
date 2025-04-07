package nl.paultegelaar.rabbitmq.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import nl.paultegelaar.rabbitmq.config.RabbitMQObjects;
import nl.paultegelaar.rabbitmq.util.config.ApplicationConfig;
import nl.paultegelaar.rabbitmq.util.exception.RabbitMQProvisioningException;

class TestRabbitMQAdminClient {

	WireMockServer wireMockServer = null;
	ApplicationConfig applicationConfig = null;
	RabbitMQAdminClient rabbitMQAdminClient = null;
	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	@BeforeEach
	void init() throws RabbitMQProvisioningException {
		
		
		wireMockServer = new WireMockServer(35672);
		wireMockServer.start();			
		
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
	
	
	/**
	 * Test the happy flow
	 * 
	 * @throws IOException
	 */
	@Test
	void testHappyFlow() throws IOException {
		RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File("src/test/resources/rabbitmq-test-config.json"), RabbitMQObjects.class);
						
		assertDoesNotThrow(() -> {
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects);     
	    }, "Happy flow in exception");
		
	}
	
	/**
	 * Test if reserved exchange names are correctly skipped
	 * 
	 * @throws IOException
	 */
	@Test
	void testReservedExchangeName() throws IOException {
		RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File("src/test/resources/rabbitmq-test-reserverd-exchange-config.json"), RabbitMQObjects.class);
						
		assertDoesNotThrow(() -> {
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects);     
	    }, "Reserved exchangename flow in exception");
		
	}
	
	/**
	 * Test if non-existant VHost leads to controlled exception
	 * 
	 * @throws IOException
	 */
	@Test
	void testNoVhostFlow() throws IOException {
		RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File("src/test/resources/rabbitmq-test-no-vhost-config.json"), RabbitMQObjects.class);
						
		assertThrows(RabbitMQProvisioningException.class, () -> {
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects); 
	    });
	}
	
	
	/**
	 * Test if bad config leads to 
	 * 
	 * @throws IOException
	 */
	@Test
	void testBadConfig() throws IOException {
		RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File("src/test/resources/rabbitmq-test-bad-config.json"), RabbitMQObjects.class);
						
		assertThrows(RabbitMQProvisioningException.class, () -> {
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects); 
	    });
	}
	
}
