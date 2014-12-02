package org.knime.knip.patents.util;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ValueToCellsNodeModel;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class AbstractPatentDownloadNodeModel extends
		ValueToCellsNodeModel<StringValue> {

	public static SettingsModelString createConsumerKeyModel() {
		return new SettingsModelString("m_consumerKey", "");
	}

	public static SettingsModelString createConsumerSecretModel() {
		return new SettingsModelString("m_consumerSecret", "");
	}

	protected final SettingsModelString m_consumerKey = createConsumerKeyModel();
	protected final SettingsModelString m_consumerSecret = createConsumerSecretModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_consumerKey);
		settingsModels.add(m_consumerSecret);
	}

	protected String parseErrorMessage(HttpURLConnection connection)
			throws SAXException, IOException, ParserConfigurationException, TransformerException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(connection.getErrorStream());

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		//initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		String xmlString = result.getWriter().toString();
		
		return xmlString;
	}
}
