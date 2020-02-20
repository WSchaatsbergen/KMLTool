package eu.gutermann.common.kmltool.impexp.dxf;

import java.awt.Color;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.kabeja.dxf.DXFColor;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFPoint;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFVertex;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Style;
import eu.gutermann.common.kmltool.impexp.exception.ImportException;
import eu.gutermann.common.kmltool.model.KmlModel;

/**
 * Class for importing Autocad DXF files and converting them to the KML model.
 * It uses the <a href="http://www.geotools.org/">GeoTools</a> library for converting the coordinates
 * in the DXF file from the original coordinate system to WGS84.
 * It uses the <a href="http://kabeja.sourceforge.net/">Kabeja</a> library to parse DXF files.
 * It uses the <a href="http://labs.micromata.de/projects/jak.html">Java API for KML</a> for creating
 * the KML model.
 * 
 * IMPORTANT: GeoTools normally uses an HSQL database from its plugin gt-epsg-hsql-11.2.jar that contains
 * the coordinate system definitions from EPSG (European Petroleum Survey group). However, the HSQL driver
 * throws exceptions when using this plugin, so I took it out.
 * Instead, I use the plugin gt-epsg-wkt-11.2.jar which contains a file epsg.properties that has the EPSG
 * definitions in a text format called WKT.
 * However, some WKT definitions in there turned out to be incorrect. For the Swiss and some other
 * coordinate systems it defined the "Hotine_Oblique_Mercator" projection whereas it should've defined
 * "Oblique_Mercator". I changed this in the epsg.properties file. Note that the gt-epsg-wkt-11.2.jar
 * file is therefore changed from the original GeoTools package.
 */
public class DxfImporter {
	private KmlModel model;
	
	private MathTransform transform;
	
	public DxfImporter(KmlModel model) {
		this.model = model;
	}
	
