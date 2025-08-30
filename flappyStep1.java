package flappyBird;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;


public class flappyStep1 extends JPanel implements java.awt.event.ActionListener {
	
	// constants 
	static final int WIDTH = 400, HEIGHT = 600, GROUND_HEIGHT = 60;
	static final double GRAVITY = 900.0;
	static final double flapVelocity = -320.0;
	
	
	//Pipes
	static final int pipeWidth = 55;
	static final double pipeSpeed = 150.0; //px per second, leftward
	static final int pipeGap  = 190; //gap size between top and bottom pipes
	static final double pipeInterval = 1.5; //seconds between spawns
	
	
	//game infra
	private final Timer timer;
	private long lastNs;
	private boolean started = false; 
	
	
	//Bird State
	private double birdX = 100.0;
	private double birdY = HEIGHT / 2.0;
	private double birdVy = 0.0;
	//image
	static private int birdSize = 32;
	private java.awt.Image birdImage;
	
	//Pipe State
	private java.util.List<PipePair> pipes = new java.util.ArrayList<>();
	private double timeSinceLastPipe = 0.0;
	private java.util.Random rng = new java.util.Random();
	
	
	//Colisions 
	private boolean gameOver = false;
	private int score = 0;
	
	
	//Inner helper class
	private static class PipePair{
		double x;
		int gapTop;
		int width;
		int gap;
		boolean scored = false;
		
		PipePair(double startX, int gapTop, int width, int gap){
			this.x = startX;
			this.gapTop = gapTop;
			this.gap = gap;
			this.width = width;
		}
		
		boolean isOffscreen() {
			return x + width < 0;
		}
		
		
		Rectangle topRect() {
			return new Rectangle((int)x, 0, width, gapTop);
		}
		
		
		Rectangle bottomRect() {
			int bottomY = gapTop + gap;
			int bottomH = flappyStep1.HEIGHT - flappyStep1.GROUND_HEIGHT - bottomY;
			if(bottomH < 0) {
				bottomH = 0;
			}
			return new Rectangle((int)x, bottomY, width, bottomH);
		}
		
		void draw(Graphics2D g2) {
			g2.setColor(Color.green.darker());
			Rectangle t = topRect();
			Rectangle b = bottomRect();
			g2.fillRect(t.x, t.y, t.width, t.height);
			g2.fillRect(b.x, b.y, b.width, b.height);
		}
			
		
	}
	
	
	
