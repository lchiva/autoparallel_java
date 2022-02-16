/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psc.java.autoparallel.android;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/**
 * ArrayUtils contains some methods that you can call to find out
 * the most efficient increments by which to grow arrays.
 */
public class ArrayUtils {
    private static final int CACHE_SIZE = 73;
    private static Object[] sCache = new Object[CACHE_SIZE];
    /**
     * Adds value to given array if not already present, providing set-like
     * behavior.
     */
    @SuppressWarnings("unchecked")
    public static  <T> T[] appendElement(Class<T> kind,  T[] array, T element) {
        final T[] result;
        final int end;
        if (array != null) {
            if (contains(array, element)) return array;
            end = array.length;
            result = (T[])Array.newInstance(kind, end + 1);
            System.arraycopy(array, 0, result, 0, end);
        } else {
            end = 0;
            result = (T[])Array.newInstance(kind, 1);
        }
        result[end] = element;
        return result;
    }
    /**
     * Adds value to given array if not already present, providing set-like
     * behavior.
     */
    public static  int[] appendInt( int[] cur, int val) {
        if (cur == null) {
            return new int[] { val };
        }
        final int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                return cur;
            }
        }
        int[] ret = new int[N + 1];
        System.arraycopy(cur, 0, ret, 0, N);
        ret[N] = val;
        return ret;
    }
    /**
     * Adds value to given array if not already present, providing set-like
     * behavior.
     */
    public static  long[] appendLong( long[] cur, long val) {
        if (cur == null) {
            return new long[] { val };
        }
        final int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                return cur;
            }
        }
        long[] ret = new long[N + 1];
        System.arraycopy(cur, 0, ret, 0, N);
        ret[N] = val;
        return ret;
    }
    public static  long[] cloneOrNull( long[] array) {
        return (array != null) ? array.clone() : null;
    }
    public static <T> boolean contains( Collection<T> cur, T val) {
        return (cur != null) ? cur.contains(val) : false;
    }
    public static boolean contains( int[] array, int value) {
        if (array == null) return false;
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }
    public static boolean contains( long[] array, long value) {
        if (array == null) return false;
        for (long element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }
    /**
     * Checks that value is present as at least one of the elements of the array.
     * @param array the array to check in
     * @param value the value to check for
     * @return true if the value is present in the array
     */
    public static <T> boolean contains( T[] array, T value) {
        return indexOf(array, value) != -1;
    }
    /**
     * Test if all {@code check} items are contained in {@code array}.
     */
    public static <T> boolean containsAll( T[] array, T[] check) {
        if (check == null) return true;
        for (T checkItem : check) {
            if (!contains(array, checkItem)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Test if any {@code check} items are contained in {@code array}.
     */
    public static <T> boolean containsAny( T[] array, T[] check) {
        if (check == null) return false;
        for (T checkItem : check) {
            if (contains(array, checkItem)) {
                return true;
            }
        }
        return false;
    }
    public static int[] convertToIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    /**
     * Returns an empty array of the specified type.  The intent is that
     * it will return the same empty array every time to avoid reallocation,
     * although this is not guaranteed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] emptyArray(Class<T> kind) {
        if (kind == Object.class) {
            return (T[]) new Object[0];
        }
        int bucket = (kind.hashCode() & 0x7FFFFFFF) % CACHE_SIZE;
        Object cache = sCache[bucket];
        if (cache == null || cache.getClass().getComponentType() != kind) {
            cache = Array.newInstance(kind, 0);
            sCache[bucket] = cache;
            // Log.e("cache", "new empty " + kind.getName() + " at " + bucket);
        }
        return (T[]) cache;
    }
    /**
     * Checks if the beginnings of two byte arrays are equal.
     *
     * @param array1 the first byte array
     * @param array2 the second byte array
     * @param length the number of bytes to check
     * @return true if they're equal, false otherwise
     */
    public static boolean equals(byte[] array1, byte[] array2, int length) {
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length < length || array2.length < length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }
    /**
     * Return first index of {@code value} in {@code array}, or {@code -1} if
     * not found.
     */
    public static <T> int indexOf( T[] array, T value) {
        if (array == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) return i;
        }
        return -1;
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static boolean isEmpty( boolean[] array) {
        return array == null || array.length == 0;
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static boolean isEmpty( byte[] array) {
        return array == null || array.length == 0;
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static boolean isEmpty( Collection<?> array) {
        return array == null || array.isEmpty();
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static boolean isEmpty( int[] array) {
        return array == null || array.length == 0;
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static boolean isEmpty( long[] array) {
        return array == null || array.length == 0;
    }
    /**
     * Checks if given array is null or has zero elements.
     */
    public static <T> boolean isEmpty( T[] array) {
        return array == null || array.length == 0;
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] newUnpaddedArray(Class<T> clazz, int minLen) {
        return (T[]) new Object[minLen];
    }
    public static boolean[] newUnpaddedBooleanArray(int minLen) {
        return new boolean[minLen];
    }
    public static byte[] newUnpaddedByteArray(int minLen) {
        return new byte[minLen];
    }
    public static char[] newUnpaddedCharArray(int minLen) {
        return new char [minLen];
    }
    public static float[] newUnpaddedFloatArray(int minLen) {
        return new float[minLen];
    }
    public static int[] newUnpaddedIntArray(int minLen) {
        return new int[minLen];
    }
    public static long[] newUnpaddedLongArray(int minLen) {
        return new long[minLen];
    }
    public static Object[] newUnpaddedObjectArray(int minLen) {
    	return new Object[minLen];
    }
    /**
     * Returns true if the two ArrayLists are equal with respect to the objects they contain.
     * The objects must be in the same order and be reference equal (== not .equals()).
     */
    public static <T> boolean referenceEquals(ArrayList<T> a, ArrayList<T> b) {
        if (a == b) {
            return true;
        }
        final int sizeA = a.size();
        final int sizeB = b.size();
        if (a == null || b == null || sizeA != sizeB) {
            return false;
        }
        boolean diff = false;
        for (int i = 0; i < sizeA && !diff; i++) {
            diff |= a.get(i) != b.get(i);
        }
        return !diff;
    }
    public static  <T> ArrayList<T> remove( ArrayList<T> cur, T val) {
        if (cur == null) {
            return null;
        }
        cur.remove(val);
        if (cur.isEmpty()) {
            return null;
        } else {
            return cur;
        }
    }
    /**
     * Removes value from given array if present, providing set-like behavior.
     */
    @SuppressWarnings("unchecked")
    public static  <T> T[] removeElement(Class<T> kind,  T[] array, T element) {
        if (array != null) {
            if (!contains(array, element)) return array;
            final int length = array.length;
            for (int i = 0; i < length; i++) {
                if (Objects.equals(array[i], element)) {
                    if (length == 1) {
                        return null;
                    }
                    T[] result = (T[])Array.newInstance(kind, length - 1);
                    System.arraycopy(array, 0, result, 0, i);
                    System.arraycopy(array, i + 1, result, i, length - i - 1);
                    return result;
                }
            }
        }
        return array;
    }
    /**
     * Removes value from given array if present, providing set-like behavior.
     */
    public static  int[] removeInt( int[] cur, int val) {
        if (cur == null) {
            return null;
        }
        final int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                int[] ret = new int[N - 1];
                if (i > 0) {
                    System.arraycopy(cur, 0, ret, 0, i);
                }
                if (i < (N - 1)) {
                    System.arraycopy(cur, i + 1, ret, i, N - i - 1);
                }
                return ret;
            }
        }
        return cur;
    }
    /**
     * Removes value from given array if present, providing set-like behavior.
     */
    public static  long[] removeLong( long[] cur, long val) {
        if (cur == null) {
            return null;
        }
        final int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                long[] ret = new long[N - 1];
                if (i > 0) {
                    System.arraycopy(cur, 0, ret, 0, i);
                }
                if (i < (N - 1)) {
                    System.arraycopy(cur, i + 1, ret, i, N - i - 1);
                }
                return ret;
            }
        }
        return cur;
    }
 
    /**
     * Removes value from given array if present, providing set-like behavior.
     */
    public static  String[] removeString( String[] cur, String val) {
        if (cur == null) {
            return null;
        }
        final int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (Objects.equals(cur[i], val)) {
                String[] ret = new String[N - 1];
                if (i > 0) {
                    System.arraycopy(cur, 0, ret, 0, i);
                }
                if (i < (N - 1)) {
                    System.arraycopy(cur, i + 1, ret, i, N - i - 1);
                }
                return ret;
            }
        }
        return cur;
    }
    public static long total( long[] array) {
        long total = 0;
        if (array != null) {
            for (long value : array) {
                total += value;
            }
        }
        return total;
    }
    public static  <T> T[] trimToSize( T[] array, int size) {
        if (array == null || size == 0) {
            return null;
        } else if (array.length == size) {
            return array;
        } else {
            return Arrays.copyOf(array, size);
        }
    }
    /**
     * Removes elements that match the predicate in an efficient way that alters the order of
     * elements in the collection. This should only be used if order is not important.
     * @param collection The ArrayList from which to remove elements.
     * @param predicate The predicate that each element is tested against.
     * @return the number of elements removed.
     */
    public static <T> int unstableRemoveIf( ArrayList<T> collection,
                                            java.util.function.Predicate<T> predicate) {
        if (collection == null) {
            return 0;
        }
        final int size = collection.size();
        int leftIdx = 0;
        int rightIdx = size - 1;
        while (leftIdx <= rightIdx) {
            // Find the next element to remove moving left to right.
            while (leftIdx < size && !predicate.test(collection.get(leftIdx))) {
                leftIdx++;
            }
            // Find the next element to keep moving right to left.
            while (rightIdx > leftIdx && predicate.test(collection.get(rightIdx))) {
                rightIdx--;
            }
            if (leftIdx >= rightIdx) {
                // Done.
                break;
            }
            Collections.swap(collection, leftIdx, rightIdx);
            leftIdx++;
            rightIdx--;
        }
        // leftIdx is now at the end.
        for (int i = size - 1; i >= leftIdx; i--) {
            collection.remove(i);
        }
        return size - leftIdx;
    }
    private ArrayUtils() { /* cannot be instantiated */ }
}