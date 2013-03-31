package game;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public enum SoundEffect {
	ASTEROID_BREAK("asteroid_break.wav"),
	EXPLOSION("explosion2.wav"),
	FIRE_BULLET("fire_bullet.wav"),
	POWER_UP("power_up.wav"),
	BACKGROUND("background3.wav", true);
	
	private static final String resourcePath = "resources/sounds/";
	
	private Clip clip;
	private boolean repeat;
	
	SoundEffect(String filename) {
		this(filename, false);
	}
	
	SoundEffect(String filename, boolean repeat) {
		this.repeat = repeat;
		
		try {
			// Get URL to resource
			URL url = this.getClass().getClassLoader().getResource(resourcePath + filename);
			
			// Set up audio stream for the resource
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
			
			// Get a clip and load the audio
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	public void play() {
		// Stop clip if it is playing already
		if (clip.isRunning()) {
			clip.stop();
		}
		
		if (this.repeat) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		// Play from beginning
		clip.setFramePosition(0);
		clip.start();
	}
	
	static void init() {
		// Load all sound files
		values();
	}
}