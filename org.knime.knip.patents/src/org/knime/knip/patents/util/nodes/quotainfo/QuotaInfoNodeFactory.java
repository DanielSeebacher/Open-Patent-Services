package org.knime.knip.patents.util.nodes.quotainfo;

import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

public class QuotaInfoNodeFactory extends
		TwoValuesToCellNodeFactory<StringValue, StringValue> {

	@Override
	public TwoValuesToCellNodeModel<StringValue, StringValue, ? extends DataCell> createNodeModel() {
		return new QuotaInfoNodeModel();
	}

	@Override
	protected TwoValuesToCellNodeDialog<StringValue, StringValue> createNodeDialog() {
		return new TwoValuesToCellNodeDialog<StringValue, StringValue>() {

			@Override
			public void addDialogComponents() {
				// TODO Auto-generated method stub
				
			}
		};
	}

}
