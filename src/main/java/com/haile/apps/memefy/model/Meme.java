package com.haile.apps.memefy.model;

public class Meme {
	
	private String imageUrl;
	private String memeText;
	private boolean top;
	
	public String getImageUrl () {
		return imageUrl;
	}
	
	public void setImageUrl (String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public String getMemeText () {
		return memeText;
	}
	
	public void setMemeText (String memeText) {
		this.memeText = memeText;
	}
	
	public boolean getTop () {
		return top;
	}
	
	public void setTop (boolean top) {
		this.top = top;
	}

}
