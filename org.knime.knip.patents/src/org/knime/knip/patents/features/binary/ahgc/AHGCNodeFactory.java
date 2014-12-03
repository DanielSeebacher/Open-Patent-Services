package org.knime.knip.patents.features.binary.ahgc;

import net.imglib2.type.logic.BitType;

import org.knime.core.data.collection.ListCell;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;

/**
 * Simple {@link ValueToCellNodeFactory}.
 * 
 * @author Daniel Seebacher, University of Konstanz
 */
public class AHGCNodeFactory extends
		ValueToCellNodeFactory<ImgPlusValue<BitType>> {

	@Override
	protected ValueToCellNodeDialog<ImgPlusValue<BitType>> createNodeDialog() {
		return new ValueToCellNodeDialog<ImgPlusValue<BitType>>() {

			@Override
			public void addDialogComponents() {
				addDialogComponent(new DialogComponentNumber(
						AHGCNodeModel.createLevelModel(), "Num. Levels", 1));

				addDialogComponent(new DialogComponentBoolean(
						AHGCNodeModel.createWhiteAsBackgroundModel(),
						"White as Background?"));
			}
		};
	}

	@Override
	public ValueToCellNodeModel<ImgPlusValue<BitType>, ListCell> createNodeModel() {
		return new AHGCNodeModel();
	}

}
