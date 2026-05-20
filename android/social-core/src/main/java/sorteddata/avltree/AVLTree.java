package sorteddata.avltree;

import sorteddata.SortedData;
import sorteddata.SortedDataSubject;

import java.util.*;

public class AVLTree<T> extends SortedData<T> {
	private static final Random random = new Random();
	private final Comparator<T> comparator;
	private AVLNode<T> root;

	public AVLTree(Comparator<T> comparator) {
		this(comparator, new AVLNodeEmpty<T>(comparator));
	}

	private AVLTree(Comparator<T> comparator, AVLNode<T> root) {
		this.comparator = comparator;
		this.root = root;
	}

	public AVLTree<T> clone() {
		return new AVLTree<>(comparator, root);
	}

	public boolean insert(T element) {
		if (root.contains(element)) return false;
		root = root.insert(element);
		return true;
	}

	public T get(T value) {
		return root.get(value);
	}

	public String toString() {
		return "AVLTree[%s]".formatted(root.toString());
	}

	public T getRandom() {
		if (root.size() == 0) return null;
		return root.getAtIndex(random.nextInt(root.size()));
	}

	public T getAtIndex(int i) {
		if (root instanceof AVLNodeEmpty<T>) return null;
		return root.getAtIndex(i);
	}

	public Iterator<T> getRange(T start, int count, boolean backwards) {
		return new AVLIterator<>(start, root, comparator, count, backwards);
	}
}
