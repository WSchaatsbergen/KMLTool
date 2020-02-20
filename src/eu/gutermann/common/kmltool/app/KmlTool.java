package eu.gutermann.common.kmltool.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.gutermann.common.kmltool.impexp.dxf.DxfImporter;
import eu.gutermann.common.kmltool.impexp.kml.KmlExporter;
import eu.gutermann.common.kmltool.impexp.kml.KmlImporter;
import eu.gutermann.common.kmltool.model.KmlModel;

/**
 * The main window of the application. Created by WindowBuilder for Swing.
 */
public class KmlTool {
	private static final String APP_TITLE = "KML Tool";
	
	private JFrame frmKmlTool;
	private JTable styleTable;
	private StyleTableModel styleTableModel;
	private JFileChooser openKmlChooser;
	private JFileChooser openDxfChooser;
	private JFileChooser saveKmzChooser;
	
	private KmlModel model = new KmlModel();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					
					KmlTool window = new KmlTool();
					window.frmKmlTool.setVisible(true);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public KmlTool() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmKmlTool = new JFrame();
		frmKmlTool.setTitle(APP_TITLE);
		frmKmlTool.setBounds(100, 100, 1024, 800);
		frmKmlTool.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// File chooser for opening KML/KMZ files.
		openKmlChooser = new JFileChooser();
		openKmlChooser.setAcceptAllFileFilterUsed(false);
		openKmlChooser.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "Google KML/KMZ Files (*.kml;*.kmz)";
			}
			
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				
				String name = f.getName().toLowerCase();
				return name.endsWith(".kml") || name.endsWith(".kmz");
			}
		});
		
		// File chooser for opening DXF files.
		openDxfChooser = new JFileChooser();
		openDxfChooser.setAcceptAllFileFilterUsed(false);
		openDxfChooser.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "Autocad DXF Files (*.dxf)";
			}
			
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				
				return f.getName().toLowerCase().endsWith(".dxf");
			}
		});
		
		// File chooser for saving KMZ files.
		saveKmzChooser = new JFileChooser();
		saveKmzChooser.setAcceptAllFileFilterUsed(false);
		saveKmzChooser.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "KMZ Files (*.kmz)";
			}
			
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				
				return f.getName().toLowerCase().endsWith(".kmz");
			}
		});
		
		JMenuBar menuBar = new JMenuBar();
		frmKmlTool.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		// Menu item for importing KML/KMZ files.
		JMenuItem mntmLoadKml = new JMenuItem("Load KML/KMZ File...");
		mntmLoadKml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int ret = openKmlChooser.showOpenDialog(frmKmlTool);
					if (ret == JFileChooser.APPROVE_OPTION) {
						resetModel();
						
						new KmlImporter(model).importFile(openKmlChooser.getSelectedFile());
						
						styleTableModel.fireTableDataChanged();
						
						updateAppTitle(openKmlChooser.getSelectedFile());
					}
					
					openKmlChooser.setSelectedFile(null);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frmKmlTool, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					updateAppTitle(null);
				}
			}
		});
		mnFile.add(mntmLoadKml);
		
		// Menu item for importing KML/KMZ files.
		JMenuItem mntmLoadDxf = new JMenuItem("Load DXF File...");
		mntmLoadDxf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int ret = openDxfChooser.showOpenDialog(frmKmlTool);
					if (ret == JFileChooser.APPROVE_OPTION) {
						// Open the Coordinate Reference System chooser after the user selected a file.
						// Note: JCRSChooser and CRSListModel were taken from the GeoTools library and
						// modified so that it works with the epsg.properties file.
						// See DxfImporter for more information.
						CoordinateReferenceSystem srcCrs = JCRSChooser.showDialog();
						if (srcCrs != null) {
							resetModel();
						}
						
						new DxfImporter(model).importFile(openDxfChooser.getSelectedFile(), srcCrs);
						
						styleTableModel.fireTableDataChanged();
						
						updateAppTitle(openDxfChooser.getSelectedFile());
					}
					
					openDxfChooser.setSelectedFile(null);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frmKmlTool, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					updateAppTitle(null);
				}
			}
		});
		mnFile.add(mntmLoadDxf);
		
		mnFile.addSeparator();
		
		// Menu item for saving a single KMZ file that works in Google Earth (but not Maps).
		JMenuItem mntmSaveKmz = new JMenuItem("Save As KMZ For Google Earth...");
		mntmSaveKmz.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int ret = saveKmzChooser.showSaveDialog(frmKmlTool);
					if (ret == JFileChooser.APPROVE_OPTION) {
						File file = saveKmzChooser.getSelectedFile();
						if (!file.getName().toLowerCase().endsWith(".kmz")) {
							file = new File(file.getPath() + ".kmz");
						}
						
						new KmlExporter(model).exportKmz(file);
					}
					
					saveKmzChooser.setSelectedFile(null);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frmKmlTool, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mnFile.add(mntmSaveKmz);
		
		// Menu item for saving a KMZ file that works in Google Maps (for ZONESCAN net).
		// Note that multiple KMZ files may be saved if a single file would be too large
		// for Google Maps.
		JMenuItem mntmSaveKmzGoogleMaps = new JMenuItem("Save As KMZ For Google Maps...");
		mntmSaveKmzGoogleMaps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int ret = saveKmzChooser.showSaveDialog(frmKmlTool);
					if (ret == JFileChooser.APPROVE_OPTION) {
						File file = saveKmzChooser.getSelectedFile();
						if (!file.getName().toLowerCase().endsWith(".kmz")) {
							file = new File(file.getPath() + ".kmz");
						}
						
						new KmlExporter(model).exportKmzForGoogleMaps(file);
					}
					
					saveKmzChooser.setSelectedFile(null);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frmKmlTool, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mnFile.add(mntmSaveKmzGoogleMaps);
		
		mnFile.addSeparator();
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmKmlTool.setVisible(false);
				frmKmlTool.dispose();
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		frmKmlTool.getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		// Table for showing the editable styles in the KML model.
		// For colors, ColorRenderer and ColorEditor are used. These were taken from Java Swing examples.
		styleTableModel = new StyleTableModel(model);
		styleTable = new JTable();
		styleTable.setColumnSelectionAllowed(true);
		styleTable.setRowSelectionAllowed(false);
		styleTable.setFillsViewportHeight(true);
		styleTable.setCellSelectionEnabled(true);
		styleTable.setDefaultRenderer(Color.class, new ColorRenderer(true));
		styleTable.setDefaultEditor(Color.class, new ColorEditor());
		styleTable.setModel(styleTableModel);
		scrollPane.setViewportView(styleTable);
		
	}

	private void resetModel() {
		model = new KmlModel();
		styleTableModel.setKmlModel(model);
	}
	
	private void updateAppTitle(File file) {
		String title = APP_TITLE;
		if (file != null) {
			title += " - [" + file + "]";
		}
		frmKmlTool.setTitle(title);
	}
	
}
