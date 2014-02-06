package org.neil.fluff.javacorners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class Hashing
{
    @Test public void putItemInMapChangeHashValueRecoverItem()
    {
        // Create a normal HashMap
        Map<Item, Item> map = new HashMap<>();
        
        // Create some mutable value types to put in the HashMap.
        final Item _42 = new Item(42);
        final Item _3142 = new Item(3142);
        
        map.put(_42, _3142);
        
        // Assert that we can get the value we just put in by its key.
        assertEquals(_3142, map.get(_42));
        
        // Now change the hash value for the key we just put in.
        _42.setNumber(2818);
        
        // Note that the item is not recoverable with its old key
        assertNull(map.get(new Item(42)));
        
        // Note that the item is not recoverable with an object that looks like its new key.
        assertNull(map.get(new Item(2818)));
        
        // So it's gone!
    }
}
