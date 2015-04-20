package org.knime.knip.patents.util.nodes.claims;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.knip.patents.KNIMEOPSPlugin;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.knime.knip.patents.util.nodes.AbstractOPSNodeModel;
import org.w3c.dom.Document;

public class OPSClaimsNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OPSClaimsNodeModel.class);

	private static final String[] ACCEPTED_COUNTRY_CODES = new String[] { "EP",
		"WO", "AT", "CA", "CH", "GB", "ES" };
	private XPathExpression claimsExpression;

	public OPSClaimsNodeModel() {
		super();

		try {
			claimsExpression = super
					.getXPath()
					.compile(
							"(((//*[local-name() = 'claims'])[@lang='EN'])[1])/child::*");
		} catch (XPathExpressionException xee) {
			throw new IllegalArgumentException(
					"Couldn't instantiate Claims Node", xee);
		}

	}

	@Override
	public URL getURL(String input) throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ input + "/claims");
	}

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		
		String countryCode = patentIDValue.getStringValue().split("\\.")[0];
		if (!Arrays.asList(ACCEPTED_COUNTRY_CODES).contains(countryCode)) {
			LOGGER.warn("Claims not available for country: " + countryCode);
			return new DataCell[] { new MissingCell(
					"Claims not available for country: " + countryCode) };
		}
		
		try {
			// check if we need to get an access token
			String consumerKey = KNIMEOPSPlugin.getOAuth2ConsumerKey();
			String consumerSecret = KNIMEOPSPlugin.getOAuth2ConsumerSecret();

			String accessToken = null;
			if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
				accessToken = AccessTokenGenerator.getInstance()
						.getAccessToken(consumerKey, consumerSecret);
			}

			// create URL and HttpURLConnection
			URL fullCycleURL = getURL(patentIDValue.getStringValue());
			HttpURLConnection fullCycleHttpConnection = (HttpURLConnection) fullCycleURL
					.openConnection();

			// set accesstoken if available
			if (accessToken != null) {
				fullCycleHttpConnection.setRequestProperty("Authorization",
						"Bearer " + accessToken);
			}

			// check html respone
			try {
				checkResponse(fullCycleHttpConnection);
			} catch (Exception e) {
				LOGGER.warn(
						"Server returned error before parsing: "
								+ e.getMessage(), e);
				return new DataCell[] { new MissingCell(e.getMessage()) };
			}

			throttle(fullCycleHttpConnection, "retrieval");

			// download doc
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fullCycleHttpConnection.getInputStream());

			String claims = (String) claimsExpression.evaluate(doc,
					XPathConstants.STRING);

			return new DataCell[] { new StringCell(claims) };
		} catch (Exception e) {
			LOGGER.warn(
					"Server returned error during parsing: " + e.getMessage(),
					e);
			return new DataCell[] { new MissingCell(e.getMessage()) };
		}

	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { StringCell.TYPE };
		String[] columnNames = new String[] { "Claims" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

}
