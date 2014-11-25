package org.knime.knip.patents.nodes.search;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.patents.nodes.AbstractPatentDownloadNodeModel;

public class PatentSearchNodeDialog extends ValueToCellsNodeDialog<StringValue> {

	@Override
	public void addDialogComponents() {
		addDialogComponent(
				"Options",
				"OAuth2 Settings",
				new DialogComponentString(AbstractPatentDownloadNodeModel
						.createConsumerKeyModel(), "Consumer Key"));

		addDialogComponent(
				"Options",
				"OAuth2 Settings",
				new DialogComponentString(AbstractPatentDownloadNodeModel
						.createConsumerSecretModel(), "Consumer Secret"));

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(PatentSearchNodeModel
						.createStartRangeModel(), "Start Range", 1));

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(PatentSearchNodeModel
						.createEndRangeModel(), "End Range", 1));
	}
}
