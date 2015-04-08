package org.knime.knip.patents.features.meta.fishervectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Node Model to compute normalized Fisher Vectors following the instructions
 * from the paper from Sánchez, Perronnin, Mensink et al.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 * 
 * @see Sánchez, J., Perronnin, F., Mensink, T., & Verbeek, J. (2013). Image
 *      classification with the fisher vector: Theory and practice.
 *      International Journal of Computer Vision, 105(3), 222–245.
 *      http://doi.org/10.1007/s11263-013-0636-x
 */
public class FisherVectorsNodeModel extends NodeModel {

	/* ****************************************************
	 * * GAUSSIAN MIXTURE MODEL PARAMETER SETTING MODELS **
	 * ****************************************************
	 */

	static SettingsModelIntegerBounded createNumMixtureComponentsModel() {
		return new SettingsModelIntegerBounded("m_numberMixtureComponents", 1,
				1, Integer.MAX_VALUE);
	}

	private static SettingsModelIntegerBounded m_numMixtureComponents = createNumMixtureComponentsModel();

	/* ****************************************************
	 * *********** COLUMN FILTER SETTING MODELS ***********
	 * ****************************************************
	 */

	static SettingsModelFilterString createColumnFilterModel() {
		return new SettingsModelFilterString("m_columnFilter");
	}

	private static SettingsModelFilterString m_selectedColumns = createColumnFilterModel();

	protected FisherVectorsNodeModel() {
		super(1, 1);
	}

	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		// get column indices of the descriptor columns
		List<Integer> descriptorColumnIndices = new ArrayList<>();
		for (String columnName : m_selectedColumns.getIncludeList()) {
			descriptorColumnIndices.add(inData[0].getDataTableSpec()
					.findColumnIndex(columnName));
		}

		// For k = 1, ..., K initialize accumulators
		double[][] accumulators = new double[m_numMixtureComponents
				.getIntValue()][3];

		// For t = 1, ..., T -> for each input
		double t = 1;
		double T = inData[0].getRowCount();
		CloseableRowIterator rowIterator = inData[0].iterator();
		while (rowIterator.hasNext()) {
			DataRow currentRow = rowIterator.next();

			double[] hist = new double[descriptorColumnIndices.size()];
			for (int i = 0; i < descriptorColumnIndices.size(); i++) {
				hist[i] = ((DoubleValue) currentRow
						.getCell(descriptorColumnIndices.get(i)))
						.getDoubleValue();
			}

			// Compute \gamma_t(k) using equation 15
			for (int k = 0; k < m_numMixtureComponents.getIntValue(); k++) {
				accumulators[k][0] = accumulators[k][0] + getGammaValue(k);
				accumulators[k][1] = accumulators[k][1]
						+ multiply(getGammaValue(k), hist);
				accumulators[k][2] = accumulators[k][2]
						+ multiply(getGammaValue(k), square(hist));

			}

			exec.checkCanceled();
			exec.setProgress((t++) / T, "Computed statistic for row "
					+ currentRow.getKey());
		}
		
		for(int k = 0; k < m_numMixtureComponents.getIntValue(); k++){
			
		}

		return inData;
	}

	private double[] square(double[] hist) {
		for (int i = 0; i < hist.length; i++) {
			hist[i] *= hist[i];
		}
		return hist;
	}

	private double multiply(double gammaValue, double[] hist) {
		// TODO Auto-generated method stub
		return 0;
	}

	private double getGammaValue(int k) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		List<String> includeList = m_selectedColumns.getIncludeList();
		if (includeList == null || includeList.isEmpty()) {
			throw new IllegalArgumentException(
					"Can't compute fisher vectors for empty input");
		}

		return null;
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_selectedColumns.saveSettingsTo(settings);
		m_numMixtureComponents.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_selectedColumns.validateSettings(settings);
		m_numMixtureComponents.validateSettings(settings);

	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_selectedColumns.loadSettingsFrom(settings);
		m_numMixtureComponents.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// do nothing
	}

}
