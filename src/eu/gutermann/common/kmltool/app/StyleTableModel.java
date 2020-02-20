package eu.gutermann.common.kmltool.app;

import java.awt.Color;

import javax.swing.table.AbstractTableModel;

import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Style;
import eu.gutermann.common.kmltool.model.KmlModel;

/**
 * The model for showing the KML Style elements in the current KML model.
 * 
 * Note: currently only LineStyle and IconStyle are editable. If a Style has a LineStyle
 * but no IconStyle then only the LineStyle fields are editable and vice versa.
 * Currently it's not possible to add or delete Styles.
 * 
 * Note: Icon URLs often refer to icon files within an imported KMZ. Currently it's not
 * possible to add a local icon file to a KMZ, so changing a local icon URL will result
 * in a missing icon. Only a remote URL location (such as
 * http://maps.google.com/mapfiles/kml/shapes/airports.png) may be set.
 */
public class StyleTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -4017609242256565379L;

	// Column definitions.
	private static enum Columns {
		STYLE_NAME("Style Name", String.class),
		LINE_WIDTH("Line Width", Double.class),
		LINE_COLOR("Line Color", Color.class),
		ICON_URL("Icon URL", String.class),
		ICON_SCALE("Icon Scale", Double.class),
		ICON_HEADING("Icon Heading", Double.class);
		
		private final String name;
		private final Class<?> dataClass;
		
		private Columns(String name, Class<?> dataClass) {
			this.name = name;
			this.dataClass = dataClass;
		}
		
		public String getName() {
			return name;
		}
		
		public Class<?> getDataClass() {
			return dataClass;
		}
	};
	
	private KmlModel model = null;
	
	public StyleTableModel(KmlModel model) {
		this.model = model;
	}
	
	public void setKmlModel(KmlModel model) {
		this.model = model;
		
		// On a new KML model, notify the table that all data was changed.
		fireTableDataChanged();
	}
	
	@Override
	public int getColumnCount() {
		return Columns.values().length;
	}
	
	@Override
	public String getColumnName(int column) {
		return Columns.values()[column].getName();
	}
	
	@Override
	public Class<?> getColumnClass(int column) {
		return Columns.values()[column].getDataClass();
	}

	@Override
	public int getRowCount() {
		return model.getStyles().size();
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		Columns colDef = Columns.values()[col];
		if (colDef == Columns.STYLE_NAME) {
			return false;
		}
		else {
			Object val = getValueForCell(row, col);
			return (val != null);
		}
	}

	@Override
	public Object getValueAt(int row, int col) {
		return getValueForCell(row, col);
	}
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		Style style = model.getStyles().get(row);
		Columns colDef = Columns.values()[col];
		
		switch (colDef) {
		case LINE_WIDTH: {
			LineStyle lineStyle = style.getLineStyle();
			lineStyle.setWidth((Double) value);
		}
		break;
		
		case LINE_COLOR: {
			LineStyle lineStyle = style.getLineStyle();
			Color color = (Color) value;
			lineStyle.setColor(colorToAbgr(color));
		}
		break;
		
		case ICON_URL: {
			IconStyle iconStyle = style.getIconStyle();
			Icon icon = iconStyle.getIcon();
			icon.setHref((String) value);
		}
		break;
		
		case ICON_SCALE: {
			IconStyle iconStyle = style.getIconStyle();
			iconStyle.setScale((Double) value);
		}
		break;
		
		case ICON_HEADING: {
			IconStyle iconStyle = style.getIconStyle();
			iconStyle.setHeading((Double) value);
		}
		break;
		}
	}
	
	private Object getValueForCell(int row, int col) {
		Style style = model.getStyles().get(row);
		Columns colDef = Columns.values()[col];
		Object res = null;
		
		switch (colDef) {
		case STYLE_NAME: {
			res = style.getId();
		}
		break;
		
		case LINE_WIDTH: {
			LineStyle lineStyle = style.getLineStyle();
			if (lineStyle != null)
				res = lineStyle.getWidth();
		}
		break;
		
		case LINE_COLOR: {
			LineStyle lineStyle = style.getLineStyle();
			if (lineStyle != null) {
				String abgr = lineStyle.getColor();
				if (abgr != null)
					return abgrToColor(abgr);
			}
		}
		break;
		
		case ICON_URL: {
			IconStyle iconStyle = style.getIconStyle();
			if (iconStyle != null) {
				Icon icon = iconStyle.getIcon();
				if (icon != null)
					res = icon.getHref();
			}
		}
		break;
		
		case ICON_SCALE: {
			IconStyle iconStyle = style.getIconStyle();
			if (iconStyle != null)
				res = iconStyle.getScale();
		}
		break;
		
		case ICON_HEADING: {
			IconStyle iconStyle = style.getIconStyle();
			if (iconStyle != null)
				res = iconStyle.getHeading();
		}
		break;
		}
		
		return res;
	}
	
	private Color abgrToColor(String abgr) {
		// Convert from a KML ABGR string to a Color object.
		int a = Integer.valueOf(abgr.substring(0, 2), 16);
		int b = Integer.valueOf(abgr.substring(2, 4), 16);
		int g = Integer.valueOf(abgr.substring(4, 6), 16);
		int r = Integer.valueOf(abgr.substring(6, 8), 16);
		return new Color(r, g, b, a);
	}
	
	private String colorToAbgr(Color color) {
		// Convert from a Color object to a KML ABGR string.
		return String.format("%02x%02x%02x%02x", color.getAlpha(), color.getBlue(), color.getGreen(), color.getRed());
	}
	
}
