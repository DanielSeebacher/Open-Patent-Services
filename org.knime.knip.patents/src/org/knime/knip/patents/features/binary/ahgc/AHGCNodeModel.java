package org.knime.knip.patents.features.binary.ahgc;

import java.util.ArrayList;
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

/**
 * Adaptive Hierarchical Geometric Centroids
 * 
 * @author Daniel Seebacher, University of Konstanz
 * 
 * @see Mai Yang, Guoping Qiu, Jiwu Huang, Dave Elliman,
 *      "Near-Duplicate Image Recognition and Content-based Image Retrieval using Adaptive Hierarchical Geometric Centroids,"
 *      Pattern Recognition, International Conference on, pp. 958-961, 18th
 *      International Conference on Pattern Recognition (ICPR'06) Volume 2, 2006
 */
public class AHGCNodeModel extends
		ValueToCellNodeModel<ImgPlusValue<BitType>, ListCell> {

	/**
	 * 
	 * @return The SettingsModel for the Levels
	 */
	static SettingsModelIntegerBounded createLevelModel() {
		return new SettingsModelIntegerBounded("m_level", 3, 1,
				Integer.MAX_VALUE);
	}

	/**
	 * 
	 * @return The SettingsModel to use white as the background color.
	 */
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

		// the first region is the whole image
		List<IntervalView<BitType>> regions = new ArrayList<>();
		regions.add(Views.interval(img, new long[] { img.min(0), img.min(1) },
				new long[] { img.max(0), img.max(1) }));

		// for each level
		List<double[]> fv = new ArrayList<>();
		for (int i = 0; i < m_levels.getIntValue(); i++) {

			// calculate the centroid for each region
			List<double[]> centroids = new ArrayList<>();
			for (IntervalView<BitType> region : regions) {
				centroids.add(getCentroid(region));
			}

			// centroid x,y are feature values. normalize.
			for (double[] ds : centroids) {
				double relative_x = ds[0] / img.dimension(0) * 100d;
				double relative_y = ds[1] / img.dimension(1) * 100d;
				fv.add(new double[] { relative_x, relative_y });
			}

			// divide each region into four subregions
			List<IntervalView<BitType>> subregions = new ArrayList<>();
			for (int l = 0; l < regions.size(); l++) {
				double[] centroid = centroids.get(l);
				subregions.addAll(getSubRegions(regions.get(l),
						(long) centroid[0], (long) centroid[1]));
			}

			// subregions are new regions.
			regions = subregions;
		}

		List<DoubleCell> cells = new ArrayList<>();
		for (int i = 0; i < fv.size(); i++) {
			double[] centroid = fv.get(i);
			cells.add(new DoubleCell(centroid[0]));
			cells.add(new DoubleCell(centroid[1]));
		}

		return CollectionCellFactory.createListCell(cells);
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

	/**
	 * Calculates the centroid for a region. If the region is empty use center
	 * of the region.
	 * 
	 * @param region
	 *            a region
	 * @return the centroid of the region
	 */
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

		// if a region is empty use the default centroid
		if (sum == 0) {
			return new double[] {
					region.min(0) + (region.max(0) - region.min(0)) / 2,
					region.min(1) + (region.max(1) - region.min(1)) / 2 };
		}

		return new double[] { cx / sum, cy / sum };
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}
}
