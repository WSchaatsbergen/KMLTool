package eu.gutermann.common.kmltool.impexp.kml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import eu.gutermann.common.kmltool.impexp.exception.ImportException;
import eu.gutermann.common.kmltool.model.KmlModel;

/**
 * Class for importing single KML files or zipped KML (KMZ) files.
 */
public class KmlImporter {
	/**
	 * The data model into which the file has to be imported.
	 */
	private KmlModel model;
	
	public KmlImporter(KmlModel model) {
		this.model = model;
	}
	
	public void importFile(File file) {
		// Choose import method depending on extension.
		if (file.getName().endsWith(".kmz")) {
			importKmz(file);
		}
		else {
			importKml(file);
		}
	}
	
	private void importKml(File file) {
		// Simply let the Java API for KML library parse the KML file.
		Kml kml = Kml.unmarshal(file);
		model.setKml(kml);
	}
	
	private void importKmz(File file) {
		try {
			// Let the Java API for KML library parse the KML files inside the KMZ.
			// NOTE: if there are more than one KML file in the KMZ then stop import. Almost all KMZ files should contain only one KML file.
			Kml[] kmlArray = Kml.unmarshalFromKmz(file);
			if (kmlArray.length != 1)
				throw new ImportException("The file '" + file + "' contains more than one KML file. Please unpack and import each KML file separately.");
			Kml kml = kmlArray[0];
			model.setKml(kml);
			model.setZipped(true);
			
			// Create a temporary directory where all contents of the KMZ other than the KML file are stored.
			// Note: this code needs a JRE7 to run.
			Path tempDir = Files.createTempDirectory("kml");
			tempDir.toFile().deleteOnExit();
			model.setTempDir(tempDir);
			
			ZipFile kmzFile = new ZipFile(file);
			@SuppressWarnings("unchecked")
			List<FileHeader> headers = kmzFile.getFileHeaders();
			for (FileHeader header : headers) {
				if (header.getFileName().toLowerCase().endsWith(".kml")) {
					// Store the path of the KML file within the zipfile in the model.
					// That way the exporter knows where to place the new KML file within the exported KMZ.
					model.setKmlFilePath(header.getFileName());
				}
				else {
					kmzFile.extractFile(header, tempDir.toString());
				}
			}
		}
		catch (Exception e) {
			throw new ImportException("Could not import KMZ file: " + e.getMessage(), e);
		}
	}
	
}
