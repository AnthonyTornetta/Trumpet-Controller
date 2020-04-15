package com.cornchipss.controller;

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
 */
public class AudioController
{
	public static void main(String[] args) throws Exception
	{
		final Map<Integer, String> values = new HashMap<>();
		
		/*
		 * Reads the config file for the pitches
		 * NOTE:
		 * multiple keys/mouse movements can be bound to the same pitch by adding a + sign between each key/mouse movement
		 */
		readPitches(values);
		
		// Handles key/mouse outputs
		Robot robot = new Robot();
		
		// Displays the audio to be seen by the user
		AudioGraph graph = new AudioGraph();
		
		final int SAMPLE_RATE = 8000; // 48k is great for normal recordings, but 8000 is good enough for pitch detection
		
		AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
		
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
		    		
					// YIN is good for the frequencies trumpets make
			    	PitchDetector detector = PitchEstimationAlgorithm.YIN.getDetector(SAMPLE_RATE, SAMPLE_RATE / 2);
			    	TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(format));
					
			    	long lastMillis = System.currentTimeMillis();
			    	
			    	// These are declared here so they're not deleted & re-allocated every time
			    	float[] micDataFloats = new float[SAMPLE_RATE / 2];
			    	byte[] micDataBytes = new byte[SAMPLE_RATE];
			    	
					while(true)
					{
					    int amtRead = line.read(micDataBytes, 0, SAMPLE_RATE);
					    
					    long deltaTime = System.currentTimeMillis() - lastMillis;
					    lastMillis = System.currentTimeMillis();
					    
					    if(amtRead > 0)
					    {
					    	// Converts stupid bytes to nice lovely floats					    	
					    	converter.toFloatArray(micDataBytes, micDataFloats);
					    	
					    	// Graphs the data so its nice and see-able
					    	graph.setData(micDataFloats);
					    	
					    	// Then gets their pitch using math i don't understand
					    	int pitch = Math.round(detector.getPitch(micDataFloats).getPitch());
					    	
							if(pitch != -1 && pitch != 11) // idk why, but it says 11 a lot for no reason and -1 means it has no idea what the pitch is
							{
								System.out.println("Pitch: " + pitch);
								
								for(int target : values.keySet())
								{
									if(isPitchEqual(pitch, target)) // A threshold of 10 is decent for most notes (this isn't good for high/low notes
									{
										String key = values.get(target);
										System.out.println("press: " + key);
										boolean sameKey = key.equals(lastKeyCode);
										
										if(!sameKey)
										{
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
												moveMouse(0, 2, deltaTime, robot);
												break;
											}
											case ">":
											{
												moveMouse(2, 0, deltaTime, robot);
												break;
											}
											case "<":
											{
												moveMouse(-2, 0, deltaTime, robot);
												break;
											}
											case "^":
											{
												moveMouse(0, -2, deltaTime, robot);
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

				private boolean isPitchEqual(int pitch, int target)
				{
					int octave = (int) Math.floor(Math.log(pitch) / Math.log(2)) - 4;
					
					// The distance between notes grows with each octave
					double threshold = Math.pow(2, octave) / 2;
										
					return pitch - threshold <= target && target <= pitch + threshold;
				}
		    }.start();
		}
		catch (LineUnavailableException ex)
		{
			System.err.println("ur mic off");
			return;
		}
	}
	
	/**
	 * Reads the YML file of pitches
	 * @param values Where to store this stuff
	 * @throws IOException If something bad happens when reading the files
	 */
	private static void readPitches(Map<Integer, String> values)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader("pitches.yml"));
			for(String line = br.readLine(); line != null; line = br.readLine())
			{
				String[] split = line.split(":");
				if(split.length == 2)
				{
					if(split[0].trim().charAt(0) != '#')
					{				
						String data = split[1];
						if(data.contains(" #"))
							data = data.substring(0, data.indexOf(" #"));
						
						values.put(Integer.parseInt(split[0]), data);
					}
				}
			}
			br.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Moves the mouse a given delta x and delta y over a given period of time
	 * @param x delta X
	 * @param y delta Y
	 * @param millis Time to move it
	 * @param robot The robot to execute this command
	 */
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
						Thread.sleep(15); // 15 is a nice amount to sleep to move the mouse not too much but not too little
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
