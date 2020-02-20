package eu.gutermann.common.kmltool.util.kmlcrawler;

import java.util.ArrayDeque;
import java.util.Deque;

import de.micromata.opengis.kml.v_2_2_0.AbstractObject;

/**
 * Utility class that wraps a KML element and a stack of its parent elements.
 * See <a href="http://labs.micromata.de/projects/jak.html">Java API for KML</a> for the KML elements used.
 */
public class KmlItem {
	private AbstractObject obj;
	private Deque<AbstractObject> stack;
	
	KmlItem(AbstractObject obj, Deque<AbstractObject> stack) {
		// package-protected constructor
		
		this.obj = obj;
		// Copy the stack since the original will be modified by KmlCrawler.
		this.stack = new ArrayDeque<AbstractObject>(stack);
	}
	
	/**
	 * Returns the KML element.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <E extends AbstractObject> E getObject() {
		return (E) obj;
	}
	
	/**
	 * Returns the stack of parent elements.
	 * @return
	 */
	public Deque<AbstractObject> getStack() {
		return stack;
	}
	
	/**
	 * Returns the direct parent of the element from the stack.
	 * @return
	 */
	public AbstractObject getParent() {
		return stack.getFirst();
	}
	
	/**
	 * Returns the first parent element from the stack that is of a certain class.
	 * @param clazz
	 * @return
	 */
	public AbstractObject getFirstParentOfType(Class<? extends AbstractObject> clazz) {
		for (AbstractObject obj : stack) {
			if (clazz.isAssignableFrom(obj.getClass()))
				return obj;
		}
		return null;
	}
	
}
