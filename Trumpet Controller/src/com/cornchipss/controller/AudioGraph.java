package com.cornchipss.controller;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

/**
 * A window that simply graphs a bunch of floats connected w/ lines
 */
public class AudioGraph extends JFrame implements KeyListener
{
	private static final long serialVersionUID = 5703816059349945839L;
	
	private DrawnGraph graph;
	
	/**
	 * A window that simply graphs a bunch of floats connected w/ lines
	 */
	public AudioGraph()
	{
		super("Graph");
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(1024, 720);
		
		add(graph = new DrawnGraph(this));
		
		pack();
		
		addKeyListener(this);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
		
		graph.addKeyListener(this);
		
		setVisible(true);
	}
	
	/**
	 * Sets the floats to graph (a clone of them is made so no modifications will be made to the actual array)
	 * @param data the floats to graph
	 */
	public void setData(float[] data)
	{
		graph.setData(data);
	}
	
	/**
	 * The actual bit that draws the floats
	 */
	private class DrawnGraph extends Canvas
	{
		private static final long serialVersionUID = 431705062756990864L;
		
		private float[] data = new float[0];
		
		public DrawnGraph(AudioGraph graph)
		{
			setBounds(0, 0, graph.getWidth(), graph.getHeight());
		}
		
		/**
		 * Clones the floats to make sure no accidental modifications are made to the array
		 * @param data The data to graph
		 */
		public void setData(float[] data)
		{
			this.data = data.clone();
			
			for(int i = 0; i < this.data.length; i++)
				this.data[i] *= (getHeight() - 20); // Multiply by height - 20 to make it span the window height with some padding
			
			repaint();
		}
		
		@Override
		public void paint(Graphics gfx)
		{
			gfx.setColor(Color.BLACK);
			gfx.fillRect(0, 0, getWidth(), getHeight());
			
			// Allows the window to graph every float no matter how small the window and large the float array by averaging values (ik not the best way but it kinda works)
			int coordsCovered = (int)Math.ceil(data.length / (double)getWidth());
			
			int lastScrnCoord = 0;
			
			int avgData = 0;
			
			gfx.setColor(Color.RED);
			
			float max = 0;
			
			int lastX = -1, lastY = -1;
			
			final int size = 2;
			
			int n = data.length;
			
			for(int i = 0; i < n; i++)
			{
				float dataHere = data[i];
				
				int x = (int)(i / coordsCovered * 1);
				
				max = Math.max(dataHere, max);				
				if(lastScrnCoord != x)
				{
					int y = (int)((avgData / coordsCovered) / size);
					
					if(lastX == -1 || lastY == -1)
						gfx.fillRect(x, y, size, size);
					else
						gfx.drawLine(x, y, lastX, lastY);
					
					lastX = x;
					lastY = y;
					
					lastScrnCoord = x;
					avgData = 0;
				}
				
				avgData += getHeight() + dataHere; // Graph the data but centered
			}
		}
	}
	
	// This stuff below was used for testing please ignore
	public static final boolean[] keys = new boolean[255];
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		keys[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		keys[e.getKeyCode()] = false;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		
	}
}