	public void importFile(File file, CoordinateReferenceSystem srcCrs) {
		try {
			// Test code for checking if the definition of the CH1903 / LV03 coordinate system was working correctly.
			//String wkt = "PROJCS[\"CH1903 / LV03\", GEOGCS[\"CH1903\", DATUM[\"CH1903\", SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], TOWGS84[674.4, 15.1, 405.3, 0.0, 0.0, 0.0, 0.0], AUTHORITY[\"EPSG\",\"6149\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic latitude\", NORTH], AXIS[\"Geodetic longitude\", EAST], AUTHORITY[\"EPSG\",\"4149\"]], PROJECTION[\"Oblique_Mercator\", AUTHORITY[\"EPSG\",\"9815\"]], PARAMETER[\"longitude_of_center\", 7.439583333333333], PARAMETER[\"latitude_of_center\", 46.952405555555565], PARAMETER[\"azimuth\", 90.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 600000.0], PARAMETER[\"false_northing\", 200000.0], PARAMETER[\"rectified_grid_angle\", 90.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"21781\"]]";
  		//CoordinateReferenceSystem testCrs = CRS.parseWKT(wkt);
			
			// Get the transformation algorithm for converting from the DXF file's coordinate system to WGS84.
			transform = CRS.findMathTransform(srcCrs, DefaultGeographicCRS.WGS84, true);
			
			// Get the root DXFDocument from the DXF file.
			Parser parser = ParserBuilder.createDefaultParser();
			parser.parse(file.getPath());
			DXFDocument dxfDoc = parser.getDocument();
			
			// Create the KML and a Document element with the DXF file's name.
			Kml kml = KmlFactory.createKml();
			Document kmlDoc = kml.createAndSetDocument();
			
			String filename = file.getName();
			int extensionIndex = filename.lastIndexOf('.');
			String docName = (extensionIndex > 0) ? filename.substring(0, extensionIndex) : filename;
			kmlDoc.setName(docName);
			
			// Convert DXF data to KML for each layer within the DXF document.
			for (Iterator<?> layerIt = dxfDoc.getDXFLayerIterator(); layerIt.hasNext();) {
				handleDxfLayer((DXFLayer) layerIt.next(), kmlDoc);
			}
			
			model.setKml(kml);
		}
		catch (Exception e) {
			throw new ImportException("Could not import DXF file: " + e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleDxfLayer(DXFLayer dxfLayer, Document kmlDoc) throws TransformException {
		String layerName = dxfLayer.getName();
		
		// There may be layers in the DXF that have an empty name or the name "0".
		// These don't have any useful data so ignore them.
		if (!"".equals(layerName) && !"0".equals(layerName)) {
			// Only extract the data that we want from the layer: points and polylines.
			// Note: most DXF files with a pipe network seem to use LwPolylines and not
			// Polylines, but they are handled in the same way.
			List<DXFPoint> points = dxfLayer.getDXFEntities(DXFConstants.ENTITY_TYPE_POINT);
			List<DXFPolyline> polyLines = dxfLayer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);
			List<DXFLWPolyline> lwPolyLines = dxfLayer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE);
			
			// If there is no data that we want in this layer then don't import this layer.
			if (points == null && polyLines == null && lwPolyLines == null)
				return;
			
			// Convert the layer's style definition to KML Style elements within the Document.
			Style style = kmlDoc.createAndAddStyle();
			style.setId(layerName);
			
			LineStyle lineStyle = style.createAndSetLineStyle();
			String lineColor;
			if (dxfLayer.getColor() < 0) {
				// Use opaque black if no color was defined.
				lineColor = "ff000000";
			}
			else {
				// Convert the DXF color to an ABGR string used in KML.
				String rgbString = DXFColor.getRGBString(dxfLayer.getColor());
				String[] components = rgbString.split(",");
				Color color = new Color(
						Integer.parseInt(components[0]),
						Integer.parseInt(components[1]),
						Integer.parseInt(components[2])
				);
				lineColor = String.format("%02x%02x%02x%02x", color.getAlpha(), color.getBlue(), color.getGreen(), color.getRed());
			}
			lineStyle.setColor(lineColor);
			
			// DXF line weigth is defined in mm whereas in KML the line width is in pixels.
			// Conversion between these is difficult so just add the number of centimeters to 1 for now.
			// If no line width was defined, set it to 1.
			double lineWidth = (dxfLayer.getLineWeight() < 0) ? 1 : 1 + dxfLayer.getLineWeight() / 100.0;
			lineStyle.setWidth(lineWidth);
			
			// Create a KML Folder for the DXF layer.
			Folder folder = kmlDoc.createAndAddFolder();
			folder.setName(layerName);
			
			// Convert the DXF geometry to KML Placemarks, converting its coordinates.
			if (points != null) {
				for (DXFPoint point : points) {
					handleDxfPoint(point, folder, style);
				}
			}
			
			if (polyLines != null) {
				for (DXFPolyline polyLine : polyLines) {
					handleDxfPolyLine(polyLine, folder, style);
				}
			}
			
			if (lwPolyLines != null) {
				for (DXFLWPolyline lwPolyLine : lwPolyLines) {
					handleDxfPolyLine(lwPolyLine, folder, style);
				}
			}
		}
	}
	
	private void handleDxfPoint(DXFPoint dxfPoint, Folder folder, Style style) throws TransformException {
		Placemark placemark = folder.createAndAddPlacemark();
		placemark.setStyleUrl("#" + style.getId());
		
		Point point = placemark.createAndSetPoint();
		double[] srcCoords = {dxfPoint.getX(), dxfPoint.getY()};
		double[] dstCoords = new double[2];
		transform.transform(srcCoords, 0, dstCoords, 0, 1);
		point.addToCoordinates(dstCoords[0], dstCoords[1]);
	}
	
	private void handleDxfPolyLine(DXFPolyline dxfPolyLine, Folder folder, Style style) throws TransformException {
		Placemark placemark = folder.createAndAddPlacemark();
		placemark.setStyleUrl("#" + style.getId());
		
		LineString line = placemark.createAndSetLineString();
		for (Iterator<?> vertexIt = dxfPolyLine.getVertexIterator(); vertexIt.hasNext();) {
			DXFVertex dxfVertex = (DXFVertex) vertexIt.next();
			double[] srcCoords = {dxfVertex.getX(), dxfVertex.getY()};
			double[] dstCoords = new double[2];
			transform.transform(srcCoords, 0, dstCoords, 0, 1);
			line.addToCoordinates(dstCoords[0], dstCoords[1]);
		}
	}
	
}
