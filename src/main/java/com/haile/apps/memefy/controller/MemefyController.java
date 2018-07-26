package com.haile.apps.memefy.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.haile.apps.memefy.model.Meme;

@Controller
@RequestMapping("/")
public class MemefyController {
	private static final Logger logger = LoggerFactory.getLogger(MemefyController.class);
	
	@Value("${ftp.api.uri}")
	private String ftpApiUri;
	
	@Value("${ftp.api.user}")
	private String ftpApiUser;
	
	@Value("${ftp.api.password}")
	private String ftpApiPassword;
	
	@RequestMapping(value="/memefy/file", method=RequestMethod.POST, headers = "content-type=multipart/form-data")
    public @ResponseBody ResponseEntity<?> ftpUpload(HttpServletRequest request, @RequestParam(value="file", required=true) MultipartFile file, @RequestParam(value="memeText", required=true) String memeText, @RequestParam(value="top", required=true) Boolean top) {
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());
		String fileName = file.getOriginalFilename();
		String suffix = null;
		BufferedImage originalImage = null;
		HashMap<String, String> map = new HashMap<String, String> ();
		if(file.isEmpty()) {
			logger.error("The file is empty!");
			map.put("error", "The file is empty!");
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		}
		try {
			originalImage = ImageIO.read(file.getInputStream());
		} catch (IOException e) {
			logger.error("Error occured while reading the image file. " + e.getMessage());
			map.put("error", "Error occured while reading the image file. " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if (originalImage == null) {
			logger.error("The file: " + fileName + " is not an image.");
			map.put("error", "The file: " + fileName + " is not an image.");
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		}		
		String contentType = file.getContentType();
		suffix = getSuffixFromContentType (contentType);
		if (suffix == null) {
			logger.error("Could not determine the image type from the file: " + fileName);
			map.put("error", "Could not determine the image type from the file: " + fileName);
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		}	
		
		MemeImage memeImage = new MemeImage();
		byte[] memeByte = null;
		try {
			memeByte = memeImage.convertToMeme(originalImage, suffix, memeText,top);
			logger.debug("meme generated.");
			map = uploadToHabeshaitFTP(memeByte, ftpApiUri, ftpApiUser, ftpApiPassword, fileName, "/memefied");
		} catch (Exception e) {
			logger.error(e.getMessage(), e.getCause());
			map.put("error", e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<> (map, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/memefy/url", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> memefyImage(HttpServletRequest request, @RequestBody Meme meme) {
		logger.info("Incomming request: " + request.getServletPath() + "_" + request.getRemoteAddr() + "_" + request.getRemoteUser());
		String fileName = null;
		String suffix = null;
		String contentType = null;
		HashMap<String, String> map = new HashMap<String, String> ();
		
		Meme newMeme = new Meme();
		newMeme.setImageUrl(meme.getImageUrl());
		newMeme.setMemeText(meme.getMemeText());
		newMeme.setTop(meme.getTop());
		if (newMeme.getImageUrl().isEmpty()) {
			logger.error("empty imageUrl in the body");
			map.put("error", "empty imageUrl in the body");
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		}
		
		BufferedImage originalImage = null;
		try {
			URL imgUrl = new URL(newMeme.getImageUrl());
			fileName = FilenameUtils.getBaseName(imgUrl.getPath());
			HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestMethod("GET");
			conn.connect();
			// Check validity of url
			if (conn.getResponseCode() == 200) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IOUtils.copy(conn.getInputStream(), baos);
				baos.flush();
				conn.disconnect();
				byte[] imageByteArray = baos.toByteArray();
				baos.close();
				// Get content type from byte array
				ByteArrayInputStream bis = new ByteArrayInputStream(imageByteArray);
				contentType = HttpURLConnection.guessContentTypeFromStream(bis);					
				// Read Buffered image from inputstream
				originalImage = ImageIO.read(bis);
				bis.close();
				if (originalImage == null) {
					logger.error("The resource represented by the url: " + newMeme.getImageUrl() + " is not an image.");
					map.put("error", "The resource represented by the url: " + newMeme.getImageUrl() + " is not an image.");
					return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
				}
				suffix = getSuffixFromContentType (contentType);
				if (suffix == null) {
					logger.error("Could not determine the image type represented by the url: " + newMeme.getImageUrl());
					map.put("error", "Could not determine the image type represented by the url: " + newMeme.getImageUrl());
					return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
				}
			}
		} catch (MalformedURLException e) {
			map.put("error", "The image url: " + newMeme.getImageUrl() + " is malformed. " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		} catch (FileNotFoundException e) {
			map.put("error", "There is no resource at url: " + newMeme.getImageUrl() + " " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			map.put("error", "The resource could not be fetched from url: " + newMeme.getImageUrl() + " " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			map.put("error", "Exception occured: " + e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.INTERNAL_SERVER_ERROR);
		}		
		
		MemeImage memeImage = new MemeImage();
		byte[] memeByte = null;
		try {
			memeByte = memeImage.convertToMeme(originalImage, suffix, newMeme.getMemeText(), newMeme.getTop());
			logger.debug("meme generated.");
			map = uploadToHabeshaitFTP(memeByte, ftpApiUri, ftpApiUser, ftpApiPassword, fileName + suffix , "/memefied");
		} catch (Exception e) {
			logger.error(e.getMessage(), e.getCause());
			map.put("error", e.getMessage());
			return new ResponseEntity<> (map, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<> (map, HttpStatus.OK);
	}
	
	private String getSuffixFromContentType (String contentType) {
		String suffix = null;
		// Get image extension from content-type
		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(contentType);
		while (suffix == null && readers.hasNext()) {
			ImageReaderSpi provider = readers.next().getOriginatingProvider();
			if (provider != null) {
				String[] suffixes = provider.getFileSuffixes();
				if (suffixes != null) {
					suffix = suffixes[0];
				}
			}
		}
		return suffix;
	}
	
	private HashMap<String, String> uploadToHabeshaitFTP (byte [] fileContent, String apiUri, String apiUser, String apiPassword, String destination, String ftpPath) throws Exception {
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
        bodyPart.setContentDisposition(FormDataContentDisposition.name("file").fileName(destination).size(fileContent.length).build());
        formDataMultiPart.bodyPart(bodyPart);
        
		Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(formDataMultiPart, formDataMultiPart.getMediaType()));
		HashMap<String, String> map = response.readEntity(new GenericType<HashMap<String, String>>() { });
		return map;
	}

}
