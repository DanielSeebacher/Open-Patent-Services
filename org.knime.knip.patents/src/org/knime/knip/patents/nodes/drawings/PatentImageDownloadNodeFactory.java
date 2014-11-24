package org.knime.knip.patents.nodes.drawings;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class PatentImageDownloadNodeFactory extends
		NodeFactory<PatentImageDownloadNodeModel> {

	@Override
	public PatentImageDownloadNodeModel createNodeModel() {
		return new PatentImageDownloadNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<PatentImageDownloadNodeModel> createNodeView(int viewIndex,
			PatentImageDownloadNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new PatentImageDownloadNodeDialog();
	}

}
