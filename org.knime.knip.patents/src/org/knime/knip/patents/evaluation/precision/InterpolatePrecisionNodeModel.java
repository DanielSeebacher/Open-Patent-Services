package org.knime.knip.patents.evaluation.precision;

import java.util.ArrayList;
import java.util.Collections;
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

		List<RecPrec> recPrecs = new ArrayList<>();

		Iterator<DataCell> it1 = cellValue1.iterator();
		Iterator<DataCell> it2 = cellValue2.iterator();

		recPrecs.add(new RecPrec(0d, 1d));
		while (it1.hasNext()) {
			double currPrecision = ((DoubleCell) it1.next()).getDoubleValue();
			double currRecall = ((DoubleCell) it2.next()).getDoubleValue();

			recPrecs.add(new RecPrec(currRecall, currPrecision));
		}

		Collections.sort(recPrecs);

		Collections.reverse(recPrecs);
		
		double maxPrecision = -1d;
		Iterator<RecPrec> iterator = recPrecs.iterator();
		while(iterator.hasNext()){
			RecPrec next = iterator.next();
			double currentPrecision = next.precision;
			if(maxPrecision > currentPrecision){
				next.precision = maxPrecision;
			} else if(maxPrecision < currentPrecision){
				maxPrecision = currentPrecision;
			}
		}
		
		Collections.reverse(recPrecs);
		iterator = recPrecs.iterator();
		Double currentRecall = -1d;
		while (iterator.hasNext()) {
			RecPrec next = iterator.next();
			if (next.recall.equals(currentRecall)) {
				iterator.remove();
			} else {
				currentRecall = next.recall;
			}
		}

		double[] precisionArr = new double[recPrecs.size()];
		double[] recallArr = new double[recPrecs.size()];

		for (int i = 0; i < recPrecs.size(); i++) {
			precisionArr[i] = recPrecs.get(i).precision;
			recallArr[i] = recPrecs.get(i).recall;
		}

		LinearInterpolator li = new LinearInterpolator();
		PolynomialSplineFunction interpolate = li.interpolate(recallArr,
				precisionArr);

		List<DoubleCell> cells = new ArrayList<>();
		
		for(int i = 0; i < 41; i++){
			double d = i * 0.025d;
			cells.add(new DoubleCell(interpolate.value(d)));	

		}

		return CollectionCellFactory.createListCell(cells);
	}

	@Override
	protected DataType getOutDataCellListCellType() {
		return DoubleCell.TYPE;
	}

	private final class RecPrec implements Comparable<RecPrec> {

		private Double recall;
		private Double precision;

		public RecPrec(Double recall, Double precision) {
			this.recall = recall;
			this.precision = precision;
		}

		@Override
		public int compareTo(RecPrec o) {

			int i = this.recall.compareTo(o.recall);
			if (i == 0) {
				return (int) -Math
						.signum(this.precision.compareTo(o.precision));
			}

			return i;
		}

		@Override
		public String toString() {
			return "[" + recall + "," + precision + "]";
		}
	}
}