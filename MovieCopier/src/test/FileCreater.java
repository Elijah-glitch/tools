package test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCreater {

	private static final String PATH = "D:\\Files\\temp\\test.txt";
	
	
	public static void run(){
		try {
			FileOutputStream fos = new FileOutputStream(PATH);
//			synchronized (fos){
				for (int i = 0; i < 1048576; i++){
					byte b = (byte) (Math.random() * 256);
					fos.write(b);
					if (i%100000 == 99999){
						fos.flush();
						System.out.println("start sleep");
						Thread.sleep(1000);
						System.out.println("end sleep");
					}
				}
//			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		run();
	}
}
