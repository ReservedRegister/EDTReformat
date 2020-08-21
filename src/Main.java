import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main
{
	public static void main(String args[])
	{
		ExecutorService threadpool = Executors.newCachedThreadPool();
		
		try
		{
			String[] files = new File("./original").list();
			
			for(String file : files)
			{
				if(file.endsWith(".edt"))
					threadpool.execute(new FormatFile(file));
			}
		}
		catch(NullPointerException e)
		{
			System.out.println("Directory \"original\" does not exist");
		}
		
		threadpool.shutdown();
	}
}
