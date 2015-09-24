package org.knime.knip.patents.evaluation.precision;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.knip.base.node.TwoValuesToCellNodeDialog;
import org.knime.knip.base.node.TwoValuesToCellNodeFactory;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

/**
 * Simple {@link TwoValuesToCellNodeFactory} which returns an empty
 * {@link TwoValuesToCellNodeDialog}
 * 
 * @author Daniel Seebacher, University of Konstanz
 */
public class InterpolatePrecisionNodeFactory
		extends TwoValuesToCellNodeFactory<CollectionDataValue, CollectionDataValue> {

	@Override
	protected TwoValuesToCellNodeDialog<CollectionDataValue, CollectionDataValue> createNodeDialog() {
		return new TwoValuesToCellNodeDialog<CollectionDataValue, CollectionDataValue>() {

			@Override
			public void addDialogComponents() {
				addDialogComponent("Options", "Settings", new DialogComponentNumber(
						InterpolatePrecisionNodeModel.createNumStepsModel(), "Num. Steps", 1));
			}
		};
	}

	@Override
	public TwoValuesToCellNodeModel<CollectionDataValue, CollectionDataValue, ? extends DataCell> createNodeModel() {
		return new InterpolatePrecisionNodeModel();
	}

}