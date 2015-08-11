package org.knime.knip.patents.util.nodes.drawings;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

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
import org.knime.knip.patents.KNIMEOPSPlugin;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.knime.knip.patents.util.nodes.AbstractOPSNodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class OPSImagesNodeModel extends AbstractOPSNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(OPSImagesNodeModel.class);

	@Override
	protected DataCell[] compute(StringValue patentIDValue) throws Exception {

		try {
			// check if we need to get an access token
			String consumerKey = KNIMEOPSPlugin.getOAuth2ConsumerKey();
			String consumerSecret = KNIMEOPSPlugin.getOAuth2ConsumerSecret();

			String accessToken = null;
			if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
				accessToken = AccessTokenGenerator.getInstance().getAccessToken(consumerKey, consumerSecret);
			}

			// create URL and HttpURLConnection
			URL imageOverviewURL = getURL(patentIDValue.getStringValue());
			HttpURLConnection imageOverviewHttpConnection = (HttpURLConnection) imageOverviewURL.openConnection();

			// set accesstoken if available
			if (accessToken != null) {
				imageOverviewHttpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
			}

			// check html respone
			try {
				checkResponse(imageOverviewHttpConnection);
			} catch (Exception e) {
				LOGGER.warn("Server returned error: " + e.getMessage(), e);
				return new DataCell[] { new MissingCell(e.getMessage()) };
			}

			// download doc and extract image link and count
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(imageOverviewHttpConnection.getInputStream());
			imageOverviewHttpConnection.disconnect();

			// download images to tmp directory and save absolute paths

			List<StringCell> pathToImages = new ArrayList<>();

			NodeList documentInstances = doc.getElementsByTagName("ops:document-instance");
			for (int l = 0; l < documentInstances.getLength(); l++) {
				Element documentInstance = (Element) documentInstances.item(l);
				if (documentInstance.getAttribute("desc").equalsIgnoreCase("Drawing")) {

					int numPages = Integer.parseInt(documentInstance.getAttribute("number-of-pages"));
					String link = documentInstance.getAttribute("link");

					for (int i = 0; i < numPages; i++) {
						URL absoluteImageURL = getAbsoluteImageURL(link, (1 + i));
						HttpURLConnection absoluteImageHttpConnection = (HttpURLConnection) absoluteImageURL
								.openConnection();

						if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
							accessToken = AccessTokenGenerator.getInstance().getAccessToken(consumerKey,
									consumerSecret);
						}

						// set accesstoken if available
						if (accessToken != null) {
							absoluteImageHttpConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
						}

						// check html respone
						try {
							checkResponse(absoluteImageHttpConnection);
						} catch (Exception e) {
							LOGGER.warn("Server returned error: " + e.getMessage(), e);
							return new DataCell[] { new MissingCell(e.getMessage()) };
						}

						throttle(absoluteImageHttpConnection, "images");

						ReadableByteChannel rbc = Channels.newChannel(absoluteImageHttpConnection.getInputStream());
						File imgFile = File.createTempFile("patent_" + patentIDValue.getStringValue(),
								"__" + (1 + i) + ".tiff");
						FileOutputStream fos = new FileOutputStream(imgFile);
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						pathToImages.add(new StringCell(imgFile.getAbsolutePath()));
						fos.close();
					}
				}
			}

			return new DataCell[] { CollectionCellFactory.createListCell(pathToImages) };
		} catch (Exception e) {
			LOGGER.warn("Other error: " + e.getMessage(), e);
			return new DataCell[] { new MissingCell(e.getMessage()) };
		}

	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { ListCell.getCollectionType(StringCell.TYPE), };
		String[] columnNames = new String[] { "Path to Images" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

	private URL getAbsoluteImageURL(String link, int i) throws MalformedURLException {
		return new URL("http://ops.epo.org/3.1/rest-services/" + link + ".tiff?Range=" + i);
	}

	@Override
	public URL getURL(String... input) throws MalformedURLException {
		return new URL("http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/" + input[0] + "/images");
	}
}
