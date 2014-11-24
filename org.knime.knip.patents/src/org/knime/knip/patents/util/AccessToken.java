package org.knime.knip.patents.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlbeans.impl.util.Base64;

public class AccessToken {

	private static String accessToken;
	private static long issuedAt;

	public static String getAccessToken(String consumerKey,
			String consumerSecret) {
		if (accessToken == null
				|| System.currentTimeMillis() - issuedAt > 1200000) {
			accessToken = requestAccessToken(consumerKey, consumerSecret);
		}

		return accessToken;
	}

	/**
	 * Tries to retrieve the access token, given the consumerKey and
	 * consumerSecret from ops.epo.org.
	 * 
	 * @param consumerKey
	 *            the consumer key
	 * @param consumerSecret
	 *            the consumer secret
	 * @return the access token if it could be retrieved, otherwise an exception
	 *         is thrown
	 */
	private static String requestAccessToken(String consumerKey,
			String consumerSecret) {

		String encodedString = new String(Base64.encode((consumerKey.trim()
				+ ":" + consumerSecret.trim()).getBytes()));

		String accessToken = null;

		// create connection and set http headers
		try {
			URL url = new URL("https://ops.epo.org/3.1/auth/accesstoken");
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", encodedString);
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			// write POST payload
			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream(), "UTF-8");
			String payload = "grant_type=client_credentials";
			writer.write(payload);
			writer.close();

			// read the respone and try to retrieve the access token
			BufferedReader br = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("access_token")) {
					String[] token = line.split(":");
					accessToken = token[1].replaceAll("\\W", "").trim();
				}
			}
			br.close();
			connection.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return accessToken;
	}
}
