package com.weidi.media.wdplayer.util;

import java.util.Arrays;

public class MyTest {

    private static int[] sourceArray = new int[]{4, 7, 2, 5, 1, 8, 9, 6, 0, 3};

    public static void main(String[] args) {
        sort2(sourceArray);
    }

    /***
     冒泡
     算法步骤
     比较相邻的元素。如果第一个比第二个大，就交换他们两个。
     对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对。这步做完后，最后的元素会是最大的数。
     针对所有的元素重复以上的步骤，除了最后一个。
     持续每次对越来越少的元素重复上面的步骤，直到没有任何一对数字需要比较。

     10个数,循环8次
     */
    private static void sort1(int[] sourceArray) {
        int length = sourceArray.length;
        int[] array = Arrays.copyOf(sourceArray, length);
        printInfo(array);
        System.out.println("----------------------------------------before");
        for (int i = 0; i < length; ++i) {
            boolean flag = true;
            for (int j = 0; j < length - i - 1; ++j) {
                if (array[j] > array[j + 1]) {
                    int tmp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = tmp;
                    flag = false;
                }
            }
            if (flag) {
                break;
            }
            printInfo(array);
        }
        System.out.println("----------------------------------------after");
        printInfo(array);
    }

    /***
     选择
     算法步骤
     首先在未排序序列中找到最小（大）元素，存放到排序序列的起始位置。
     再从剩余未排序元素中继续寻找最小（大）元素，然后放到已排序序列的末尾。
     重复第二步，直到所有元素均排序完毕。

     10个数,循环9次
     总共10个数,每循环1次,挑出1个数;那么循环9次后,挑出9个数,第10个数也就不需要再循环做了.所以最大循环9次.
     */
    private static void sort2(int[] sourceArray) {
        int length = sourceArray.length;
        int[] array = Arrays.copyOf(sourceArray, length);
        printInfo(array);
        System.out.println("----------------------------------------before");
        for (int i = 0; i < length - 1; ++i) {
            for (int j = i + 1; j < length; ++j) {
                if (array[i] > array[j]) {
                    int tmp = array[i];
                    array[i] = array[j];
                    array[j] = tmp;
                }
            }
            printInfo(array);
        }
        System.out.println("----------------------------------------after");
        /*int minIndex = 0;
        for (int i = 0; i < length - 1; ++i) {
            minIndex = i;
            for (int j = i + 1; j < length; ++j) {
                if (array[minIndex] > array[j]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int tmp = array[i];
                array[i] = array[minIndex];
                array[minIndex] = tmp;
            }
            printInfo(array);
        }
        System.out.println("----------------------------------------after");*/
        printInfo(array);
    }

    /***
     插入
     */
    private static void sort3(int[] sourceArray) {
        int length = sourceArray.length;
        int[] array = Arrays.copyOf(sourceArray, length);
        printInfo(array);
        System.out.println("----------------------------------------before");
        for (int i = 0; i < length; ++i) {
            boolean flag = true;
            for (int j = 0; j < length - i - 1; ++j) {
                if (array[j] > array[j + 1]) {
                    int tmp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = tmp;
                    flag = false;
                }
            }
            if (flag) {
                break;
            }
            printInfo(array);
        }
        System.out.println("----------------------------------------after");
        printInfo(array);
    }

    private static void printInfo(int[] array) {
        StringBuilder sb = new StringBuilder();
        int length = array.length;
        for (int i = 0; i < length; ++i) {
            sb.append(array[i]);
            if (i < length - 1) {
                sb.append(" ");
            }
        }
        System.out.println(sb.toString());
    }
}
