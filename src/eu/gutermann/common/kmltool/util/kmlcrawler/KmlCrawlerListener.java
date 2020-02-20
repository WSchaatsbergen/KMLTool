package eu.gutermann.common.kmltool.util.kmlcrawler;

import java.util.EventListener;

/**
 * Listener for the {@link KmlCrawler}.
 */
public interface KmlCrawlerListener extends EventListener {

	/**
	 * Fired when a Feature element is encountered (e.g. Folder, Placemark, etc.)
	 * @param item
	 */
	void onFeature(KmlItem item);
	
	/**
	 * Fired when a Style element is encountered (Style or StyleMap)
	 * @param item
	 */
	void onStyleSelector(KmlItem item);
	
}
