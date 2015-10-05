package org.knime.knip.patents;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.knip.patents.prefs.OPSPreferenceConstants;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class KNIMEOPSPlugin extends AbstractUIPlugin {

	// The shared instance.
	private static KNIMEOPSPlugin plugin;

	/** The plugin ID. */
	public static final String PLUGIN_ID = "org.knime.ip";

	/**
	 * Returns the shared instance.
	 *
	 * @return Singleton instance of the Plugin
	 */
	public static KNIMEOPSPlugin getDefault() {
		return plugin;
	}

	/**
	 * The constructor.
	 */
	public KNIMEOPSPlugin() {
		plugin = this;
	}

	public static final boolean isThrottlingEnabled() {
		return KNIMEOPSPlugin.getDefault().getPreferenceStore()
				.getBoolean(OPSPreferenceConstants.P_ENABLE_THROTTLING);
	}

	public static final String getOAuth2ConsumerKey() {
		return KNIMEOPSPlugin.getDefault().getPreferenceStore()
				.getString(OPSPreferenceConstants.P_OAUTH2_CONSUMER_KEY);
	}

	public static final String getOAuth2ConsumerSecret() {
		return KNIMEOPSPlugin.getDefault().getPreferenceStore()
				.getString(OPSPreferenceConstants.P_OAUTH2_CONSUMER_SECRET);
	}
}
