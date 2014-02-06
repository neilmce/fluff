package org.neil.fluff.javacorners;

/** A simple mutable value type for use in testing some corners of Java. */
public class Item implements Comparable<Item> {
    private int number;
    
    public Item(int number)             { this.number = number; }
    
    public void setNumber(int newValue) { this.number = newValue; }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Item) {
            Item that = (Item)o;
            return this.number == that.number;
        }
        else {
            return false;
        }
    }
    
    @Override public int hashCode()        { return Integer.valueOf(number).hashCode(); }
    
    @Override public int compareTo(Item o) { return this.number - o.number; }
    
    @Override public String toString()     { return Integer.toString(this.number); }
}