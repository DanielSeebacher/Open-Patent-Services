package org.knime.knip.patents.util.nodes.fullcycle;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.knip.patents.util.AbstractOPSModel;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class OPSFullCycleNodeModel extends
		AbstractOPSModel {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OPSFullCycleNodeModel.class);
	
	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {
		try {
			// check if we need to get an access token
			String consumerKey = m_consumerKey.getStringValue();
			String consumerSecret = m_consumerSecret.getStringValue();

			String accessToken = null;
			if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
				accessToken = AccessTokenGenerator.getInstance()
						.getAccessToken(consumerKey, consumerSecret);
			}

			// create URL and HttpURLConnection
			URL fullCycleURL = getFullCycleURL(patentIDValue.getStringValue());
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
				LOGGER.warn("Server returned error before parsing: " + e.getMessage(), e);
				return new DataCell[] { new MissingCell(e.getMessage()),
						new MissingCell(e.getMessage()),
						new MissingCell(e.getMessage()),
						new MissingCell(e.getMessage()),
						new MissingCell(e.getMessage()) };
			}

			throttle(fullCycleHttpConnection, "retrieval");

			// download doc
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(fullCycleHttpConnection.getInputStream());

			// extract cpc numbers
			Set<StringCell> cpcStringCells = new HashSet<>();
			NodeList cpcNodeList = doc
					.getElementsByTagName("patent-classification");
			for (int i = 0; i < cpcNodeList.getLength(); i++) {
				Element patentClassification = (Element) cpcNodeList.item(i);

				// check classification schema
				try {
					Element classificationScheme = (Element) patentClassification
							.getElementsByTagName("classification-scheme")
							.item(0);

					if (!classificationScheme.getAttribute("scheme")
							.equalsIgnoreCase("CPC")) {
						continue;
					}
				} catch (Exception e) {
					setWarningMessage("Error parsing classification scheme "
							+ e.getMessage());
					continue;
				}

				String cpcString = "";
				try {
					cpcString += ((Element) patentClassification
							.getElementsByTagName("section").item(0))
							.getTextContent();
					cpcString += ((Element) patentClassification
							.getElementsByTagName("class").item(0))
							.getTextContent();

					cpcString += ((Element) patentClassification
							.getElementsByTagName("subclass").item(0))
							.getTextContent();
					cpcString += ((Element) patentClassification
							.getElementsByTagName("main-group").item(0))
							.getTextContent();

					cpcString += "/";

					cpcString += ((Element) patentClassification
							.getElementsByTagName("subgroup").item(0))
							.getTextContent();

					cpcString += " ";

					cpcString += ((Element) patentClassification
							.getElementsByTagName("classification-value").item(
									0)).getTextContent();
				} catch (Exception e) {
					setWarningMessage("Error parsing cpc string "
							+ e.getMessage());
					continue;
				}

				cpcStringCells.add(new StringCell(cpcString));
			}

			// extract applicants
			Set<StringCell> applicantStringCells = new HashSet<>();
			NodeList applicantsNodeList = doc.getElementsByTagName("applicant");
			for (int i = 0; i < applicantsNodeList.getLength(); i++) {
				Element item = (Element) applicantsNodeList.item(i);

				if (!item.getAttribute("data-format").equals("original")) {
					continue;
				}

				applicantStringCells
						.add(new StringCell(item.getElementsByTagName("name")
								.item(0).getTextContent()));
			}

			// extract inventors
			Set<StringCell> inventorsStringCells = new HashSet<>();
			NodeList inventorsNodeList = doc.getElementsByTagName("inventor");
			for (int i = 0; i < inventorsNodeList.getLength(); i++) {
				Element item = (Element) inventorsNodeList.item(i);

				if (!item.getAttribute("data-format").equals("original")) {
					continue;
				}

				inventorsStringCells
						.add(new StringCell(item.getElementsByTagName("name")
								.item(0).getTextContent()));
			}

			// extract title
			String title = doc.getElementsByTagName("invention-title").item(0)
					.getTextContent();

			// extract abstract
			String abstractString = doc.getElementsByTagName("abstract")
					.item(0).getTextContent().trim();

			DataCell[] cells = new DataCell[5];
			cells[0] = CollectionCellFactory.createListCell(cpcStringCells);
			cells[1] = CollectionCellFactory
					.createListCell(applicantStringCells);
			cells[2] = CollectionCellFactory
					.createListCell(inventorsStringCells);
			cells[3] = new StringCell(title);
			cells[4] = new StringCell(abstractString);

			return cells;

		} catch (Exception e) {
			LOGGER.warn(
					"Server returned during parsing: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()),
					new MissingCell(e.getMessage()),
					new MissingCell(e.getMessage()),
					new MissingCell(e.getMessage()),
					new MissingCell(e.getMessage()) };
		}
	}

	private URL getFullCycleURL(String patentID) throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ patentID + "/full-cycle");
	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] {
				ListCell.getCollectionType(StringCell.TYPE),
				ListCell.getCollectionType(StringCell.TYPE),
				ListCell.getCollectionType(StringCell.TYPE), StringCell.TYPE,
				StringCell.TYPE };
		String[] columnNames = new String[] { "CPC", "Applicants", "Inventors",
				"Title", "Abstract" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}
}
