package org.knime.knip.patents.nodes.fullcycle;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeDialog;

public class PatentFullCycleDownloadNodeDialog extends
		AbstractPatentDownloadNodeDialog {

	@SuppressWarnings("unchecked")
	public PatentFullCycleDownloadNodeDialog() {
		super();

		createNewGroup("Patent Informations");

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentFullCycleDownloadNodeModel.createCountryCodeModel(),
				"Country", 0, false, true, StringValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentFullCycleDownloadNodeModel.createDocNumberModel(),
				"DocNumber", 0, false, true, StringValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				PatentFullCycleDownloadNodeModel.createKindCodeModel(), "Kind", 0,
				false, true, StringValue.class));
	}

}
