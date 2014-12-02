package org.knime.knip.patents.features.binary.geometricblur;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;

public class GeometricBlurNodeFactory<T extends RealType<T> & NativeType<T>> extends
		ValueToCellNodeFactory<ImgPlusValue<T>> {

	@Override
	protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
		return new GeometricBlurNodeDialog<T>();
	}

	@Override
	public  ValueToCellNodeModel<ImgPlusValue<T>, ? extends DataCell> createNodeModel() {
		return new GeometricBlurNodeModel<T>();
	}

}
