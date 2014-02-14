package org.neil.fluff.collections;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a naive in-memory data store for objects with support for very basic querying and some very basic
 * support for uniqueness constraints.
 * 
 * @param <T> the type of entries in this collection.
 * 
 * @author Neil Mc Erlean
 */
public class IndexedCollection<T extends Serializable>
                              implements Collection<T>, Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Using a List to store the entries allows us to maintain insertion order.
     * These are the raw data of this collection.
     */
    private List<T> entries = new ArrayList<>();
    
    /**
     * These sorted views are lists of the same object references that are stored in
     * {@link #entries} but each one will be sorted based on the natural order of a field
     * defined on the class {@code <T>} in order to support efficient retrieval.
     */
    private Map<IndexedField<T>, List<T>> sortedViews = new HashMap<>();
    
    /**
     * This map holds the set of {@link IndexedField} objects, which define the class
     * fields which are to be 'indexed' and those fields whose values must be unique.
     */
    private Map<String, IndexedField<T>> indexedFields;
    
    /**
     * This class represents the name of a field declared in {@code <T>} along with an indication
     * of whether that field should have a unique value within this collection.
     */
    public static class IndexedField<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String  fieldNameToIndex;
        private final boolean isValueUnique;
        
        public IndexedField(String fieldName, boolean isValueUnique) {
            this.fieldNameToIndex = fieldName;
            this.isValueUnique = isValueUnique;
        }
        
        public String  getFieldName()  { return this.fieldNameToIndex; }
        public boolean isValueUnique() { return this.isValueUnique; }
    }
    
    /** Constructs a new, empty collection configured with the provided index configurations. */
    @SafeVarargs
    public IndexedCollection(IndexedField<T>... indexedFields) {
        // Use a LinkedHashMap to retain insertion order and also to allow later retrieval by field name.
        this.indexedFields = new LinkedHashMap<String, IndexedField<T>>();
        
        for (IndexedField<T> indexedField : indexedFields) {
            // Store the indexers by field name, which is by definition unique for a single class.
            this.indexedFields.put(indexedField.getFieldName(), indexedField);
            
            // Create a List in which we'll store a sorted view of the entries in this collection.
            this.sortedViews.put(indexedField, new ArrayList<T>());
        }
    }
    
    /**
     * This method queries for a single entry object having a field with the specified name and value.
     * If there are more than one matching entries, it is not defined which will be returned.
     * 
     * @param fieldName the Java field name to look for.
     * @param fieldValue the value that that Java field must have.
     * @return the 'first' matching entry if there is one, else {@code null}.
     * @throws IllegalArgumentException if the specified fieldName is not configured for indexing.
     */
    public T findOne(String fieldName, @SuppressWarnings("rawtypes") Comparable fieldValue) {
        List<T> list = this.find(fieldName, fieldValue);
        
        return list.isEmpty() ? null : list.get(0);
    }
    
    /**
     * This method queries for all entry objects having a field with the specified name and value.
     * 
     * @param fieldName the Java field name to look for.
     * @param fieldValue the value that that Java field must have.
     * @return A list of all those entries having the specified field and value.
     * @throws IllegalArgumentException if the specified fieldName is not indexed.
     */
    public List<T> find(String fieldName, @SuppressWarnings("rawtypes") Comparable fieldValue) {
        IndexedField<T> indexer = this.indexedFields.get(fieldName);
        
        if (indexer == null) { throw new IllegalArgumentException("No indexer defined for a field called " + fieldName); }
        
        List<T> sortedView = sortedViews.get(indexer);
        
        int indexOfMatchingObject = binarySearch(sortedView, fieldName, fieldValue);
        return indexOfMatchingObject == -1 ? Collections.<T>emptyList() :
                                             expandToList(sortedView, fieldName, indexOfMatchingObject);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int binarySearch(List<T> sortedView, String fieldName, Comparable requiredFieldValue) {
        if (sortedView.isEmpty())        { return -1; }
        
        int lowLimit = 0;
        int highLimit = sortedView.size() - 1;
        while (lowLimit <= highLimit) {
            int middle = (lowLimit + highLimit) / 2;
            // Are we above or below or at our result?
            final T objectAtMiddle = sortedView.get(middle);
            Comparable fieldValueAtMiddle = getFieldValue(fieldName, objectAtMiddle);
            
            if (fieldValueAtMiddle.compareTo(requiredFieldValue) < 0) {
                lowLimit = middle + 1;
            }
            else if (fieldValueAtMiddle.compareTo(requiredFieldValue) > 0) {
                highLimit = middle - 1;
            }
            else {
                return middle;
            }
        }
        // Not found.
        return -1;
    }
    
    @SuppressWarnings({ "rawtypes" })
    private List<T> expandToList(List<T> sortedView, String fieldName, int indexOfMatch) {
        int indexOfFirstMatch = indexOfMatch;
        int indexOfLastMatch = indexOfMatch;
        final Comparable valueToMatch = getFieldValue(fieldName, sortedView.get(indexOfMatch));
        
        while (indexOfFirstMatch > 0) {
            T previousEntry = sortedView.get(indexOfFirstMatch - 1);
            final Comparable previousFieldValue = getFieldValue(fieldName, previousEntry);
            if (previousFieldValue.equals(valueToMatch)) {
                indexOfFirstMatch = indexOfFirstMatch - 1;
            }
            else {
                break;
            }
        }
        while (indexOfLastMatch < sortedView.size() - 1) {
            T nextEntry = sortedView.get(indexOfLastMatch + 1);
            if (getFieldValue(fieldName, nextEntry).equals(valueToMatch)) {
                indexOfLastMatch = indexOfLastMatch + 1;
            }
            else {
                break;
            }
        }
        return sortedView.subList(indexOfFirstMatch, indexOfLastMatch + 1);
    }
    
    /** {@inheritDoc} */
    @Override public int size()                 { return entries.size(); }
    
    /** {@inheritDoc} */
    @Override public boolean isEmpty()          { return entries.isEmpty(); }
    
    /** {@inheritDoc} */
    @Override public boolean contains(Object o) { return entries.contains(o); }
    
    /** {@inheritDoc} */
    @Override public Iterator<T> iterator()     { return entries.iterator(); }
    
    /** Get an iterator over the values in this collection, sorted by the values of the specified field name.
     *
     * @param fieldName the name of the field in the Java object {@code <T>} by whose values the sort is required.
     * @return a sorted set of collection entries.
     * @throws IllegalArgumentException if the specified fieldName is not indexed.
     */
    public Iterator<T> iteratorSortedBy(String fieldName) {
        final IndexedField<T> indexedField = indexedFields.get(fieldName);
        if (indexedField == null) { throw new IllegalArgumentException("Cannot get an iterator sorted by '" +
                                                                       fieldName + "' as it is not indexed."); }
        
        final List<T> sortedList = sortedViews.get(indexedField);
        return sortedList.iterator();
    }
    
    /** {@inheritDoc} */
    @Override public Object[] toArray()         { return entries.toArray(); }
    
    /** {@inheritDoc} */
    @Override public <AT> AT[] toArray(AT[] a)  { return entries.toArray(a); }
    
    /** {@inheritDoc} */
    @Override public boolean add(T item) {
        validateEntry(item);
        
        // Add the item and update the indexes.
        this.entries.add(item);
        reindex();
        
        // We always accept the item and therefore always change the collection.
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private void reindex() {
        for (IndexedField<T> indexField : indexedFields.values()) {
            final String fieldName = indexField.getFieldName();
            
            // The easiest way to make this work is to clear the list every time. TODO Better alternative?
            final List<T> view = sortedViews.get(indexField);
            view.clear();
            
            // Add every entry to the list
            for (T entry : entries) {
                view.add(entry);
            }
            
            // and sort the view by the appropriate algorithm
            Collections.sort(view, new FieldBasedComparator(fieldName));
        }
    }
    
    /**
     * This method ensures that the configured indexers can all be applied to the provided object.
     * 
     * @throws IllegalArgumentException if any indexer cannot be applied.
     * @throws IllegalArgumentException if this item violates any uniqueness constraint.
     * @throws ClassCastException if any index operates on a field which does not implement Comparable.
     */
    private void validateEntry(T item) {
        Class<?> itemClass = item.getClass();
        
        for (IndexedField<T> indexedField : indexedFields.values()) {
            // What Java field does this index use?
            final String fieldName = indexedField.getFieldName();
            
            // What is the type of that field?
            final Field field = getDeclaredField(itemClass, fieldName);
            
            final Class<?> fieldClass = field.getType();
            
            // The indexers use a sorted list view of the entries to provide efficient searching.
            // Therefore all indexed fields in the entries must be Comparable.
            // TODO Could add support for Comparators.
            if ( !Comparable.class.isAssignableFrom(fieldClass)) {
                throw new ClassCastException("Field " + fieldName + " does not implement Comparable.");
            }
            
            if (indexedField.isValueUnique) {
                // Does the collection already contain any entry with a matching value for this field?
                if (findOne(fieldName, getFieldValue(fieldName, item)) != null)
                {
                    throw new IllegalArgumentException("Cannot add element " + item + " as there is already an entry with " +
                                                       fieldName + " = " + getFieldValue(fieldName, item));
                }
            }
        }
    }
    
    private Field getDeclaredField(Class<?> itemClass, final String fieldName) {
        final Field field;
        try {
            field = itemClass.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Entry of type " + itemClass.getSimpleName() +
                                               " does not have declared field named " + fieldName, e);
        } catch (SecurityException e)
        {
            throw new IllegalArgumentException("Error accessing fields of class", e);
        }
        return field;
    }
    
    /** {@inheritDoc} */
    @Override public boolean remove(Object o) {
        boolean result = this.entries.remove(o);
        reindex();
        return result;
    }
    
    /** {@inheritDoc} */
    @Override public boolean containsAll(Collection<?> items) {
        for (Object item : items) {
            if ( !this.contains(item)) {
                return false;
            }
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override public boolean addAll(Collection<? extends T> c) {
        for (T entry : c) {
            // TODO This will lead to a reindexing on every add(). It would be better to prevent that.
            add(entry);
        }
        return !c.isEmpty();
    }
    
    /** {@inheritDoc} */
    @Override public boolean retainAll(Collection<?> c) {
        boolean result = this.entries.retainAll(c);
        reindex();
        return result;
    }
    
    /** {@inheritDoc} */
    @Override public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object entry : c) {
            if (remove(entry)) { result = true; };
        }
        return result;
    }
    
    /** {@inheritDoc} */
    @Override public void clear() {
        this.entries.clear();
        
        // Let's retain the Maps for each of the indexers, but just clear out the data in the sorted views.
        for (Map.Entry<IndexedField<T>, List<T>> sortedView : sortedViews.entrySet()) {
            sortedView.getValue().clear();
        }
    }
    
    @SuppressWarnings("rawtypes")
    private Comparable getFieldValue(String fieldName, Object object) {
        final Field declaredField = getDeclaredField(object.getClass(), fieldName);
        declaredField.setAccessible(true);
        
        Object fieldValue;
        try
        {
            fieldValue = declaredField.get(object);
        } catch (IllegalAccessException e)
        {
            throw new IllegalArgumentException("Illegal access to field.", e);
        }
        
        return (Comparable) fieldValue;
    }
    
    @SuppressWarnings("rawtypes")
    private class FieldBasedComparator implements Comparator {
        private final String fieldName;
        
        public FieldBasedComparator(String fieldName) { this.fieldName = fieldName; }
        
        @SuppressWarnings("unchecked")
        @Override public int compare(Object o1, Object o2) {
            if (o1 == null || o2 == null) { throw new NullPointerException("Cannot compare a null value."); }
            
            if ( !o1.getClass().equals(o2.getClass())) { throw new ClassCastException("Cannot compare two objects of different classes."); }
            
            // Now to get the field values and compare them...
            final Comparable fieldValue1 = getFieldValue(fieldName, o1);
            final Comparable fieldValue2 = getFieldValue(fieldName, o2);
            
            // Special handling for null-valued fields.
            if (fieldValue1 == null && fieldValue2 == null) {
                return 0;
            }
            else if (fieldValue1 == null) {
                return -1;
            }
            else if (fieldValue2 == null) {
                return 1;
            }
            else {
                return fieldValue1.compareTo(fieldValue2);
            }
        }
    }
}
