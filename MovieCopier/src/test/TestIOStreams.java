package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestIOStreams {

	
	
	public class TestInputStream extends InputStream{

		private int nrRead = 0;
		
		@Override
		public int read(byte[] bytes) throws IOException {
			if (nrRead == 100000){
				return -1;
			}
			byte b = (byte) (Math.floor(Math.random() * 256) - 128);
			for (int i = 0; i < bytes.length; i++){
				bytes[i] = b;
			}
			nrRead++;
			return bytes.length;
		}

		@Override
		public int read() throws IOException {
			return 0;
		}
		
	}
	
	
	public class TestOutputStream extends OutputStream{

		@Override
		public void write(byte[] bytes) throws IOException {
			byte b = (byte) (Math.floor(Math.random() * 256) - 128);
			for (int i = 0; i < bytes.length; i++){
				bytes[i] = b;
			}
		}

		@Override
		public void write(int b) throws IOException {
		}
		
	}
}
