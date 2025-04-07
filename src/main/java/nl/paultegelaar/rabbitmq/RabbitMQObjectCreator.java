package nl.paultegelaar.rabbitmq;


import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.paultegelaar.rabbitmq.config.RabbitMQObjects;
import nl.paultegelaar.rabbitmq.util.RabbitMQAdminClient;
import nl.paultegelaar.rabbitmq.util.config.ApplicationConfig;
import nl.paultegelaar.rabbitmq.util.exception.RabbitMQProvisioningException;

public class RabbitMQObjectCreator {
	
	private static final String CONFIG_FILE_PROPERTY = "configFile";
	private static final String PASSWORD_PROPERTY = "password";
	private static final String USERNAME_PROPERTY = "username";
	private static final String ENDPOINT_PROPERTY = "endpoint";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQObjectCreator.class);
	

	public static void main(String[] args) {
		
		//Create options
		Options options = buildOptions();
		
		//Build commandline
		CommandLine cmd;
	    CommandLineParser parser = new DefaultParser();
	    HelpFormatter helper = new HelpFormatter();
		
	    //Run it
	    try {
	    	//Parse options
	        cmd = parser.parse(options, args);
	        String adminEndpoint = cmd.getOptionValue(ENDPOINT_PROPERTY);
	        String username = cmd.getOptionValue(USERNAME_PROPERTY);
	        String password = cmd.getOptionValue(PASSWORD_PROPERTY);
	        String configFile = cmd.getOptionValue(CONFIG_FILE_PROPERTY);
	        	  
	        //Read config file
	        RabbitMQObjects rabbitMQObjects = OBJECT_MAPPER.readValue(new File(configFile), RabbitMQObjects.class);
	        
	        //Build application config
	        ApplicationConfig applicationConfig = new ApplicationConfig();
			applicationConfig.setApiBaseURL(adminEndpoint);
			applicationConfig.setApiUsername(username);
			applicationConfig.setApiPassword(password.toCharArray());
	        
			//Create client
			RabbitMQAdminClient rabbitMQAdminClient = new RabbitMQAdminClient(applicationConfig);
			
			//Run provisioning
			rabbitMQAdminClient.processRabbitMQConfig(rabbitMQObjects);
	        
	    } catch (ParseException e) {
	    	LOGGER.info("Exception parsing config JSON: {}", e.getMessage());
	        helper.printHelp("Usage:", options);
	        System.exit(0);
	    }  catch (IOException e) {
	    	LOGGER.info("Unable to read config file: {}", e.getMessage());
	        System.exit(0);
		} catch (RabbitMQProvisioningException e) {
			LOGGER.info("Error while running RabbitMQ provisioning: {}", e.getMessage());
	        System.exit(0);
		}
	    
		
	}

	/**
	 * Builds all options used by main method.
	 * 
	 * @return Options
	 */
	private static Options buildOptions() {
		Options options = new Options();
		
		Option adminEndpoint = Option.builder("e").longOpt(ENDPOINT_PROPERTY)
		        						   .argName(ENDPOINT_PROPERTY)
		        						   .hasArg()
		        						   .required(true)
		        						   .desc("Set RabbitMQ admin endpoint").build();
		
		Option username = Option.builder("u").longOpt(USERNAME_PROPERTY)
										   .argName(USERNAME_PROPERTY)
										   .hasArg()
										   .required(true)
										   .desc("Set RabbitMQ admin username").build();
		
		Option password = Option.builder("p").longOpt(PASSWORD_PROPERTY)
										   .argName(PASSWORD_PROPERTY)
										   .hasArg()
										   .required(true)
										   .desc("Set RabbitMQ admin password").build();
		
		Option configFile = Option.builder("c").longOpt(CONFIG_FILE_PROPERTY)
				   .argName(CONFIG_FILE_PROPERTY)
				   .hasArg()
				   .required(true)
				   .desc("Set config file containing objects").build();
		

		return options.addOption(password).addOption(username).addOption(adminEndpoint).addOption(configFile);
	}

}