package org.knime.knip.patents.util.nodes.numberservice;

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
import org.w3c.dom.NodeList;

public class OPSNumberServiceNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(OPSNumberServiceNodeModel.class);

	static SettingsModelString createInputFormatModel() {
		return new SettingsModelString("m_inputFormat", "docdb");
	}

	static SettingsModelString createOutputFormatModel() {
		return new SettingsModelString("m_outputFormat", "epodoc");
	}

	private final SettingsModelString m_inputFormat = createInputFormatModel();
	private final SettingsModelString m_outputFormat = createOutputFormatModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		super.addSettingsModels(settingsModels);
		settingsModels.add(m_inputFormat);
		settingsModels.add(m_outputFormat);
	}

	@Override
	public URL getURL(String... input) throws MalformedURLException {
		return new URL("http://ops.epo.org/3.1/rest-services/number-service/application/" + input[0] + "/" + input[1]
				+ "/" + input[2]);
	}

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		if (m_inputFormat.getStringValue().equalsIgnoreCase("epodoc")
				&& m_inputFormat.getStringValue().equalsIgnoreCase("docdb")) {
			throw new IllegalArgumentException(
					"Following conversion of formats is not supported by the OPS epodoc => docdb.");
		}

		XPathExpression numberServiceExpression = null;
		try {
			numberServiceExpression = super.getXPath()
					.compile("(((//*[local-name() = 'output']//*[local-name() = 'document-id']))[1])/child::*");
		} catch (XPathExpressionException xee) {
			throw new IllegalArgumentException("Couldn't instantiate Number service Node", xee);
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
			URL fullCycleURL = getURL(m_inputFormat.getStringValue(), patentIDValue.getStringValue(),
					m_outputFormat.getStringValue());

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

			NodeList abstracts = (NodeList) numberServiceExpression.evaluate(doc, XPathConstants.NODESET);

			String output = new String("");
			for (int i = 0; i < abstracts.getLength(); i++) {
				if (i > 0) {
					output += ".";
				}

				output += abstracts.item(i).getTextContent();
			}

			return new DataCell[] {
					(output.isEmpty()) ? new MissingCell("No abstracts available.") : new StringCell(output) };
		} catch (Exception e) {
			LOGGER.info("Server returned error during parsing: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()) };
		}
	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { StringCell.TYPE };
		String[] columnNames = new String[] { "Output Number" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

}
