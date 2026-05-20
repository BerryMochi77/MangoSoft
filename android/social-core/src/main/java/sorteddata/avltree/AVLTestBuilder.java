package sorteddata.avltree;

import sorteddata.SortedDataSubject;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides a way to make AVL nodes directly without relying upon
 * the insert function of AVLTree. This is useful for unit testing, because
 * otherwise bugs within the insert method could cause errors in our test cases.
 * Moreover, if you didn't implement insert in the appropriate lab, you wouldn't
 * be able to write unit tests otherwise.
 * <p>
 * Of course, because this class may break the contract of AVLTree (because it
 * does not automatically re-balance) it should not be used outside tests. To do so,
 * we would generally put it within our test code so that it cannot be accessed from
 * a production build. Unfortunately, because we need to mark your test cases, we can't
 * do that... so please just pretend that this is a test file!
 * <p>
 * It's also worth noting that this class is a good example of the Builder design pattern.
 */
public class AVLTestBuilder<T> {
	private final Comparator<T> comparator;
	private final AVLTree<T> tree;

	public AVLTestBuilder(Comparator<T> comparator) {
		this.comparator = comparator;
		this.tree = new AVLTree<>(comparator);
	}

	public AVLNode<T> makeWithCast(T value, Object left, Object right) {
		return make(value, (AVLNode<T>) left, (AVLNode<T>) right);
	}

	public AVLNode<T> make(T value, AVLNode<T> left, AVLNode<T> right) {
		return new AVLNodeFilled<T>(comparator, value, left, right);
	}

	public AVLNode<T> make(T value) {
		return new AVLNodeFilled<T>(comparator, value, empty(), empty());
	}

	public AVLNode<T> empty() {
		return new AVLNodeEmpty<>(comparator);
	}

	public void setTreeRoot(AVLNode<T> root) {
		try {
			// Here, we use an advanced Java technique called reflection
			// to overwrite a private field (root) within the AVLTree class
			// Reflection is a code smell in that it should typically be avoided,
			// but it can be extremely useful for writing test code.
			Field rootField = tree.getClass().getDeclaredField("root");
			rootField.setAccessible(true);
			rootField.set(tree, root);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTreeRootWithCast(Object root) {
		setTreeRoot((AVLNode<T>) root);
	}

	public AVLTree<T> getTree() {
		return tree;
	}
}
