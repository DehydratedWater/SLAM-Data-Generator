package controllers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;

public class Server 
{
	private ServerSocket socketServer;
	private Socket socket;
	private dataExchanger data;
	public int actualPort;
	private OutputStreamWriter out;
	private boolean isRunning;
	protected boolean clientErr;
	


	public Server(dataExchanger data)
	{
		this.data = data;
	}
	
	public void startServer(int port) throws IOException
	{

		stopServer();
		socketServer = new ServerSocket(port);
		actualPort = port;
		System.out.println("Waiting for connection..");
		socket = socketServer.accept();
		
		out = new OutputStreamWriter(socket.getOutputStream());
		isRunning = true;
		System.out.println("Connected");
	}
	
	public void sendData()
	{
		
		if(isRunning)
		{
//			System.out.println("Sending data");
		
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					try {
					
						String message = "";
						for(double d : data.getData())
							message+=d+"|";
						message+="\n";
						out.write(message);
//						System.out.print(message);
						out.flush();
					} catch (IOException e) {
						stopServer();
						clientErr = true;
						System.out.println("Client has been closed");
					}
				}
			});
				
				
			
		}
		
		
	}
	
	public void stopServer()
	{
		if(!isRunning)
			return;
		isRunning = false;
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(socketServer!=null||!socketServer.isClosed())
					try {
						socketServer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
			}
		});
		
	}
	
	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
}
