package sorteddata;

import java.util.List;

/**
 * A movable slice reflecting a small section of a (generally) much larger data structure.
 * <p></p>
 * It has two possible states: one where it is pinned to the bottom (tail end) of the list,
 * and one where it is unpinned, which affect how inserted elements are reflected.
 * It always begins in the pinned state.
 * @param <T> the data type stored in the slice
 * @implSpec Must be initialised with a positive desired length. Throws an exception otherwise
 */
public interface SortedDataSlice<T> extends SortedDataSubject<T> {
	/**
	 * Returns a list reflecting the slice. This list is mutable in
	 * that it is updated by this class in accordance with changes
	 * to the underlying data structure, or as the slice is shifted.
	 * <p>
	 * The length of this list will typically be the desired size of the slice,
	 * but may be smaller if the underlying data structure contains fewer
	 * than this number of elements.
	 *
	 * @implSpec This structure must not be modified by the client class, or
	 * this class will enter a state of undefined behaviour.
	 * If this is necessary, perform a clone on the result first.
	 * @return the list
	 */
	public List<T> getResult();

	/**
	 * Moves the slice earlier, so that elements before the current start are brought into the slice
	 * and elements towards the end are removed.
	 * If the slice moves, the slice exits the pinned state.
	 * @param distance the number of spots to shift
	 * @return the number of spots actually shifted, which may be lower than the desired distance
	 * if the start of the list is reached prematurely
	 */
	public int shiftBackward(int distance);

	/**
	 * Moves the slice later, so that elements after the current end are brought into the slice
	 * and elements towards the start are removed.
	 * If the client attempts to shift the slice beyond the end of the array, the shift stops at the
	 * end of the array, and the slice enters the pinned state.
	 * @param distance the number of spots to shift
	 * @return the number of spots actually shifted, which may be lower than the desired distance
	 * if the end of the list is reached prematurely
	 */
	public int shiftForward(int distance);

	/**
	 * A callback that should not be called externally to inform the slice that a new element has
	 * been added to the underlying data structure. If this element lies inside the range currently
	 * reflected by the slice, the element will be inserted.
	 * Additionally, if the slice has been scrolled all the way to the bottom, elements inserted beyond
	 * the slice will always be included. Similarly, if the slice has been scrolled all the way to the top,
	 * elements inserted before the slice will always be included.
	 * <p>
	 * If necessary to maintain the desired length
	 * of the slice, an element may be removed. This element should be taken from the end of the list
	 * (so that the start remains the same), unless the list is in the pinned state, in which case
	 * the element is removed from the start.
	 * <p>
	 * Of course, if the element is added outside the range in the data structure that the slice
	 * currently reflects, the element will not currently be reflected in the slice.
	 * @param element the element added to the underlying data structure
	 */
	public void onAdd(T element);
}
