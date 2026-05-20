package sorteddata.sortedarraylist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

public class SortedArrayListIterator<T> implements Iterator<T> {
	private final ArrayList<T> data;
	private int index;
	private int count;

	public SortedArrayListIterator(ArrayList<T> data, Comparator<T> comparator, T from, int count) {
		this.data = data;
		this.index = 0;
		if (from != null)
			while (index < data.size() && comparator.compare(from, data.get(index)) > 0) index++;
		this.count = count;
	}
	@Override
	public boolean hasNext() {
		return index < data.size() && count != 0;
	}

	@Override
	public T next() {
		count--;
		return data.get(index++);
	}
}
