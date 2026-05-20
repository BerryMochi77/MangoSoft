package sorteddata.avltree;

import java.util.Comparator;

class AVLNodeEmpty<T> extends AVLNode<T> {
	public AVLNodeEmpty(Comparator<T> comparator) {
		super(comparator);
	}

	public int height() {
		return 0;
	}

	public int balanceFactor() {
		return 0;
	}

	public int size() {
		return 0;
	}

	public AVLNodeFilled<T> insert(T element) {
		return new AVLNodeFilled<>(comparator, element, this, this);
	}

	public String toString() {
		return ".";
	}

	public T getAtIndex(int i) {
		return null;
	}

	public boolean contains(T element) {
		return false;
	}

	public T get(T element) {
		return null;
	}
}
