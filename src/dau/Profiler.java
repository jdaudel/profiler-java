package dau;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dau.Error;

public class Profiler
{
	public static Profiler singleton = new Profiler();
	
	public Profiler()
	{
		enable = false;
	}
	
	public static class Metric
	{
		public Metric( String name )
		{
			this.name = name;
		}
		
		public void clear()
		{
			Error.check( level == 0 );
			count = 0;
			inclusiveTimeMs = inclusiveTimeNs = 0;
			childTimeMs = childTimeNs = 0;
		}
		
		
		String name;
		int level; // handle recursion
		long count;
		
		long inclusiveTimeMs;
		long inclusiveTimeNs;
		
		long childTimeMs;
		long childTimeNs;
	}
	
	public static class Counter
	{
		public Counter( Metric metric )
		{
			this.metric = metric;
			startMs = System.currentTimeMillis();
			startNs = System.nanoTime();
		}
		
		Metric metric;
		long startMs;
		long startNs;
	}
		
	public Metric createMetric( String name )
	{
		Metric metric = findMetric( name );
		Error.check( metric == null );
		
		metric = new Metric( name );
		metrics.put( name, metric );
			
		return( metric );
	}
	
	// begain a session for a thread
	private Thread thread;
	
	public void beginSession()
	{
		Error.check( thread == null );
		Error.check( stack.size() == 0 );
		clear();
		thread = Thread.currentThread();
	}
	
	public void endSession()
	{
		Error.check( thread == Thread.currentThread() );
		Error.check( stack.size() == 0 );
		thread = null;
	}
	
	public void begin( Metric metric )
	{
		if (thread != Thread.currentThread() )
		{
			// skip
			return;
		}
		
		Counter counter = new Counter( metric );
		counter.startMs = System.currentTimeMillis();
		counter.startNs = System.nanoTime();
		metric.count++;
		metric.level++;
		
		stack.add( counter );
	}	
	
	public void begin( String name )
	{
		if (thread != Thread.currentThread() )
		{
			// skip
			return;
		}
				
		Metric metric = findMetric( name );
		if (metric == null)
		{
			metric = createMetric( name );
		}
		
		begin( metric );
	}
	
	public void end()
	{
		if (thread != Thread.currentThread() )
		{
			// skip
			return;
		}
		
		Error.check( stack.size() > 0 );
		Counter counter = stack.get( stack.size() - 1 );
		
		long elapsedTimeMs = System.currentTimeMillis() - counter.startMs;
		long elapsedTimeNs = System.nanoTime() - counter.startNs;
		//Error.check( elapsedTimeMs < 100000 );
		Metric metric = counter.metric;
		
		metric.level--;
		if (metric.level == 0)
		{
			metric.inclusiveTimeMs += elapsedTimeMs;
			metric.inclusiveTimeNs += elapsedTimeNs;
		}
		
		stack.remove( stack.size() - 1 );
		
		if (stack.size() > 0)
		{
			Counter parentCounter = stack.get( stack.size() - 1 );
			
			if (parentCounter.metric == metric)
			{
				// don't add to child time, since parent and child are the same metric
			}
			else
			{
				parentCounter.metric.childTimeMs += elapsedTimeMs;
				parentCounter.metric.childTimeNs += elapsedTimeNs;
			}
		}
	}
	
	public void clear()
	{
		// must not be profiling
		Error.check( stack.size() == 0 );
		for( Metric m : metrics.values() )
		{
			m.clear();
		}
	}
	
	private Metric findMetric( String name )
	{
		return( metrics.get( name ));
	}
	
	public String getStats()
	{
		//Error.check( stack.size() == 0 );
		
		List< Metric > list = new ArrayList<>( metrics.values() );
		Collections.sort( list, new Comparator< Metric >()
		{
			@Override
			public int compare( Metric m1, Metric m2 )
			{
				return( Long.compare( m2.inclusiveTimeMs, m1.inclusiveTimeMs ));
			}
			
		});
		
		DecimalFormat df = new DecimalFormat( "#,###" );
		
		String s = new String();
		s += "-----------------------\n";
		s += "Profiler Stats:\n";
		
		for( Iterator< Metric > iter = list.iterator(); iter.hasNext(); )
		{
			Metric m = iter.next();
			long exclusiveTimeMs = m.inclusiveTimeMs - m.childTimeMs;
			long exclusiveTimeNs = m.inclusiveTimeNs - m.childTimeNs;
			
			s += m.name + " inclusive time: " + df.format( m.inclusiveTimeMs ) + " ms ("
					+ df.format( m.inclusiveTimeNs / 1000000.0d ) + " ms)"
					+ ", exclusive: " + df.format( exclusiveTimeMs )
					+ " ms (" + df.format( exclusiveTimeNs / 1000000d ) + " ms)"
					+ ", count: " + m.count + ".\n";
		}
		
		s += "---------------------\n";
		
		return( s );
	}
	
	public void printStats()
	{
		if (enable)
		{
			String stats = getStats();
			System.out.print( stats );
		}
	}
	
	private List< Counter > stack = new ArrayList<>();
	private Map< String, Metric > metrics = new HashMap<>();
	private boolean enable = false;
}
