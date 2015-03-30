package org.knime.knip.patents.util.nodes.search;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.knip.base.node.ValueToCellsNodeDialog;

public class OPSSearchNodeDialog extends ValueToCellsNodeDialog<StringValue> {

	@Override
	public void addDialogComponents() {

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(OPSSearchNodeModel
						.createStartRangeModel(), "Start Range", 1));

		addDialogComponent(
				"Options",
				"Search Parameters",
				new DialogComponentNumber(OPSSearchNodeModel
						.createEndRangeModel(), "End Range", 1));
	}
}
