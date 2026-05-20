package sorteddata.bstree;

import sorteddata.SortedData;

import java.util.*;

public class BSTree<T> extends SortedData<T> {
	private static final Random random = new Random();
	private final Comparator<T> comparator;
	private BSNode<T> root;

	public BSTree(Comparator<T> comparator) {
		this(comparator, new BSNodeEmpty<T>(comparator));
	}

	private BSTree(Comparator<T> comparator, BSNode<T> root) {
		this.comparator = comparator;
		this.root = root;
	}

	public BSTree<T> clone() {
		return new BSTree<>(comparator, root);
	}

	public boolean insert(T element) {
		if (root.contains(element)) return false;
		root = root.insert(element);
		return true;
	}

	public T get(T value) {
		return root.get(value);
	}

	public T getAtIndex(int i) {
		if (root instanceof BSNodeEmpty<T>) return null;
		return root.getAtIndex(i);
	}

	public String toString() {
		return "Tree[%s]".formatted(root.toString());
	}

	public T getRandom() {
		if (root.size() == 0) return null;
		return root.getAtIndex(random.nextInt(root.size()));
	}

	public Iterator<T> getRange(T start, int count, boolean backwards) {
		return null;
	}
}
