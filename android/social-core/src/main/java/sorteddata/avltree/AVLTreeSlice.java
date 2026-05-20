package sorteddata.avltree;
import sorteddata.SortedDataSlice;

import java.util.Comparator;
import java.util.List;

/**
 * In the black-box testing paradigm, you don't look at the code under consideration
 * while writing the tests. To enforce this, we've hidden the code for AVLTreeSlice
 * and it is only substituted when running the code on the CI. Refer to the definitions
 * in SortedDataSlice to understand how this class should function.
 */
public class AVLTreeSlice<T> implements SortedDataSlice<T> {
	AVLTreeSlice(AVLTree<T> tracking, Comparator<T> comparator, int desiredLength) { }

	public static void setVersion(int flaw) {

	}

	public List<T> getResult() { return null; }

	public int shiftBackward(int amount) { return 0; }

	public int shiftForward(int amount) { return 0; }

	public void onAdd(T element) {}
}