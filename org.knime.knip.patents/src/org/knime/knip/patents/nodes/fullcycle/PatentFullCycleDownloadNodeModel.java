package org.knime.knip.patents.nodes.fullcycle;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.Pair;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeModel;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PatentFullCycleDownloadNodeModel extends
		AbstractPatentDownloadNodeModel {

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		// check if we need to get an access token
		String consumerKey = m_consumerKey.getStringValue();
		String consumerSecret = m_consumerSecret.getStringValue();

		String accessToken = null;
		if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
			accessToken = AccessTokenGenerator.getInstance().getAccessToken(
					consumerKey, consumerSecret);
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
		int overviewResponseCode = fullCycleHttpConnection.getResponseCode();
		if (overviewResponseCode >= 400) {
			throw new RuntimeException("Server returned error ["
					+ overviewResponseCode + ", "
					+ fullCycleHttpConnection.getResponseMessage() + "]");
		}

		// download doc
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(fullCycleHttpConnection.getInputStream());

		// extract applicants
		Set<StringCell> applicantStringCells = new HashSet<>();
		NodeList applicantsNodeList = doc.getElementsByTagName("applicant");
		for (int i = 0; i < applicantsNodeList.getLength(); i++) {
			Element item = (Element) applicantsNodeList.item(i);

			if (!item.getAttribute("data-format").equals("original")) {
				continue;
			}

			applicantStringCells.add(new StringCell(item
					.getElementsByTagName("name").item(0).getTextContent()));
		}

		// extract inventors
		Set<StringCell> inventorsStringCells = new HashSet<>();
		NodeList inventorsNodeList = doc.getElementsByTagName("inventor");
		for (int i = 0; i < inventorsNodeList.getLength(); i++) {
			Element item = (Element) inventorsNodeList.item(i);

			if (!item.getAttribute("data-format").equals("original")) {
				continue;
			}

			inventorsStringCells.add(new StringCell(item
					.getElementsByTagName("name").item(0).getTextContent()));
		}

		// extract title
		String title = doc.getElementsByTagName("invention-title").item(0)
				.getTextContent();

		// extract abstract
		String abstractString = doc.getElementsByTagName("abstract").item(0)
				.getTextContent().trim();

		DataCell[] cells = new DataCell[4];
		cells[0] = CollectionCellFactory.createListCell(applicantStringCells);
		cells[1] = CollectionCellFactory.createListCell(inventorsStringCells);
		cells[2] = new StringCell(title);
		cells[3] = new StringCell(abstractString);

		return cells;
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
				ListCell.getCollectionType(StringCell.TYPE), StringCell.TYPE,
				StringCell.TYPE };
		String[] columnNames = new String[] { "Applicants", "Inventors",
				"Title", "Abstract" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}
}
