package org.knime.knip.patents.util.nodes.title;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

public class OPSTitleNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(OPSTitleNodeModel.class);

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
		return new URL("http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/" + input[0] + "/biblio");
	}

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		XPathExpression titleExpression;
		try {
			if (m_language.getStringValue().equalsIgnoreCase("DE")) {
				titleExpression = super.getXPath()
						.compile("(((//*[local-name() = 'invention-title'])[@lang='DE' or @lang='de'])[1])");
			} else if (m_language.getStringValue().equalsIgnoreCase("FR")) {
				titleExpression = super.getXPath()
						.compile("(((//*[local-name() = 'invention-title'])[@lang='FR' or @lang='fr'])[1])");
			} else {
				titleExpression = super.getXPath()
						.compile("(((//*[local-name() = 'invention-title'])[@lang='EN' or @lang='en'])[1])");

			}
		} catch (XPathExpressionException xee) {
			throw new IllegalArgumentException("Couldn't instantiate Title Node", xee);
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

			String title = (String) titleExpression.evaluate(doc, XPathConstants.STRING);

			return new DataCell[] { (title == null || title.isEmpty()) ? new MissingCell("No abstracts available.")
					: new StringCell(title) };
		} catch (Exception e) {
			LOGGER.info("Server returned error during parsing: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()) };
		}

	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { StringCell.TYPE };
		String[] columnNames = new String[] { "Title" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}
}
