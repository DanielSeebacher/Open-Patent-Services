package org.knime.knip.base.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.util.Pair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.ValueToCellsNodeModel;

public abstract class BulkValueToCellsNodeModel<VIN extends DataValue> extends
		NodeModel {

	/**
	 * column creation modes
	 */
	public static final String[] COL_CREATION_MODES = new String[] {
			"New Table", "Append" };

	/*
	 * Inport of the table to be processed
	 */
	private static final int IN_TABLE_PORT_INDEX = 0;

	/*
	 * Logging
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(ValueToCellsNodeModel.class);

	/**
	 * @return the settings model for the column creation mode
	 */
	public static SettingsModelString createColCreationModeModel() {
		return new SettingsModelString("column_creation_mode",
				COL_CREATION_MODES[0]);
	}

	/**
	 * @return the settings model for the column name
	 */
	public static SettingsModelString createColSelectionModel() {
		return new SettingsModelString("column_selection", "");
	}

	/**
	 * Class of the first argument type.
	 */
	protected final Class<VIN> m_inValueClass;

	/*
	 * Settings for the column creation mode
	 */
	private final SettingsModelString m_colCreationMode = createColCreationModeModel();

	/*
	 * Settings Model to store the column to be processed
	 */
	private final SettingsModelString m_column = createColSelectionModel();

	private final SettingsModel[] m_additionalModels;

	private final int m_numRowsToProcess;

	public BulkValueToCellsNodeModel(final int numRowsToProcess,
			final Class<VIN> inValueClass,
			final SettingsModel... additionalModels) {
		super(1, 1);

		m_numRowsToProcess = numRowsToProcess;
		m_inValueClass = inValueClass;
		m_additionalModels = additionalModels;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {

		final int colIndex = NodeUtils.getColumnIndex(m_column,
				inData[IN_TABLE_PORT_INDEX].getDataTableSpec(), m_inValueClass,
				this.getClass());

		BufferedDataContainer container = exec
				.createDataContainer(getOutSpec(inData[IN_TABLE_PORT_INDEX]
						.getDataTableSpec()));

		List<DataRow> rowsToProcess = new ArrayList<DataRow>();
		CloseableRowIterator rowIterator = inData[IN_TABLE_PORT_INDEX]
				.iterator();
		double progress = 0;
		while (rowIterator.hasNext()) {

			rowsToProcess.add(rowIterator.next());
			if (m_numRowsToProcess == rowsToProcess.size()
					|| !rowIterator.hasNext()) {

				List<VIN> dataValues = new ArrayList<VIN>();
				for (DataRow row : rowsToProcess) {
					dataValues.add((VIN) row.getCell(colIndex));
				}

				List<DataCell[]> results = compute(dataValues);
				for (int rowNum = 0; rowNum < rowsToProcess.size(); rowNum++) {

					List<DataCell> newRow = new ArrayList<DataCell>();
					if (m_colCreationMode.getStringValue().equalsIgnoreCase(
							COL_CREATION_MODES[1])) {
						for (int inputCellNum = 0; inputCellNum < inData[IN_TABLE_PORT_INDEX]
								.getDataTableSpec().getNumColumns(); inputCellNum++) {
							newRow.add(rowsToProcess.get(rowNum).getCell(
									inputCellNum));
						}
					}

					for (int resultCellNum = 0; resultCellNum < results
							.get(rowNum).length; resultCellNum++) {
						newRow.add(results.get(rowNum)[resultCellNum]);
					}

					container.addRowToTable(new DefaultRow(rowsToProcess.get(
							rowNum).getKey(), newRow
							.toArray(new DataCell[newRow.size()])));
				}

				rowsToProcess.clear();
			}

			++progress;
			exec.setProgress((progress / inData[IN_TABLE_PORT_INDEX]
					.getRowCount()));
			exec.checkCanceled();
		}

		rowIterator.close();
		container.close();

		return new BufferedDataTable[] { container.getTable() };
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		final int colIndex = NodeUtils.getColumnIndex(m_column,
				inSpecs[IN_TABLE_PORT_INDEX], m_inValueClass, this.getClass());

		if (-1 == colIndex) {
			LOGGER.warn("At least one String Column must be selected");
			throw new InvalidSettingsException(
					"At least one String Column must be selected");
		}

		return new DataTableSpec[] { getOutSpec(inSpecs[IN_TABLE_PORT_INDEX]) };
	}

	private DataTableSpec getOutSpec(DataTableSpec inSpecs) {
		List<DataColumnSpec> outSpec = new ArrayList<DataColumnSpec>();
		List<Pair<String, DataType>> additionalOutSpec = getAdditionalOutSpec();

		if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
			for (int i = 0; i < inSpecs.getNumColumns(); i++) {
				outSpec.add(inSpecs.getColumnSpec(i));
			}
		}

		for (Pair<String, DataType> entry : additionalOutSpec) {
			outSpec.add(new DataColumnSpecCreator(entry.getA(), entry.getB())
					.createSpec());
		}

		return new DataTableSpec(outSpec.toArray(new DataColumnSpec[outSpec
				.size()]));
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// do nothing
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// do nothing
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_colCreationMode.saveSettingsTo(settings);
		m_column.saveSettingsTo(settings);

		for (SettingsModel sm : m_additionalModels) {
			sm.saveSettingsTo(settings);
		}
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_colCreationMode.validateSettings(settings);
		m_column.validateSettings(settings);

		for (SettingsModel sm : m_additionalModels) {
			sm.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_colCreationMode.loadSettingsFrom(settings);
		m_column.loadSettingsFrom(settings);

		for (SettingsModel sm : m_additionalModels) {
			sm.loadSettingsFrom(settings);
		}
	}

	@Override
	protected void reset() {
		// do nothing
	}

	/*
	 * abstract methods
	 */
	public abstract List<Pair<String, DataType>> getAdditionalOutSpec();

	protected abstract List<DataCell[]> compute(List<VIN> values)
			throws Exception;
}
