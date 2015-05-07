package org.knime.knip.patents.util.nodes.fullcycle;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.knip.patents.util.nodes.AbstractOPSBulkNodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FullCycleNodeModel extends AbstractOPSBulkNodeModel<StringValue> {

	public FullCycleNodeModel() {
		super(100, StringValue.class);
	}

	@Override
	public List<Pair<String, DataType>> getAdditionalOutSpec() {

		List<Pair<String, DataType>> additionalOutSpec = new ArrayList<Pair<String, DataType>>();

		additionalOutSpec.add(new ValuePair<String, DataType>("Title",
				StringCell.TYPE));
		additionalOutSpec.add(new ValuePair<String, DataType>("Abstract",
				StringCell.TYPE));

		additionalOutSpec.add(new ValuePair<String, DataType>("CPCs", ListCell
				.getCollectionType(StringCell.TYPE)));
		additionalOutSpec.add(new ValuePair<String, DataType>("IPCRs", ListCell
				.getCollectionType(StringCell.TYPE)));

		additionalOutSpec.add(new ValuePair<String, DataType>("Applicants",
				ListCell.getCollectionType(StringCell.TYPE)));
		additionalOutSpec.add(new ValuePair<String, DataType>("Inventors",
				ListCell.getCollectionType(StringCell.TYPE)));

		return additionalOutSpec;
	}

	@Override
	protected List<DataCell[]> compute(List<StringValue> values)
			throws Exception {

		List<DataCell[]> results = new ArrayList<DataCell[]>();

		URL url = getURL(values.toArray(new StringValue[values.size()]));
		Document doc = super.downloadDocument(url);

		NodeList nl = doc.getElementsByTagName("exchange-document");
		for (int i = 0; i < nl.getLength(); i++) {

			DataCell[] row = new DataCell[getAdditionalOutSpec().size()];

			Element currentElement = (Element) nl.item(i);

			row[0] = getTitle(currentElement);
			row[1] = getAbstract(currentElement);
			row[2] = getCPCs(currentElement);
			row[3] = getIPCRs(currentElement);
			row[4] = getApplicants(currentElement);
			row[5] = getInventors(currentElement);

			results.add(row);
		}

		return results;
	}

	@Override
	protected URL getURL(StringValue... stringValue)
			throws MalformedURLException {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stringValue.length; i++) {
			if (i > 0) {
				sb.append(",");
			}

			sb.append(stringValue[i].getStringValue());
		}

		return new URL(
				"http://ops.epo.org/3.1/rest-services/published-data/publication/docdb/"
						+ sb.toString() + "/full-cycle");
	}

	private DataCell getTitle(Element patentElement) {

		String title = "";

		NodeList inventionTitleNodes = patentElement
				.getElementsByTagName("invention-title");
		for (int j = 0; j < inventionTitleNodes.getLength(); j++) {
			Element inventionTitleNode = (Element) inventionTitleNodes.item(j);
			if (inventionTitleNode.getAttribute("lang").equalsIgnoreCase("en")) {
				title = inventionTitleNode.getTextContent();
			}
		}

		return (title == null || title.isEmpty()) ? new MissingCell(
				"No english title available!") : new StringCell(title);
	}

	private DataCell getAbstract(Element patentElement) {

		String abztract = "";

		NodeList abstractNodes = patentElement.getElementsByTagName("abstract");
		for (int j = 0; j < abstractNodes.getLength(); j++) {
			Element inventionTitleNode = (Element) abstractNodes.item(j);
			if (inventionTitleNode.getAttribute("lang").equalsIgnoreCase("en")) {
				abztract = inventionTitleNode.getTextContent()
						.replaceAll("\\n+", "").trim();
			}
		}

		return (abztract == null || abztract.isEmpty()) ? new MissingCell(
				"No english title available!") : new StringCell(abztract);
	}

	private DataCell getCPCs(Element patentElement) {

		Set<String> cpcs = new HashSet<String>();

		NodeList cpcNodes = patentElement
				.getElementsByTagName("patent-classification");
		for (int j = 0; j < cpcNodes.getLength(); j++) {
			Element cpcNode = (Element) cpcNodes.item(j);

			NodeList classificationSchemeNodeList = cpcNode
					.getElementsByTagName("classification-scheme");

			if (classificationSchemeNodeList == null
					|| classificationSchemeNodeList.getLength() == 0) {
				continue;
			}

			for (int k = 0; k < classificationSchemeNodeList.getLength(); k++) {
				NamedNodeMap attributes = classificationSchemeNodeList.item(k)
						.getAttributes();
				if (attributes == null || attributes.getLength() == 0) {
					continue;
				}

				Node namedItem = attributes.getNamedItem("scheme");
				if (namedItem == null
						|| !namedItem.getTextContent().equalsIgnoreCase("CPC")) {
					continue;
				}
			}

			cpcs.add(cpcNode.getTextContent().replaceAll("\\n+", "")
					.replaceAll("\\s+", " ").trim());
		}

		return (cpcs.isEmpty()) ? new MissingCell("No CPCs available!") : super
				.stringsToListCell(cpcs);
	}

	private DataCell getIPCRs(Element patentElement) {

		Set<String> ipcrs = new HashSet<String>();

		NodeList ipcrNodes = patentElement
				.getElementsByTagName("classification-ipcr");
		for (int j = 0; j < ipcrNodes.getLength(); j++) {
			Element ipcrNode = (Element) ipcrNodes.item(j);
			ipcrs.add(ipcrNode.getTextContent().replaceAll("\\n+", "")
					.replaceAll("\\s+", " ").trim());
		}

		return (ipcrs.isEmpty()) ? new MissingCell("No IPCRs available!")
				: super.stringsToListCell(ipcrs);
	}

	private DataCell getApplicants(Element patentElement) {

		Set<String> applicants = new HashSet<String>();

		NodeList applicantNodes = patentElement
				.getElementsByTagName("applicant");
		for (int j = 0; j < applicantNodes.getLength(); j++) {
			Element applicantNode = (Element) applicantNodes.item(j);
			if (!applicantNode.getAttribute("data-format").equalsIgnoreCase(
					"original")) {
				continue;
			}

			applicants.add(applicantNode.getTextContent()
					.replaceAll("\\n+", "").replaceAll("\\s+", " ").trim());
		}

		return (applicants.isEmpty()) ? new MissingCell(
				"No applicants available!") : super
				.stringsToListCell(applicants);
	}

	private DataCell getInventors(Element patentElement) {

		Set<String> inventors = new HashSet<String>();

		NodeList applicantNodes = patentElement
				.getElementsByTagName("inventor");
		for (int j = 0; j < applicantNodes.getLength(); j++) {
			Element applicantNode = (Element) applicantNodes.item(j);
			if (!applicantNode.getAttribute("data-format").equalsIgnoreCase(
					"original")) {
				continue;
			}

			inventors.add(applicantNode.getTextContent().replaceAll("\\n+", "")
					.replaceAll("\\s+", " ").trim());
		}

		return (inventors.isEmpty()) ? new MissingCell(
				"No inventors available!") : super.stringsToListCell(inventors);
	}
}
