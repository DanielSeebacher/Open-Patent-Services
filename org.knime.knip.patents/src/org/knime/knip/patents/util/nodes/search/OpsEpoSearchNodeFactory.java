package org.knime.knip.patents.util.nodes.search;

import org.knime.core.data.StringValue;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.base.node.ValueToCellsNodeFactory;
import org.knime.knip.base.node.ValueToCellsNodeModel;

public class OpsEpoSearchNodeFactory extends
		ValueToCellsNodeFactory<StringValue> {

	@Override
	protected ValueToCellsNodeDialog<StringValue> createNodeDialog() {
		return new OpsEpoSearchNodeDialog();
	}

	@Override
	public ValueToCellsNodeModel<StringValue> createNodeModel() {
		return new OpsEpoSearchNodeModel();
	}

}
