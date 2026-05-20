package sorteddata.bstree;

import java.util.Comparator;

abstract class BSNode<T> {
	protected final Comparator<T> comparator;
	public BSNode(Comparator<T> comparator) {
		this.comparator = comparator;
	}

	public abstract int height();

	public abstract int size();

	public abstract BSNodeFilled<T> insert(T element);

	public abstract T getAtIndex(int i);

	public abstract T get(T element);

	public abstract boolean contains(T element);
}