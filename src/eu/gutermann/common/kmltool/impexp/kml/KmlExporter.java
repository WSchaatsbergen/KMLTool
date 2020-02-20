package eu.gutermann.common.kmltool.impexp.kml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import de.micromata.opengis.kml.v_2_2_0.Container;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import eu.gutermann.common.kmltool.impexp.exception.ExportException;
import eu.gutermann.common.kmltool.model.KmlModel;
import eu.gutermann.common.kmltool.util.ExtendedDataToDescriptionConverter;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawler;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlCrawlerListener;
import eu.gutermann.common.kmltool.util.kmlcrawler.KmlItem;

/**
 * Class for exporting a KML model to a KML or KMZ file(s).
 */
public class KmlExporter {
	/**
	 * KMLCrawler listener for getting the number of Placemark elements in the KML.
	 */
	private class PlacemarkCountListener implements KmlCrawlerListener {
		public int res = 0;
		
		@Override
		public void onStyleSelector(KmlItem item) {}
		
		@Override
		public void onFeature(KmlItem item) {
			if (item.getObject() instanceof Placemark)
				res++;
		}
	};
	
	/**
	 * The maximum size that an uncompressed KML file can have before it's split over multiple KMZ files.
	 */
	private static final int MAX_KML_SIZE = 5 * 1024 * 1024; // 5 MB
	
	private KmlModel model;
	
	public KmlExporter(KmlModel model) {
		this.model = model;
	}
	
