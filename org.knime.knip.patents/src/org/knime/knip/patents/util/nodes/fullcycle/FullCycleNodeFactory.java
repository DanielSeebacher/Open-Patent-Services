package org.knime.knip.patents.util.nodes.fullcycle;

import org.knime.core.data.StringValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.node.ValueToCellsNodeDialog;

public class FullCycleNodeFactory extends NodeFactory<FullCycleNodeModel> {

	@Override
	public FullCycleNodeModel createNodeModel() {
		return new FullCycleNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<FullCycleNodeModel> createNodeView(int viewIndex, FullCycleNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ValueToCellsNodeDialog<StringValue>() {

			@Override
			public void addDialogComponents() {
				// TODO Auto-generated method stub

			}

		};
	}
}
