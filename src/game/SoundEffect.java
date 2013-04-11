package game;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public enum SoundEffect {
	ALIEN_APPEAR("alien_appear.wav"),
	ALIEN_DIE("alien_die.wav"),
	ASTEROID_BREAK("asteroid_break.wav"),
	HYPERSPACE("hyperspace.wav"),
	SHIP_CRASH("explosion.wav"),
	FIRE_BULLET("fire_bullet.wav"),
	FIRE_BULLET_ALIEN("fire_bullet_alien.wav"),
	GAME_START("game_start.wav"),
	POWER_UP("power_up.wav");
	
	private static final String RESOURCE_PATH = "resources/sounds/";
	
	private Clip clip;
	
	SoundEffect(String filename) {
		try {
			// Get URL to resource
			URL url = this.getClass().getClassLoader().getResource(RESOURCE_PATH + filename);
			
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
		
		// Play from beginning
		clip.setFramePosition(0);
		clip.start();
	}
	
	public boolean isPlaying() {
		return clip.isRunning();
	}
	
	static void init() {
		// Load all sound files
		values();
	}
}
