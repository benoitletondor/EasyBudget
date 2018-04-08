/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ajapplications.budgeteerbuddy.helper;

import android.util.Log;

import com.ajapplications.budgeteerbuddy.BuildConfig;
//import com.crashlytics.android.Crashlytics;

/**
 * Helper to easily log into the application.
 * 
 * @author Benoit LETONDOR
 */
public final class Logger 
{
	/**
	 * Default logger tag.
	 */
	public static final String DEFAULT_TAG	= "EasyBudget";
	
// ----------------------------------------->
	
	/**
	 * Return the default tag depending on the debug variable
	 * 
	 * @param debug
	 * @return
	 */
	private static String getDefaultTag(boolean debug)
	{
		if( debug )
		{
			return DEFAULT_TAG + "-debug";
		}
		
		return DEFAULT_TAG;
	}
	
// ----------------------------------------->
	
	/**
	 * Error log with debug, tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the error.
	 * @param t		Throwable exception.
	 */
	public static void error(String tag, boolean debug, String msg, Throwable t)
	{
		if( !debug || BuildConfig.DEBUG_LOG )
		{
			Log.e(tag, msg, t);
		}

		if( BuildConfig.CRASHLYTICS_ACTIVATED && t != null )
		{
//			Crashlytics.logException(new Throwable(msg, t));
		}
	}
	
	/**
	 * Error log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the error.
	 * @param t		Throwable exception.
	 */
	public static void error(String tag, String msg, Throwable t)
	{
		error(tag, true, msg, t);
	}
	
	/**
	 * Error log with tag and message.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the error.
	 */
	public static void error(String tag, String msg)
	{
		error(tag, msg, null);
	}
	
	/**
	 * Error log with message and throwable. Uses the default tag.
	 * 
	 * @param msg	Message of the error.
	 * @param t		Throwable exception.
	 */
	public static void error(String msg, Throwable t)
	{
		error(getDefaultTag(true), msg, t);
	}
	
	/**
	 * Error log with debug, message and throwable. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the error.
	 * @param t		Throwable exception.
	 */
	public static void error(boolean debug, String msg, Throwable t)
	{
		error(getDefaultTag(debug), debug, msg, t);
	}
	
	/**
	 * Error log with message. Uses the default tag.
	 * 
	 * @param msg	Message of the error.
	 */
	public static void error(String msg)
	{
		error(getDefaultTag(true), msg, null);
	}
	
	/**
	 * Error log with message. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the error.
	 */
	public static void error(boolean debug, String msg)
	{
		error(getDefaultTag(debug), debug, msg, null);
	}
	
// ------------------------------------------>
	
	/**
	 * Warning log with tag, debug, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the warning.
	 * @param t		Throwable exception.
	 */
	public static void warning(String tag, boolean debug, String msg, Throwable t)
	{
		if( !debug || BuildConfig.DEBUG_LOG )
		{
			Log.w(tag, msg, t);
		}
	}
	
	/**
	 * Warning log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the warning.
	 * @param t		Throwable exception.
	 */
	public static void warning(String tag, String msg, Throwable t)
	{
		warning(tag, true, msg, t);
	}
	
	/**
	 * Warning log with tag, message.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the warning.
	 */
	public static void warning(String tag, String msg)
	{
		warning(tag, msg, null);
	}
	
	/**
	 * Warning log with message and throwable. Uses the default tag.
	 * 
	 * @param msg	Message of the warning.
	 * @param t		Throwable exception.
	 */
	public static void warning(String msg, Throwable t)
	{
		warning(getDefaultTag(true), msg, t);
	}
	
	/**
	 * Warning log with debug, message and throwable. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the warning.
	 * @param t		Throwable exception.
	 */
	public static void warning(boolean debug, String msg, Throwable t)
	{
		warning(getDefaultTag(debug), debug, msg, t);
	}
	
	/**
	 * Warning log with debug & message. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the warning.
	 */
	public static void warning(boolean debug, String msg)
	{
		warning(getDefaultTag(debug), debug, msg, null);
	}
	
	/**
	 * Warning log with message. Uses the default tag.
	 * 
	 * @param msg	Message of the warning.
	 */
	public static void warning(String msg)
	{
		warning(getDefaultTag(true), msg, null);
	}
	
// ------------------------------------------>
	
	/**
	 * Debug log with tag, debug, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of debug.
	 * @param t		Throwable exception.
	 */
	public static void debug(String tag, boolean debug, String msg, Throwable t)
	{
		if( !debug || BuildConfig.DEBUG_LOG )
		{
			Log.d(tag, msg, t);
		}	
	}
	
	/**
	 * Debug log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of debug.
	 * @param t		Throwable exception.
	 */
	public static void debug(String tag, String msg, Throwable t)
	{
		debug(tag, true, msg, t);
	}
	
