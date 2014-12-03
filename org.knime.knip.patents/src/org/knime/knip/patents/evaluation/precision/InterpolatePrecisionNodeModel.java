package org.knime.knip.patents.evaluation.precision;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

/**
 * Takes a list of already sorted recall and precision values and returns the
 * interpolated values at 0, 0.1, ..., 1.0.
 * 
 * @author Daniel Seebacher, University of Konstanz
 */
public class InterpolatePrecisionNodeModel
		extends
		TwoValuesToCellNodeModel<CollectionDataValue, CollectionDataValue, ListCell> {

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		// no settings needed
	}

	@Override
	protected ListCell compute(CollectionDataValue cellValue1,
			CollectionDataValue cellValue2) throws Exception {

		Iterator<DataCell> it1 = cellValue1.iterator();
		Iterator<DataCell> it2 = cellValue2.iterator();

		List<Double> precision = new ArrayList<>();
		List<Double> recall = new ArrayList<>();

		precision.add(1d);
		recall.add(0d);

		while (it1.hasNext()) {

			double currPrecision = ((DoubleCell) it1.next()).getDoubleValue();
			double currRecall = ((DoubleCell) it2.next()).getDoubleValue();

			int index = recall.indexOf(currRecall);

			if (index != -1) {

				if (precision.get(index) < currPrecision) {
					precision.set(index, currPrecision);
				}

			} else {
				recall.add(currRecall);
				precision.add(currPrecision);
			}
		}

		for (int i = 0; i < precision.size(); i++) {
			for (int j = i; j < precision.size(); j++) {
				if (precision.get(i) < precision.get(j)) {
					precision.set(i, precision.get(j));
				}
			}
		}

		double[] precisionArr = new double[precision.size()];
		double[] recallArr = new double[recall.size()];

		for (int i = 0; i < precision.size(); i++) {
			precisionArr[i] = precision.get(i);
			recallArr[i] = recall.get(i);

		}

		LinearInterpolator li = new LinearInterpolator();
		PolynomialSplineFunction interpolate = li.interpolate(recallArr,
				precisionArr);

		List<DoubleCell> cells = new ArrayList<>();
		for (double d = 0; d <= 1; d += 0.1) {
			cells.add(new DoubleCell(interpolate.value(d)));
		}

		return CollectionCellFactory.createListCell(cells);
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}
}
