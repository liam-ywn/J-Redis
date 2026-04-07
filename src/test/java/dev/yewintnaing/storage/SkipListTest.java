package dev.yewintnaing.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkipListTest {

    @Test
    void testBasicInsertionAndSearch() {
        SkipList sl = new SkipList();
        
        // These will fail until insert is implemented
        sl.insert("a", 1.0);
        sl.insert("b", 2.0);
        sl.insert("c", 1.5);

        assertNotNull(sl.search(1.0, "a"), "Should find node 'a'");
        assertNotNull(sl.search(2.0, "b"), "Should find node 'b'");
        assertNotNull(sl.search(1.5, "c"), "Should find node 'c'");
        assertNull(sl.search(3.0, "d"), "Should not find non-existent node");
    }

    @Test
    void testTieBreaking() {
        SkipList sl = new SkipList();
        
        // Members with the same score should be sorted alphabetically
        sl.insert("z", 10.0);
        sl.insert("a", 10.0);
        sl.insert("m", 10.0);

        SkipList.Node node = sl.search(10.0, "a");
        assertNotNull(node);
        assertEquals("a", node.member);
        
        // Check the order at level 0
        // (Assuming you've linked the nodes correctly)
        // If your head.forward[0] = "a", then "a".forward[0] should be "m"...
    }

    @Test
    void testDeletion() {
        SkipList sl = new SkipList();
        sl.insert("x", 100.0);
        assertNotNull(sl.search(100.0, "x"));
        
        sl.delete("x", 100.0);
        assertNull(sl.search(100.0, "x"), "Node should be gone after deletion");
    }
}
