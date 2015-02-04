package org.knime.knip.patents.features.binary.ahdh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
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

/**
 * Adaptive Hierarchical Density Histogram.
 *
 * @author Daniel Seebacher, University of Konstanz
 *
 * @see Sidiropoulos, P.; Vrochidis, S.; Kompatsiaris, I.,
 *      "Adaptive hierarchical density histogram for complex binary image retrieval,"
 *      Content-Based Multimedia Indexing (CBMI), 2010 International Workshop on
 *      , vol., no., pp.1,6, 23-25 June 2010
 */
public class AHDHNodeModel extends
		ValueToCellNodeModel<ImgPlusValue<BitType>, ListCell> {

	/**
	 *
	 * @return The SettingsModel for the Levels
	 */
	static SettingsModelIntegerBounded createLevelModel() {
		return new SettingsModelIntegerBounded("m_level", 10, 1,
				Integer.MAX_VALUE);
	}

	/**
	 *
	 * @return The SettingsModel for the starting level for the relative
	 *         density.
	 */
	static SettingsModelIntegerBounded createLdModel() {
		return new SettingsModelIntegerBounded("m_ld", 4, 1, Integer.MAX_VALUE);
	}

	/**
	 *
	 * @return The SettingsModel to use white as the background color.
	 */
	static SettingsModelBoolean createWhiteAsBackgroundModel() {
		return new SettingsModelBoolean("m_foreground", true);
	}

	private final SettingsModelIntegerBounded m_levels = createLevelModel();
	private final SettingsModelIntegerBounded m_ld = createLdModel();
	private final SettingsModelBoolean m_whiteAsForeground = createWhiteAsBackgroundModel();

	@Override
	protected void addSettingsModels(final List<SettingsModel> settingsModels) {
		settingsModels.add(this.m_levels);
		settingsModels.add(this.m_ld);
		settingsModels.add(this.m_whiteAsForeground);
	}

	@Override
	protected ListCell compute(final ImgPlusValue<BitType> cellValue)
			throws Exception {

		final ImgPlus<BitType> img = cellValue.getImgPlus();

		// the first region is the whole image
		List<IntervalView<BitType>> regions = new ArrayList<>();
		regions.add(Views.interval(img, new long[] { img.min(0), img.min(1) },
				new long[] { img.max(0), img.max(1) }));

		// for each level
		final List<Double> fv = new ArrayList<>();
		for (int currentLevel = 1; currentLevel <= this.m_levels.getIntValue(); currentLevel++) {

			// calculate the centroid for each region
			final List<double[]> centroids = new ArrayList<>();
			for (final IntervalView<BitType> region : regions) {
				centroids.add(getCentroid(region));
			}

			final List<IntervalView<BitType>> subregions = new ArrayList<>();

			// for each region
			final double[] histogram = new double[16];
			for (int l = 0; l < regions.size(); l++) {

				// divide the region into four subregions using centroid
				final double[] centroid = centroids.get(l);
				final List<IntervalView<BitType>> temp = getSubRegions(
						regions.get(l), (long) centroid[0], (long) centroid[1]);

				// use density or relative density
				if (currentLevel < this.m_ld.getIntValue()) {

					final List<Double> calculateDensity = calculateDensity(temp);
					for (final Double double1 : calculateDensity) {
						if (Double.isNaN(double1)) {
							fv.add(0d);
						} else {
							fv.add(double1);
						}
					}

				} else {
					final List<Double> calculateRelativeDensity = calculateRelativeDensity(temp);

					// simple binary to get index (0110 -> 6)
					int index = 0;
					for (int i = 0; i < calculateRelativeDensity.size(); i++) {
						index += ((calculateRelativeDensity.get(i) >= 1) ? 1d
								: 0d) * Math.pow(2, i);
					}
					histogram[index]++;
				}

				subregions.addAll(temp);
			}

			// normalize histogram
			if (currentLevel >= this.m_ld.getIntValue()) {
				for (final double element : histogram) {

					final double normalized = element / subregions.size();
					if (Double.isNaN(normalized)) {
						fv.add(0d);
					} else {
						fv.add(normalized);
					}

				}
			}

			// subregions are new regions
			regions = subregions;
		}

		final List<DoubleCell> cells = new ArrayList<>();
		for (final Double d : fv) {
			cells.add(new DoubleCell(d));
		}

		return CollectionCellFactory.createListCell(cells);
	}

	/**
	 * Calculates the relative density for four subregions as described in the
	 * paper.
	 *
	 * @param subregions
	 *            the four subregions
	 * @return the relative density
	 */
	private List<Double> calculateRelativeDensity(
			final List<IntervalView<BitType>> subregions) {

		final double[] pixels = new double[subregions.size()];
		final double[] area = new double[subregions.size()];

		double sumPixels = 0;
		double sumArea = 0;

		for (int i = 0; i < subregions.size(); i++) {
			final Cursor<BitType> cursor = subregions.get(i).cursor();
			while (cursor.hasNext()) {
				final BitType next = cursor.next();
				area[i]++;
				sumArea++;
				if (next.get() ^ this.m_whiteAsForeground.getBooleanValue()) {
					pixels[i]++;
					sumPixels++;
				}
			}
		}

		final Double[] result = new Double[subregions.size()];
		for (int i = 0; i < subregions.size(); i++) {
			result[i] = (sumArea * pixels[i]) / (sumPixels * area[i]);
			if (Double.isNaN(result[i])) {
				result[i] = Double.POSITIVE_INFINITY;
			}
		}

		return Arrays.asList(result);
	}

	/**
	 * Calculates the density for four subregions as described in the paper.
	 *
	 * @param subregions
	 *            the four subregions
	 * @return the relative density
	 */
	private List<Double> calculateDensity(
			final List<IntervalView<BitType>> subregions) {
		final double[] pixels = new double[subregions.size()];
		double sumPixels = 0;

		for (int i = 0; i < subregions.size(); i++) {
			final Cursor<BitType> cursor = subregions.get(i).cursor();
			while (cursor.hasNext()) {
				final BitType next = cursor.next();
				if (next.get() ^ this.m_whiteAsForeground.getBooleanValue()) {
					pixels[i]++;
					sumPixels++;
				}
			}
		}

		final Double[] result = new Double[subregions.size()];
		for (int i = 0; i < subregions.size(); i++) {
			result[i] = pixels[i] / sumPixels;
			if (Double.isNaN(result[i])) {
				result[i] = 0d;
			}
		}

		return Arrays.asList(result);
	}

	/**
	 * Divides one region into four subregions using the centroid.
	 *
	 * @param region
	 *            a region
	 * @param cx
	 *            the x value of the centroid
	 * @param cy
	 *            the y value of the centroid
	 * @return the four subregions
	 */
	private List<IntervalView<BitType>> getSubRegions(
			final IntervalView<BitType> region, final long cx, final long cy) {

		final List<IntervalView<BitType>> subregions = new ArrayList<>();

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

	/**
	 * Calculates the centroid for a region. If the region is empty use center
	 * of the region.
	 *
	 * @param region
	 *            a region
	 * @return the centroid of the region
	 */
	private double[] getCentroid(final IntervalView<BitType> region) {
		double cx = 0;
		double cy = 0;
		double sum = 0;

		final Cursor<BitType> cursor = region.localizingCursor();
		while (cursor.hasNext()) {
			final BitType next = cursor.next();
			if (next.get() ^ this.m_whiteAsForeground.getBooleanValue()) {
				cx += cursor.getLongPosition(0);
				cy += cursor.getLongPosition(1);
				sum++;
			}
		}

		// if a region is empty use the default centroid
		if (sum == 0) {
			return new double[] {
					region.min(0) + ((region.max(0) - region.min(0)) / 2),
					region.min(1) + ((region.max(1) - region.min(1)) / 2) };
		}

		return new double[] { cx / sum, cy / sum };
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}
}
