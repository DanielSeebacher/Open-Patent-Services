package org.knime.knip.patents.nodes.drawings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeModel;
import org.knime.knip.patents.util.AccessToken;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class PatentImageDownloadNodeModel extends
		AbstractPatentDownloadNodeModel {

	static SettingsModelString createCountryCodeModel() {
		return new SettingsModelString("m_country", "");
	}

	static SettingsModelString createDocNumberModel() {
		return new SettingsModelString("m_docnumber", "");
	}

	static SettingsModelString createKindCodeModel() {
		return new SettingsModelString("m_kind", "");
	}

	private final SettingsModelString m_country = createCountryCodeModel();
	private final SettingsModelString m_docnumber = createDocNumberModel();
	private final SettingsModelString m_kind = createKindCodeModel();

	protected PatentImageDownloadNodeModel() {
		super(1, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		// check if we need to get an access token
		String consumerKey = m_consumerKey.getStringValue();
		String consumerSecret = m_consumerSecret.getStringValue();

		String accessToken = null;
		if (consumerKey.length() > 0 && consumerSecret.length() > 0) {
			accessToken = AccessToken.getAccessToken(consumerKey,
					consumerSecret);
		}

		// get index of country, docnumber and kind
		int countryIdx = getColIdx(inData[0].getDataTableSpec(), "Country",
				m_country);
		int docnumberIdx = getColIdx(inData[0].getDataTableSpec(), "DocNumber",
				m_docnumber);
		int kindIdx = getColIdx(inData[0].getDataTableSpec(), "Kind", m_kind);

		// for each row
		BufferedDataContainer con = exec.createDataContainer(getOutSpec());
		CloseableRowIterator iterator = inData[0].iterator();
		double rowCount = 0;
		while (iterator.hasNext()) {

			// get country, docnumber and kind
			DataRow currentRow = iterator.next();
			StringCell countryCell = (StringCell) currentRow
					.getCell(countryIdx);
			StringCell docnumberCell = (StringCell) currentRow
					.getCell(docnumberIdx);
			StringCell kindCell = (StringCell) currentRow.getCell(kindIdx);

			// create URL and HttpURLConnection
			URL imageOverviewURL = getImageOverviewURL(
					countryCell.getStringValue(),
					docnumberCell.getStringValue(), kindCell.getStringValue());
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
				throw new RuntimeException("Server returned error ["
						+ overviewResponseCode + ", "
						+ imageOverviewHttpConnection.getResponseMessage()
						+ "]");
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
						.newChannel(absoluteImageHttpConnection
								.getInputStream());
				File imgFile = File.createTempFile(
						"patent_" + countryCell.getStringValue() + "_"
								+ docnumberCell.getStringValue() + "_"
								+ kindCell.getStringValue() + "__",
						"__" + (dch.getStartPage() + i) + ".tiff");
				FileOutputStream fos = new FileOutputStream(imgFile);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				pathToImages.add(new StringCell(imgFile.getAbsolutePath()));
			}

			DataCell[] cells = new DataCell[4];
			cells[0] = countryCell;
			cells[1] = docnumberCell;
			cells[2] = kindCell;
			cells[3] = CollectionCellFactory.createListCell(pathToImages);

			con.addRowToTable(new DefaultRow(currentRow.getKey(), cells));

			exec.setProgress((++rowCount) / (double) inData[0].getRowCount());
			exec.checkCanceled();
		}

		con.close();

		return new BufferedDataTable[] { con.getTable() };
	}

	private URL getAbsoluteImageURL(String link, int i)
			throws MalformedURLException {
		return new URL("http://ops.epo.org/3.1/rest-services/" + link
				+ ".tiff?Range=" + i);
	}

	private URL getImageOverviewURL(String country, String docnumber,
			String kind) throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ country + "." + docnumber + "." + kind + "/images");
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		int countryIdx = getColIdx(inSpecs[0], "Country", m_country);
		int docnumberIdx = getColIdx(inSpecs[0], "DocNumber", m_docnumber);
		int kindIdx = getColIdx(inSpecs[0], "Kind", m_kind);

		if (countryIdx == -1 || docnumberIdx == -1 || kindIdx == -1) {
			throw new IllegalArgumentException(
					"A Country, DocNumber and Kind have to be selected!");
		}

		return new DataTableSpec[] { getOutSpec() };
	}

	private DataTableSpec getOutSpec() {
		DataColumnSpec[] outSpec = new DataColumnSpec[4];
		outSpec[0] = new DataColumnSpecCreator("Country", StringCell.TYPE)
				.createSpec();
		outSpec[1] = new DataColumnSpecCreator("DocNumber", StringCell.TYPE)
				.createSpec();
		outSpec[2] = new DataColumnSpecCreator("Kind", StringCell.TYPE)
				.createSpec();
		outSpec[3] = new DataColumnSpecCreator("Img Path",
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();

		return new DataTableSpec(outSpec);
	}

	private int getColIdx(final DataTableSpec inSpec, String name,
			SettingsModelString model) throws InvalidSettingsException {
		int colIdx = -1;
		if (null == model.getStringValue()) {
			return colIdx;
		}

		colIdx = inSpec.findColumnIndex(model.getStringValue());
		if (colIdx == -1) {
			if ((colIdx = NodeUtils.autoOptionalColumnSelection(inSpec, model,
					StringValue.class)) >= 0) {
				setWarningMessage("Auto-configure " + name + " Column: "
						+ model.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}

		return colIdx;
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_country.saveSettingsTo(settings);
		m_docnumber.saveSettingsTo(settings);
		m_kind.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		m_country.validateSettings(settings);
		m_docnumber.validateSettings(settings);
		m_kind.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_country.loadSettingsFrom(settings);
		m_docnumber.loadSettingsFrom(settings);
		m_kind.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// nothing to do
	}

}
