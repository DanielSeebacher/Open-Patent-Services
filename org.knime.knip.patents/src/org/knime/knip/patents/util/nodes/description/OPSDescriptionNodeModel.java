package org.knime.knip.patents.util.nodes.description;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.Pair;
import org.knime.knip.patents.KNIMEOPSPlugin;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.knime.knip.patents.util.nodes.AbstractOPSNodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class OPSDescriptionNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(OPSDescriptionNodeModel.class);

	private static final String[] ACCEPTED_COUNTRY_CODES = new String[] { "EP", "WO", "AT", "CA", "CH", "GB", "ES" };

	static SettingsModelString createLanguageModel() {
		return new SettingsModelString("m_language", "EN");
	}

	private final SettingsModelString m_language = createLanguageModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		super.addSettingsModels(settingsModels);
		settingsModels.add(m_language);
	}

	@Override
	public URL getURL(String... input) throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/" + input[0] + "/description");
	}

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		String countryCode = patentIDValue.getStringValue().split("\\.")[0];
		if (!Arrays.asList(ACCEPTED_COUNTRY_CODES).contains(countryCode)) {
			LOGGER.warn("Description not available for country: " + countryCode);
			return new DataCell[] { new MissingCell("Description not available for country: " + countryCode) };
		}

		XPathExpression descriptionExpression = null;
		try {
			if (m_language.getStringValue().equalsIgnoreCase("DE")) {
				descriptionExpression = super.getXPath()
						.compile("(((//*[local-name() = 'description'])[@lang='DE' or @lang='de'])[1])//child::*");
			} else if (m_language.getStringValue().equalsIgnoreCase("FR")) {
				descriptionExpression = super.getXPath()
						.compile("(((//*[local-name() = 'description'])[@lang='FR' or @lang='fr'])[1])//child::*");
			} else {
				descriptionExpression = super.getXPath()
						.compile("(((//*[local-name() = 'description'])[@lang='EN' or @lang='en'])[1])//child::*");
			}
		} catch (XPathExpressionException xee) {
			throw new IllegalArgumentException("Couldn't instantiate Description Node", xee);
		}

		try {
			// check if we need to get an access token
			String consumerKey = KNIMEOPSPlugin.getOAuth2ConsumerKey();
			String consumerSecret = KNIMEOPSPlugin.getOAuth2ConsumerSecret();

			String accessToken = null;
			if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
				accessToken = AccessTokenGenerator.getInstance().getAccessToken(consumerKey, consumerSecret);
			}

			// create URL and HttpURLConnection
			URL fullCycleURL = getURL(patentIDValue.getStringValue());
			HttpURLConnection fullCycleHttpConnection = (HttpURLConnection) fullCycleURL.openConnection();

			// set accesstoken if available
			if (accessToken != null) {
				fullCycleHttpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
			}

			// check html respone
			try {
				checkResponse(fullCycleHttpConnection);
			} catch (Exception e) {
				LOGGER.warn("Server returned error before parsing: " + e.getMessage(), e);
				return new DataCell[] { new MissingCell(e.getMessage()) };
			}

			throttle(fullCycleHttpConnection, "retrieval");

			// download doc
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fullCycleHttpConnection.getInputStream());

			NodeList descriptionParagraphs = (NodeList) descriptionExpression.evaluate(doc, XPathConstants.NODESET);

			String descriptionString = "";
			for (int i = 0; i < descriptionParagraphs.getLength(); i++) {
				descriptionString = descriptionString.concat(descriptionParagraphs.item(i).getTextContent().trim());
			}

			return new DataCell[] { (descriptionString == null || descriptionString.isEmpty())
					? new MissingCell("No claims available.") : new StringCell(descriptionString) };
		} catch (Exception e) {
			LOGGER.info("Server returned error during parsing: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()) };
		}

	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { StringCell.TYPE };
		String[] columnNames = new String[] { "Description" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

}
