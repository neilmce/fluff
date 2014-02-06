package org.neil.fluff.javacorners;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

public class Sorting
{
    @Test public void createListSortItAndMutateAnItem()
    {
        // Create a  sequence of items.
        List<Item> items = Arrays.asList(new Item[] { new Item(1), new Item(3), new Item(2) });
        
        // Sort the sequence by its natural order
        Collections.sort(items);
        System.out.println(items);
        
        // Mutate one of the value items.
        items.get(1).setNumber(42);
        System.out.println(items);
        
        // Perhaps it's obvious, the sequence does not retain its order. It is now unordered.
    }
        
    @Test public void createSortedSetAndMutateAnItem()
    {
        // Now a SortedSet
        SortedSet<Item> set = new TreeSet<>();
        
        // Add some items, which will be sorted based on their natural ordering.
        Item three = new Item(3);
        set.add(new Item(1));
        set.add(new Item(2));
        set.add(three);
        
        System.out.println(set);
        
        // Now mutate one of the values.
        three.setNumber(-2);
        System.out.println(set);
        
        // Note that this SortedSet is now not sorted.
        
        // Will it resort if I add a 4th item? (no)
        set.add(new Item(4));
        System.out.println(set);
    }
}
