package scikit.jobs;

import java.util.Vector;

public class JobManager {
	static Vector<Job> jobs = new Vector();
	
	public static int add(Job job) {
		jobs.add(job);
		return jobs.size()-1;
	}
	
	public static Job get(int i) {
		return jobs.get(i);
	}
	
	public static void step(int i) {
		get(i).step();
	}
	
	public static void stop(int i) {
		get(i).stop();
	}
	
	public static void start(int i) {
		get(i).start();
	}
	
	public static void terminate(int i) {
		get(i).kill();
		get(i).dispose();
		jobs.removeElementAt(i);
	}
	
	public static String list() {
		String ret = "";
		int i = 0;
		for (Job j : jobs) {
			ret += i++ + " : " + j.toString() + "\n";
		}
		return ret;
	}
}
