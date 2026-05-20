package sorteddata.bstree;


import java.util.Comparator;

class BSNodeEmpty<T> extends BSNode<T> {
	public BSNodeEmpty(Comparator<T> comparator) {
		super(comparator);
	}

	public int height() {
		return 0;
	}

	public int size() {
		return 0;
	}

	public BSNodeFilled<T> insert(T element) {
		return new BSNodeFilled<>(comparator, element, this, this);
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
