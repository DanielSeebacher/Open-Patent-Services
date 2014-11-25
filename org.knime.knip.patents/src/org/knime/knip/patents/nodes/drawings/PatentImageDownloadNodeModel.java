package org.knime.knip.patents.nodes.drawings;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.Pair;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeModel;
import org.knime.knip.patents.util.AccessTokenGenerator;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class PatentImageDownloadNodeModel extends
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
		URL imageOverviewURL = getImageOverviewURL(patentIDValue
				.getStringValue());
		HttpURLConnection imageOverviewHttpConnection = (HttpURLConnection) imageOverviewURL
				.openConnection();

		// set accesstoken if available
		if (accessToken != null) {
			imageOverviewHttpConnection.setRequestProperty("Authorization",
					"Bearer " + accessToken);
		}

		// check html respone
		int overviewResponseCode = imageOverviewHttpConnection
				.getResponseCode();
		if (overviewResponseCode >= 400) {
			if (overviewResponseCode == 404) {
				return new DataCell[] { new MissingCell("404, not found") };
			} else {
				throw new RuntimeException("Server returned error ["
						+ overviewResponseCode + ", "
						+ imageOverviewHttpConnection.getResponseMessage()
						+ "]");
			}
		}

		// download doc and extract image link and count
		DrawingsContentHandler dch = new DrawingsContentHandler();
		XMLReader myReader = XMLReaderFactory.createXMLReader();
		myReader.setContentHandler(dch);
		myReader.parse(new InputSource(imageOverviewHttpConnection
				.getInputStream()));

		// download images to tmp directory and save absolute paths
		List<StringCell> pathToImages = new ArrayList<>();
		for (int i = 0; i < dch.getNumberOfPages(); i++) {
			URL absoluteImageURL = getAbsoluteImageURL(dch.getLink(),
					(dch.getStartPage() + i));
			HttpURLConnection absoluteImageHttpConnection = (HttpURLConnection) absoluteImageURL
					.openConnection();

			// check html respone
			int absoluteResponseCode = absoluteImageHttpConnection
					.getResponseCode();
			if (absoluteResponseCode >= 400) {
				throw new RuntimeException("Server returned error ["
						+ absoluteResponseCode + ", "
						+ absoluteImageHttpConnection.getResponseMessage()
						+ "]");
			}

			ReadableByteChannel rbc = Channels
					.newChannel(absoluteImageHttpConnection.getInputStream());
			File imgFile = File.createTempFile(
					"patent_" + patentIDValue.getStringValue(),
					"__" + (dch.getStartPage() + i) + ".tiff");
			FileOutputStream fos = new FileOutputStream(imgFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			pathToImages.add(new StringCell(imgFile.getAbsolutePath()));
		}

		return new DataCell[] { CollectionCellFactory
				.createListCell(pathToImages) };
	}

	@Override
	protected Pair<DataType[], String[]> getDataOutTypeAndName() {
		DataType[] datatypes = new DataType[] { ListCell
				.getCollectionType(StringCell.TYPE), };
		String[] columnNames = new String[] { "Path to Images" };

		return new Pair<DataType[], String[]>(datatypes, columnNames);
	}

	private URL getAbsoluteImageURL(String link, int i)
			throws MalformedURLException {
		return new URL("http://ops.epo.org/3.1/rest-services/" + link
				+ ".tiff?Range=" + i);
	}

	private URL getImageOverviewURL(String patentID)
			throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ patentID + "/images");
	}
}
