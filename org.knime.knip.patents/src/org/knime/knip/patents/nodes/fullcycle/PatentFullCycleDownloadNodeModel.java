package org.knime.knip.patents.nodes.fullcycle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PatentFullCycleDownloadNodeModel extends
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

	protected PatentFullCycleDownloadNodeModel() {
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
			URL fullCycleURL = getFullCycleURL(countryCell.getStringValue(),
					docnumberCell.getStringValue(), kindCell.getStringValue());
			HttpURLConnection fullCycleHttpConnection = (HttpURLConnection) fullCycleURL
					.openConnection();

			// set accesstoken if available
			if (accessToken != null) {
				System.out.println(accessToken);
				fullCycleHttpConnection.setRequestProperty("Authorization",
						"Bearer " + accessToken);
			}

			// check html respone
			int overviewResponseCode = fullCycleHttpConnection
					.getResponseCode();
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

			DataCell[] cells = new DataCell[7];
			cells[0] = countryCell;
			cells[1] = docnumberCell;
			cells[2] = kindCell;
			cells[3] = CollectionCellFactory
					.createListCell(applicantStringCells);
			cells[4] = CollectionCellFactory
					.createListCell(inventorsStringCells);
			cells[5] = new StringCell(title);
			cells[6] = new StringCell(abstractString);

			con.addRowToTable(new DefaultRow(currentRow.getKey(), cells));

			exec.setProgress((++rowCount) / (double) inData[0].getRowCount());
			exec.checkCanceled();
		}

		con.close();

		return new BufferedDataTable[] { con.getTable() };
	}

	private URL getFullCycleURL(String country, String docnumber, String kind)
			throws MalformedURLException {
		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ country + "." + docnumber + "." + kind
						+ "/full-cycle");
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
		DataColumnSpec[] outSpec = new DataColumnSpec[7];
		outSpec[0] = new DataColumnSpecCreator("Country", StringCell.TYPE)
				.createSpec();
		outSpec[1] = new DataColumnSpecCreator("DocNumber", StringCell.TYPE)
				.createSpec();
		outSpec[2] = new DataColumnSpecCreator("Kind", StringCell.TYPE)
				.createSpec();
		outSpec[3] = new DataColumnSpecCreator("Applicants",
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		outSpec[4] = new DataColumnSpecCreator("Inventors",
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		outSpec[5] = new DataColumnSpecCreator("Title", StringCell.TYPE)
				.createSpec();
		outSpec[6] = new DataColumnSpecCreator("Abstract", StringCell.TYPE)
				.createSpec();

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
