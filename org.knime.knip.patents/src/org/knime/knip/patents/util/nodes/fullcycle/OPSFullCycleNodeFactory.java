package org.knime.knip.patents.util.nodes.fullcycle;

import org.knime.core.data.StringValue;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.base.node.ValueToCellsNodeFactory;
import org.knime.knip.base.node.ValueToCellsNodeModel;

public class OPSFullCycleNodeFactory extends
		ValueToCellsNodeFactory<StringValue> {

	@Override
	protected ValueToCellsNodeDialog<StringValue> createNodeDialog() {
		return new ValueToCellsNodeDialog<StringValue>() {
			@Override
			public void addDialogComponents() {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public ValueToCellsNodeModel<StringValue> createNodeModel() {
		return new OPSFullCycleNodeModel();
	}

}
