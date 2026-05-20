package sorteddata;

import java.util.Comparator;

public class SortedDataFactory {
	public static <T> SortedData<T> makeSortedData(Comparator<T> comparator) {
		return new sorteddata.sortedarraylist.SortedArrayList<>(comparator);
		//return new sorteddata.avltree.AVLTree<>(comparator);
	}
}