	/**
	 * Debug log with tag and message.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of debug.
	 */
	public static void debug(String tag, String msg)
	{
		debug(tag, msg, null);
	}
	
	/**
	 * Debug log with debug, message and throwable. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of debug.
	 * @param t		Throwable exception.
	 */
	public static void debug(boolean debug, String msg, Throwable t)
	{
		debug(getDefaultTag(debug), debug, msg, t);
	}
	
	/**
	 * Debug log with message and throwable. Uses the default tag.
	 * 
	 * @param msg	Message of debug.
	 * @param t		Throwable exception.
	 */
	public static void debug(String msg, Throwable t)
	{
		debug(getDefaultTag(true), msg, t);
	}
	
	/**
	 * Debug log with debug & message. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of debug.
	 */
	public static void debug(boolean debug, String msg)
	{
		debug(getDefaultTag(debug), debug, msg, null);
	}
	
	/**
	 * Debug log with message. Uses the default tag.
	 * 
	 * @param msg	Message of debug.
	 */
	public static void debug(String msg)
	{
		debug(getDefaultTag(true), msg, null);
	}
	
// ------------------------------------------>

	/**
	 * Information log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the information.
	 * @param t		Throwable exception.
	 */
	public static void info(String tag, boolean debug, String msg, Throwable t)
	{
		if( !debug || BuildConfig.DEBUG_LOG )
		{
			Log.i(tag, msg, t);
		}
	}
	
	/**
	 * Information log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the information.
	 * @param t		Throwable exception.
	 */
	public static void info(String tag, String msg, Throwable t)
	{
		info(tag, true, msg, t);
	}
	
	/**
	 * Information log with tag and message.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message of the information.
	 */
	public static void info(String tag, String msg)
	{
		info(tag, msg, null);
	}
	
	/**
	 * Information log with debug, message and throwable. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the information.
	 * @param t		Throwable exception.
	 */
	public static void info(boolean debug, String msg, Throwable t)
	{
		info(getDefaultTag(debug), debug, msg, t);
	}
	
	/**
	 * Information log with tag, message and throwable. Uses the default tag.
	 * 
	 * @param msg	Message of the information.
	 * @param t		Throwable exception.
	 */
	public static void info(String msg, Throwable t)
	{
		info(getDefaultTag(true), msg, t);
	}
	
	/**
	 * Information log with debug & message. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message of the information.
	 */
	public static void info(boolean debug, String msg)
	{
		info(getDefaultTag(debug), debug, msg, null);
	}
	
	/**
	 * Information log with message. Uses the default tag.
	 * 
	 * @param msg	Message of the information.
	 */
	public static void info(String msg)
	{
		info(getDefaultTag(true), msg, null);
	}
	
// ------------------------------------------>

	/**
	 * Verbose log with tag, debug, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message.
	 * @param t		Throwable exception.
	 */
	public static void verbose(String tag, boolean debug, String msg, Throwable t)
	{
		if( !debug || BuildConfig.DEBUG_LOG )
		{
			Log.v(tag, msg, t);
		}
	}
	
	/**
	 * Verbose log with tag, message and throwable.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message.
	 * @param t		Throwable exception.
	 */
	public static void verbose(String tag, String msg, Throwable t)
	{
		verbose(tag, true, msg, t);
	}
	
	/**
	 * Verbose log with tag, message.
	 * 
	 * @param tag	Tag to show.
	 * @param msg	Message.
	 */
	public static void verbose(String tag, String msg)
	{
		verbose(tag, msg, null);
	}
	
	/**
	 * Verbose log with debug, message and throwable. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message.
	 * @param t		Throwable exception.
	 */
	public static void verbose(boolean debug, String msg, Throwable t)
	{
		verbose(getDefaultTag(debug), debug, msg, t);
	}
	
	/**
	 * Verbose log with message and throwable. Uses the default tag.
	 * 
	 * @param msg	Message.
	 * @param t		Throwable exception.
	 */
	public static void verbose(String msg, Throwable t)
	{
		verbose(getDefaultTag(true), msg, t);
	}
	
	/**
	 * Verbose log with debug & message. Uses the default tag.
	 * 
	 * @param debug	Is this a debug log (= for the lib) or a log that should be displayed to the dev
	 * @param msg	Message.
	 */
	public static void verbose(boolean debug, String msg)
	{
		verbose(getDefaultTag(debug), debug, msg, null);
	}
	
	/**
	 * Verbose log with message. Uses the default tag.
	 * 
	 * @param msg	Message.
	 */
	public static void verbose(String msg)
	{
		verbose(getDefaultTag(true), msg, null);
	}
}
