package com.cornchipss.controller;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

/**
 * Controls your keyboard/mouse using audio from a microphone.
 * @author Cornchip
 */
public class AudioController
{
	public static void main(String[] args) throws LineUnavailableException, IOException, AWTException
	{
		final Map<Integer, String> values = new HashMap<>();
		
		/*
		 * Reads the config file for the pitches
		 * NOTE:
		 * multiple keys/mouse movements can be bound to the same pitch by adding a + sign between each key/mouse movement
		 */
		readPitches(values);
		
		// Handles key/mouse outputs (beep bop)
		Robot robot = new Robot();
		
		// Displays the audio to be seen by the user
		AudioGraph graph = new AudioGraph();
		
		final int SAMPLE_RATE = 48100;
		
		AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true); //48k is great for voices and such, but 8000 is good enough for pitch detection
		
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		
		if (!AudioSystem.isLineSupported(info))
		{
			System.err.println("ur mic bad");
			return;
		}
		
		try
		{
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		    line.open(format);
		    line.start();
			
		    // Threaded so the window can draw stuff
		    new Thread()
		    {	
		    	@Override
		    	public void run()
		    	{
					int[] lastKeysPressed = new int[0];
					String lastKeyCode = "";
		    		
					while(true)
					{
						byte[] byteBuf = new byte[(int)format.getSampleRate() * format.getFrameSize() / 2];
						
					    int amtRead = line.read(byteBuf, 0, byteBuf.length);
					    
					    if(amtRead > 0)
					    {
					    	// Converts stupid bytes to nice lovely floats
					    	float[] floats = new float[byteBuf.length / 2];
					    	
					    	PitchDetector detector = PitchEstimationAlgorithm.YIN.getDetector(SAMPLE_RATE, floats.length);
					    	
					    	TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
					    	
					    	converter.toFloatArray(byteBuf, floats);
					    	
					    	graph.setData(floats);
					    	
					    	// Then gets their pitch using math i dont understand
					    	int pitch = Math.round(detector.getPitch(floats).getPitch());
					    	
							if(pitch != -1 && pitch != 11) // idk why, but it says 11 a lot for no reason and -1 means it has no idea the pitch
							{
								System.out.println("Pitch: " + pitch);
								
								for(int p : values.keySet())
								{
									if(p - 10 <= pitch && pitch <= p + 10) // A threshold of 10 is decent for most notes (this isn't good for high/low notes tho and im too lazy to make it dynamic)
									{
										String key = values.get(p);
										
										final long MILLIS_BETWEEN_MEASURES = 1000 / 3; // 3 measurements per second is a good guesstimate
										
										boolean sameKey = key.equals(lastKeyCode);
										
										if(!sameKey)
										{
											System.out.println(key);
											if(lastKeysPressed.length != 0)
											{
												for(int k : lastKeysPressed)
												{
													if(k == InputEvent.BUTTON3_DOWN_MASK || k == InputEvent.BUTTON2_DOWN_MASK || k == InputEvent.BUTTON1_DOWN_MASK)
														robot.mouseRelease(k);
													else
														robot.keyRelease(k);
												}
												
												lastKeysPressed = new int[0];
											}
										}
										
										switch(key.toLowerCase())
										{
											case "v":
											{
												moveMouse(0, 2, MILLIS_BETWEEN_MEASURES, robot);
												break;
											}
											case ">":
											{
												moveMouse(2, 0, MILLIS_BETWEEN_MEASURES, robot);
												break;
											}
											case "<":
											{
												moveMouse(-2, 0, MILLIS_BETWEEN_MEASURES, robot);
												break;
											}
											case "^":
											{
												moveMouse(0, -2, MILLIS_BETWEEN_MEASURES, robot);
												break;
											}
											case "lmb":
											{
												if(!sameKey)
												{
													robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
													lastKeysPressed = new int[] { InputEvent.BUTTON1_DOWN_MASK };
												}
												break;
											}
											case "rmb":
											{
												if(!sameKey)
												{
													robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
													lastKeysPressed = new int[] { InputEvent.BUTTON3_DOWN_MASK };
												}
												break;
											}
											case "mmb":
											{
												if(!sameKey)
												{
													robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
													lastKeysPressed = new int[] { InputEvent.BUTTON2_DOWN_MASK };
												}
											}
											default:
											{
												String[] spl = key.split("\\+");
												
												lastKeysPressed = new int[spl.length];
												
												for(int i = 0; i < spl.length; i++)
												{
													int keycode;
													
													if(spl[i].toUpperCase().equals("ADD")) // the + sign is already used to have multiple key inputs, so it had to be 'add' and not '+'
														keycode = KeyEvent.VK_ADD;
													else
														keycode = KeyEvent.getExtendedKeyCodeForChar(spl[i].toUpperCase().charAt(0));
													
													lastKeysPressed[i] = keycode;
													robot.keyPress(keycode);
												}
											}
										}
										
										lastKeyCode = key;
									}
								}
							}
							else
							{
								for(int i = 0; i < lastKeysPressed.length; i++)
								{
									if(lastKeysPressed[i] == InputEvent.BUTTON3_DOWN_MASK || lastKeysPressed[i] == InputEvent.BUTTON1_DOWN_MASK)
										robot.mouseRelease(lastKeysPressed[i]);
									else
										robot.keyRelease(lastKeysPressed[i]);
								}
								
								lastKeysPressed = new int[0];
								lastKeyCode = "";
							}
					    }
					}
		    	}
		    }.start();
		}
		catch (LineUnavailableException ex)
		{
			System.err.println("ur mic off");
			return;
		}
	}
	
	private static void readPitches(Map<Integer, String> values) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader("pitches.yml"));
		for(String line = br.readLine(); line != null; line = br.readLine())
		{
			String[] split = line.split(":");
			if(split.length == 2)
				values.put(Integer.parseInt(split[0]), split[1]);
		}
		br.close();
	}

	private static void moveMouse(int x, int y, long millis, Robot robot)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				long start = System.currentTimeMillis();
				
				while(System.currentTimeMillis() < start + millis)
				{
					int oX = (int)MouseInfo.getPointerInfo().getLocation().getX();
					int oY = (int)MouseInfo.getPointerInfo().getLocation().getY();
					
					robot.mouseMove(oX + x, oY + y);
					
					try
					{
						Thread.sleep(10); // 10 is a nice amount to sleep to move the mouse not too much but not too little
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
