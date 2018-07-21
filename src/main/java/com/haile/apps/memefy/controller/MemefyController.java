package com.haile.apps.memefy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.haile.apps.memefy.model.Meme;

@Controller
@RequestMapping("/")
public class MemefyController {
	private static final Logger logger = LoggerFactory.getLogger(MemefyController.class);
			
	@RequestMapping(value="/memefy", method=RequestMethod.POST)
    public @ResponseBody byte[] memefyImage(@RequestBody Meme meme) {
		MemeImage memeImage = new MemeImage();
		byte [] memeByte = null;
		try {
			memeByte = memeImage.convertToMeme(meme.getImageUrl(), meme.getMemeText());
			logger.debug("meme generated.");
		} catch (Exception e) {
			logger.error("Error occured while trying to generate meme: " + e.getMessage(), e.getCause());
			return null;
		}
		return memeByte;
    }
}
