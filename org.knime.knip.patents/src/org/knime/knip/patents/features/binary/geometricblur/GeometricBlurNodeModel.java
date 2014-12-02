package org.knime.knip.patents.features.binary.geometricblur;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeModel;

@SuppressWarnings("deprecation")
public class GeometricBlurNodeModel<T extends RealType<T> & NativeType<T>>
		extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<DoubleType>> {

	static SettingsModelDoubleBounded createSigmaModel() {
		return new SettingsModelDoubleBounded("sigma", 5, 0.0001,
				Double.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createBlurStepsModel() {
		return new SettingsModelIntegerBounded("blursteps", 20, 1,
				Integer.MAX_VALUE);
	}

	private final SettingsModelDoubleBounded m_sigma = createSigmaModel();
	private final SettingsModelIntegerBounded m_blurSteps = createBlurStepsModel();
	private ImgPlusCellFactory m_imgCellFactory;

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_sigma);
		settingsModels.add(m_blurSteps);
	}

	@Override
	protected void prepareExecute(ExecutionContext exec) {
		m_imgCellFactory = new ImgPlusCellFactory(exec);
	}

	@Override
	protected ImgPlusCell<DoubleType> compute(ImgPlusValue<T> cellValue)
			throws Exception {

		Img<T> input = cellValue.getImgPlus().getImg();

		if (input.numDimensions() != 2) {
			throw new IllegalArgumentException(
					"Only 2d binary images are supported!");
		}

		double cx = input.dimension(0) / 2;
		double cy = input.dimension(1) / 2;

		List<Img<DoubleType>> blurs = new ArrayList<>();

		for (int i = 1; i < m_blurSteps.getIntValue(); i++) {
			double tmpSigma = i * m_sigma.getDoubleValue()
					/ (1d * m_blurSteps.getIntValue());
			blurs.add(Gauss.toDouble(tmpSigma, input));
		}

		Img<DoubleType> output = ArrayImgs.doubles(input.dimension(0),
				input.dimension(1));
		Cursor<DoubleType> localizingCursor = output.localizingCursor();

		Set<Integer> slices = new HashSet<>();

		double maxdistance = Math.hypot(cx, cy);

		while (localizingCursor.hasNext()) {
			localizingCursor.fwd();

			long x = localizingCursor.getLongPosition(0);
			long y = localizingCursor.getLongPosition(1);

			double distance = Math.hypot(cx - x, cy - y);

			int slice = (int) (distance / maxdistance * m_blurSteps.getIntValue());
			if (slice < 0) {
				slice = 0;
			} else if (slice > blurs.size() - 1) {
				slice = blurs.size() - 1;
			}

			slices.add(slice);

			Img<DoubleType> blurred = blurs.get(slice);

			RandomAccess<DoubleType> randomAccess = blurred.randomAccess();
			randomAccess.setPosition(localizingCursor);
			localizingCursor.get().set(randomAccess.get().get());
		}

		return m_imgCellFactory.createCell(new ImgPlus<>(output));
	}

}
