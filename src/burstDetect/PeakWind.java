package burstDetect;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class PeakWind {
	private Integer[] feature, total;
	private ArrayList<int[]> window = new ArrayList<int[]>();
	private Date startTime;
	private int timeFrame;
	private SimpleDateFormat df = new SimpleDateFormat("dd/MM, mm:ss");
	public PeakWind(Integer[] featureCount, Integer[] totalCount, Date startTime, int timeFrame){
		this.feature = featureCount;
		this.total = totalCount;
		this.startTime = startTime;
		this.timeFrame = timeFrame;
	}
	
	public ArrayList<int[]> findPeak(){
		int start=-1,end=-1;
		double pj = 0, p0, sigm_p0 = 0;
		
		
		for(int i =0; i<feature.length; i++){
			int L = 0;
			p0 = (double) feature[i]/ (double) total[i];			

				for(int j = 0; j< i; j++){
					if(feature[j]>0) L++;
				}
				pj = sigm_p0/(double) L;


				if(p0>pj){
					long time = startTime.getTime()+timeFrame*1;
					Date td = new Date(time);
					System.out.println("*********************"+i+":"+df.format(td));
					if(start == -1) start = i;
					else continue;
				}else{
					if(start!= -1){
						end = i;
						int[] win = {start,end};
						window.add(win);
						start = -1;
						end = -1;
					}
				}
				if(i == feature.length){
					end =i;
					int[] win = {start,end};
					window.add(win);
				}
				
			
			sigm_p0 += p0;
		}
		
		for(int i = 0; i<window.size(); i++){
			System.out.println(window.get(i)[0]+":"+window.get(i)[1]);
		}
		return window;
	}

	
}

