package org.knime.knip.patents.nodes.search;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.util.Pair;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeModel;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PatentSearchNodeModel extends AbstractPatentDownloadNodeModel {

	private static final int MAX_ALLOWED_PER_REQUEST = 100;

	static SettingsModelIntegerBounded createStartRangeModel() {
		return new SettingsModelIntegerBounded("startRange", 1, 1,
				Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createEndRangeModel() {
		return new SettingsModelIntegerBounded("endRange", 50, 1,
				Integer.MAX_VALUE);
	}

	private final SettingsModelIntegerBounded m_startRange = createStartRangeModel();
	private final SettingsModelIntegerBounded m_endRange = createEndRangeModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		super.addSettingsModels(settingsModels);

		settingsModels.add(m_startRange);
		settingsModels.add(m_endRange);
	}

	@Override
	protected DataCell[] compute(StringValue queryValue) throws Exception {

		// check if we need to get an access token
		String consumerKey = m_consumerKey.getStringValue();
		String consumerSecret = m_consumerSecret.getStringValue();

		String accessToken = null;
		if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
			accessToken = AccessTokenGenerator.getInstance().getAccessToken(
					consumerKey, consumerSecret);
		}

		List<Patent> patentsList = new ArrayList<>();
		for (int fromRange = m_startRange.getIntValue(); fromRange < m_endRange
				.getIntValue(); fromRange += MAX_ALLOWED_PER_REQUEST) {
			// build search url
			URL queryURL = getQueryURL(
					queryValue.getStringValue(),
					fromRange,
					Math.min(m_endRange.getIntValue(), (fromRange
							+ MAX_ALLOWED_PER_REQUEST - 1)));
						
			HttpURLConnection searchHttpConnection = (HttpURLConnection) queryURL
					.openConnection();

			
			// set accesstoken if available
			if (accessToken != null) {
				searchHttpConnection.setRequestProperty("Authorization",
						"Bearer " + accessToken);
			}

			// check html respone
			int searchResponeCode = searchHttpConnection
					.getResponseCode();
			if (searchResponeCode >= 400) {
				throw new RuntimeException("Server returned error ["
						+ searchResponeCode + ", "
						+ searchHttpConnection.getResponseMessage() + "]"
						+ "\n Respone: \n"
						+ parseErrorMessage(searchHttpConnection));
			}

			// download doc
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(searchHttpConnection.getInputStream());

			NodeList patentsNodeList = doc.getElementsByTagName("document-id");
			for (int i = 0; i < patentsNodeList.getLength(); i++) {
				Element patent = (Element) patentsNodeList.item(i);

				if (!patent.getAttribute("document-id-type").equals("docdb")) {
					continue;
				}

				String country = patent.getElementsByTagName("country").item(0)
						.getTextContent();
				String docnumber = patent.getElementsByTagName("doc-number")
						.item(0).getTextContent();
				String kind = patent.getElementsByTagName("kind").item(0)
						.getTextContent();

				patentsList.add(new Patent(country, docnumber, kind));
			}
		}

		List<StringCell> cells = new ArrayList<>();
		for (Patent patent : patentsList) {
			cells.add(new StringCell(patent.getCountry() + "."
					+ patent.getDocnumber() + "." + patent.getKind()));

		}

		return new DataCell[] { CollectionCellFactory.createListCell(cells) };
	}

	private URL getQueryURL(String query, int startRange, int endRange)
			throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/search?q="
						+ query + "&Range=" + startRange + "-" + endRange);
	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {

		DataType[] datatypes = new DataType[] { ListCell
				.getCollectionType(StringCell.TYPE) };
		String[] columnNames = new String[] { "Patent ID" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

	private static class Patent {

		private final String country;
		private final String docnumber;
		private final String kind;

		public Patent(String country, String docnumber, String kind) {
			this.country = country;
			this.docnumber = docnumber;
			this.kind = kind;
		}

		public String getCountry() {
			return country;
		}

		public String getDocnumber() {
			return docnumber;
		}

		public String getKind() {
			return kind;
		}

	}
}
