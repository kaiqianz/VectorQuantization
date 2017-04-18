package hw2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel; 

public class VectorQuantization {
	
	static JFrame frame;
	static JLabel lbIm1; 
	static BufferedImage img;
	
	public static void main(String[] args) {
		String fileName = args[0];
		int vectorNumber = Integer.parseInt(args[1]);
		if((vectorNumber & (vectorNumber - 1)) != 0) {
			System.out.println("vectorNumber should be the power of 2");
			System.exit(0);
		}
		int width = 352;
		int height = 288;
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);   
		JPanel  panel = new JPanel ();
	    panel.add (new JLabel(new ImageIcon (showBefore(fileName, width, height))));
	    panel.add (new JLabel(new ImageIcon (showAfter(fileName, vectorNumber, width, height))));
		
		GridBagConstraints c = new GridBagConstraints(); 
		c.anchor = GridBagConstraints.CENTER; 
		c.gridx = 0;
		c.gridy = 0;  
		c.fill = GridBagConstraints.HORIZONTAL;  
		frame.getContentPane().add (panel);
		frame.pack();
		frame.setVisible(true); 
	}
	
	public static BufferedImage showBefore(String fileName, int width, int height) {
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
		try {
			File file = new File(fileName);
			InputStream is = new FileInputStream(file); 
			long len = file.length();
			byte[] bytes = new byte[(int)len]; 
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			} 
			int ind = 0;
			for(int y = 0; y < height; y++){ 
				for(int x = 0; x < width; x++){ 
					if (fileName.substring(fileName.indexOf('.')).equals(".raw")) {
						byte r = bytes[ind]; 
	
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((r & 0xff) << 8) | (r & 0xff); 
						img.setRGB(x,y,pix);
						ind++;
					}
					if (fileName.substring(fileName.indexOf('.')).equals(".rgb")) {
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff); 
						img.setRGB(x,y,pix);
						ind++;
					}
				}
			} 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}
	
	public static BufferedImage showAfter(String fileName, int vectorNumber, int width, int height) {
		int[][] r_info = new int[height][width];
		int[][] g_info = new int[height][width];
		int[][] b_info = new int[height][width];
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
		try {
			File file = new File(fileName);
			InputStream is = new FileInputStream(file); 
			long len = file.length();
			byte[] bytes = new byte[(int)len]; 
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			} 
			int ind = 0; 
			if (fileName.substring(fileName.indexOf('.')).equals(".raw")) { 
				int[][] new_r_info = new int[height][width];
				for(int y = 0; y < height; y++){ 
					for(int x = 0; x < width; x++){ 
						byte r = bytes[ind];  
						r_info[y][x] = r & 0xff; 
						ind++;
					}
				}
				new_r_info = vectorHelper(r_info, vectorNumber, width, height);
				int a = 0;
				for(int y = 0; y < height; y++){ 
					for(int x = 0; x < width; x++){ 
						int r = new_r_info[y][x];
						int pix = ((a << 24) + (r << 16) + (r << 8) + r);
						img.setRGB(x,y,pix);
					}
				}  
			}
			if (fileName.substring(fileName.indexOf('.')).equals(".rgb")) {
				int[][] new_rgb_info = new int[height * 3][width * 3]; 
				for(int y = 0; y < height; y++){ 
					for(int x = 0; x < width; x++){ 
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						r_info[y][x] = r & 0xff;
						g_info[y][x] = g & 0xff;
						b_info[y][x] = b & 0xff;
						ind++;
					}
				}
				new_rgb_info = vectorHelper2(r_info, g_info, b_info, vectorNumber, width, height); 
				int a = 0;
				for(int y = 0; y < height; y++){ 
					for(int x = 0; x < width; x++){ 
						int r = new_rgb_info[y][x];
						int g = new_rgb_info[y + height][x];
						int b = new_rgb_info[y + 2 * height][x];
						int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						img.setRGB(x,y,pix);
					}
				}  
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}
	
	//raw
	public static int[][] vectorHelper(int[][] r_info, int vectorNumber, int width, int height) {
		int[][] vectorResult = new int[height][width];
		ArrayList<MyVector> vectors = new ArrayList<MyVector>(); 
		ArrayList<CodeWord> codeWords = new ArrayList<CodeWord>();
		// init vectors 
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				MyVector vector = new MyVector(r_info[y][x], r_info[y][x + 1]); 
				vectors.add(vector);  
			}
		}
		//init codewords
		int codeNumber = (int)(Math.log(vectorNumber) / Math.log(2));
		int codeHeight = (int)Math.pow(2, codeNumber / 2);
		int hStepLength = 256 / codeHeight;
		int codeWidth = (int)Math.pow(2, codeNumber - codeNumber / 2);
		int wStepLength = 256 / codeWidth;
 
		if (vectorNumber == 2) {
			CodeWord code_word1 = new CodeWord(127, 127);
			CodeWord code_word2 = new CodeWord(255, 255);
			codeWords.add(code_word1);
			codeWords.add(code_word2);
		}
		else { 
			for (int i = 1; i <= codeHeight; i++) {
				for (int j = 1; j <= codeWidth; j++) {
					CodeWord code_word = new CodeWord(hStepLength * i - hStepLength/2, wStepLength * j - wStepLength / 2);
					codeWords.add(code_word);  
				}
			} 
		} 
		kMeans(vectors, codeWords);
		for (MyVector vector : vectors) { 
			vector.setX((int)vector.word_code.getX());
			vector.setY((int)vector.word_code.getY());
		}
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				 vectorResult[y][x] = vectors.get(i).getX();
				 vectorResult[y][x + 1] = vectors.get(i).getY();
				 i++;
			}
		}
		return vectorResult;
	}
	// get the CodeWord
	public static void kMeans(ArrayList<MyVector> vectors, ArrayList<CodeWord> codeWords) {
		
		double minDistance = 1000000.0;
		double distance = 0.0;
		double precode_x = 0.0, precode_y = 0.0, curcode_x = 0.0, curcode_y = 0.0;
		boolean running = true;
		while (running == true) {
			for (MyVector vector: vectors) {
				minDistance = 1000000.0;
				for (CodeWord code_word : codeWords) {
					distance = (double)Math.sqrt(Math.pow((code_word.getY() - vector.getY()), 2) + Math.pow((code_word.getX() - vector.getX()), 2));
					if (minDistance > distance) {
						minDistance = distance;
						vector.setCode(code_word);
					}
				}
			}
			// new center
			for (CodeWord code : codeWords) {
				int totalVector = 0;
				int allVector_x = 0;
				int allVector_y = 0;
				for (MyVector vector : vectors) {
					if (vector.getCode().equals(code)) {
						allVector_x += vector.getX();
						allVector_y += vector.getY();
						totalVector++;
					}
				}
				precode_x = code.getX();
				precode_y = code.getY();
				if (totalVector != 0) {
					code.setX((double)allVector_x / (double)totalVector);
					code.setY((double)allVector_y / (double)totalVector);
				}
				curcode_x = code.getX();
				curcode_y = code.getY();
			}
			if ((Math.sqrt(Math.pow((precode_y - curcode_y), 2) + Math.pow((precode_x - curcode_x), 2))) < 0.000001) {
				running = false;
			}
		} 
	}
	
	// rgb 
	public static int[][] vectorHelper2(int[][] r_info, int[][] g_info, int[][] b_info, int vectorNumber, int width, int height) {
		int[][] vectorResult = new int[height * 3][width * 3];
		ArrayList<MyVector> vectors = new ArrayList<MyVector>(); 
		ArrayList<CodeWord> codeWords = new ArrayList<CodeWord>();
		// init vectors 
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				MyVector vector = new MyVector(r_info[y][x], r_info[y][x + 1], g_info[y][x], g_info[y][x + 1], b_info[y][x], b_info[y][x + 1]); 
				vectors.add(vector);  
			}
		}
		
		//init codewords 
		int codeNumber = (int)(Math.log(vectorNumber) / Math.log(2));
		int codeHeight = (int)Math.pow(2, codeNumber / 2);
		int hStepLength = 256 / codeHeight;
		int codeWidth = (int)Math.pow(2, codeNumber - codeNumber / 2);
		int wStepLength = 256 / codeWidth;
 
		if (vectorNumber == 2) {
			CodeWord code_word1 = new CodeWord(127, 127);
			CodeWord code_word2 = new CodeWord(255, 255);
			codeWords.add(code_word1);
			codeWords.add(code_word2);
		}
		else { 
			for (int i = 1; i <= codeHeight; i++) {
				for (int j = 1; j <= codeWidth; j++) {
					CodeWord code_word = new CodeWord(hStepLength * i - hStepLength/2, wStepLength * j - wStepLength / 2, hStepLength * i - hStepLength/2, wStepLength * j - wStepLength / 2, hStepLength * i - hStepLength/2, wStepLength * j - wStepLength / 2);
					codeWords.add(code_word);  
				}
			} 
		}  
		kMeans2(vectors, codeWords);
		for (MyVector vector : vectors) { 
			vector.setR1((int)vector.word_code.getR1());
			vector.setR2((int)vector.word_code.getR2());
			vector.setG1((int)vector.word_code.getG1());
			vector.setG2((int)vector.word_code.getG2());
			vector.setB1((int)vector.word_code.getB1());
			vector.setB2((int)vector.word_code.getB2());
		}
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				 vectorResult[y][x] = vectors.get(i).getR1();
				 vectorResult[y][x + 1] = vectors.get(i).getR2();
				 vectorResult[y + height][x] = vectors.get(i).getG1();
				 vectorResult[y + height][x + 1] = vectors.get(i).getG2();
				 vectorResult[y + height * 2][x] = vectors.get(i).getB1();
				 vectorResult[y + height * 2][x + 1] = vectors.get(i).getB2();
				 i++;
			}
		}
		return vectorResult;
	}
	// get the CodeWord
	public static void kMeans2(ArrayList<MyVector> vectors, ArrayList<CodeWord> codeWords) {
		
		double minDistance = 1000000.0;
		double distance = 0.0;
		double precode_r1 = 0.0, precode_r2 = 0.0, precode_g1 = 0.0, precode_g2 = 0.0, precode_b1 = 0.0, precode_b2 = 0.0, curcode_r1 = 0.0, curcode_r2 = 0.0, curcode_g1 = 0.0,curcode_g2 = 0.0, curcode_b1 = 0.0, curcode_b2 = 0.0;
		boolean running = true; 
		while (running == true) { 
			for (MyVector vector: vectors) {
				minDistance = 1000000.0;
				for (CodeWord code_word : codeWords) {
					distance = Math.sqrt(Math.pow((code_word.getR1() - vector.getR1()), 2) + Math.pow((code_word.getR2() - vector.getR2()), 2) + Math.pow((code_word.getG1() - vector.getG1()), 2) + Math.pow((code_word.getG2() - vector.getG2()), 2) + Math.pow((code_word.getB1() - vector.getB1()), 2) + Math.pow((code_word.getB2() - vector.getB2()), 2));
					if (minDistance > distance) {
						minDistance = distance;
						vector.setCode(code_word);
					}
				}
			}
			// new center
			for (CodeWord code : codeWords) {
				int totalVector = 0;
				int allVector_r1 = 0;
				int allVector_r2 = 0;
				int allVector_g1 = 0;
				int allVector_g2 = 0;
				int allVector_b1 = 0;
				int allVector_b2 = 0;
				for (MyVector vector : vectors) {
					if (vector.getCode().equals(code)) {
						allVector_r1 += vector.getR1();
						allVector_r2 += vector.getR2();
						allVector_g1 += vector.getG1();
						allVector_g2 += vector.getG2();
						allVector_b1 += vector.getB1();
						allVector_b2 += vector.getB2();
						totalVector++;
					}
				}
				precode_r1 = code.getR1(); 
				precode_r2 = code.getR2();
				precode_g1 = code.getG1();
				precode_g2 = code.getG2();
				precode_b1 = code.getB1();
				precode_b2 = code.getB2();
				if (totalVector != 0) {
					code.setR1((double)allVector_r1 / (double)totalVector);
					code.setR2((double)allVector_r2 / (double)totalVector);
					code.setG1((double)allVector_g1 / (double)totalVector);
					code.setG2((double)allVector_g2 / (double)totalVector);
					code.setB1((double)allVector_b1 / (double)totalVector);
					code.setB2((double)allVector_b2 / (double)totalVector); 
				}
				curcode_r1 = code.getR1(); 
				curcode_r2 = code.getR2();
				curcode_g1 = code.getG1();
				curcode_g2 = code.getG2();
				curcode_b1 = code.getB1();
				curcode_b2 = code.getB2();
			}
			
			if ((Math.sqrt(Math.pow((precode_r1 - curcode_r1), 2) + Math.pow((precode_r2 - curcode_r2), 2) + Math.pow((precode_g1 - curcode_g1), 2) + Math.pow((precode_g2 - curcode_g2), 2) + Math.pow((precode_b1 - curcode_b1), 2) + Math.pow((precode_b2 - curcode_b2), 2))) < 0.000001) {
				running = false;
			}
		} 
	}
}
class MyVector {
	int x;
	int y;
	int r1;
	int r2;
	int g1;
	int g2;
	int b1;
	int b2;
	CodeWord word_code;
	MyVector(int x, int y) {
		this.x = x;
		this.y = y;
	}
	MyVector(int r1, int r2, int g1, int g2, int b1, int b2) {
		this.r1 = r1;
		this.r2 = r2;
		this.g1 = g2;
		this.g2 = g2;
		this.b1 = b1;
		this.b2 = b2;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getR1() {
		return r1;
	}
	public void setR1(int r1) {
		this.r1 = r1;
	}
	public int getR2() {
		return r2;
	}
	public void setR2(int r2) {
		this.r2 = r2;
	}
	public int getG1() {
		return g1;
	}
	public void setG1(int g1) {
		this.g1 = g1;
	}
	public int getG2() {
		return g2;
	}
	public void setG2(int g2) {
		this.g2 = g2;
	} 
	public int getB1() {
		return b1;
	}
	public void setB1(int b1) {
		this.b1 = b1;
	}
	public int getB2() {
		return b2;
	}
	public void setB2(int b2) {
		this.b2 = b2;
	} 
	public void setCode(CodeWord code) {
		this.word_code = code;
	}
	public CodeWord getCode() {
		return word_code;
	}
}
class CodeWord {
	double x;
	double y;
	double r1;
	double r2;
	double g1;
	double g2;
	double b1; 
	double b2;
	CodeWord(double x, double y) {
		this.x = x;
		this.y = y;
	}
	CodeWord(double r1, double r2, double g1, double g2, double b1, double b2) {
		this.r1 = r1;
		this.r2 = r2;
		this.g1 = g1;
		this.g2 = g2;
		this.b1 = b1;
		this.b2 = b2;
	}
//	public boolean equals(CodeWord object2) {
//        if (this.x == object2.x && this.y == object2.y) { 
//            return true;
//        }
//        else return false;
//	} 
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public double getR1() {
		return r1;
	}
	public void setR1(double r1) {
		this.r1 = r1;
	}
	public double getR2() {
		return r2;
	}
	public void setR2(double r2) {
		this.r2 = r2;
	}
	public double getG1() {
		return g1;
	}
	public void setG1(double g1) {
		this.g1 = g1;
	}
	public double getG2() {
		return g2;
	}
	public void setG2(double g2) {
		this.g2 = g2;
	} 
	public double getB1() {
		return b1;
	}
	public void setB1(double b1) {
		this.b1 = b1;
	}
	public double getB2() {
		return b2;
	}
	public void setB2(double b2) {
		this.b2 = b2;
	} 
}
