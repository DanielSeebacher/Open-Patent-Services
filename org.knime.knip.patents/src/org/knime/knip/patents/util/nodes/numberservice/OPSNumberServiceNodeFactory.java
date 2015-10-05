package org.knime.knip.patents.util.nodes.numberservice;

import java.util.Arrays;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.node.ValueToCellsNodeDialog;
import org.knime.knip.base.node.ValueToCellsNodeFactory;
import org.knime.knip.base.node.ValueToCellsNodeModel;

public class OPSNumberServiceNodeFactory extends ValueToCellsNodeFactory<StringValue> {

	@Override
	protected ValueToCellsNodeDialog<StringValue> createNodeDialog() {
		return new ValueToCellsNodeDialog<StringValue>() {
			@Override
			public void addDialogComponents() {
				addDialogComponent("Options", "Input Format",
						new DialogComponentStringSelection(OPSNumberServiceNodeModel.createInputFormatModel(),
								"Input Format", Arrays.asList("docdb", "epodoc", "original")));

				addDialogComponent("Options", "Output Format",
						new DialogComponentStringSelection(OPSNumberServiceNodeModel.createOutputFormatModel(),
								"Output Format", Arrays.asList("docdb", "epodoc", "original")));
			}
		};
	}

	@Override
	public ValueToCellsNodeModel<StringValue> createNodeModel() {
		return new OPSNumberServiceNodeModel();
	}

}
