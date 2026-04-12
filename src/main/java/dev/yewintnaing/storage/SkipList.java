package dev.yewintnaing.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A standalone Skip List implementation for Sorted Sets.
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

    public void delete(String member, double score) {
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

    public List<Node> range(int startInclusive, int stopInclusive) {
        if (startInclusive < 0 || stopInclusive < startInclusive || startInclusive >= size) {
            return List.of();
        }

        List<Node> result = new ArrayList<>();
        Node current = header.forward[0];
        int index = 0;

        while (current != null && index <= stopInclusive) {
            if (index >= startInclusive) {
                result.add(current);
            }
            current = current.forward[0];
            index++;
        }

        return result;
    }

    public int size() {
        return size;
    }

    // Helper for testing
    public int getLevelCount() {
        return levelCount;
    }
}
