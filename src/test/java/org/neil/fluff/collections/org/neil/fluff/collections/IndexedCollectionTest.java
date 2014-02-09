package org.neil.fluff.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.neil.fluff.collections.IndexedCollection.IndexedField;

/**
 * Tests for {@link IndexedCollection}.
 * 
 * @author Neil Mc Erlean
 */
public class IndexedCollectionTest {
    private static final List<Item> ITEMS_TO_SEARCH_IN = Arrays.asList(new Item[] {
                                                                           new Item(-2, ""),
                                                                           new Item(0,  null),
                                                                           new Item(1,  "a"),
                                                                           new Item(2,  "a"),
                                                                           new Item(3,  "a"),
                                                                           new Item(4,  "b"),
                                                                           new Item(5,  "d"),
                                                                           new Item(5,  "d"),
                                                                           new Item(9,  "z")
                                                                           });
    private static final IndexedCollection<Item> NO_INDEXES_COLLECTION = new IndexedCollection<Item>();
    
    private static final IndexedField<Item> integerIndexedField = new IndexedField<Item>("integer", false);
    private static final IndexedField<Item> stringIndexedField = new IndexedField<Item>("string", false);
    
    @Test public void addingAndFindingSingleEntries() {
        IndexedCollection<Item> ic = new IndexedCollection<Item>(integerIndexedField, stringIndexedField);
        List<Item> shuffledCollection = new ArrayList<>(ITEMS_TO_SEARCH_IN);
        Collections.shuffle(shuffledCollection);
        ic.addAll(shuffledCollection);
        
        assertEquals(new Item(9, "z"), ic.findOne("integer", 9));
        assertEquals(new Item(4, "b"), ic.findOne("string", "b"));
    }
    
    @Test public void addingAndFindingMultipleEntries() {
        IndexedCollection<Item> ic = new IndexedCollection<Item>(integerIndexedField, stringIndexedField);
        List<Item> shuffledCollection = new ArrayList<>(ITEMS_TO_SEARCH_IN);
        Collections.shuffle(shuffledCollection);
        ic.addAll(shuffledCollection);
        
        assertEquals(Arrays.asList(new Item[] { new Item(5, "d"), new Item(5, "d") }), ic.find("integer", 5));
        assertEquals(Arrays.asList(new Item[] { new Item(5, "d"), new Item(5, "d") }), ic.find("string", "d"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void cannotInsertValueTwiceViolatingUniquenessConstraint() {
        IndexedCollection<Item> ic = new IndexedCollection<Item>(new IndexedField<Item>("integer", true));
        final Item item1 = new Item(100, "hello");
        final Item item2 = new Item(100, "goodbye");
        ic.add(item1);
        
        ic.add(item2);
    }
    
    @Test public void aCollectionWithNoIndexesIsWastefulButStillWorks() {
        final Item item = new Item(100, "hello");
        assertTrue(NO_INDEXES_COLLECTION.add(item));
        assertTrue(NO_INDEXES_COLLECTION.contains(item));
        
        assertEquals(1, NO_INDEXES_COLLECTION.size());
    }
    
    @Test(expected=IllegalArgumentException.class) public void findingEntriesByUnconfiguredIndex() {
        NO_INDEXES_COLLECTION.findOne("integer", 42);
     }
    
    @Test(expected=IllegalArgumentException.class) public void findingEntriesByNonexistentField() {
        NO_INDEXES_COLLECTION.findOne("noSuchField", 42);
     }
}

/** A simple mutable POJO to test the Map. */
class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer integer;
    private String  string;
    
    public Item(Integer i, String s) {
        this.integer = i;
        this.string  = s;
    }
    
    public Integer getInteger()                { return integer; }
    public void    setInteger(Integer integer) { this.integer = integer; }
    
    public String getString()              { return string; }
    public void   setString(String string) { this.string = string; }
    
    @Override public boolean equals(Object thatObject) {
        boolean result = false;
        
        if (thatObject instanceof Item) {
            Item that = (Item) thatObject;
            result = this.integer == that.integer && this.string.equals(that.string);
        }
        return result;
    }
    
    @Override public int hashCode() { return integer.hashCode() + 7 * string.hashCode(); }
    
    @Override public String toString() { return this.integer + ":" + this.string; }
}