	public flappyStep1() {
		
		// JPanel attributes (size, background, focusability)
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(new Color(0x70C5CE));
		setFocusable(true);
		
		//Key Binding block
		InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = getActionMap();
		
		im.put(KeyStroke.getKeyStroke("pressed SPACE"), "flap");
		am.put("flap", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				if(gameOver) {
					resetGame();
					return;
				}
				if(!started) {
					started = true;
				}
				birdVy = flapVelocity;
				System.out.println("FLAP! Vy = " + birdVy);
					}
				
		});
		
		
		try (var in = getClass().getResourceAsStream("bird.png")){
			birdImage = ImageIO.read(in);
			System.out.println("Loaded bird.png from same package" + birdImage);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		
		// FPS loop
		timer = new Timer(1000 / 60, this);
		lastNs = System.nanoTime();
		timer.start();
		
	}
	
		// Swing timer callback
	@Override public void actionPerformed(ActionEvent e) {
		long now = System.nanoTime();
		double dt = (now - lastNs) / 1_000_000_000.0; // seconds
		lastNs = now;
		update(dt);
		repaint();
	}
	
	
		//Advance game state
	private void update(double dt) {
		if(!started || gameOver) {
			return;
		}
		
		birdVy += GRAVITY * dt;
		birdY += birdVy * dt;
		
		if(birdY < 0) {
			birdY = 0;
			birdVy = 0;
			gameOver = true;
			return;
		}
		
		double floorY = HEIGHT - GROUND_HEIGHT - birdRadius();
		
		if(birdY > floorY) {
			birdY = floorY;
			birdVy = 0;
			gameOver = true;
			return;
		}
		
		//Pipe spawning
		timeSinceLastPipe += dt;
		if(timeSinceLastPipe >= pipeInterval) {
			timeSinceLastPipe = 0;
			spawnPipe();
		}
		
		//Move Pipes left
		java.util.Iterator<PipePair> it = pipes.iterator();
		while(it.hasNext()) {
			PipePair p = it.next();
			p.x -= pipeSpeed * dt;
			
			if(!p.scored && p.x + p.width < birdX) {
				p.scored = true;
				score++;
			}
			
			if(p.isOffscreen()) {
				it.remove();
			}	
		}
		
		//Colision vs pipe
		if(collidesWithAnyPipe()) {
			gameOver = true;
		}
		
	}
	
	private boolean collidesWithAnyPipe() {
		int r = birdRadius();
		double cx = birdX, cy = birdY;
		
		for(PipePair p: pipes) {
			Rectangle t = p.topRect();
			if(circleRectIntersect(cx, cy, r, t.x, t.y, t.width, t.height)) {
				return true;
			}
			Rectangle b = p.bottomRect();
			if(circleRectIntersect(cx, cy, r, b.x, b.y, b.width, b.height)) {
				return true;
			}
		}
		
		return false;
	}
	
	
		private boolean circleRectIntersect(double cx, double cy, double radius, int rx, int ry, int rw, int rh) {
			double nearestX = Math.max(rx, Math.min(cx, rx + rw));
			double nearestY = Math.max(ry, Math.min(cy, ry + rh));
			double dx = cx - nearestX;
			double dy = cy - nearestY;
			
			return dx*dx + dy*dy <= radius*radius;
		}
		
	
	private void spawnPipe() {
		int minGapTop = 80;
		int maxGapTop = HEIGHT - GROUND_HEIGHT - 80 - pipeGap;
		int gapTop = minGapTop - rng.nextInt(1, maxGapTop - minGapTop +1);
		
		pipes.add(new PipePair(WIDTH, gapTop, pipeWidth, pipeGap));
	}
	
	
		//Draw everything
	@Override protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		
		//Sky gradient
		Paint old = g2.getPaint();
		g2.setPaint(new GradientPaint(0,0, new Color(0x70C5CE),0,HEIGHT, new Color(0xBDE7F2)));
		g2.fillRect(0,0, WIDTH, HEIGHT);
		g2.setPaint(old);
		
		//Ground
		g2.setColor(new Color(0xDEB887));
		g2.fillRect(0,HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
		
		//Bird
		int drawW = birdSize;
		int drawH = birdSize;
		
		
		
		int drawX = (int)Math.round(birdX) - drawW / 2;
		int drawY = (int)Math.round(birdY) - drawH / 2;
		
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		if(birdImage != null) {
			g2.drawImage(birdImage, drawX, drawY, drawW, drawH, null);
		} else {
		g2.setColor(Color.yellow);
		g2.fillOval(drawX,drawY, drawW, drawH);
		g2.setStroke(new BasicStroke(2f));
		g2.setColor(Color.black);
		g2.drawOval(drawX, drawY, drawW, drawH);
		}
		
		
		//Draw Pipes
		for(PipePair p : pipes) {
			p.draw(g2);
		}
		
		//score (top-left)
		g2.setColor(Color.BLACK);
		g2.setFont(new Font("Consolas", Font.BOLD, 32));
		g2.drawString(Integer.toString(score), 20, 50);
		
		
		//Prompt & Game Over
		g2.setFont(new Font("Consolas", Font.BOLD, 18));
		if(!started && !gameOver) {
			g2.drawString("Press SPACE to Start", 90, HEIGHT / 2);
		} else if(gameOver) {
			g2.drawString("Game Over! Press SPACE to restart", 50, HEIGHT / 2);
		}
		
		//UI Text
		/*g2.setColor(Color.BLACK);
		g2.setFont(new Font("consolas", Font.BOLD, 18));
		String msg = started ? "Press SPACE to flap" : "Press SPACE to start";
		g2.drawString(msg, 90, HEIGHT / 2); */
		
		g2.dispose(); 
	}
	
	private int birdRadius() {
		return birdSize / 2 - 2;
	}
	
	private void resetGame() {
		//reset bird
		birdX = 100.0;
		birdY = HEIGHT / 2;
		birdVy = 0.0;
		
		//reset pipes & score
		pipes.clear();
		timeSinceLastPipe = 0.0;
		score = 0;
		
		//state flags
		started = false;
		gameOver = false;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame("Flappy Cirlcle");
			flappyStep1 panel = new flappyStep1();
			f.setContentPane(panel);
			f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			f.pack();
			f.setLocationRelativeTo(null);
			f.setResizable(false);
			f.setVisible(true);
		});
	}

}
