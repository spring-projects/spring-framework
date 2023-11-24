/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.util;

abstract class SorterTemplate {
    private static final int MERGESORT_THRESHOLD = 12;
    private static final int QUICKSORT_THRESHOLD = 7;

    abstract protected void swap(int i, int j);
    abstract protected int compare(int i, int j);

    protected void quickSort(int lo, int hi) {
        quickSortHelper(lo, hi);
        insertionSort(lo, hi);
    }

    private void quickSortHelper(int lo, int hi) {
        for (;;) {
            int diff = hi - lo;
            if (diff <= QUICKSORT_THRESHOLD) {
                break;
            }
            int i = (hi + lo) / 2;
            if (compare(lo, i) > 0) {
                swap(lo, i);
            }
            if (compare(lo, hi) > 0) {
                swap(lo, hi);
            }
            if (compare(i, hi) > 0) {
                swap(i, hi);
            }
            int j = hi - 1;
            swap(i, j);
            i = lo;
            int v = j;
            for (;;) {
                while (compare(++i, v) < 0) {
                    /* nothing */
                }
                while (compare(--j, v) > 0) {
                    /* nothing */
                }
                if (j < i) {
                    break;
                }
                swap(i, j);
            }
            swap(i, hi - 1);
            if (j - lo <= hi - i + 1) {
                quickSortHelper(lo, j);
                lo = i + 1;
            } else {
                quickSortHelper(i + 1, hi);
                hi = j;
            }
        }
    }

    private void insertionSort(int lo, int hi) {
        for (int i = lo + 1 ; i <= hi; i++) {
            for (int j = i; j > lo; j--) {
                if (compare(j - 1, j) > 0) {
                    swap(j - 1, j);
                } else {
                    break;
                }
            }
        }
    }

    protected void mergeSort(int lo, int hi) {
        int diff = hi - lo;
        if (diff <= MERGESORT_THRESHOLD) {
            insertionSort(lo, hi);
            return;
        }
        int mid = lo + diff / 2;
        mergeSort(lo, mid);
        mergeSort(mid, hi);
        merge(lo, mid, hi, mid - lo, hi - mid);
    }

    private void merge(int lo, int pivot, int hi, int len1, int len2) {
        if (len1 == 0 || len2 == 0) {
            return;
        }
        if (len1 + len2 == 2) {
            if (compare(pivot, lo) < 0) {
                swap(pivot, lo);
            }
            return;
        }
        int first_cut, second_cut;
        int len11, len22;
        if (len1 > len2) {
            len11 = len1 / 2;
            first_cut = lo + len11;
            second_cut = lower(pivot, hi, first_cut);
            len22 = second_cut - pivot;
        } else {
            len22 = len2 / 2;
            second_cut = pivot + len22;
            first_cut = upper(lo, pivot, second_cut);
            len11 = first_cut - lo;
        }
        rotate(first_cut, pivot, second_cut);
        int new_mid = first_cut + len22;
        merge(lo, first_cut, new_mid, len11, len22);
        merge(new_mid, second_cut, hi, len1 - len11, len2 - len22);
    }

    private void rotate(int lo, int mid, int hi) {
        int lot = lo;
        int hit = mid - 1;
        while (lot < hit) {
            swap(lot++, hit--);
        }
        lot = mid; hit = hi - 1;
        while (lot < hit) {
            swap(lot++, hit--);
        }
        lot = lo; hit = hi - 1;
        while (lot < hit) {
            swap(lot++, hit--);
        }
    }

    private int lower(int lo, int hi, int val) {
        int len = hi - lo;
        while (len > 0) {
            int half = len / 2;
            int mid= lo + half;
            if (compare(mid, val) < 0) {
                lo = mid + 1;
                len = len - half -1;
            } else {
                len = half;
            }
        }
        return lo;
    }

    private int upper(int lo, int hi, int val) {
        int len = hi - lo;
        while (len > 0) {
            int half = len / 2;
            int mid = lo + half;
            if (compare(val, mid) < 0) {
                len = half;
            } else {
                lo = mid + 1;
                len = len - half -1;
            }
        }
        return lo;
    }
}
