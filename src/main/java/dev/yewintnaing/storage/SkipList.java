package dev.yewintnaing.storage;

import java.util.Random;

/**
 * A standalone Skip List implementation for Sorted Sets.
 * 
 * TODO: Implement the 'insert' and 'delete' methods.
 */
public class SkipList {
    private static final int MAX_LEVEL = 32;
    private static final double P = 0.25;
    private final Random random = new Random();

    private final Node header;
    private int levelCount;
    private int size;

    public static class Node {
        public final String member;
        public final double score;
        public final Node[] forward;

        public Node(String member, double score, int level) {
            this.member = member;
            this.score = score;
            this.forward = new Node[level + 1];
        }
    }

    public SkipList() {
        // Header node has no member/score and always has MAX_LEVEL pointers
        this.header = new Node(null, 0, MAX_LEVEL);
        this.levelCount = 1;
        this.size = 0;
    }

    /**
     * Determines the level of a new node using a coin flip logic.
     * P = 0.25 means a 25% chance to increase level.
     */
    private int randomLevel() {
        int lvl = 0;
        while (random.nextDouble() < P && lvl < MAX_LEVEL - 1) {
            lvl++;
        }
        return lvl;
    }

    /**
     * Search for a member by its score and name.
     * Returns the node if found, or null otherwise.
     * 
     * This serves as a reference for how to navigate the levels.
     */
    public Node search(double score, String member) {
        Node current = header;

        // Start from the top level and drop down
        for (int i = levelCount - 1; i >= 0; i--) {
            while (current.forward[i] != null &&
                    (current.forward[i].score < score ||
                            (current.forward[i].score == score && current.forward[i].member.compareTo(member) < 0))) {
                current = current.forward[i];
            }
        }

        current = current.forward[0];

        if (current != null && current.score == score && current.member.equals(member)) {
            return current;
        }
        return null;
    }

    /**
     * Inserts a new member with a given score.
     * 
     * TODO: Implement the insertion logic.
     * Steps:
     * 1. Create an 'update' array of size MAX_LEVEL.
     * 2. Search for the insertion point, saving the last node at each level in
     * 'update'.
     * 3. Generate a random level for the new node.
     * 4. Update levelCount if the new level is higher.
     * 5. Create the new node and link it into each level using the 'update' array.
     */
    public void insert(String member, double score) {
        Node[] update = new Node[MAX_LEVEL];
        Node current = header;

        // 1. Search from the highest active level down to 0
        for (int i = levelCount - 1; i >= 0; i--) {
            while (current.forward[i] != null &&
                    (current.forward[i].score < score ||
                            (current.forward[i].score == score && current.forward[i].member.compareTo(member) < 0))) {
                current = current.forward[i];
            }
            update[i] = current;
        }

        int randomLevel = randomLevel();

        // 2. If new node is taller than current list, update levels
        if (randomLevel >= levelCount) {
            for (int i = levelCount; i <= randomLevel; i++) {
                update[i] = header; // New levels start from the header
            }
            levelCount = randomLevel + 1;
        }

        Node newNode = new Node(member, score, randomLevel);

        // 3. Link the new node into every level it belongs to
        for (int i = 0; i <= randomLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }

        size++;
    }

    /**
     * Deletes a member with a given score.
     * 
     * TODO: Implement the deletion logic.
     * Steps:
     * 1. Create an 'update' array.
     * 2. Search for the node, saving the path in 'update'.
     * 3. If the node is found, update the 'forward' pointers of 'update' nodes to
     * skip it.
     * 4. Update levelCount if the highest level is now empty.
     */
    public void delete(String member, double score) {
        // TODO: Implement me!
        Node[] update = new Node[MAX_LEVEL];
        Node current = header;

        // 1. Search for the node to delete, saving the path in 'update'
        for (int i = levelCount - 1; i >= 0; i--) {
            while (current.forward[i] != null &&
                    (current.forward[i].score < score ||
                            (current.forward[i].score == score && current.forward[i].member.compareTo(member) < 0))) {
                current = current.forward[i];
            }
            update[i] = current;
        }

        // 2. Check if the node was actually found
        Node nodeToDelete = current.forward[0];
        if (nodeToDelete == null ||
                nodeToDelete.score != score ||
                !nodeToDelete.member.equals(member)) {
            return; // Not found, nothing to delete
        }

        // 3. Unlink the node at every level it appears in
        for (int i = 0; i <= levelCount - 1; i++) {
            // If the next node at this level is the one we want to delete
            if (update[i].forward[i] == nodeToDelete) {
                // Bypass it
                update[i].forward[i] = nodeToDelete.forward[i];
            }
        }

        // 4. Update levelCount if the highest level is now empty
        while (levelCount > 1 && header.forward[levelCount - 1] == null) {
            levelCount--;
        }

        size--;
    }

    public int size() {
        return size;
    }

    // Helper for testing
    public int getLevelCount() {
        return levelCount;
    }
}
