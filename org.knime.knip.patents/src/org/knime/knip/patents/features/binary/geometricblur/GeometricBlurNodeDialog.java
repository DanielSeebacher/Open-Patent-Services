package org.knime.knip.patents.features.binary.geometricblur;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;

public class GeometricBlurNodeDialog<T extends RealType<T> & NativeType<T>> extends
		ValueToCellNodeDialog<ImgPlusValue<T>> {

	@Override
	public void addDialogComponents() {
		addDialogComponent(new DialogComponentNumber(
				GeometricBlurNodeModel.createSigmaModel(), "Sigma", 0.01));
		addDialogComponent(new DialogComponentNumber(
				GeometricBlurNodeModel.createBlurStepsModel(), "Blursteps", 1));
	}

}
