package eu.gutermann.common.kmltool.util;

import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Data;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawler;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawlerListener;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlItem;

/**
 * Utility class for converting KML ExtendedData elements to an HTML description within a Feature (such as a Placemark).
 * It also erases the Schemas within the Document.
 * See <a href="http://labs.micromata.de/projects/jak.html">Java API for KML</a> for the KML elements used.
 * 
 * IMPORTANT: The Java API for KML library does not allow to set CDATA inside a description element. The HTML written
 * inside will be HTML-encoded, meaning that all tags will look like "&lt;table&gt;" etc. This however is still rendered
 * correctly by Google Earth and Maps.
 */
public class ExtendedDataToDescriptionConverter {
	
	/**
	 * Execute the conversion on a Kml instance.
	 * @param kml
	 */
	public void execute(Kml kml) {
		KmlCrawlerListener listener = new KmlCrawlerListener() {
			@Override
			public void onStyleSelector(KmlItem item) {}
			
			@Override
			public void onFeature(KmlItem item) {
				// Handle Document and other Features separately.
				if (item.getObject() instanceof Document) {
					handleDocument(item);
				}
				else {
					handleOtherFeatures(item);
				}
			}
		};
		
		new KmlCrawler(kml).addListener(listener).crawl();
	}
	
	private void handleDocument(KmlItem item) {
		// Remove all Schemas from the Document.
		Document doc = item.getObject();
		doc.setSchema(null);
	}
	
	private void handleOtherFeatures(KmlItem item) {
		// If a Feature contains an ExtendedData element then start the conversion.
		Feature feat = item.getObject();
		ExtendedData extData = feat.getExtendedData();
		if (extData != null) {
			// Create an HTML table for the key-value pairs in the ExtendedData.
			StringBuilder b = new StringBuilder();
			b.append("<center><table border='0'>\n");
			
			boolean oddLine = true;
			
			// Convert the key-value pairs to HTML table rows for all SchemaData-SimpleData elements within the ExtendedData element.
			List<SchemaData> schemaDatas = extData.getSchemaData();
			if (schemaDatas != null) {
				for (SchemaData schemaData : schemaDatas) {
					List<SimpleData> simpleDatas = schemaData.getSimpleData();
					if (simpleDatas != null) {
						for (SimpleData simpleData : simpleDatas) {
							addTableRow(b, simpleData.getName(), simpleData.getValue(), oddLine);
							oddLine = !oddLine;
						}
					}
				}
			}
			
			// Convert the key-value pairs to HTML table rows for all Data elements within the ExtendedData element.
			List<Data> datas = extData.getData();
			if (datas != null) {
				for (Data data : datas) {
					addTableRow(b, data.getName(), data.getValue(), oddLine);
					oddLine = !oddLine;
				}
			}
			
			b.append("</table></center>");
			
			// Set the HTML table as the description of the feature.
			feat.setDescription(b.toString());
			
			// Remove the ExtendedData element.
			feat.setExtendedData(null);
		}
	}
	
	private void addTableRow(StringBuilder b, String name, String value, boolean oddLine) {
		// Add a new table row with a key-value pair. Color odd and even rows differently.
		b.append("<tr bgcolor='");
		b.append(oddLine ? "#E3E3F3" : "#FFFFFF");
		b.append("'>");
		b.append("<th>");
		b.append(name);
		b.append("</th>");
		b.append("<td>");
		b.append(value);
		b.append("</td>");
		b.append("</tr>");
		b.append('\n');
	}
	
}
