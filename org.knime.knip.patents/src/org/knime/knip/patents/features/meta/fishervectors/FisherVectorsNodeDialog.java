package org.knime.knip.patents.features.meta.fishervectors;

import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;

public class FisherVectorsNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public FisherVectorsNodeDialog() {

		createNewGroup("Column Selection");

		addDialogComponent(new DialogComponentColumnFilter(
				FisherVectorsNodeModel.createColumnFilterModel(), 0, true,
				DoubleCell.getPreferredValueClass()));
	}

}
