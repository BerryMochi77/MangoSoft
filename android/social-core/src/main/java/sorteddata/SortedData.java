package sorteddata;

import java.util.Iterator;

/**
 * Maintains a set of data, with no duplicates, in sorted order.
 * @param <T> The type of data to be stored.
 */
public abstract class SortedData<T> {
	/**
		Attempts to insert a value into the data structure.
	 	@param value The value to insert.
		@return true if successful, false if data structure already contains value.
	 */
	public abstract boolean insert(T value);

	/**
	 Attempts to find a value inside the data structure.
	 @param value The value to insert.
	 @return the element if found, and null otherwise.
	 @apiNote Note that this method looks for equality according to the comparator. If this data structure's comparator
	 only checks some fields of the parameter type T, this can be used to search the data structure for elements
	 matching those conditions
	 */
	public abstract T get(T value);

	/**
	 * Gets a particular element, in sorted order.
	 * @param i the index of teh desired item
	 * @return the item at that index
	 */
	public abstract T getAtIndex(int i);

	/**
	 * Generates an Iterator that searches through the current state of the data structure.
	 * @param start the element from which to start looking
	 * @param count the maximum number of elements to search for. If the end of the data
	 *              structure is reached before count elements are found, it returns early.
	 *              If negative, searches for an unlimited number of elements.
	 * @param backwards in which direction to perform the iteration
	 * @return the Iterator
	 * @apiNote If the data structure is modified while the Iterator lives, the Iterator
	 * will not reflect changes to the data structure.
	 */
	public abstract Iterator<T> getRange(T start, int count, boolean backwards);

	/**
	 * Creates an iterator that goes through each element in the data structure in order.
	 * @return the Iterator
	 * @apiNote equivalent to getRange(null, -1, false)
	 */
	public Iterator<T> getAll() {
		return getRange(null, -1, false);
	}

	/**
	 * Uniformly selects a random element from the data structure.
	 * @return null if the structure is empty, otherwise a randomly selected element.
	 * @implNote This function uses PRNG and is thus not suitable for cryptographic purposes.
	 */
	public abstract T getRandom();
}
