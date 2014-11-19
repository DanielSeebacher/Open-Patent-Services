package org.knime.knip.patents.features.binary.ahdh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeModel;

public class AHDHNodeModel extends
		ValueToCellNodeModel<ImgPlusValue<BitType>, ListCell> {

	static SettingsModelIntegerBounded createLevelModel() {
		return new SettingsModelIntegerBounded("m_level", 10, 1,
				Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createLdModel() {
		return new SettingsModelIntegerBounded("m_ld", 4, 1, Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createWhiteAsBackgroundModel() {
		return new SettingsModelBoolean("m_foreground", true);
	}

	private final SettingsModelIntegerBounded m_levels = createLevelModel();
	private final SettingsModelIntegerBounded m_ld = createLdModel();
	private final SettingsModelBoolean m_whiteAsForeground = createWhiteAsBackgroundModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_levels);
		settingsModels.add(m_ld);
		settingsModels.add(m_whiteAsForeground);
	}

	@Override
	protected ListCell compute(ImgPlusValue<BitType> cellValue)
			throws Exception {
		ImgPlus<BitType> img = cellValue.getImgPlus();

		List<IntervalView<BitType>> regions = new ArrayList<>();
		regions.add(Views.interval(img, new long[] { img.min(0), img.min(1) },
				new long[] { img.max(0), img.max(1) }));

		List<Double> fv = new ArrayList<>();
		for (int currentLevel = 0; currentLevel < m_levels.getIntValue(); currentLevel++) {

			List<double[]> centroids = new ArrayList<>();
			for (IntervalView<BitType> region : regions) {
				centroids.add(getCentroid(region));
			}

			List<IntervalView<BitType>> subregions = new ArrayList<>();

			double[] histogram = new double[16];
			for (int l = 0; l < regions.size(); l++) {
				double[] centroid = centroids.get(l);
				List<IntervalView<BitType>> temp = getSubRegions(
						regions.get(l), (long) centroid[0], (long) centroid[1]);

				if (currentLevel < m_ld.getIntValue()) {
					fv.addAll(calculateDensity(temp));
				} else {
					List<Double> calculateRelativeDensity = calculateRelativeDensity(temp);

					int index = 0;
					for (int i = 0; i < calculateRelativeDensity.size(); i++) {
						index += ((calculateRelativeDensity.get(i) >= 1) ? 1d
								: 0d) * Math.pow(2, i);
					}
					histogram[index]++;
				}

				subregions.addAll(temp);
			}

			// normalize
			for (int i = 0; i < histogram.length; i++) {
				fv.add(histogram[i] / subregions.size());
			}

			regions = subregions;

		}

		List<DoubleCell> cells = new ArrayList<>();
		for (Double d : fv) {
			cells.add(new DoubleCell(d));
		}

		return CollectionCellFactory.createListCell(cells);
	}

	private List<Double> calculateRelativeDensity(
			List<IntervalView<BitType>> subregions) {

		double[] pixels = new double[subregions.size()];
		double[] area = new double[subregions.size()];

		double sumPixels = 0;
		double sumArea = 0;

		for (int i = 0; i < subregions.size(); i++) {
			Cursor<BitType> cursor = subregions.get(i).cursor();
			while (cursor.hasNext()) {
				BitType next = cursor.next();
				area[i]++;
				sumArea++;
				if (next.get() ^ m_whiteAsForeground.getBooleanValue()) {
					pixels[i]++;
					sumPixels++;
				}
			}
		}

		Double[] result = new Double[subregions.size()];
		for (int i = 0; i < subregions.size(); i++) {
			result[i] = (sumArea * pixels[i]) / (sumPixels * area[i]);
			if (Double.isNaN(result[i])) {
				result[i] = 0d;
			}
		}

		return Arrays.asList(result);
	}

	private List<Double> calculateDensity(List<IntervalView<BitType>> subregions) {

		double[] pixels = new double[subregions.size()];
		double sumPixels = 0;

		for (int i = 0; i < subregions.size(); i++) {
			Cursor<BitType> cursor = subregions.get(i).cursor();
			while (cursor.hasNext()) {
				BitType next = cursor.next();
				if (next.get() ^ m_whiteAsForeground.getBooleanValue()) {
					pixels[i]++;
					sumPixels++;
				}
			}
		}

		Double[] result = new Double[subregions.size()];
		for (int i = 0; i < subregions.size(); i++) {
			result[i] = pixels[i] / sumPixels;
			if (Double.isNaN(result[i])) {
				result[i] = 0d;
			}
		}

		return Arrays.asList(result);
	}

	private List<IntervalView<BitType>> getSubRegions(
			IntervalView<BitType> region, long cx, long cy) {

		List<IntervalView<BitType>> subregions = new ArrayList<>();

		// top left
		subregions.add(Views.interval(region, new long[] { region.min(0),
				region.min(1) }, new long[] { cx, cy }));

		// top right
		subregions.add(Views.interval(region, new long[] { cx, region.min(1) },
				new long[] { region.max(0), cy }));

		// bottom left
		subregions.add(Views.interval(region, new long[] { region.min(0), cy },
				new long[] { cx, region.max(1) }));

		// bottom right
		subregions.add(Views.interval(region, new long[] { cx, cy },
				new long[] { region.max(0), region.max(1) }));

		return subregions;
	}

	private double[] getCentroid(IntervalView<BitType> region) {
		double cx = 0;
		double cy = 0;
		double sum = 0;

		Cursor<BitType> cursor = region.localizingCursor();
		while (cursor.hasNext()) {
			BitType next = cursor.next();
			if (next.get() ^ m_whiteAsForeground.getBooleanValue()) {
				cx += cursor.getLongPosition(0);
				cy += cursor.getLongPosition(1);
				sum++;
			}
		}

		if (sum == 0) {
			cx = region.min(0) + (region.max(0) - region.min(0));
			cy = region.min(1) + (region.max(1) - region.min(1));
			sum = 1;
		}

		return new double[] { cx / sum, cy / sum };
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}
}
