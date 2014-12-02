package org.knime.knip.patents.util.nodes.quotainfo;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import org.apache.mahout.math.Arrays;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.patents.util.AccessTokenGenerator;

public class QuotaInfoNodeModel extends
		TwoValuesToCellNodeModel<StringValue, StringValue, StringCell> {

	@Override
	protected void addSettingsModels(List<SettingsModel> settingsModels) {

	}

	@Override
	protected StringCell compute(StringValue cellValue1, StringValue cellValue2)
			throws Exception {

		String accessToken = AccessTokenGenerator.getInstance().getAccessToken(
				cellValue1.getStringValue(), cellValue2.getStringValue());

		if (accessToken == null) {
			throw new IllegalArgumentException(
					"Couldn't request access token with given consumer key and consumer secret!");
		}

		// create connection and set access token
		HttpURLConnection quotaInfoHttpConnection = (HttpURLConnection) new URL(
				"http://ops.epo.org/3.1/rest-services/register/search?q=ti%3Dplastic")
				.openConnection();
		quotaInfoHttpConnection.setRequestProperty("Authorization", "Bearer "
				+ accessToken);


		System.out.println(quotaInfoHttpConnection.toString());
		for (Entry<String, List<String>> httpHeader : quotaInfoHttpConnection
				.getHeaderFields().entrySet()) {
			System.out.println("\t"
					+ httpHeader.getKey()
					+ ", "
					+ Arrays.toString(httpHeader.getValue().toArray(
							new String[httpHeader.getValue().size()])));
		}

		String throttlingControl = quotaInfoHttpConnection
				.getHeaderField("X-Throttling-Control");
		System.out.println(throttlingControl.indexOf("images="));

		return null;
	}
}
