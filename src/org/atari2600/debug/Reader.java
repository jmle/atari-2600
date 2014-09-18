package org.atari2600.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Reader {
	public Reader() {
		
	}
	
	public String read() {
		String s = "";

		try {
			BufferedReader bufferRead = new BufferedReader(
					new InputStreamReader(System.in));

			s = bufferRead.readLine();

			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return s;
	}
}