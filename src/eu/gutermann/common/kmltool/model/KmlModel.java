package eu.gutermann.common.kmltool.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Style;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawler;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawlerListener;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlItem;

/**
 * The main data model for handling a single KML file or the contents of a KMZ file.
 */
public class KmlModel {
	/**
	 * The parsed contents of the KML file. See <a href="http://labs.micromata.de/projects/jak.html">Java API for KML</a> for the Java API for KML library.
	 */
	private Kml kml;
	
	/**
	 * True if the current loaded file is a KMZ file (containing multiple files potentially). False if it's a single KML file.
	 * Note: DXF files are always converted to a single KML.
	 */
	private boolean zipped = false;
	
	/**
	 * The path of the KML file within a loaded KMZ file. Only set it a KMZ file was loaded.
	 */
	private String kmlFilePath;
	
	/**
	 * The temporary directory where the contents of the KMZ file are unzipped, excluding the KML file itself. Only set it a KMZ file was loaded.
	 */
	private Path tempDir;
	
	/**
	 * A list of style definitions from the currently loaded KML.
	 */
	private List<Style> styles = new ArrayList<Style>();

	public Kml getKml() {
		return kml;
	}

	public void setKml(Kml kml) {
		this.kml = kml;
		
		// Extract all style definitions in the newly set KML.
		extractStyles();
	}

	public boolean isZipped() {
		return zipped;
	}

	public void setZipped(boolean zipped) {
		this.zipped = zipped;
	}

	public String getKmlFilePath() {
		return kmlFilePath;
	}

	public void setKmlFilePath(String kmlFilePath) {
		this.kmlFilePath = kmlFilePath;
	}

	public Path getTempDir() {
		return tempDir;
	}

	public void setTempDir(Path tempDir) {
		this.tempDir = tempDir;
	}

	public List<Style> getStyles() {
		return styles;
	}

	public void setStyles(List<Style> styles) {
		if (styles == null)
			styles = new ArrayList<Style>();
		else
			this.styles = styles;
	}

	private void extractStyles() {
		// Crawl over all Style elements and add them into a list.
		KmlCrawlerListener listener = new KmlCrawlerListener() {
			@Override
			public void onStyleSelector(KmlItem item) {
				if (item.getObject() instanceof Style) {
					styles.add((Style) item.getObject());
				}
			}
			
			@Override
			public void onFeature(KmlItem item) {}
		};
		
		new KmlCrawler(kml).addListener(listener).crawl();
	}
	
}
