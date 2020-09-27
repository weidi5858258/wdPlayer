/*
 * Copyright 2016 - 2017 Sony Corporation
 */
package com.weidi.media.wdplayer.exo;


/*
 * This class extends android.util.Log.
 * The added functions are below.
 *  1.Use "SonyExoPlayer" as the tag of the log to be output.
 *  2.Add the class name or the tag of argument, the method name and the line number to the beginning of the message.
 *  3.Add log output control function by adb command.
 * Important point:
 *  When calling from super class, if you want to use sub class name, need explicitly specified as an argument tag.
 */
public class Log {
  private static final String TAG = "SonyExoPlayer";//Abbreviation for sony exo player.

  public static void v(String msg) {
    outputLog(android.util.Log.VERBOSE, null, msg, null);
  }

  public static void v(String tag, String msg) {
    outputLog(android.util.Log.VERBOSE, tag, msg, null);
  }

  public static void v(String msg, Throwable tr) {
    outputLog(android.util.Log.VERBOSE, null, msg, tr);
  }

  public static void v(String tag, String msg, Throwable tr) {
    outputLog(android.util.Log.VERBOSE, tag, msg, tr);
  }

  public static void d(String msg) {
    outputLog(android.util.Log.DEBUG, null, msg, null);
  }

  public static void d(String tag, String msg) {
    outputLog(android.util.Log.DEBUG, tag, msg, null);
  }

  public static void d(String msg, Throwable tr) {
    outputLog(android.util.Log.DEBUG, null, msg, tr);
  }

  public static void d(String tag, String msg, Throwable tr) {
    outputLog(android.util.Log.DEBUG, tag, msg, tr);
  }

  public static void i(String msg) {
    outputLog(android.util.Log.INFO, null, msg, null);
  }

  public static void i(String tag, String msg) {
    outputLog(android.util.Log.INFO, tag, msg, null);
  }

  public static void i(String msg, Throwable tr) {
    outputLog(android.util.Log.INFO, null, msg, tr);
  }

  public static void i(String tag, String msg, Throwable tr) {
    outputLog(android.util.Log.INFO, tag, msg, tr);
  }

  public static void w(String msg) {
    outputLog(android.util.Log.WARN, null, msg, null);
  }

  public static void w(String tag, String msg) {
    outputLog(android.util.Log.WARN, tag, msg, null);
  }

  public static void w(String msg, Throwable tr) {
    outputLog(android.util.Log.WARN, null, msg, tr);
  }

  public static void w(String tag, String msg, Throwable tr) {
    outputLog(android.util.Log.WARN, tag, msg, tr);
  }

  public static void e(String msg) {
    outputLog(android.util.Log.ERROR, null, msg, null);
  }

  public static void e(String tag, String msg) {
    outputLog(android.util.Log.ERROR, tag, msg, null);
  }

  public static void e(String msg, Throwable tr) {
    outputLog(android.util.Log.ERROR, null, msg, tr);
  }

  public static void e(String tag, String msg, Throwable tr) {
    outputLog(android.util.Log.ERROR, tag, msg, tr);
  }

  private static void outputLog(int type, String tag, String msg, Throwable tr) {
    if (android.util.Log.VERBOSE == type && android.util.Log.isLoggable(TAG, type) == false) {
      return;
    }

    StackTraceElement element = Thread.currentThread().getStackTrace()[4];
    String tmpMsg = tag != null ? tag : getClassName(element);
    tmpMsg += "#" + element.getMethodName() + ": line " + element.getLineNumber() + ": " + msg;

    switch (type) {
      case android.util.Log.VERBOSE:
        if (tr == null) {
          android.util.Log.v(TAG, tmpMsg);
        } else {
          android.util.Log.v(TAG, tmpMsg, tr);
        }
        break;
      case android.util.Log.DEBUG:
        if (tr == null) {
          android.util.Log.d(TAG, tmpMsg);
        } else {
          android.util.Log.d(TAG, tmpMsg, tr);
        }
        break;
      case android.util.Log.INFO:
        if (tr == null) {
          android.util.Log.i(TAG, tmpMsg);
        } else {
          android.util.Log.i(TAG, tmpMsg, tr);
        }
        break;
      case android.util.Log.WARN:
        if (tr == null) {
          android.util.Log.w(TAG, tmpMsg);
        } else {
          android.util.Log.w(TAG, tmpMsg, tr);
        }
        break;
      case android.util.Log.ERROR:
        if (tr == null) {
          android.util.Log.e(TAG, tmpMsg);
        } else {
          android.util.Log.e(TAG, tmpMsg, tr);
        }
        break;
      default:
        break;
    }
  }

  private static String getClassName(StackTraceElement element) {
    String fullName = element.getClassName();
    return fullName.substring(fullName.lastIndexOf(".") + 1);
  }
}
