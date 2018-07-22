package com.haile.apps.memefy.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import javax.ws.rs.core.MediaType;

import com.haile.apps.memefy.model.Meme;

@Controller
@RequestMapping("/")
public class MemefyController {
	private static final Logger logger = LoggerFactory.getLogger(MemefyController.class);

	@RequestMapping(value = "/memefy", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> memefyImage(@RequestBody Meme meme) {
		HashMap<String, String> map = new HashMap<String, String> ();
		String originalFileName = "haile.jpg";
		MemeImage memeImage = new MemeImage();
		String imageUrl = meme.getImageUrl();
		String memeText = meme.getMemeText();
		byte[] memeByte = null;
		try {
			memeByte = memeImage.convertToMeme(imageUrl, memeText);
			logger.debug("meme generated.");
		} catch (Exception e) {
			logger.error("Error occured while trying to generate meme: " + e.getMessage(), e.getCause());
			map.put("error", "Error occured while trying to generate meme: " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		String filePath = System.getProperty("java.io.tmpdir");
		
		map = uploadToHabeshaitFTP(memeByte, filePath + originalFileName , "/memefied");
		
		return new ResponseEntity<> (map, HttpStatus.OK);
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, String> uploadToHabeshaitFTP (byte [] fileContent, String destination, String ftpPath) {
		
		try (FileOutputStream fos = new FileOutputStream(destination)) {
			fos.write(fileContent);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File fileToUpload = new File(destination);

		final String API_URI = "https://ftpupload.herokuapp.com/ftp/upload";

		final ClientConfig config = new DefaultClientConfig();
		config.getClasses().add(JacksonJsonProvider.class);
		final Client client = Client.create(config);
		final WebResource resource = client.resource(API_URI);

		// the file to upload, represented as FileDataBodyPart
		FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", fileToUpload);
		fileDataBodyPart.setContentDisposition(
				FormDataContentDisposition.name("file").fileName(fileToUpload.getName()).build());

		final MultiPart multiPart = new FormDataMultiPart()
				.field("path", ftpPath, MediaType.TEXT_PLAIN_TYPE).bodyPart(fileDataBodyPart);
		multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

		// POST request final
		ClientResponse response = resource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class, multiPart);
		client.destroy();
		
		return response.getEntity(HashMap.class);
	}

}
