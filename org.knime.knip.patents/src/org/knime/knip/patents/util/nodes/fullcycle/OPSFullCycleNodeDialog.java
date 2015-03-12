package org.knime.knip.patents.util.nodes.fullcycle;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.patents.util.AbstractOPSModel;

public class OPSFullCycleNodeDialog extends ValueToCellsNodeDialog<StringValue> {

	@Override
	public void addDialogComponents() {
		addDialogComponent(
				"Options",
				"Connection Settings",
				new DialogComponentBoolean(AbstractOPSModel
						.createUseThrottleControlModel(), "Throttling"));
		
		addDialogComponent(
				"Options",
				"Connection Settings",
				new DialogComponentString(AbstractOPSModel
						.createConsumerKeyModel(), "Consumer Key"));

		addDialogComponent(
				"Options",
				"Connection Settings",
				new DialogComponentString(AbstractOPSModel
						.createConsumerSecretModel(), "Consumer Secret"));
	}
}
