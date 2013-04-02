package game;

import events.BulletFiredEvent;
import events.BulletFiredListener;
import game.entities.Asteroid;
import game.entities.Bullet;
import game.entities.Entity;
import game.entities.Ship;
import game.ui.GameCanvas;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game implements BulletFiredListener, KeyListener {
	// Time constants
	private static final long FPS = 45;
	private static final long NANOS_PER_SECOND = 1000000000;
	private static final double SPF = 1 / FPS;
	private static final long NANOS_PER_FRAME = (long)(NANOS_PER_SECOND * SPF);
	private static final long NANOS_PER_COLLISION = NANOS_PER_FRAME * 2;
	private static final long NANOS_PER_RENDER = (long)(NANOS_PER_FRAME * 1.5);
	private static final long NANOS_PER_LEVEL_WAIT = (long)(NANOS_PER_SECOND * 0.75);

	private static final int SAFE_RADIUS = 100;
	
	// Points constants
	private static final long POINTS_ASTEROID = 1000;
	private static final long POINTS_CLEAR_LEVEL = 2000;
	
	// Game state
	private int level = 1;
	private long points = 0;
	private long pointsFluid = points;
	private double multiplier = 1;
	private boolean paused = false;
	private boolean levelEnded = false;
	
	// Game components
	private List<Bullet> bullets;
	private List<Entity> entities;
	private Ship player;
	
	private GameCanvas canvas;
	
	public Game(GameCanvas canvas) {
		this.bullets = new ArrayList<Bullet>();
		this.entities = new ArrayList<Entity>();
		this.canvas = canvas;
		this.canvas.addKeyListener(this);
	}
	
	public void start() {
		// Create player and listen to its bullet fired events
		player = new Ship(new Point(GameCanvas.WIDTH / 2, GameCanvas.HEIGHT / 2));
		player.addBulletFiredListener(this);
		
		populateField();
		loop();
	}
	
	public void togglePause() {
		this.paused = !this.paused;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public void bulletFired(BulletFiredEvent e) {
		bullets.add(new Bullet(e.getSource(), e.getOrigin(), e.getAngle()));
		SoundEffect.FIRE_BULLET.play();
	}
	
	public void keyPressed(KeyEvent e) {
		// Check for pause key
		if (e.getKeyCode() == KeyEvent.VK_P) {
			this.paused = !this.paused;
		}
	}
	
	public long getPoints() {
		return points;
	}
	
	public int getLevel() {
		return level;
	}
	
	public double getMultipliter() {
		return multiplier;
	}
	
	private void populateField() {
		int asteroidCount = level * 2;
		
		for (int i = 0; i < asteroidCount; i++) {
			entities.add(new Asteroid(Point.getRandom(GameCanvas.WIDTH, GameCanvas.HEIGHT, player.getCenter(), SAFE_RADIUS)));
		}
	}
	
	private void loop() {
		long delta, now, lastLoop = System.nanoTime();
		long lastUpdate = 0;
		long lastRender = 0;
		long lastSecond = 0;
		long waitUntil = 0;
		long lastCollisionCheck = 0;
		
		while(player.isAlive()) {
			// Adjust counters
			now = System.nanoTime();
			delta = now - lastLoop;
			lastLoop = now;
			
			// Bail if paused
			if (paused || now < waitUntil) {
				continue;
			}
			
			
			if (levelEnded) {
				nextLevel();
				levelEnded = false;
				waitUntil = now + NANOS_PER_LEVEL_WAIT;
				continue;
			}
			
			lastUpdate += delta;
			lastRender += delta;
			lastCollisionCheck += delta;
			lastSecond += delta;
			
			// Process collisions
			if (lastCollisionCheck > NANOS_PER_COLLISION) {
				lastCollisionCheck = 0;
				collisionCheck();
			}
			
			// Process renders
			if (lastRender > NANOS_PER_RENDER) {
				lastRender = 0;
				canvas.render(player, bullets, entities, pointsFluid, level);
			}
			
			// Process updates
			if (lastUpdate > NANOS_PER_FRAME) {
				lastUpdate = 0;
				update(delta);
			}
			
			// Process once per second
			if (lastSecond > NANOS_PER_SECOND) {
				lastSecond = 0;
			}
		}
	}
	
	private void update(long delta) {
		player.update(delta);
		
		updateBullets(delta);
		updateEntities(delta);
		
		if (pointsFluid < points) {
			pointsFluid += 2 * multiplier;
			Math.min(points, pointsFluid);
		}
	}
	
	private void updateBullets(long delta) {
		Iterator<Bullet> i = bullets.iterator();
		Bullet b;
		
		while(i.hasNext()) {
			b = i.next();
			
			if (b.isExpired()) {
				i.remove();
			} else {
				b.update(delta);
			}
		}
	}
	
	private void updateEntities(long delta) {
		for (Entity e : entities) {
			e.update(delta);
		}
	}
	
	private void collisionCheck() {
		// Loop through bullets on outer, it's faster when there are no bullets!
		Iterator<Bullet> bulletIterator = bullets.iterator();
		
		while(bulletIterator.hasNext()) {
			Bullet b = bulletIterator.next();
			
			// Player cannot shoot self
			if (player != b.getSource() && player.getBounds().intersects((Rectangle)b.getBounds())) {
				// Check collision with player
				bulletIterator.remove();
				player.die();
				SoundEffect.EXPLOSION.play();
			} else {
				// No player collision, check other entities
				Iterator<Entity> entityIterator = entities.iterator();
				
				while (entityIterator.hasNext()) {
					Entity e = entityIterator.next();
					
					if (e.getBounds().intersects((Rectangle)b.getBounds())) {
						bulletIterator.remove();
						entityIterator.remove();
						SoundEffect.ASTEROID_BREAK.play();
						
						if (b.getSource() == player) {
							points += multiplier * POINTS_ASTEROID;
						}
					}
				}
				
				if (entities.size() == 0) {
					level++;
					points += multiplier * POINTS_CLEAR_LEVEL;
					multiplier += 0.5;
					levelEnded = true;
				}
			}
		}
		
		Iterator<Entity> entityIterator = entities.iterator();
		
		while(entityIterator.hasNext()) {
			Entity e = entityIterator.next();
			
			if (e instanceof Asteroid) {
				Area area = new Area(player.getBounds());
				area.intersect(
					new Area(e.getBounds())
				);
				
				if (!area.isEmpty()) {
					player.die();
					SoundEffect.EXPLOSION.play();
				}
			}
		}
	}
	
	private void nextLevel() {
		if (levelEnded) {
			populateField();
			
			levelEnded = false;
		}
	}
	
	public void keyReleased(KeyEvent e) {
	}
	
	public void keyTyped(KeyEvent e) {
	}
}