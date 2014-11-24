package org.knime.knip.patents.nodes.fullcycle;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class PatentFullCycleDownloadNodeFactory extends
		NodeFactory<PatentFullCycleDownloadNodeModel> {

	@Override
	public PatentFullCycleDownloadNodeModel createNodeModel() {
		return new PatentFullCycleDownloadNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<PatentFullCycleDownloadNodeModel> createNodeView(int viewIndex,
			PatentFullCycleDownloadNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new PatentFullCycleDownloadNodeDialog();
	}

}