	/**
	 * Exports the current KML model to a KMZ file that works in Google Earth (but likely not in Google Maps).
	 * @param file
	 */
	public void exportKmz(File file) {
		try {
			Kml kml = model.getKml();
			byte[] bytes = createKmlBytes(kml);
			
			saveKmzFile(file, bytes);
		}
		catch (Exception e) {
			throw new ExportException("Could not export KMZ file: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Exports the current KML model to one or more KMZ files (depending on the KML size).
	 * The KML is modified in such a way that it will work as an overlay in Google Maps.
	 * In case of multiple files the file names will be "<base file name>01.kmz", etc.
	 * @param file
	 */
	public void exportKmzForGoogleMaps(File file) {
		try {
			// Clone the KML from the model so that the original model is untouched.
			Kml kml = model.getKml().clone();
			
			// Convert ExtendedData elements to HTML descriptions.
			new ExtendedDataToDescriptionConverter().execute(kml);
			
			byte[] bytes = createKmlBytes(kml);
			
			// Save one or multiple KMZ files depending on the number of KML bytes.
			int numKmzFiles = calcNumKmzFiles(bytes);
			if (numKmzFiles == 1) {
				saveKmzFile(file, bytes);
			}
			else {
				saveMultipleKmzFiles(file, kml, numKmzFiles);
			}
		}
		catch (Exception e) {
			throw new ExportException("Could not export KMZ file: " + e.getMessage(), e);
		}
	}
	
	private byte[] createKmlBytes(Kml kml) throws FileNotFoundException {
		// Output the KML to a byte array.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		kml.marshal(baos);
		return baos.toByteArray();
	}
	
	private int calcNumKmzFiles(byte[] kmlBytes) {
		return (int) Math.ceil((double) kmlBytes.length / MAX_KML_SIZE);
	}
	
	private ZipFile saveKmzFile(File file, byte[] kmlBytes) throws ZipException {
		// Remove an existing KMZ file to avoid "File already exists" errors from Zip4j.
		if (file.exists())
			file.delete();
		
		ZipFile kmz = new ZipFile(file);
		
		// In case the model contains a loaded KMZ file then the original contents must be
		// packed in the new KMZ file again.
		if (model.isZipped() && model.getTempDir().toFile().list().length > 0) {
			ZipParameters params = new ZipParameters();
			params.setIncludeRootFolder(false);
			params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_MAXIMUM);
			kmz.createZipFileFromFolder(model.getTempDir().toString(), params, false, -1);
		}
		
		// Add the KML byte array to the new KMZ file.
		// The name of the KML file within the KMZ is either the original name if it came
		// from a KMZ originally, or "doc.kml" if it was loaded from another source.
		ByteArrayInputStream bais = new ByteArrayInputStream(kmlBytes);
		ZipParameters params = new ZipParameters();
		String kmlFileName = model.isZipped() ? model.getKmlFilePath() : "doc.kml";
		params.setFileNameInZip(kmlFileName);
		params.setSourceExternalStream(true);
		params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
		params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_MAXIMUM);
		kmz.addStream(bais, params);
		
		return kmz;
	}
	
	private void saveMultipleKmzFiles(File baseFile, Kml kml, int numFiles) throws ZipException, FileNotFoundException {
		// Determine the number of Placemarks in the KML so that we can subdivide them
		// over multiple KMZ files.
		int numPlacemarks = getNumPlacemarks(kml);
		int numPlacemarksPerFile = (int) Math.ceil((double) numPlacemarks / numFiles);
		
		// For each file to write, clone the KML and remove all Placemarks that are
		// going to be in other files.
		// NOTE: This code is slow because Java API for KML doesn't provide detach functions
		// for separate elements. Instead, we must get the parent Folder or Document and
		// remove it from the list there. Since JAK implements them as ArrayLists it's slow
		// to remove single elements all the time.
		// A faster solution would be to iterate over the original KML and build new KMLs
		// from it from scratch instead of cloning and deleting. This would take a lot of
		// coding though.
		for (int fileCount = 0; fileCount < numFiles; fileCount++) {
			Kml curKml = kml.clone();
			final int startIdx = fileCount * numPlacemarksPerFile;
			final int endIdx = startIdx + numPlacemarksPerFile - 1;
			
			KmlCrawlerListener listener = new KmlCrawlerListener() {
				int placemarktCount = 0;
				
				@Override
				public void onStyleSelector(KmlItem item) {}
				
				@Override
				public void onFeature(KmlItem item) {
					if (item.getObject() instanceof Placemark) {
						if (placemarktCount < startIdx || placemarktCount > endIdx) {
							Container cont = (Container) item.getParent();
							List<Feature> features = (cont instanceof Document) ? ((Document) cont).getFeature() : ((Folder) cont).getFeature();
							features.remove(item.getObject());
						}
						
						placemarktCount++;
					}
				}
			};
			new KmlCrawler(curKml).addListener(listener).crawl();
			
			// After removal of the Placemarks, the KML may contain empty folders. These are removed.
			removeEmptyFolders(curKml);
			
			// TODO Remove styles that aren't used anymore as well.
			
			// Create the new file name for the current KML and save it as KMZ.
			File numberedFile = createFileWithNumber(baseFile, fileCount+1);
			byte[] bytes = createKmlBytes(curKml);
			saveKmzFile(numberedFile, bytes);
		}
	}
	
	private File createFileWithNumber(File baseFile, int number) {
		// Add a number styles like "01" between the base file name and the extension.
		String basePath = baseFile.getPath();
		String numberString = String.format("%02d", number);
		int offset = basePath.lastIndexOf('.');
		if (offset < 0)
			offset = basePath.length();
		StringBuilder b = new StringBuilder(basePath);
		b.insert(offset, numberString);
		return new File(b.toString());
	}
	
	private int getNumPlacemarks(Kml kml) {
		PlacemarkCountListener listener = new PlacemarkCountListener();
		new KmlCrawler(kml).addListener(listener).crawl();
		return listener.res;
	}
	
	private void removeEmptyFolders(Kml kml) {
		KmlCrawlerListener listener = new KmlCrawlerListener() {
			@Override
			public void onStyleSelector(KmlItem item) {}
			
			@Override
			public void onFeature(KmlItem item) {
				if (item.getObject() instanceof Folder) {
					Folder folder = item.getObject();
					if (folder.getFeature().isEmpty()) {
						Container cont = (Container) item.getParent();
						if (cont instanceof Document)
							((Document) cont).getFeature().remove(folder);
						else
							((Folder) cont).getFeature().remove(folder);
					}
				}
			}
		};
		
		new KmlCrawler(kml).addListener(listener).crawl();
	}
	
}
