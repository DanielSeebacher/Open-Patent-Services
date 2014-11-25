package org.knime.knip.patents.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.impl.util.Base64;

public class AccessTokenGenerator {

	private static AccessTokenGenerator instance;

	private AccessTokenGenerator() {
	}

	public static AccessTokenGenerator getInstance() {
		if (AccessTokenGenerator.instance == null) {
			AccessTokenGenerator.instance = new AccessTokenGenerator();
		}
		return AccessTokenGenerator.instance;
	}

	private Map<String, AccessToken> accessTokens = new HashMap<>();

	public String getAccessToken(String consumerKey, String consumerSecret) {

		String encodedString = toBase64String(consumerKey, consumerSecret);

		if (accessTokens.containsKey(encodedString)) {
			AccessToken accessToken = accessTokens.get(encodedString);
			if (!accessToken.isValid()) {
				accessTokens.put(encodedString,
						requestAccessToken(consumerKey, consumerSecret));
			}
		} else {
			accessTokens.put(encodedString,
					requestAccessToken(consumerKey, consumerSecret));
		}

		AccessToken request = accessTokens.get(encodedString);
		if (request != null) {
			return request.getAccessTokenString();
		}

		return null;
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
	private AccessToken requestAccessToken(String consumerKey,
			String consumerSecret) {

		String encodedString = toBase64String(consumerKey, consumerSecret);

		String accessToken = null;
		long issuedAt = 0;

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

				if (line.contains("issued_at")) {
					String[] token = line.split(":");
					issuedAt = Long.parseLong(token[1].replaceAll("\\W", "")
							.trim());
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

		if (accessToken != null && issuedAt > 0) {
			return new AccessToken(accessToken, issuedAt);
		}

		return null;
	}

	private String toBase64String(String consumerKey, String consumerSecret) {
		return new String(
				Base64.encode((consumerKey.trim() + ":" + consumerSecret.trim())
						.getBytes()));
	}

	private static class AccessToken {

		private final String accessTokenString;
		private final long issuedAt;

		public AccessToken(String accessTokenString, long issuedAt) {
			this.accessTokenString = accessTokenString;
			this.issuedAt = issuedAt;
		}

		public String getAccessTokenString() {
			return accessTokenString;
		}

		public boolean isValid() {
			if (accessTokenString == null) {
				return false;
			}

			if (System.currentTimeMillis() - issuedAt > 1199000) {
				return false;
			}

			return true;
		}
	}
}
