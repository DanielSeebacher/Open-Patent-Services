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

			row[0] = new StringCell(getTitle(currentElement));
			row[1] = new StringCell(getAbstract(currentElement));

			row[2] = super.stringsToListCell(getCPCs(currentElement));

			row[3] = super.stringsToListCell(getIPCRs(currentElement));

			row[4] = super.stringsToListCell(getApplicants(currentElement));

			row[5] = super.stringsToListCell(getInventors(currentElement));

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

	private String getTitle(Element patentElement) {

		String title = "";

		NodeList inventionTitleNodes = patentElement
				.getElementsByTagName("invention-title");
		for (int j = 0; j < inventionTitleNodes.getLength(); j++) {
			Element inventionTitleNode = (Element) inventionTitleNodes.item(j);
			if (inventionTitleNode.getAttribute("lang").equalsIgnoreCase("en")) {
				title = inventionTitleNode.getTextContent();
			}
		}

		return title;
	}

	private String getAbstract(Element patentElement) {

		String abztract = "";

		NodeList abstractNodes = patentElement.getElementsByTagName("abstract");
		for (int j = 0; j < abstractNodes.getLength(); j++) {
			Element inventionTitleNode = (Element) abstractNodes.item(j);
			if (inventionTitleNode.getAttribute("lang").equalsIgnoreCase("en")) {
				abztract = inventionTitleNode.getTextContent()
						.replaceAll("\\n+", "").trim();
			}
		}

		return abztract;
	}

	private static Set<String> getCPCs(Element patentElement) {

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

		return cpcs;
	}

	private static Set<String> getIPCRs(Element patentElement) {

		Set<String> ipcrs = new HashSet<String>();

		NodeList ipcrNodes = patentElement
				.getElementsByTagName("classification-ipcr");
		for (int j = 0; j < ipcrNodes.getLength(); j++) {
			Element ipcrNode = (Element) ipcrNodes.item(j);
			ipcrs.add(ipcrNode.getTextContent().replaceAll("\\n+", "")
					.replaceAll("\\s+", " ").trim());
		}

		return ipcrs;
	}

	private static Set<String> getApplicants(Element patentElement) {

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

		return applicants;
	}

	private static Set<String> getInventors(Element patentElement) {

		Set<String> applicants = new HashSet<String>();

		NodeList applicantNodes = patentElement
				.getElementsByTagName("inventor");
		for (int j = 0; j < applicantNodes.getLength(); j++) {
			Element applicantNode = (Element) applicantNodes.item(j);
			if (!applicantNode.getAttribute("data-format").equalsIgnoreCase(
					"original")) {
				continue;
			}

			applicants.add(applicantNode.getTextContent()
					.replaceAll("\\n+", "").replaceAll("\\s+", " ").trim());
		}

		return applicants;
	}
}
