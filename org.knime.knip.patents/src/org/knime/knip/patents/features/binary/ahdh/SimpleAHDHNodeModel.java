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

public class SimpleAHDHNodeModel extends
		ValueToCellNodeModel<ImgPlusValue<BitType>, ListCell> {

	static SettingsModelIntegerBounded createLevelModel() {
		return new SettingsModelIntegerBounded("m_level", 5, 1,
				Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createWhiteAsBackgroundModel() {
		return new SettingsModelBoolean("m_foreground", true);
	}

	private final SettingsModelIntegerBounded m_levels = createLevelModel();
	private final SettingsModelBoolean m_whiteAsForeground = createWhiteAsBackgroundModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_levels);
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
		for (int i = 0; i < m_levels.getIntValue(); i++) {

			List<double[]> centroids = new ArrayList<>();
			for (IntervalView<BitType> region : regions) {
				centroids.add(getCentroid(region));
			}

			List<IntervalView<BitType>> subregions = new ArrayList<>();
			for (int l = 0; l < regions.size(); l++) {
				double[] centroid = centroids.get(l);
				List<IntervalView<BitType>> temp = getSubRegions(
						regions.get(l), (long) centroid[0], (long) centroid[1]);
				fv.addAll(calculateDensity(temp));
				subregions.addAll(temp);
			}

			regions = subregions;

		}

		List<DoubleCell> cells = new ArrayList<>();
		for (Double d : fv) {
			cells.add(new DoubleCell(d));
		}

		return CollectionCellFactory.createListCell(cells);
	}

	private List<Double> calculateDensity(List<IntervalView<BitType>> subregions) {

		Double[] densities = new Double[subregions.size()];
		Arrays.fill(densities, 0d);
		double sum = 0;

		for (int i = 0; i < subregions.size(); i++) {
			Cursor<BitType> cursor = subregions.get(i).cursor();
			while (cursor.hasNext()) {
				BitType next = cursor.next();
				if (next.get() ^ m_whiteAsForeground.getBooleanValue()) {
					densities[i]++;
					sum++;
				}
			}
		}

		for (int i = 0; i < subregions.size(); i++) {
			densities[i] /= sum;
			if (Double.isNaN(densities[i])) {
				densities[i] = 0d;
			}
		}

		return Arrays.asList(densities);
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

		return new double[] { cx / sum, cy / sum };
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}
}
