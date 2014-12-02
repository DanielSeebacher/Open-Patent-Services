package org.knime.knip.patents.evaluation.precision;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

public class InterpolatePrecisionNodeFactory extends TwoValuesToCellNodeFactory<CollectionDataValue, CollectionDataValue> {

	@Override
	protected TwoValuesToCellNodeDialog<CollectionDataValue, CollectionDataValue> createNodeDialog() {
		return new TwoValuesToCellNodeDialog<CollectionDataValue, CollectionDataValue>() {

			@Override
			public void addDialogComponents() {
				// TODO Auto-generated method stub
				
			}
		};
	}

	@Override
	public TwoValuesToCellNodeModel<CollectionDataValue, CollectionDataValue, ? extends DataCell> createNodeModel() {
		return new InterpolatePrecisionNodeModel();
	}

}
