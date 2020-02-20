package eu.gutermann.common.kmltool.util.kmlcrawler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.AbstractObject;
import de.micromata.opengis.kml.v_2_2_0.Container;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;

/**
 * Utility class for iteration over a KML's Document, nested Folders and the elements they hold.
 * With a {@link KmlCrawlerListener} operations may be executed on elements during iteration.
 * See <a href="http://labs.micromata.de/projects/jak.html">Java API for KML</a> for the KML elements used.
 */
public class KmlCrawler {
	private Kml kml;
	
	/**
	 * Contains the stack of nested elements in which the crawler is currently iterating.
	 */
	private Deque<AbstractObject> stack;
	
	private List<KmlCrawlerListener> listeners = new ArrayList<KmlCrawlerListener>();
	
	public KmlCrawler(Kml kml) {
		this.kml = kml;
	}
	
	/**
	 * Adds a listener to the crawler.
	 * @param listener
	 * @return
	 */
	public KmlCrawler addListener(KmlCrawlerListener listener) {
		listeners.add(listener);
		return this;
	}
	
	/**
	 * Removes a listener from the crawler.
	 * @param listener
	 * @return
	 */
	public KmlCrawler removeListener(KmlCrawlerListener listener) {
		listeners.remove(listener);
		return this;
	}
	
	/**
	 * Execute the iteration.
	 * @return
	 */
	public KmlCrawler crawl() {
		stack = new ArrayDeque<AbstractObject>();
		
		// Start iterating with the top Feature. This is usually a Document, but may also be a single Placemark.
		Feature topFeat = kml.getFeature();
		fireEventFor(topFeat);

		// If the Feature is a Container (should be a Document), start iterating over its elements.
		if (topFeat instanceof Container) {
			stack.push(topFeat);
			
			try {
				crawlContainer((Container) topFeat);
			}
			finally {
				stack.pop();
			}
		}
		
		return this;
	}
	
	private void crawlContainer(Container cont) {
		// Assemble all elements in the current Container (Document or Folder) in a list.
		// We are interested in Features (including other Containers) and StyleSelectors. Other elements are ignored.
		List<AbstractObject> objects = new ArrayList<AbstractObject>();
		
		if (cont instanceof Document) {
			objects.addAll(((Document) cont).getFeature());
		}
		else if (cont instanceof Folder) {
			objects.addAll(((Folder) cont).getFeature());
		}
		
		objects.addAll(cont.getStyleSelector());
		
		// For each of the elements, fire an event. If the element is a Container (should be a Folder) recursively iterate over its elements.
		for (AbstractObject obj : objects) {
			fireEventFor(obj);
			
			if (obj instanceof Container) {
				stack.push(obj);
			
				try {
					Container childCont = (Container) obj;
					crawlContainer(childCont);
				}
				finally {
					stack.pop();
				}
			}
		}
	}
	
	private void fireEventFor(AbstractObject obj) {
		// Wrap the current element and the current stack in a KmlItem and fire the appropriate event for the element.
		
		// features
		if (obj instanceof Feature) {
			onFeature(new KmlItem(obj, stack));
		}
		// style
		else if (obj instanceof StyleSelector) {
			onStyleSelector(new KmlItem(obj, stack));
		}
		// ignore all other items
	}
	
	private void onFeature(KmlItem item) {
		for (KmlCrawlerListener l : listeners) {
			l.onFeature(item);
		}
	}
	
	private void onStyleSelector(KmlItem item) {
		for (KmlCrawlerListener l : listeners) {
			l.onStyleSelector(item);
		}
	}
	
}
