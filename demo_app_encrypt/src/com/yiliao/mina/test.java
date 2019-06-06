package com.yiliao.mina;

public class test {

	public static void main(String[] args) throws Exception{

		for (int i = 1; i <= 1000; i++) {
			
			Thread.sleep(1000);
			
			StartThread st = new StartThread(i);
			st.start();
			
		}

	}

}
