package org.knime.knip.patents.features.meta.fishervectors;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FisherVectorsNodeFactory extends
		NodeFactory<FisherVectorsNodeModel> {

	@Override
	public FisherVectorsNodeModel createNodeModel() {
		return new FisherVectorsNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<FisherVectorsNodeModel> createNodeView(int viewIndex,
			FisherVectorsNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new FisherVectorsNodeDialog();
	}

}
