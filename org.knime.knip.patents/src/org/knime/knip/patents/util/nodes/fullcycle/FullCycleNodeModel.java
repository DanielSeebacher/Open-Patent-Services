package org.knime.knip.patents.util.nodes.fullcycle;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.knip.patents.KNIMEOPSPlugin;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.knime.knip.patents.util.nodes.AbstractOPSNodeModel;
import org.knime.knip.patents.util.nodes.abstracts.OPSAbstractNodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FullCycleNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(OPSAbstractNodeModel.class);

	@Override
	public URL getURL(String... input) throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/" + input[0] + "/full-cycle");
	}

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		//
		// row[0] = getTitle(currentElement);
		// row[1] = getAbstract(currentElement);
		// row[2] = getCPCs(currentElement);
		// row[3] = getIPCRs(currentElement);
		// row[4] = getApplicants(currentElement);
		// row[5] = getInventors(currentElement);

		XPathExpression cpcExpression;
		XPathExpression ipcrExpression;
		XPathExpression familyidExpression;

		try {

			cpcExpression = super.getXPath().compile("(//*[local-name() = 'patent-classifications'])/child::*");
			ipcrExpression = super.getXPath().compile("(//*[local-name() = 'classification-ipcr'])/child::*");
			familyidExpression = super.getXPath().compile("(//*[local-name() = 'exchange-document'])/@family-id");

		} catch (XPathExpressionException xee) {
			throw new IllegalArgumentException("Couldn't instantiate Abstract Node", xee);
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
				return new DataCell[] { new MissingCell(e.getMessage()), new MissingCell(e.getMessage()), new MissingCell(e.getMessage()) };
			}

			throttle(fullCycleHttpConnection, "retrieval");

			// download doc
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fullCycleHttpConnection.getInputStream());

			NodeList cpcNodeList = (NodeList) cpcExpression.evaluate(doc, XPathConstants.NODESET);

			List<StringCell> cpcs = new ArrayList<StringCell>();
			for (int k = 0; k < cpcNodeList.getLength(); k++) {
				String[] split = cpcNodeList.item(k).getTextContent().trim().split("\\s+");
				cpcs.add(new StringCell(
						split[0] + "," + split[1] + "," + split[2] + "," + split[3] + "/" + split[4] + "," + split[5]));
			}

			NodeList ipcrNodeList = (NodeList) ipcrExpression.evaluate(doc, XPathConstants.NODESET);
			List<StringCell> ipcrs = new ArrayList<StringCell>();
			for (int k = 0; k < ipcrNodeList.getLength(); k++) {
				ipcrs.add(new StringCell(ipcrNodeList.item(k).getTextContent().trim()));
			}

			String familyid = (String) familyidExpression.evaluate(doc, XPathConstants.STRING);
			DataCell familyidCell = (familyid == null || familyid.isEmpty()) ? new MissingCell("No familyid available.")
					: new StringCell(familyid);

			DataCell cpcOutcell = (!cpcs.isEmpty()) ? CollectionCellFactory.createListCell(cpcs)
					: new MissingCell("No cpcs available.");
			DataCell ipcrOutcell = (!ipcrs.isEmpty()) ? CollectionCellFactory.createListCell(ipcrs)
					: new MissingCell("No ipcrs available.");

			return new DataCell[] { cpcOutcell, ipcrOutcell, familyidCell};
		} catch (Exception e) {
			LOGGER.info("Server returned error during parsing: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()), new MissingCell(e.getMessage()), new MissingCell(e.getMessage())};
		}

	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { ListCell.getCollectionType(StringCell.TYPE),
				ListCell.getCollectionType(StringCell.TYPE), StringCell.TYPE};
		String[] columnNames = new String[] { "CPCs", "IPCRs", "familyid"};

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}
}
