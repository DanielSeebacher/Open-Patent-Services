package org.knime.knip.patents.util.nodes.search;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.patents.util.AbstractOpsEpoModel;

public class OpsEpoSearchNodeDialog extends ValueToCellsNodeDialog<StringValue> {

	@Override
	public void addDialogComponents() {
		addDialogComponent(
				"Options",
				"OAuth2 Settings",
				new DialogComponentString(AbstractOpsEpoModel
						.createConsumerKeyModel(), "Consumer Key"));

		addDialogComponent(
				"Options",
				"OAuth2 Settings",
				new DialogComponentString(AbstractOpsEpoModel
						.createConsumerSecretModel(), "Consumer Secret"));

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(OpsEpoSearchNodeModel
						.createStartRangeModel(), "Start Range", 1));

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(OpsEpoSearchNodeModel
						.createEndRangeModel(), "End Range", 1));
	}
}
