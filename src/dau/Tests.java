package dau;

import static org.junit.Assert.*;

import org.junit.Test;
import dau.Profiler;

public class Tests
{
	@Test
	public void test()
	{
		Profiler.singleton.beginSession();
		try
		{	
			try { Profiler.singleton.begin( "program" );
			for( int i=0; i< 5; ++i )
			{
				try {Profiler.singleton.begin( "parent1" );
				Thread.sleep( 5 );
				
				for( int j=0; j<5; ++ j )
				{
					try { Profiler.singleton.begin( "child1" );
					Thread.sleep( 5 );
					} finally { Profiler.singleton.end(); } // child1
				}
				} finally { Profiler.singleton.end(); } // parent1
			}
			
			} finally { Profiler.singleton.end(); } // program
						
		}
		catch( InterruptedException e )
		{
			Error.error( e );
		}
						
		String s = Profiler.singleton.getStats();
		System.out.print( s );
	}

	// Expected output:
	// Profiler Stats:
	// parent1 inclusive time: 150 ms (150 ms), exclusive: 25 ms (25 ms), count: 5.
	// program inclusive time: 150 ms (150 ms), exclusive: 0 ms (0 ms), count: 1.
	// child1 inclusive time: 125 ms (125 ms), exclusive: 125 ms (125 ms), count: 25.
}
