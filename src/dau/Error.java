package dau;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Error
{
	public static String mLog = "";

	public static void clearLog()
	{
		mLog = "";
	}
	
	public static void error( String msg ) 
	{
		String error = "Error " + msg;
		
		mLog += error;
		
		System.out.print( error );
				
		throw new RuntimeException( msg );
	}
	
	public static void error( Throwable t )
	{
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter( writer );
		t.printStackTrace( printWriter );
		printWriter.flush();

		String stackTrace = writer.toString();
		error( stackTrace );
	}
	
	public static void check( boolean expr )
	{
		if (expr == false)
		{
			error( "check failed" );
		}
		
	}
	
	public static void check( long num, long expected )
	{
		if (num != expected)
		{
			String error = "" + num + " is not " + expected;
			Error.error( error );
		}
	}
	
}
