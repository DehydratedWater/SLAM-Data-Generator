package controllers;



import java.io.IOException;

import application.Cont;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SampleController 
{
	private static final float draw_size = 10;
	private WritableImage drawnMap;
	private long time = 0;
	private float posX, posY;
	private float angle, rx = 0, ry = 1;
	private float lastX, lastY;
	private float speed = 2, angle_speed = 4;
	private float d = 20;
	private float prec = 1f;
	private double[] measurment;
	private long lastTime, tlast2;
	private float waitTime = 0;
	SnapshotParameters params;
	private Color trackColor = new Color(0.3, 0.3, 1, 0.2);
	private boolean drawCollisions, drawDots, drawRays;
	private int encoderLeft = 0, encoderRight = 0;
	
	
	
	private GraphicsContext gc, rangeContext;
	private Canvas drawCanvas;
	private GraphicsContext drawContext, trackContext;
	@FXML private Canvas canvas, rangeCanvas, trackCanvas;
	@FXML private BorderPane panel;
	@FXML private Slider range;
	@FXML Slider speedSlider, rotSpeedSlider, posXSlider, posYSlider, rotSlider, weelSlider, weelSizeSlider, encoderSlider;
	@FXML CheckBox showTrack, showRays, showDots, showCollisions;
	@FXML TextField leftEncoder, rightEncoder, diffrenceEncoder, precision;
	@FXML Button serverButton;
	@FXML TextField portAddres;
	private double encoderPrecision = 120;
	private double weelSize = 12;
	private Server server;
	
	@FXML public void initialize()
	{
		System.out.println("Inicjalizacja "+trackCanvas);
		gc = canvas.getGraphicsContext2D();
		clearCanvas();
		
		drawCanvas = new Canvas(400, 400);
		drawnMap = new WritableImage(400, 400);
		drawContext = drawCanvas.getGraphicsContext2D();
		trackContext = trackCanvas.getGraphicsContext2D();
		rangeContext = rangeCanvas.getGraphicsContext2D();
		
		posXSlider.setOnMouseDragged(e->resetTrack());
		posYSlider.setOnMouseDragged(e->resetTrack());
		
		params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);
		
		rangeContext.setFill(Color.WHITE);
		rangeContext.fillRect(0, 0, 360, 120);
		rangeContext.strokeLine(0, 100, 360, 100);
		rangeContext.setFill(Color.BLACK);
		
		for(int i = 40; i < 360; i+=40)
		{
			rangeContext.strokeLine(i, 100, i, 105);
			rangeContext.fillText(i+"", i-10, 117);
		}
			
		
		trackCanvas.setOnMouseDragged((e)->{draw(e);});
		trackCanvas.setOnMouseReleased(e->{drawCanvas.snapshot(new SnapshotParameters(), drawnMap);});
		trackCanvas.setOnMouseClicked(e->{draw(e);});
		
		measurment = new double[(int) (360/prec)];
		
		lastTime = System.currentTimeMillis();
		
		server = new Server(()->
		{
			double[] message = new double[] {time, posX, posY, angle, encoderLeft, encoderRight};
			double[] res = new double[message.length+measurment.length];
			int i = 0;
			for(double d : message)
				res[i++] = d;
			for(double d : measurment)
				res[i++] = d;
			return res;
		});
		
		AnimationTimer anim = new AnimationTimer() {
			@Override
			public void handle(long now) {
				tlast2 = System.currentTimeMillis();
				
				getControls();
				drawMap();
				collision(move());
				checkLidarCollision((int) range.getValue());
				drawUpper();
				drawTrack();
				drawRangePlot();
				setControls();
				server();

				time = (System.currentTimeMillis()-lastTime);
				waitTime=System.currentTimeMillis()-tlast2;
				int fps = (int)(1000.0/waitTime);
				if(fps>60)
					fps = 60;
				gc.setFont(new Font(18));
				gc.fillText(fps+"FPS", 340, 20);
				
			}
			
			private void server() {
				server.sendData();
				if(server.clientErr)
				{
					server.clientErr = false;
					serverButton.setText("Start server");
				}
				
			}

			@Override
			protected void finalize() throws Throwable {
				server.stopServer();
				super.finalize();
			}
		};
		anim.start();
	}
	
	
	public void shutdown()
	{
		server.stopServer();
	}
	private void drawMap()
	{
		clearCanvas();
		gc.drawImage(drawnMap, 0, 0);
	}
	
	public void drawRangePlot()
	{
		rangeContext.setFill(Color.WHITE);
		rangeContext.fillRect(0, 0, 360, 100);
		
		rangeContext.setStroke(Color.BLACK);
		for(int i = 0; i < 359; i++)
		{
			
			rangeContext.strokeLine(i, 100-(measurment[(int) (i/prec)]/range.getValue())*100, i+1, 100-(measurment[(int) ((i+1)/prec)]/range.getValue())*100);
		}
	}
	
	private void getControls()
	{
		posX = (float) posXSlider.getValue();
		posY = (float) posYSlider.getValue();
		angle = (float) rotSlider.getValue();
		speed = (float) speedSlider.getValue();
		angle_speed = (float) rotSpeedSlider.getValue();
		d = (float) weelSlider.getValue();
		drawDots = showDots.isSelected();
		drawRays = showRays.isSelected();
		drawCollisions = showCollisions.isSelected();
		encoderPrecision = (float) encoderSlider.getValue();
		weelSize = (float) weelSizeSlider.getValue();
	}
	
	private void setControls()
	{
		posXSlider.setValue(posX);
		posYSlider.setValue(posY);
		if(angle<0)
			angle = 360+angle;
		else if(angle>=360)
			angle = angle%360;
//		System.out.println(angle);
		rotSlider.setValue(angle);
		leftEncoder.setText(""+encoderLeft);
		rightEncoder.setText(""+encoderRight);
		diffrenceEncoder.setText(""+(encoderLeft-encoderRight));
	}
	
	private void drawTrack()
	{
		if(showTrack.isSelected())
			trackCanvas.setOpacity(1);
		else
			trackCanvas.setOpacity(0);
		trackContext.setStroke(trackColor);
		trackContext.strokeLine(lastX, lastY, posX, posY);
	}
	
	private void drawUpper()
	{
		gc.setFill(Color.RED);
		gc.fillText("Time: "+(time/1000.0)+"s", 10, 20);
		gc.setFill(Color.LIGHTGREEN);
		gc.fillOval(posX-2, posY-2, 5, 5);
		gc.setStroke(Color.DARKGREEN);
		gc.strokeLine(posX, posY, posX+rx*10, posY+ry*10);
	}
	
	private float[] move()
	{
//		System.out.println("Poruszanie");
		lastX = posX;
		lastY = posY;
		
		boolean move = Cont.up||Cont.down;
		float speed_salar = speed;
		float[] xy = new float[2];
		if(!move)
		{
			if(Cont.left)
				angle-=2*angle_speed;
			if(Cont.right)
				angle+=2*angle_speed;
			if(Cont.right||Cont.left)
			{
				encoderLeft+=(speed/weelSize)*encoderPrecision;
				encoderRight+=(speed/weelSize)*encoderPrecision;
			}
		}
		
		else
		{
		if(Cont.left)
			angle-=angle_speed;
		if(Cont.right)
			angle+=angle_speed;
		if(Cont.right||Cont.left&&!(Cont.right&&Cont.left))
		{
			speed_salar = (float) ((speed+(speed-(2*Math.PI*d*angle_speed)/360.0))/2);
			if(Cont.left)
			{
				encoderLeft+=((speed-(2*Math.PI*d*angle_speed)/360.0)/weelSize)*encoderPrecision;
				encoderRight+=(speed/weelSize)*encoderPrecision;
			}
			if(Cont.right)
			{
				encoderLeft+=(speed/weelSize)*encoderPrecision;
				encoderRight+=((speed-(2*Math.PI*d*angle_speed)/360.0)/weelSize)*encoderPrecision;
			}
		}
		else
		{
			encoderLeft+=(speed/weelSize)*encoderPrecision;
			encoderRight+=(speed/weelSize)*encoderPrecision;
		}
		}
		
		float[] xy2 = rotate(0, -1, angle);
		rx = xy2[0];
		ry = xy2[1];
		if(Cont.up)
		{	
			xy[0]+=speed_salar*rx;
			xy[1]+=speed_salar*ry;
		}
		if(Cont.down)
		{	
			xy[0]-=speed_salar*rx;
			xy[1]-=speed_salar*ry;
		}
		
		
		posX+=xy[0];
		posY+=xy[1];

		if(posX<0)
			posX = 0;
		if(posY<0)
			posY = 0;
		if(posX>=400)
			posX = 399;
		if(posY>=400)
			posY = 399;
//		System.out.println(encoderLeft+" "+encoderRight);
//		System.out.println("Speed "+speed_salar);
		return xy;
		
	}
	
	
	private void collision(float[] xy)
	{
		int count = 0;
		while(count++<5&&checkCollision(xy));

		if(count==5)
		{
			posX-=xy[0];
			posY-=xy[1];
		}
	}

	private boolean checkCollision(float[] xy) {
//		System.out.println("Kolizje");
		float dx = 0;
		float dy = 0;
		int count = 0;
		for(float i = (posX-5); i <= posX+5; i++)
		{
			for(float j = (posY-5); j <= posY+5; j++)
			{
				if(!(i>=0&&j>=0&&i<400&&j<400))
					continue;

				if(Math.sqrt(Math.pow((i-posX), 2)+Math.pow((j-posY), 2))<=2.5)
				{
					if((drawnMap.getPixelReader().getArgb((int)i, (int)j)==java.awt.Color.BLACK.getRGB()))
					{
						if(drawCollisions)
						{
							gc.setFill(Color.YELLOW);
							gc.fillOval(i-2, j-2, 4, 4);
						}
						dx += i;
						dy += j;
						count++;
					}
				}
			}
		}
		if(count>0)
		{
			posX+=posX-dx/count;
			posY+=posY-dy/count;
			if(posX<0)
				posX = 0;
			if(posY<0)
				posY = 0;
			if(posX>=400)
				posX = 399;
			if(posY>=400)
				posY = 399;
			return true;
		}
		return false;
	}
	
	private float[] rotate(float x, float y, float angle)
	{
		angle = (float) Math.toRadians(angle);
		float[] xy = new float[2];
		xy[0] = (float) (x*Math.cos(angle)-y*Math.sin(angle));
		xy[1] = (float) (x*Math.sin(angle)+y*Math.cos(angle));
		return xy;
	}
	
	private void checkLidarCollision(int range)
	{
		for(int i = 0; i < 360/prec; i++)
		{
			measurment[i] = getDyst(prec*i, range);
		}
	}
	
	private float getDyst(float angle, int range)
	{
//		System.out.println(angle);
		float[] xy = rotate(rx, ry, angle);
		float x = posX, y = posY;
		
		boolean drawLine = false;
		
		for(int i = 0; i < range; i++)
		{
			x+=xy[0];
			y+=xy[1];
			
			if(!(x>=0&&y>=0&&x<400&&y<400))
			{
				if(drawRays)
				{
					gc.setStroke(Color.GRAY);
					gc.strokeLine(x, y, posX, posY);
				}
				if(drawDots)
				{
					gc.setFill(Color.RED);
					gc.fillOval(x-2, y-2, 4, 4);
				}
				drawLine = true;
				break;
			}
			if((drawnMap.getPixelReader().getArgb((int)x, (int)y)==java.awt.Color.BLACK.getRGB()))
			{
				if(drawRays)
				{
					gc.setStroke(Color.GRAY);
					gc.strokeLine(x, y, posX, posY);
				}
				if(drawDots)
				{
					gc.setFill(Color.RED);
					gc.fillOval(x-1, y-1, 2, 2);
				}
				drawLine = true;
				break;
			}
			
		}
		if(!drawLine)
		{
			if(drawRays)
			{
				gc.setStroke(Color.DARKGRAY);
				gc.strokeLine(x, y, posX, posY);
			}
			if(drawDots)
			{
				gc.setFill(Color.RED);
				gc.fillOval(x-1, y-1, 2, 2);
			}
		}
		return (float) Math.sqrt(Math.pow(x-posX, 2)+Math.pow(y-posY, 2));
			
	}
	
	
	private void draw(MouseEvent e)
	{
		if(e.getButton() == MouseButton.PRIMARY)
		{
			drawContext.setFill(Color.BLACK);
			drawContext.fillOval(e.getX()-draw_size/2, e.getY()-draw_size/2, draw_size, draw_size);
		}
		if(e.getButton() == MouseButton.SECONDARY)
		{
			drawContext.setFill(Color.WHITE);
			drawContext.fillOval(e.getX()-draw_size/2, e.getY()-draw_size/2, draw_size, draw_size);
		}
		if(e.getButton() == MouseButton.MIDDLE)
		{
			posX = (float) e.getX();
			posY = (float) e.getY();
			lastX = posX;
			lastY = posY;
			setControls();
			trackContext.clearRect(0, 0, 400, 400);
		}
		drawCanvas.snapshot(new SnapshotParameters(), drawnMap);
	}
	
	private void clearCanvas()
	{
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, 400, 400);
	}
	
	@FXML public void resetTrack()
	{
		lastX = posX;
		lastY = posY;
		trackContext.clearRect(0, 0, 400, 400);
	}
	
	@FXML public void resetMap()
	{
		drawContext.setFill(Color.WHITE);
		drawContext.fillRect(0, 0, 400, 400);
		drawCanvas.snapshot(new SnapshotParameters(), drawnMap);
	}
	@FXML public void resetTime()
	{
		lastTime = System.currentTimeMillis();
	}
	@FXML public void resetEncoder()
	{
		encoderLeft = 0;
		encoderRight = 0;
	}

	@FXML public void setPrecision()
	{
		float prec2 = Float.parseFloat(precision.getText());
		prec2 = 360/prec2;
		if(((int)(360f/prec2))*prec2==360)
			prec = prec2;
		precision.setText((360/prec)+"");
		measurment = new double[(int) (360f/prec)];
	}
	
	@FXML public void serverAction()
	{
		if(!server.isRunning())
		{
			System.out.println("Starting server");
			serverButton.setDisable(true);
			Task<Void> task = new Task<Void>() {
				
				@Override
				protected Void call() throws Exception {
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							serverButton.setDisable(true);
							serverButton.setText("Waiting for connection..");
						}
					});
					
					
					try {
						server.startServer(Integer.parseInt(portAddres.getText()));
					} catch (NumberFormatException e) {
						portAddres.setText("Wrong port..");
						e.printStackTrace();
					} catch (IOException e) {
						portAddres.setText("err..");
						e.printStackTrace();
					}
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							serverButton.setDisable(false);
							serverButton.setText("Stop server");
						}
					});
					return null;
				}
			};
			
		
		new Thread(task).start();
		
		}
		else
		{
			System.out.println("Stoping server");
			server.stopServer();
			serverButton.setText("Start server");
			
		}
	}
}
