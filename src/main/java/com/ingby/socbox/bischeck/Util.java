package com.ingby.socbox.bischeck;

public class Util {

	public static String obfuscatePassword(String url) {
		return url.replaceAll("password=[0-9A-Za-z]*","password=xxxxx");
	}

}
