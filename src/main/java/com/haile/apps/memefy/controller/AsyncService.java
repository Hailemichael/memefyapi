package com.haile.apps.memefy.controller;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {
	private static final Logger logger = LogManager.getLogger(AsyncService.class.getName());
	
	@Async("asyncServiceExecutor")
	public void uploadToHabeshaitFTP (byte [] fileContent, String apiUri, String apiUser, String apiPassword, String targetFilename, String ftpPath) throws Exception {
		logger.info("FTP upload initiated for file with name: " + targetFilename);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(apiUser, apiPassword);	   
		//Client client = ClientBuilder.newBuilder().register(feature).register(JacksonFeature.class).register(MultiPartFeature.class).build();
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(feature);
		clientConfig.register(JacksonFeature.class);
		clientConfig.register(MultiPartFeature.class);
		Client client = ClientBuilder.newClient(clientConfig);	
		WebTarget webTarget = client.target(apiUri).path("/");
        
		FormDataMultiPart formDataMultiPart = new FormDataMultiPart();        
        formDataMultiPart.field("path", ftpPath);
        formDataMultiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);        
        FormDataBodyPart bodyPart = new FormDataBodyPart("file", new ByteArrayInputStream(fileContent), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        bodyPart.setContentDisposition(FormDataContentDisposition.name("file").fileName(targetFilename).size(fileContent.length).build());
        formDataMultiPart.bodyPart(bodyPart);
        
		Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(formDataMultiPart, formDataMultiPart.getMediaType()));
		HashMap<String, String> map = response.readEntity(new GenericType<HashMap<String, String>>() { });
		logger.info("Successful FTP upload: " + map);
	}
	
}
