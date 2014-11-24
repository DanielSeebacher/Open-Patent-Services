package org.knime.knip.patents.nodes.drawings;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeDialog;

public class PatentImageDownloadNodeDialog extends
		AbstractPatentDownloadNodeDialog {

	@SuppressWarnings("unchecked")
	public PatentImageDownloadNodeDialog() {
		super();

		createNewGroup("Patent Informations");

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentImageDownloadNodeModel.createCountryCodeModel(),
				"Country", 0, false, true, StringValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentImageDownloadNodeModel.createDocNumberModel(),
				"DocNumber", 0, false, true, StringValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentImageDownloadNodeModel.createKindCodeModel(), "Kind", 0,
				false, true, StringValue.class));
	}

}
