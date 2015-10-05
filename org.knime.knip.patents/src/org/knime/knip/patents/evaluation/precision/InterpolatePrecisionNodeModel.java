package org.knime.knip.patents.evaluation.precision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;

/**
 * Takes a list of already sorted recall and precision values and returns the
 * interpolated values at 0, 0.1, ..., 1.0.
 * 
 * @author Daniel Seebacher, University of Konstanz
 */
public class InterpolatePrecisionNodeModel
		extends TwoValuesToCellNodeModel<CollectionDataValue, CollectionDataValue, ListCell> {

	static SettingsModelIntegerBounded createNumStepsModel() {
		return new SettingsModelIntegerBounded("m_steps", 20, 1, Integer.MAX_VALUE);
	}

	private final SettingsModelIntegerBounded m_steps = createNumStepsModel();

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {
		settingsModels.add(m_steps);
	}

	@Override
	protected ListCell compute(CollectionDataValue cellValue1, CollectionDataValue cellValue2) throws Exception {
		if (cellValue1.size() != cellValue2.size()) {
			throw new IllegalArgumentException("Cell Value Sizes must be equal");
		}

		// get input
		Iterator<DataCell> it1 = cellValue1.iterator();
		Iterator<DataCell> it2 = cellValue2.iterator();

		List<RecallPrecisionPairs> recall_precision_pairs = new ArrayList<>();
		recall_precision_pairs.add(new RecallPrecisionPairs(0d, 1d));
		while (it1.hasNext()) {
			double currPrecision = ((DoubleCell) it1.next()).getDoubleValue();
			double currRecall = ((DoubleCell) it2.next()).getDoubleValue();
			recall_precision_pairs.add(new RecallPrecisionPairs(currRecall, currPrecision));
		}

		// sort (start by recall asc and precision desc)
		Collections.sort(recall_precision_pairs, new PrecisionRecallPairComparator());

		// interpolate precision -> highest precision for all recall values
		// bigger than the current one
		for (int i = 0; i < recall_precision_pairs.size() - 1; i++) {
			RecallPrecisionPairs currentPair = recall_precision_pairs.get(i);

			for (int j = i + 1; j < recall_precision_pairs.size(); j++) {
				RecallPrecisionPairs otherPair = recall_precision_pairs.get(j);

				if (otherPair.getRecall() >= currentPair.getRecall()
						&& otherPair.getPrecision() > currentPair.getPrecision()) {
					currentPair.setPrecision(otherPair.getPrecision());
				}
			}
		}

		// only keep the precision recall pair with the highest precision for a
		// given recall
		Collections.sort(recall_precision_pairs, new PrecisionRecallPairComparator());
		Set<RecallPrecisionPairs> remove = new HashSet<>();
		for (int i = 0; i < recall_precision_pairs.size() - 1; i++) {
			RecallPrecisionPairs currentPair = recall_precision_pairs.get(i);

			for (int j = i + 1; j < recall_precision_pairs.size(); j++) {
				RecallPrecisionPairs otherPair = recall_precision_pairs.get(j);

				if (currentPair.getRecall().equals(otherPair.getRecall())
						&& currentPair.getPrecision() >= otherPair.getPrecision()) {
					remove.add(otherPair);
				}
			}
		}

		
		remove.forEach(pair -> recall_precision_pairs.remove(pair));

		double[] precisionArr = new double[recall_precision_pairs.size()];
		double[] recallArr = new double[recall_precision_pairs.size()];

		for (int i = 0; i < recall_precision_pairs.size(); i++) {
			precisionArr[i] = recall_precision_pairs.get(i).getPrecision();
			recallArr[i] = recall_precision_pairs.get(i).getRecall();
		}

		LinearInterpolator li = new LinearInterpolator();
		PolynomialSplineFunction interpolate = li.interpolate(recallArr, precisionArr);

		List<DoubleCell> cells = new ArrayList<>();
		double increment = 1d / m_steps.getIntValue();

		for (int i = 0; i <= m_steps.getIntValue(); i++) {
			double d = i * increment;
			cells.add(new DoubleCell(interpolate.value(d)));
		}

		return CollectionCellFactory.createListCell(cells);
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}

	private final class RecallPrecisionPairs {

		private Double precision;
		private Double recall;

		public RecallPrecisionPairs(Double recall, Double precision) {
			this.recall = recall;
			this.precision = precision;
		}

		public Double getPrecision() {
			return precision;
		}

		public void setPrecision(Double precision) {
			this.precision = precision;
		}

		public Double getRecall() {
			return recall;
		}

		@Override
		public String toString() {
			return "RecallPrecisionPairs [precision=" + precision + ", recall=" + recall + "]";
		}
	}

	private class PrecisionRecallPairComparator implements Comparator<RecallPrecisionPairs> {

		@Override
		public int compare(RecallPrecisionPairs o1, RecallPrecisionPairs o2) {

			double comp = o1.getRecall().compareTo(o2.getRecall());
			if (comp == 0) {
				comp = o2.getPrecision().compareTo(o1.getPrecision());
			}

			return (int) comp;
		}

	}
}